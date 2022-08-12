/*
 * Copyright (c) 2021 Windscribe Limited.
 */

package com.windscribe.vpn.backend.utils

import android.app.ActivityManager
import android.content.Context.ACTIVITY_SERVICE
import android.content.Intent
import android.net.VpnService
import androidx.work.Data
import com.windscribe.vpn.ServiceInteractor
import com.windscribe.vpn.Windscribe.Companion.appContext
import com.windscribe.vpn.alert.showErrorDialog
import com.windscribe.vpn.alert.showRetryDialog
import com.windscribe.vpn.backend.VPNState
import com.windscribe.vpn.backend.VPNState.Status
import com.windscribe.vpn.backend.VpnBackendHolder
import com.windscribe.vpn.backend.openvpn.WindStunnelUtility
import com.windscribe.vpn.backend.utils.SelectedLocationType.CityLocation
import com.windscribe.vpn.backend.utils.SelectedLocationType.StaticIp
import com.windscribe.vpn.commonutils.WindUtilities
import com.windscribe.vpn.constants.NetworkErrorCodes.ERROR_UNABLE_TO_SELECT_WIRE_GUARD_IP
import com.windscribe.vpn.constants.NetworkErrorCodes.ERROR_VALID_CONFIG_NOT_FOUND
import com.windscribe.vpn.constants.NetworkErrorCodes.ERROR_WG_INVALID_PUBLIC_KEY
import com.windscribe.vpn.constants.NetworkErrorCodes.ERROR_WG_KEY_LIMIT_EXCEEDED
import com.windscribe.vpn.constants.NetworkErrorCodes.ERROR_WG_UNABLE_TO_GENERATE_PSK
import com.windscribe.vpn.constants.NetworkErrorCodes.EXPIRED_OR_BANNED_ACCOUNT
import com.windscribe.vpn.constants.PreferencesKeyConstants
import com.windscribe.vpn.constants.PreferencesKeyConstants.PROTO_WIRE_GUARD
import com.windscribe.vpn.errormodel.WindError
import com.windscribe.vpn.exceptions.InvalidVPNConfigException
import com.windscribe.vpn.repository.CallResult
import com.windscribe.vpn.repository.LocationRepository
import com.windscribe.vpn.repository.UserRepository
import com.windscribe.vpn.repository.WgConfigRepository
import com.windscribe.vpn.serverlist.entity.Node
import com.windscribe.vpn.services.NetworkWhiteListService
import com.windscribe.vpn.state.VPNConnectionStateManager
import dagger.Lazy
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collectLatest
import org.slf4j.LoggerFactory
import javax.inject.Inject
import javax.inject.Singleton


