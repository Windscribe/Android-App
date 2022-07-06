/*
 * Copyright (c) 2021 Windscribe Limited.
 */

package com.windscribe.vpn.backend.utils

import android.content.Intent
import android.net.VpnService
import androidx.work.Data
import com.windscribe.vpn.BuildConfig
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
import com.windscribe.vpn.exceptions.WindScribeException
import com.windscribe.vpn.repository.CallResult
import com.windscribe.vpn.repository.LocationRepository
import com.windscribe.vpn.repository.UserRepository
import com.windscribe.vpn.repository.WgConfigRepository
import com.windscribe.vpn.serverlist.entity.Node
import com.windscribe.vpn.services.NetworkWhiteListService
import com.windscribe.vpn.state.VPNConnectionStateManager
import dagger.Lazy
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import org.slf4j.LoggerFactory

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

    private suspend fun createProfile(config: ProtocolConfig): String {
        return when (WindUtilities.getSourceTypeBlocking()) {
            CityLocation -> createVpnProfileFromCity(interactor.preferenceHelper.selectedCity, config)
            StaticIp -> createVpnProfileFromStaticIp(
                    interactor.preferenceHelper.selectedCity,
                    config
            )
            else -> createProfileFromConfig(interactor.preferenceHelper.selectedCity)
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

    private fun createProfileFromConfig(selectedCity: Int): String {
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
        val selectedNode:Node? = if(interactor.preferenceHelper.isDecoyTrafficOn && BuildConfig.DEV){
            cityAndRegion.city.nodes.firstOrNull {
                it.hostname == "us-013.whiskergalaxy.dev"
            }
        }else{
            cityAndRegion.city.getNodes()[WindUtilities.getRandomNode(nodeSize)]
        }
        if(selectedNode== null){
            throw WindScribeException("us-013.whiskergalaxy.dev is not available.")
        }
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
            PreferencesKeyConstants.PROTO_WIRE_GUARD -> {
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
        appContext.preference.saveCredentials(PreferencesKeyConstants.STATIC_IP_CREDENTIAL,staticRegion.credentials)
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
            PreferencesKeyConstants.PROTO_WIRE_GUARD -> {
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

    open fun connect() {
        connect(false)
    }

    open fun connect(alwaysOnVPN: Boolean = false) {
        scope.launch {
            try {
                if (alwaysOnVPN){
                    userRepository.get().synchronizedReload()
                }
                logger.debug("Connecting to VPN.")
                interactor.preferenceHelper.whitelistOverride = true
                vpnConnectionStateManager.setState(VPNState(Status.Connecting))
                val city = locationRepository.updateLocation()
                locationRepository.setSelectedCity(city)
                logger.debug("Selected location to connect: $city.")
                if (vpnBackendHolder.activeBackend != null) {
                    interactor.preferenceHelper.isReconnecting = true
                    disconnect(reconnecting = true)
                } else if (vpnConnectionStateManager.state.value.status == Status.UnsecuredNetwork) {
                    stopNetworkWhiteListService()
                }
                var config: ProtocolConfig = if (alwaysOnVPN) {
                    ProtocolConfig(
                            interactor.preferenceHelper.selectedProtocol,
                            interactor.preferenceHelper.selectedPort,
                            interactor.preferenceHelper.selectedProtocolType
                    )
                } else {
                    protocolManager.protocolConfigList.value.nextProtocolToConnect
                }
                if(interactor.preferenceHelper.isDecoyTrafficOn){
                    config = ProtocolConfig(PROTO_WIRE_GUARD, interactor.preferenceHelper.wireGuardPort, ProtocolConfig.Type.Manual)
                }
                logger.debug("$config.")
                protocolManager.setSelectedProtocolConfig(config)
                val profileToConnect = createProfile(config)
                logger.debug("Profile: $profileToConnect")
                logger.debug("Launching VPN Services.")
                launchVPNService()
            } catch (e: Exception) {
                scope.launch {
                    WindError.instance.rxErrorToString(e)
                    if (e is InvalidVPNConfigException) {
                        disconnect(error = e.error)
                    } else {
                        logger.debug("Error connecting to VPN: ${e.message}")
                        disconnect()
                    }
                }
            }
        }
    }

    suspend fun disconnect(waitForNextProtocol: Boolean = false, reconnecting: Boolean = false, error: CallResult.Error? = null) {
        logger.debug("Disconnecting from VPN: Waiting for next protocol: $waitForNextProtocol Reconnecting: $reconnecting")
        scope.launch {
            if (WindStunnelUtility.isStunnelRunning) {
                WindStunnelUtility.stopLocalTunFromAppContext(appContext)
            }
            if (vpnConnectionStateManager.state.value.status == Status.UnsecuredNetwork) {
                stopNetworkWhiteListService()
            }
            vpnBackendHolder.disconnect()
            if (!reconnecting) {
                interactor.preferenceHelper.whitelistOverride = false
                interactor.preferenceHelper.globalUserConnectionPreference = false
                try {
                    withTimeout(500) {
                        vpnConnectionStateManager.state.collectLatest {
                            if (vpnConnectionStateManager.isVPNActive()) {
                                cancel()
                            }
                        }
                    }
                } catch (e: Exception) {
                    logger.debug("Sending Disconnect event to UI")
                    vpnConnectionStateManager.setState(VPNState(Status.Disconnected))
                }
            }
        }.invokeOnCompletion {
            if(vpnBackendHolder.activeBackend == null){
                interactor.preferenceHelper.isReconnecting = false
            }
            logger.debug("VPN Disconnected.")
            if (waitForNextProtocol) {
                interactor.preferenceHelper.globalUserConnectionPreference = true
                vpnConnectionStateManager.setState(VPNState(Status.UnsecuredNetwork))
                NetworkWhiteListService.startService(appContext)
            }
            error?.let { item -> handleError(item) }
        }
    }

    private fun handleError(error: CallResult.Error) {
        logger.debug("code: ${error.code} error: ${error.errorMessage}")
        when (error.code) {
            ERROR_WG_KEY_LIMIT_EXCEEDED -> {
                showRetryDialog(error.errorMessage) {
                    vpnProfileCreator.wgForceInit.set(true)
                    connect()
                }
            }
            ERROR_UNABLE_TO_SELECT_WIRE_GUARD_IP, ERROR_WG_UNABLE_TO_GENERATE_PSK -> {
                showErrorDialog(error.errorMessage)
            }
            ERROR_WG_INVALID_PUBLIC_KEY -> {
                wgConfigRepository.deleteKeys()
                showErrorDialog(error.errorMessage)
            }
            EXPIRED_OR_BANNED_ACCOUNT , ERROR_VALID_CONFIG_NOT_FOUND -> {
                logger.debug("Forcing session update.")
                val data = Data.Builder().putBoolean("forceUpdate", true).build()
                appContext.workManager.updateSession(data)
            }
        }
    }

    private suspend fun stopNetworkWhiteListService() {
        NetworkWhiteListService.stopService(appContext)
        delay(100)
    }
}
