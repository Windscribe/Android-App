/*
 * Copyright (c) 2021 Windscribe Limited.
 */

package com.windscribe.vpn.backend.openvpn

import android.content.Intent
import android.os.Build
import com.windscribe.vpn.Windscribe
import com.windscribe.vpn.Windscribe.Companion.appContext
import com.windscribe.vpn.api.IApiCallManager
import com.windscribe.vpn.apppreference.PreferencesHelper
import com.windscribe.vpn.autoconnection.ProtocolInformation
import com.windscribe.vpn.backend.ProxyDNSManager
import com.windscribe.vpn.backend.VPNState
import com.windscribe.vpn.backend.VPNState.Status.Connecting
import com.windscribe.vpn.backend.VPNState.Status.Disconnected
import com.windscribe.vpn.backend.VpnBackend
import com.windscribe.vpn.backend.VpnBackend.Companion.DISCONNECT_DELAY
import com.windscribe.vpn.commonutils.ResourceHelper
import com.windscribe.vpn.localdatabase.LocalDbInterface
import com.windscribe.vpn.repository.AdvanceParameterRepository
import com.windscribe.vpn.state.NetworkInfoManager
import com.windscribe.vpn.state.VPNConnectionStateManager
import com.windscribe.vpn.wsnet.WSNetWrapper
import com.wireguard.android.backend.GoBackend
import de.blinkt.openvpn.core.ConnectionStatus
import de.blinkt.openvpn.core.OpenVPNService
import de.blinkt.openvpn.core.VpnStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.cancellable
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.slf4j.LoggerFactory
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class OpenVPNBackend @Inject constructor(
    var backend: GoBackend,
    var scope: CoroutineScope,
    var networkInfoManager: NetworkInfoManager,
    vpnStateManager: VPNConnectionStateManager,
    var preferencesHelper: PreferencesHelper,
    advanceParameterRepository: AdvanceParameterRepository,
    val proxyDNSManager: ProxyDNSManager,
    apiManager: IApiCallManager,
    localDbInterface: LocalDbInterface,
    wsNetWrapper: WSNetWrapper,
    resourceHelper: ResourceHelper
) : VpnBackend(scope, vpnStateManager, preferencesHelper, networkInfoManager, advanceParameterRepository, apiManager, localDbInterface, wsNetWrapper, resourceHelper),
    VpnStatus.StateListener, VpnStatus.ByteCountListener {

    override var active = false
    private var stickyDisconnectEvent = false
    private val openVPNLogger = LoggerFactory.getLogger("openvpn")
    var service: OpenVPNWrapperService? = null
    private var connectionStateJob: Job? = null
    private val openVpnTunnel = OpenVpnTunnel()
    private val logListener: VpnStatus.LogListener = VpnStatus.LogListener {
        if(it.logLevel == VpnStatus.LogLevel.INFO || it.logLevel == VpnStatus.LogLevel.ERROR) {
            openVPNLogger.debug(it.toString())
        }
    }

    fun serviceCreated(openVPNWrapperService: OpenVPNWrapperService) {
        vpnLogger.info("OpenVPN service created.")
        service = openVPNWrapperService
    }

    fun serviceDestroyed() {
        vpnLogger.info("OpenVPN service destroyed.")
        service = null
    }

    fun getTunnel() = openVpnTunnel

    override fun activate() {
        stickyDisconnectEvent = true
        vpnLogger.info("Activating OpenVPN backend.")

        connectionStateJob = scope.launch {
            openVpnTunnel.stateFlow.cancellable().collectLatest { state ->
                when (state) {
                    OpenVpnTunnel.State.DOWN -> {
                        if (!stickyDisconnectEvent && !reconnecting) {
                            connectionJob?.cancel()
                            updateState(VPNState(Disconnected))
                        }
                    }
                    OpenVpnTunnel.State.CONNECTING -> {
                        if (stateManager.state.value.status == VPNState.Status.Connected) {
                            return@collectLatest
                        }
                        stickyDisconnectEvent = false
                        updateState(VPNState(Connecting))
                    }
                    OpenVpnTunnel.State.CONNECTED -> {
                        if (stateManager.state.value.status == VPNState.Status.Connected) {
                            return@collectLatest
                        }
                        stickyDisconnectEvent = false
                        testConnectivity()
                    }
                    OpenVpnTunnel.State.DISCONNECTING -> {
                        updateState(VPNState(VPNState.Status.Disconnecting))
                    }
                }
            }
        }

        scope.launch {
            openVpnTunnel.errorFlow.cancellable().collectLatest { error ->
                vpnLogger.error("OpenVPN error: ${error.errorType} - ${error.message}")
                when (error.errorType) {
                    VPNState.ErrorType.AuthenticationError -> {
                        preferencesHelper.isReconnecting = false
                        disconnect(VPNState.Error(error.errorType, error.message))
                    }
                    else -> {
                        val status = error.vpnStatus ?: VPNState.Status.Disconnected
                        updateState(VPNState(status, VPNState.Error(error.errorType, error.message)))
                    }
                }
            }
        }

        VpnStatus.addStateListener(this)
        VpnStatus.addLogListener(logListener)
        VpnStatus.addByteCountListener(this)

        active = true
        startNetworkInfoObserver()
        vpnLogger.info("OpenVPN backend activated.")
    }

    override fun deactivate() {
        connectionStateJob?.cancel()
        VpnStatus.removeLogListener(logListener)
        VpnStatus.removeStateListener(this)
        VpnStatus.removeByteCountListener(this)
        stopNetworkInfoObserver()
        active = false
        vpnLogger.info("OpenVPN backend deactivated.")
    }

    override fun connect(protocolInformation: ProtocolInformation, connectionId: UUID) {
        this.protocolInformation = protocolInformation
        this.connectionId = connectionId
        vpnLogger.info("Starting Open VPN Service.")
        startConnectionJob()
        scope.launch {
            proxyDNSManager.startControlDIfRequired()
            startOpenVPN(null)
        }
    }

    override suspend fun disconnect(error: VPNState.Error?) {
        this.error = error
        if (proxyDNSManager.invalidConfig){
            proxyDNSManager.stopControlD()
        }
        if (active) {
            vpnLogger.info("Stopping OpenVPN Service.")
            connectionJob?.cancel()
            service?.stopVPN()
            delay(20)
            service?.close()
            delay(DISCONNECT_DELAY)
            deactivate()
        }
    }

    private fun startOpenVPN(action: String?) {
        val ovpnService = Intent(Windscribe.appContext, OpenVPNWrapperService::class.java)
        if (action != null) {
            vpnLogger.info("Sending stop event to OpenVPN service")
            ovpnService.action = action
        } else {
            vpnLogger.info("Sending start event to OpenVPN service")
        }
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                Windscribe.appContext.startForegroundService(ovpnService)
            } else {
                Windscribe.appContext.startService(ovpnService)
            }
        } catch (e: Exception) {
            vpnLogger.error("Failed to start OpenVPN service: ${e.message}")
            scope.launch {
                disconnect(VPNState.Error(
                    error = VPNState.ErrorType.GenericError,
                    message = "Failed to start VPN service: ${e.message}"
                ))
            }
        }
    }

    override fun updateState(
            openVpnState: String?,
            logmessage: String?,
            localizedResId: Int,
            level: ConnectionStatus?,
            Intent: Intent?
    ) {
    }

    override fun updateByteCount(download: Long, upload: Long, diffIn: Long, diffOut: Long) {}

    override fun setConnectedVPN(uuid: String?) {
    }

    fun protect(fd: Int): Boolean {
        return service?.protect(fd) ?: false
    }

    override fun handleNetworkChange() {
        if (active) {
            (service as? OpenVPNService)?.getmDeviceStateReceiver()?.networkStateChange(appContext)
        }
    }
}