@Singleton
open class WindVpnController @Inject constructor(
        val scope: CoroutineScope,
        private val interactor: ServiceInteractor,
        private val vpnProfileCreator: VPNProfileCreator,
        private val vpnConnectionStateManager: VPNConnectionStateManager,
        val vpnBackendHolder: VpnBackendHolder,
        private val locationRepository: LocationRepository,
        private val protocolManager: ProtocolManager,
        private val wgConfigRepository: WgConfigRepository,
        private val userRepository: Lazy<UserRepository>

) {

    private val logger = LoggerFactory.getLogger("vpn_backend")
    private var disconnectTask: Job? = null
    private var isConnecting = false

    private suspend fun createVPNProfile(config: ProtocolConfig): String {
        return when (WindUtilities.getSourceTypeBlocking()) {
            CityLocation -> createVpnProfileFromCity(interactor.preferenceHelper.selectedCity, config)
            StaticIp -> createVpnProfileFromStaticIp(
                    interactor.preferenceHelper.selectedCity,
                    config
            )
            else -> createProfileFromCustomConfig(interactor.preferenceHelper.selectedCity)
        }
    }

    open fun launchVPNService() {
        try {
            if (VpnService.prepare(appContext) == null) {
                logger.debug("VPN Permission available.")
                vpnBackendHolder.connect()
            } else {
                logger.debug("Requesting VPN Permission")
                val startIntent = Intent(appContext, VPNPermissionActivity::class.java)
                startIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                appContext.startActivity(startIntent)
            }
        } catch (e: Exception) {
            logger.error("Unexpected Error while checking permission for VPN", e)
            scope.launch { disconnect() }
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
        protocolManager.setSelectedProtocolConfig(profile.second)
        return profile.first
    }

    private suspend fun createVpnProfileFromCity(selectedCity: Int, config: ProtocolConfig): String {
        val cityAndRegion = interactor.getCityAndRegionByID(selectedCity)
        val city = cityAndRegion.city
        val nodeSize = city.getNodes().size
        val selectedNode: Node = cityAndRegion.city.getNodes()[WindUtilities.getRandomNode(nodeSize)]
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
                return vpnProfileCreator
                        .createIkEV2Profile(
                                location,
                                vpnParameters,
                                config
                        )
            }
            PROTO_WIRE_GUARD -> {
                return vpnProfileCreator
                        .createVpnProfileFromWireGuardProfile(
                                location,
                                vpnParameters,
                                config
                        )
            }
            else -> {
                return vpnProfileCreator.createOpenVpnProfile(
                        location,
                        vpnParameters,
                        config
                )
            }
        }
    }

    private suspend fun createVpnProfileFromStaticIp(staticId: Int, config: ProtocolConfig): String {
        val staticRegion = interactor.getStaticRegionByID(staticId).blockingGet()
        val node = staticRegion.staticIpNode
        appContext.preference.saveCredentials(PreferencesKeyConstants.STATIC_IP_CREDENTIAL, staticRegion.credentials)
        val location = LastSelectedLocation(
                staticRegion.id,
                staticRegion.cityName,
                staticRegion.staticIp,
                staticRegion.countryCode
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
        when (config.protocol) {
            PreferencesKeyConstants.PROTO_IKev2 -> {
                return vpnProfileCreator
                        .createIkEV2Profile(
                                location, vpnParameters, config
                        )
            }
            PROTO_WIRE_GUARD -> {
                return vpnProfileCreator
                        .createVpnProfileFromWireGuardProfile(
                                location, vpnParameters, config
                        )
            }
            else -> {
                return vpnProfileCreator
                        .createOpenVpnProfile(
                                location, vpnParameters, config
                        )
            }
        }
    }

    /**
     * Connects or reconnect to the VPN
     * @param alwaysOnVPN if vpn service was launched by system.
     */
    open fun connect(alwaysOnVPN: Boolean = false) {
        if (isConnecting) return
         isConnecting = true
        when {
            // Disconnect from VPN and connect to next selected location.
            vpnBackendHolder.activeBackend != null -> {
                interactor.preferenceHelper.isReconnecting = true
                disconnect(reconnecting = true).invokeOnCompletion {
                    createProfileAndLaunchService(alwaysOnVPN).invokeOnCompletion {
                        logger.debug("Connect job completed")
                    }
                }
            }
            // Stop Network list service and connect
            vpnConnectionStateManager.state.value.status == Status.UnsecuredNetwork -> {
                stopNetworkWhiteListService().invokeOnCompletion {
                    createProfileAndLaunchService(alwaysOnVPN)
                }
            }
            else -> {
                // Make a fresh connection
                createProfileAndLaunchService(alwaysOnVPN)
            }
        }
    }

    private fun createProfileAndLaunchService(alwaysOnVPN: Boolean = false): Job {
        return scope.launch {
            try {
                logger.debug("Connecting to VPN.")
                vpnConnectionStateManager.setState(VPNState(Status.Connecting))
                interactor.preferenceHelper.whitelistOverride = true
                setLocationToConnect(alwaysOnVPN)
                val protocolConfig = getProtocolConfigToConnect(alwaysOnVPN)
                logger.debug("$protocolConfig.")
                val profileToConnect = createVPNProfile(protocolConfig)
                logger.debug("Profile: $profileToConnect")
                logger.debug("Launching VPN Services.")
                launchVPNService()
                isConnecting = false
            } catch (e: Exception) {
                isConnecting = false
                scope.launch {
                    if (e is InvalidVPNConfigException) {
                        disconnect().invokeOnCompletion {
                            handleVPNError(e.error)
                        }
                    } else {
                        logger.debug(WindError.instance.rxErrorToString(e))
                        disconnect().invokeOnCompletion { }
                    }
                }
            }
        }
    }

    private fun getProtocolConfigToConnect(alwaysOnVPN: Boolean): ProtocolConfig {
        var config: ProtocolConfig = if (alwaysOnVPN) {
            ProtocolConfig(
                    interactor.preferenceHelper.selectedProtocol,
                    interactor.preferenceHelper.selectedPort,
                    interactor.preferenceHelper.selectedProtocolType
            )
        } else {
            protocolManager.protocolConfigList.value.nextProtocolToConnect
        }
        //Decoy traffic only works in Wireguard
        if (interactor.preferenceHelper.isDecoyTrafficOn) {
            config = ProtocolConfig(PROTO_WIRE_GUARD, interactor.preferenceHelper.wireGuardPort, ProtocolConfig.Type.Manual)
        }
        protocolManager.setSelectedProtocolConfig(config)
        return config
    }

    private suspend fun setLocationToConnect(alwaysOnVPN: Boolean) {
        if (alwaysOnVPN) {
            userRepository.get().synchronizedReload()
        }
        val city = locationRepository.updateLocation()
        locationRepository.setSelectedCity(city)
        logger.debug("Selected location ID to connect: $city.")
    }

    /**
     * @param waitForNextProtocol Disconnects VPN and starts NetworkWhiteListService.
     * @param reconnecting only disconnecting to change location/protocol config.
     * @return Job A cancellable Job
     * */
    fun disconnect(waitForNextProtocol: Boolean = false, reconnecting: Boolean = false): Job {
        if(disconnectTask?.isActive == true){
            return disconnectTask?.job ?: scope.launch {}
        }
        if(waitForNextProtocol && isServiceRunning(NetworkWhiteListService::class.java) && vpnConnectionStateManager.state.value.status == Status.UnsecuredNetwork){
            return scope.launch {}
        }
        logger.debug("Disconnecting from VPN: Waiting for next protocol: $waitForNextProtocol Reconnecting: $reconnecting")
        disconnectTask = scope.launch {
            if (WindStunnelUtility.isStunnelRunning) {
                WindStunnelUtility.stopLocalTunFromAppContext(appContext)
            }
            if (isServiceRunning(NetworkWhiteListService::class.java)) {
                stopNetworkWhiteListService()
            }
            vpnBackendHolder.disconnect()
            // Force disconnect if state did not change to disconnect
            if (reconnecting.not()) {
                interactor.preferenceHelper.whitelistOverride = false
                try {
                    withTimeout(500) {
                        vpnConnectionStateManager.state.collectLatest {
                            if (vpnConnectionStateManager.isVPNActive()) {
                                cancel()
                            }
                        }
                    }
                } catch (ignored: Exception){
                    logger.debug("Successfully disconnected")
                    checkForReconnect(waitForNextProtocol)
                }
            }
        }
        return disconnectTask ?: scope.launch {  }
    }

    private fun checkForReconnect(waitForNextProtocol: Boolean){
        if (waitForNextProtocol) {
            interactor.preferenceHelper.globalUserConnectionPreference = true
            vpnConnectionStateManager.setState(VPNState(Status.UnsecuredNetwork))
            NetworkWhiteListService.startService(appContext)
        } else {
            vpnConnectionStateManager.setState(VPNState(Status.Disconnected))
        }
    }

    /**
     * @param serviceClass Service class name
     * @return return true if service is running
     */
    private fun isServiceRunning(serviceClass: Class<*>): Boolean {
        val manager: ActivityManager? = appContext.getSystemService(ACTIVITY_SERVICE) as ActivityManager?
        if (manager != null) {
            for (service in manager.getRunningServices(Int.MAX_VALUE)) {
                if (serviceClass.name == service.service.className) {
                    return true
                }
            }
        }
        return false
    }

    private fun handleVPNError(error: CallResult.Error) {
        logger.debug("code: ${error.code} error: ${error.errorMessage}")
        when (error.code) {
            ERROR_WG_KEY_LIMIT_EXCEEDED -> {
                showRetryDialog(error.errorMessage, {
                    vpnProfileCreator.wgForceInit.set(true)
                    connect()
                }, {
                    disconnect()
                })
            }
            ERROR_UNABLE_TO_SELECT_WIRE_GUARD_IP, ERROR_WG_UNABLE_TO_GENERATE_PSK -> {
                showErrorDialog(error.errorMessage)
            }
            ERROR_WG_INVALID_PUBLIC_KEY -> {
                wgConfigRepository.deleteKeys()
                showErrorDialog(error.errorMessage)
            }
            EXPIRED_OR_BANNED_ACCOUNT, ERROR_VALID_CONFIG_NOT_FOUND -> {
                logger.debug("Forcing session update.")
                val data = Data.Builder().putBoolean("forceUpdate", true).build()
                appContext.workManager.updateSession(data)
            }
        }
    }

    private fun stopNetworkWhiteListService(): Job {
        return scope.launch {
            NetworkWhiteListService.stopService(appContext)
            delay(100)
        }
    }
}
