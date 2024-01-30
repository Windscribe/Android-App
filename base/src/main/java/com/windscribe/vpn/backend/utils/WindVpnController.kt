/*
 * Copyright (c) 2021 Windscribe Limited.
 */

package com.windscribe.vpn.backend.utils

import android.app.ActivityManager
import android.content.Context.ACTIVITY_SERVICE
import android.content.Intent
import android.net.VpnService
import androidx.work.Data
import com.windscribe.vpn.BuildConfig.DEV
import com.windscribe.vpn.R
import com.windscribe.vpn.ServiceInteractor
import com.windscribe.vpn.Windscribe.Companion.appContext
import com.windscribe.vpn.alert.showRetryDialog
import com.windscribe.vpn.autoconnection.AutoConnectionManager
import com.windscribe.vpn.autoconnection.ProtocolConnectionStatus
import com.windscribe.vpn.autoconnection.ProtocolInformation
import com.windscribe.vpn.backend.Util
import com.windscribe.vpn.backend.VPNState
import com.windscribe.vpn.backend.VPNState.Status.*
import com.windscribe.vpn.backend.VpnBackendHolder
import com.windscribe.vpn.backend.openvpn.WindStunnelUtility
import com.windscribe.vpn.backend.utils.SelectedLocationType.CityLocation
import com.windscribe.vpn.backend.utils.SelectedLocationType.StaticIp
import com.windscribe.vpn.commonutils.WindUtilities
import com.windscribe.vpn.constants.AdvanceParamKeys
import com.windscribe.vpn.constants.NetworkErrorCodes.ERROR_UNABLE_TO_REACH_API
import com.windscribe.vpn.constants.NetworkErrorCodes.ERROR_UNABLE_TO_SELECT_WIRE_GUARD_IP
import com.windscribe.vpn.constants.NetworkErrorCodes.ERROR_UNEXPECTED_API_DATA
import com.windscribe.vpn.constants.NetworkErrorCodes.ERROR_VALID_CONFIG_NOT_FOUND
import com.windscribe.vpn.constants.NetworkErrorCodes.ERROR_WG_INVALID_PUBLIC_KEY
import com.windscribe.vpn.constants.NetworkErrorCodes.ERROR_WG_KEY_LIMIT_EXCEEDED
import com.windscribe.vpn.constants.NetworkErrorCodes.ERROR_WG_UNABLE_TO_GENERATE_PSK
import com.windscribe.vpn.constants.NetworkErrorCodes.EXPIRED_OR_BANNED_ACCOUNT
import com.windscribe.vpn.constants.PreferencesKeyConstants
import com.windscribe.vpn.constants.PreferencesKeyConstants.PROTO_IKev2
import com.windscribe.vpn.constants.PreferencesKeyConstants.PROTO_WIRE_GUARD
import com.windscribe.vpn.errormodel.WindError
import com.windscribe.vpn.exceptions.InvalidVPNConfigException
import com.windscribe.vpn.exceptions.WindScribeException
import com.windscribe.vpn.repository.*
import com.windscribe.vpn.serverlist.entity.City
import com.windscribe.vpn.serverlist.entity.Node
import com.windscribe.vpn.services.NetworkWhiteListService
import com.windscribe.vpn.services.canAccessNetworkName
import com.windscribe.vpn.services.startAutoConnectService
import com.windscribe.vpn.state.VPNConnectionStateManager
import dagger.Lazy
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.takeWhile
import kotlinx.coroutines.launch
import org.slf4j.LoggerFactory
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.coroutineContext


