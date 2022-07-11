/*
 * Copyright (c) 2021 Windscribe Limited.
 */

package com.windscribe.vpn.backend.utils

import com.google.gson.Gson
import com.windscribe.vpn.ServiceInteractor
import com.windscribe.vpn.Windscribe.Companion.appContext
import com.windscribe.vpn.api.response.PortMapResponse
import com.windscribe.vpn.backend.VPNState
import com.windscribe.vpn.backend.VPNState.Status
import com.windscribe.vpn.backend.utils.ProtocolConfig.Type.Auto
import com.windscribe.vpn.backend.utils.ProtocolConfig.Type.Manual
import com.windscribe.vpn.backend.utils.ProtocolConfig.Type.Preferred
import com.windscribe.vpn.constants.PreferencesKeyConstants
import com.windscribe.vpn.localdatabase.tables.NetworkInfo
import com.windscribe.vpn.state.NetworkInfoListener
import com.windscribe.vpn.state.NetworkInfoManager
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.slf4j.LoggerFactory

@Singleton
class ProtocolManager(
        val networkInfoManager: NetworkInfoManager,
        val scope: CoroutineScope,
        val serviceInteractor: ServiceInteractor
) :
        NetworkInfoListener {

    private val protocolListEvents = MutableStateFlow(ProtocolConfigList())
    val protocolConfigList: StateFlow<ProtocolConfigList> = protocolListEvents
    private var preferredProtocol: ProtocolConfig? = null
    private var manualProtocol: ProtocolConfig? = null
    var reconnectJob: Job? = null
    private var failedAttempts = 0
    private var logger = LoggerFactory.getLogger("protocol_manager")

    private val availableProtocolsEvents = MutableStateFlow(emptyList<ProtocolConfig>())
    var availableProtocols: StateFlow<List<ProtocolConfig>> = availableProtocolsEvents

    init {
        loadProtocolConfigs()
        networkInfoManager.addNetworkInfoListener(this)
    }

    private fun getFirstProtocolConfig(): ProtocolConfig? {
        return availableProtocols.value.firstOrNull()
    }

    fun setSelectedProtocolConfig(protocolConfig: ProtocolConfig) {
        logger.debug("Emitting Selected protocol being used to connect: $protocolConfig")
        scope.launch {
            val list = ProtocolConfigList()
            serviceInteractor.preferenceHelper.selectedProtocol = protocolConfig.protocol
            serviceInteractor.preferenceHelper.selectedPort = protocolConfig.port
            serviceInteractor.preferenceHelper.selectedProtocolType = protocolConfig.type
            list.selectedProtocol = protocolConfig
            list.nextProtocolToConnect = protocolConfigList.value.nextProtocolToConnect
            protocolListEvents.emit(list)
        }
    }

    fun setNextProtocolConfig(protocolConfig: ProtocolConfig) {
        reconnectJob?.cancel()
        scope.launch {
            val list = ProtocolConfigList()
            list.nextProtocolToConnect = protocolConfig
            list.selectedProtocol = protocolConfigList.value.selectedProtocol
            protocolListEvents.emit(list)
        }
    }

    fun protocolFailed() {
        reconnectJob?.cancel()
        failedAttempts += 1
        reconnectJob = scope.launch {
            val reducedList = availableProtocols.value.drop(failedAttempts)
            logger.debug("Protocol Failed: ${reducedList.size}")
            availableProtocolsEvents.emit(reducedList)
            availableProtocols.value.firstOrNull()?.let { config ->
                logger.debug("Waiting for protocol switch.")
                appContext.vpnConnectionStateManager.setState(VPNState(Status.ProtocolSwitch))
                if (appContext.applicationInterface.isTV) {
                    delay(3000)
                } else {
                    delay(10000)
                }
                if (serviceInteractor.preferenceHelper.globalUserConnectionPreference && serviceInteractor.preferenceHelper.isConnectingToConfiguredLocation().not()) {
                    logger.debug("Next protocol: $config")
                    setNextProtocolConfig(config)
                    appContext.vpnController.connect()
                } else {
                    disconnect()
                }
            } ?: kotlin.run {
                disconnect()
            }
        }
        reconnectJob?.start()
    }

    fun disconnect() {
        scope.launch {
            serviceInteractor.preferenceHelper.globalUserConnectionPreference = false
            serviceInteractor.preferenceHelper.isReconnecting = false
            reconnectJob?.cancel()
            loadProtocolConfigs()
            appContext.vpnController.disconnect()
            getFirstProtocolConfig()?.let {
                protocolConfigList.value.nextProtocolToConnect = it
            }
        }
    }

    fun onConnectionSuccessful() {
        failedAttempts = 0
        logger.debug("Connection successful")
    }

    fun loadProtocolConfigs() {
        failedAttempts = 0
        logger.debug("Loading protocol list.")
        // Set Manual Protocol
        val connectionMode =
                serviceInteractor.preferenceHelper.getResponseString(PreferencesKeyConstants.CONNECTION_MODE_KEY)
        manualProtocol = if (connectionMode == PreferencesKeyConstants.CONNECTION_MODE_MANUAL) {
            getManualProtocolConfig()
        }else{
            null
        }
        // Set Auto Protocols
        val availableProtocolList = mutableListOf(
                ProtocolConfig(PreferencesKeyConstants.PROTO_IKev2, "500", Auto),
                ProtocolConfig(PreferencesKeyConstants.PROTO_UDP, "443", Auto),
                ProtocolConfig(PreferencesKeyConstants.PROTO_TCP, "443", Auto),
                ProtocolConfig(PreferencesKeyConstants.PROTO_STEALTH, "443", Auto),
                ProtocolConfig(PreferencesKeyConstants.PROTO_WIRE_GUARD, "443", Auto)
        )
        val portMapJson: String? = appContext.preference.getResponseString(PreferencesKeyConstants.PORT_MAP)
        if(portMapJson!=null){
            val portMapResponse = Gson().fromJson(portMapJson, PortMapResponse::class.java)
            if (portMapResponse.isProtocolSuggested){
                portMapResponse.suggested?.let { suggested ->
                    val suggestedProtocolConfig = ProtocolConfig(suggested.protocol, suggested.port.toString(), Auto)
                    val index = availableProtocolList.indexOfFirst { it.protocol == suggestedProtocolConfig.protocol }
                    if (index != -1) {
                        availableProtocolList.removeAt(index)
                        availableProtocolList.add(0, suggestedProtocolConfig)
                    }
                }
            }
        }
        // Move manual protocol to top
        manualProtocol?.let { manual ->
            availableProtocolList.clear()
            availableProtocolList.add(manual)
        }
        // Move preferred protocol to top
        preferredProtocol?.let { preferred ->
            if (manualProtocol == null) {
                val index = availableProtocolList.indexOfFirst { it.protocol == preferred.protocol }
                if (index != -1) {
                    availableProtocolList.removeAt(index)
                    availableProtocolList.add(0, preferred)
                }
            } else {
                availableProtocolList.add(0, preferred)
            }
        }
        scope.launch {
            availableProtocolsEvents.emit(availableProtocolList)
            getFirstProtocolConfig()?.let {
                setNextProtocolConfig(it)
            }
        }
    }

    override fun onNetworkInfoUpdate(networkInfo: NetworkInfo?, userReload: Boolean) {
        preferredProtocol = if (networkInfo != null && networkInfo.isPreferredOn) {
            ProtocolConfig(networkInfo.protocol, networkInfo.port, Preferred)
        } else {
            null
        }
        loadProtocolConfigs()
    }

    private fun getManualProtocolConfig(): ProtocolConfig {
        val protocol = serviceInteractor.preferenceHelper.savedProtocol
        val port: String = when (protocol) {
            PreferencesKeyConstants.PROTO_IKev2 -> serviceInteractor.preferenceHelper.iKEv2Port
            PreferencesKeyConstants.PROTO_UDP -> serviceInteractor.preferenceHelper.savedUDPPort
            PreferencesKeyConstants.PROTO_TCP -> serviceInteractor.preferenceHelper.savedTCPPort
            PreferencesKeyConstants.PROTO_STEALTH -> serviceInteractor.preferenceHelper.savedSTEALTHPort
            PreferencesKeyConstants.PROTO_WIRE_GUARD -> serviceInteractor.preferenceHelper.wireGuardPort
            else -> PreferencesKeyConstants.DEFAULT_IKEV2_PORT
        }
        return ProtocolConfig(protocol, port, Manual)
    }
}

class ProtocolConfigList {
    var selectedProtocol: ProtocolConfig? = null
    var nextProtocolToConnect: ProtocolConfig = ProtocolConfig(
            PreferencesKeyConstants.PROTO_IKev2,
            PreferencesKeyConstants.DEFAULT_IKEV2_PORT,
            Preferred
    )
}