@Singleton
open class WindVpnController @Inject constructor(
    val scope: CoroutineScope,
    private val interactor: ServiceInteractor,
    private val vpnProfileCreator: VPNProfileCreator,
    private val vpnConnectionStateManager: VPNConnectionStateManager,
    val vpnBackendHolder: VpnBackendHolder,
    private val locationRepository: LocationRepository,
    private val wgConfigRepository: WgConfigRepository,
    private val advanceParameterRepository: Lazy<AdvanceParameterRepository>,
    private val autoConnectionManager: AutoConnectionManager,
    private val emergencyConnectRepository: EmergencyConnectRepository

) {

    private val logger = LoggerFactory.getLogger("controller_v")
    private var isConnecting = false

    private suspend fun createVPNProfile(
        protocolInformation: ProtocolInformation, attempt: Int = 0
    ): String {
        return when (WindUtilities.getSourceTypeBlocking()) {
            CityLocation -> createVpnProfileFromCity(
                interactor.preferenceHelper.selectedCity, protocolInformation, attempt
            )
            StaticIp -> createVpnProfileFromStaticIp(
                interactor.preferenceHelper.selectedCity, protocolInformation
            )
            else -> createProfileFromCustomConfig(interactor.preferenceHelper.selectedCity)
        }
    }

    open suspend fun launchVPNService(
        protocolInformation: ProtocolInformation, connectionId: UUID
    ) {
        try {
            if (VpnService.prepare(appContext) == null) {
                logger.debug("VPN Permission available.")
                vpnBackendHolder.connect(protocolInformation, connectionId)
            } else {
                logger.debug("Requesting VPN Permission")
                val startIntent = Intent(appContext, VPNPermissionActivity::class.java)
                startIntent.putExtra("protocolInformation", protocolInformation)
                startIntent.putExtra("connectionId", connectionId)
                startIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                appContext.startActivity(startIntent)
            }
        } catch (e: Exception) {
            logger.error("Unexpected Error while checking permission for VPN", e)
            disconnectAsync()
        }
    }

    private fun createProfileFromCustomConfig(selectedCity: Int): String {
        val configFile = interactor.getConfigFile(selectedCity).blockingGet()
        val profile = vpnProfileCreator.createVpnProfileFromConfig(configFile)
        if (!configFile.isRemember) {
            configFile.username = null
            configFile.password = null
        }
        interactor.addConfigFile(configFile)
        autoConnectionManager.setSelectedProtocol(profile.second)
        return profile.first
    }

    private var lastUsedRandomIndex = 0

    private fun getForcedNodeIndex(city: City): Int {
        val forceNode = advanceParameterRepository.get().getForceNode()
        return if (forceNode != null && city.nodesAvailable()) {
            city.nodes.indexOfFirst { it.hostname == forceNode }
        } else {
            -1
        }
    }

    private suspend fun createVpnProfileFromCity(
        selectedCity: Int, config: ProtocolInformation, attempt: Int = 0
    ): String {
        val cityAndRegion = interactor.getCityAndRegionByID(selectedCity)
        val city = cityAndRegion.city
        val nodes = city.getNodes()
        var randomIndex = Util.getRandomNode(lastUsedRandomIndex, attempt, nodes)
        val forcedNodeIndex = getForcedNodeIndex(city)
        if (forcedNodeIndex != -1){
            logger.debug("Forcing node to ${nodes[forcedNodeIndex]}")
            randomIndex = forcedNodeIndex
        }
        val selectedNode: Node = nodes[randomIndex]
        logger.debug("$selectedNode")
        lastUsedRandomIndex = randomIndex
        val coordinatesArray = city.coordinates.split(",".toRegex()).toTypedArray()
        val ikev2Ip = selectedNode.ip
        val udpIp = selectedNode.ip2
        val tcpIp = selectedNode.ip2
        val stealthIp = selectedNode.ip3
        val hostname = selectedNode.hostname
        val publicKey = city.pubKey
        val location = LastSelectedLocation(
            city.id,
            city.nodeName,
            city.nickName,
            cityAndRegion.region.countryCode,
            coordinatesArray[0],
            coordinatesArray[1]
        )
        val vpnParameters =
            VPNParameters(ikev2Ip, udpIp, tcpIp, stealthIp, hostname, publicKey, city.ovpnX509)
        when (config.protocol) {
            PreferencesKeyConstants.PROTO_IKev2 -> {
                return vpnProfileCreator.createIkEV2Profile(
                    location, vpnParameters, config
                )
            }
            PROTO_WIRE_GUARD -> {
                return vpnProfileCreator.createVpnProfileFromWireGuardProfile(
                    location, vpnParameters, config
                )
            }
            else -> {
                return vpnProfileCreator.createOpenVpnProfile(
                    location, vpnParameters, config
                )
            }
        }
    }

    private suspend fun createVpnProfileFromStaticIp(
        staticId: Int, protocolInformation: ProtocolInformation
    ): String {
        val staticRegion = interactor.getStaticRegionByID(staticId).blockingGet()
        val node = staticRegion.staticIpNode
        appContext.preference.saveCredentials(
            PreferencesKeyConstants.STATIC_IP_CREDENTIAL, staticRegion.credentials
        )
        val location = LastSelectedLocation(
            staticRegion.id, staticRegion.cityName, staticRegion.staticIp, staticRegion.countryCode
        )
        val vpnParameters = VPNParameters(
            node.ip,
            node.ip,
            node.ip2,
            node.ip3,
            node.hostname,
            staticRegion.wgPubKey,
            staticRegion.ovpnX509
        )
        when (protocolInformation.protocol) {
            PreferencesKeyConstants.PROTO_IKev2 -> {
                return vpnProfileCreator.createIkEV2Profile(
                    location, vpnParameters, protocolInformation
                )
            }
            PROTO_WIRE_GUARD -> {
                return vpnProfileCreator.createVpnProfileFromWireGuardProfile(
                    location, vpnParameters, protocolInformation
                )
            }
            else -> {
                return vpnProfileCreator.createOpenVpnProfile(
                    location, vpnParameters, protocolInformation
                )
            }
        }
    }

    fun connectAsync() {
        scope.launch {
            connect()
        }
    }

    fun disconnectAsync(waitForNextProtocol: Boolean = false, reconnecting: Boolean = false) {
        scope.launch {
            disconnect(
                waitForNextProtocol,
                reconnecting,
                error = VPNState.Error(error = VPNState.ErrorType.UserDisconnect)
            )
        }
    }

    /**
     * Connects or reconnect to the VPN
     * @param alwaysOnVPN if vpn service was launched by system.
     */
    open suspend fun connect(
        connectionId: UUID = UUID.randomUUID(),
        protocolInformation: ProtocolInformation? = null,
        attempt: Int = 0
    ) {
        when {
            // Disconnect from VPN and connect to next selected location.
            vpnBackendHolder.activeBackend?.active == true -> {
                interactor.preferenceHelper.isReconnecting = true
                disconnect(
                    reconnecting = true,
                    error = VPNState.Error(error = VPNState.ErrorType.UserReconnect)
                )
                createProfileAndLaunchService(
                    connectionId, protocolInformation, attempt
                )
            }
            // Stop Network list service and connect
            vpnConnectionStateManager.state.value.status == UnsecuredNetwork -> {
                stopNetworkWhiteListService()
                createProfileAndLaunchService(
                    connectionId, protocolInformation, attempt
                )
            }
            else -> {
                // Make a fresh connection
                createProfileAndLaunchService(
                    connectionId, protocolInformation, attempt
                )
            }
        }
    }

    private suspend fun createProfileAndLaunchService(
        connectionId: UUID = UUID.randomUUID(),
        selectedProtocol: ProtocolInformation? = null,
        attempt: Int = 0
    ) {
        try {
            logger.debug("Connecting to VPN with connectionId: $connectionId")
            setLocationToConnect()
            vpnConnectionStateManager.setState(VPNState(Connecting, connectionId = connectionId))
            interactor.preferenceHelper.whitelistOverride = true
            val protocolInformation = selectedProtocol?.let {
                autoConnectionManager.setSelectedProtocol(it)
                return@let it
            } ?: getProtocolInformationToConnect()
            logger.debug("Protocol: $protocolInformation")
            val profileToConnect = createVPNProfile(protocolInformation, attempt)
            logger.debug("Location: $profileToConnect")
            logger.debug("Selecting VPN service to launch.")
            launchVPNService(protocolInformation, connectionId)
        } catch (e: Exception) {
            if (e is InvalidVPNConfigException) {
                handleVPNError(e.error, connectionId, selectedProtocol)
            } else {
                logger.debug(WindError.instance.rxErrorToString(e))
                disconnect()
            }
        }
    }

    private fun getProtocolInformationToConnect(): ProtocolInformation {
        // use default protocol if list protocol is not ready yet.
        if (autoConnectionManager.listOfProtocols.isEmpty()) {
            return ProtocolInformation(
                PROTO_IKev2,
                PreferencesKeyConstants.DEFAULT_IKEV2_PORT,
                "IKEv2 is an IPsec based tunneling protocol.",
                ProtocolConnectionStatus.Disconnected
            )
        }
        val config: ProtocolInformation =
            autoConnectionManager.listOfProtocols.firstOrNull { it.type == ProtocolConnectionStatus.NextUp }
                ?: autoConnectionManager.listOfProtocols.first()
        //Decoy traffic only works in Wireguard
        if (interactor.preferenceHelper.isDecoyTrafficOn) {
            Util.buildProtocolInformation(
                autoConnectionManager.listOfProtocols,
                PROTO_WIRE_GUARD,
                interactor.preferenceHelper.wireGuardPort
            )
        }
        autoConnectionManager.setSelectedProtocol(config)
        return config
    }

    private suspend fun setLocationToConnect() {
        val city = locationRepository.updateLocation()
        locationRepository.setSelectedCity(city)
    }

    /**
     * @param waitForNextProtocol Disconnects VPN and starts NetworkWhiteListService.
     * @param reconnecting only disconnecting to change location/protocol config.
     * */
    private suspend fun disconnect(
        waitForNextProtocol: Boolean = false,
        reconnecting: Boolean = false,
        error: VPNState.Error? = null
    ) {
        if (waitForNextProtocol && isServiceRunning(NetworkWhiteListService::class.java) && vpnConnectionStateManager.state.value.status == UnsecuredNetwork) {
            return
        }
        logger.debug("Disconnecting from VPN: Waiting for next protocol: $waitForNextProtocol Reconnecting: $reconnecting")
        if (WindStunnelUtility.isStunnelRunning) {
            WindStunnelUtility.stopLocalTunFromAppContext(appContext)
        }
        if (isServiceRunning(NetworkWhiteListService::class.java)) {
            stopNetworkWhiteListService()
        }
        vpnBackendHolder.disconnect(error)
        if (reconnecting.not()) {
            interactor.preferenceHelper.whitelistOverride = false
        }
        if (vpnConnectionStateManager.state.value.status != Disconnected) {
            // Force disconnect if state did not change to disconnect
            vpnConnectionStateManager.setState(VPNState(Disconnected, error = error), true)
            delay(100)
            logger.debug("Force disconnected")
        }
        checkForReconnect()
    }

    private fun checkForReconnect() {
        if (appContext.preference.autoConnect && appContext.canAccessNetworkName()) {
            appContext.preference.globalUserConnectionPreference = true
            appContext.startAutoConnectService()
        }
    }

    /**
     * @param serviceClass Service class name
     * @return return true if service is running
     */
    private fun isServiceRunning(serviceClass: Class<*>): Boolean {
        val manager: ActivityManager? =
            appContext.getSystemService(ACTIVITY_SERVICE) as ActivityManager?
        if (manager != null) {
            for (service in manager.getRunningServices(Int.MAX_VALUE)) {
                if (serviceClass.name == service.service.className) {
                    return true
                }
            }
        }
        return false
    }

    private suspend fun handleVPNError(
        error: CallResult.Error, connectionId: UUID, protocolInformation: ProtocolInformation?
    ) {
        logger.debug("code: ${error.code} error: ${error.errorMessage}")
        val context = coroutineContext
        when (error.code) {
            ERROR_UNABLE_TO_REACH_API, ERROR_UNEXPECTED_API_DATA -> {
                disconnect()
            }
            ERROR_WG_KEY_LIMIT_EXCEEDED -> {
                showRetryDialog(error.errorMessage, {
                    CoroutineScope(context).launch {
                        vpnProfileCreator.wgForceInit.set(true)
                        val vpnState = VPNState(
                            Disconnected, error = VPNState.Error(
                                error = VPNState.ErrorType.WireguardAuthenticationError,
                                "Wireguard wg key limit exceeded."
                            )
                        )
                        vpnState.protocolInformation = protocolInformation
                        vpnState.connectionId = connectionId
                        vpnConnectionStateManager.setState(vpnState)
                    }
                }, {
                    CoroutineScope(context).launch {
                        disconnect(
                            error = VPNState.Error(
                                error = VPNState.ErrorType.WireguardApiError,
                                "Wireguard key limited exceeded."
                            )
                        )
                    }
                })
            }
            ERROR_UNABLE_TO_SELECT_WIRE_GUARD_IP, ERROR_WG_UNABLE_TO_GENERATE_PSK -> {
                disconnect(
                    error = VPNState.Error(
                        error = VPNState.ErrorType.WireguardApiError,
                        error.errorMessage,
                        showError = true
                    )
                )
            }
            ERROR_WG_INVALID_PUBLIC_KEY -> {
                wgConfigRepository.deleteKeys()
                disconnect(
                    error = VPNState.Error(
                        error = VPNState.ErrorType.WireguardApiError,
                        error.errorMessage,
                        showError = true
                    )
                )
            }
            EXPIRED_OR_BANNED_ACCOUNT, ERROR_VALID_CONFIG_NOT_FOUND -> {
                logger.debug("Forcing session update.")
                val data = Data.Builder().putBoolean("forceUpdate", true).build()
                appContext.workManager.updateSession(data)
                disconnect(
                    error = VPNState.Error(
                        error = VPNState.ErrorType.WireguardApiError,
                        error.errorMessage,
                        showError = true
                    )
                )
            }
        }
    }

    private suspend fun stopNetworkWhiteListService() {
        NetworkWhiteListService.stopService(appContext)
        delay(100)
    }

    suspend fun connectUsingEmergencyProfile(callback: (String) -> Unit): Result<Unit> {
        return emergencyConnectRepository.getConnectionInfo().mapCatching { connectionInfo ->
            var connectionAttempt = 0
            vpnConnectionStateManager.state.takeWhile {
                    it.status != VPNState.Status.Connected
                }.collect { state ->
                    logger.debug("$connectionAttempt ${state.status}")
                    if (state.status == VPNState.Status.Disconnected && connectionAttempt < connectionInfo.size) {
                        callback(interactor.getResourceString(R.string.connecting))
                        vpnConnectionStateManager.setState(VPNState(status = VPNState.Status.Connecting))
                        val connectionUUID = UUID.randomUUID()
                        val openVPNInfo = connectionInfo[connectionAttempt]
                        vpnProfileCreator.createOpenVPNProfile(openVPNInfo)
                        val lastSelectedLocation = LastSelectedLocation(
                            -1,
                            nodeName = "Emergency",
                            nickName = "Windscribe location"
                        )
                        Util.saveSelectedLocation(lastSelectedLocation)
                        val protocolInformation = openVPNInfo.getProtocolInformation()
                        autoConnectionManager.setSelectedProtocol(protocolInformation)
                        launchVPNService(protocolInformation, connectionUUID)
                        connectionAttempt++
                    } else if (state.status != VPNState.Status.Connecting) {
                        if (connectionAttempt >= connectionInfo.size) {
                            throw WindScribeException("No more profiles left to connect.")
                        }
                    }
                }
        }
    }
}
