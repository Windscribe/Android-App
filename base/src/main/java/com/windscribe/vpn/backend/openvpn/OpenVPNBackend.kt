/*
 * Copyright (c) 2021 Windscribe Limited.
 */

package com.windscribe.vpn.backend.openvpn

import android.content.Intent
import android.os.Build
import com.windscribe.vpn.ServiceInteractor
import com.windscribe.vpn.Windscribe
import com.windscribe.vpn.autoconnection.ProtocolInformation
import com.windscribe.vpn.backend.VPNState
import com.windscribe.vpn.backend.VpnBackend
import com.windscribe.vpn.repository.AdvanceParameterRepository
import com.windscribe.vpn.state.NetworkInfoManager
import com.windscribe.vpn.state.VPNConnectionStateManager
import com.wireguard.android.backend.GoBackend
import de.blinkt.openvpn.core.ConnectionStatus
import de.blinkt.openvpn.core.OpenVPNService
import de.blinkt.openvpn.core.VpnStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.*
import javax.inject.Singleton

@Singleton
class OpenVPNBackend(
    var backend: GoBackend,
    var scope: CoroutineScope,
    var networkInfoManager: NetworkInfoManager,
    vpnStateManager: VPNConnectionStateManager,
    var serviceInteractor: ServiceInteractor,
    advanceParameterRepository: AdvanceParameterRepository
) : VpnBackend(scope, vpnStateManager, serviceInteractor, networkInfoManager, advanceParameterRepository),
    VpnStatus.StateListener, VpnStatus.ByteCountListener {

    override var active = false
    private var stickyDisconnectEvent = false
    var service: OpenVPNWrapperService? = null
    override fun activate() {
        vpnLogger.debug("Activating OpenVPN backend.")
        stickyDisconnectEvent = true
        VpnStatus.addStateListener(this)
        VpnStatus.addLogListener {
            vpnLogger.debug(it.toString())
        }
        VpnStatus.addByteCountListener(this)
        active = true
        vpnLogger.debug("Open VPN backend activated.")
    }

    override fun deactivate() {
        VpnStatus.removeLogListener {}
        VpnStatus.removeStateListener(this)
        VpnStatus.removeByteCountListener(this)
        active = false
        vpnLogger.debug("Open VPN backend deactivated.")
    }

    override fun connect(protocolInformation: ProtocolInformation, connectionId: UUID) {
        this.protocolInformation = protocolInformation
        this.connectionId = connectionId
        vpnLogger.debug("Starting Open VPN Service.")
        startConnectionJob()
        startOpenVPN(null)
    }

    override suspend fun disconnect(error: VPNState.Error?) {
        this.error = error
        if (active) {
            stickyDisconnectEvent = false
            vpnLogger.debug("Stopping Open VPN Service.")
            connectionJob?.cancel()
            startOpenVPN(OpenVPNService.PAUSE_VPN)
            delay(DISCONNECT_DELAY)
            deactivate()
        }
    }

    private fun startOpenVPN(action: String?) {
        val ovpnService = Intent(Windscribe.appContext, OpenVPNWrapperService::class.java)
        if (action != null) {
            vpnLogger.debug("Sending stop event to OpenVPN service")
            ovpnService.action = action
        } else {
            vpnLogger.debug("Sending start event to OpenVPN service")
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Windscribe.appContext.startForegroundService(ovpnService)
        } else {
            Windscribe.appContext.startService(ovpnService)
        }
    }

    override fun updateState(
            openVpnState: String?,
            logmessage: String?,
            localizedResId: Int,
            level: ConnectionStatus?,
            Intent: Intent?
    ) {
        vpnLogger.debug("$openVpnState $localizedResId $level")
        level?.let {
            when (it) {
                ConnectionStatus.LEVEL_START, ConnectionStatus.LEVEL_CONNECTING_NO_SERVER_REPLY_YET,
                ConnectionStatus.LEVEL_CONNECTING_SERVER_REPLIED -> {
                    //  updateState(VPNState(VPNState.Status.Connecting, connectionId = connectionId))
                }
                ConnectionStatus.LEVEL_NOTCONNECTED -> {
                    if (stickyDisconnectEvent && stateManager.state.value.status == VPNState.Status.Connecting) {
                        stickyDisconnectEvent = false
                        return
                    }
                    connectionJob?.cancel()
                    updateState(VPNState(VPNState.Status.Disconnected, connectionId = connectionId))
                }
                ConnectionStatus.LEVEL_CONNECTED -> {
                    testConnectivity()
                }
                ConnectionStatus.LEVEL_MULTI_USER_PERMISSION -> {
                    updateState(
                        VPNState(
                            VPNState.Status.Disconnected,
                            VPNState.Error(VPNState.ErrorType.GenericError)
                        )
                    )
                }
                ConnectionStatus.LEVEL_AUTH_FAILED -> {
                    serviceInteractor.preferenceHelper.isReconnecting = false
                    scope.launch {
                        disconnect(
                            VPNState.Error(
                                error = VPNState.ErrorType.AuthenticationError,
                                message = "Authentication failed."
                            )
                        )
                    }
                }
                ConnectionStatus.LEVEL_WAITING_FOR_USER_INPUT -> {
                    updateState(
                        VPNState(
                            VPNState.Status.RequiresUserInput,
                            VPNState.Error(VPNState.ErrorType.GenericError)
                        )
                    )
                }
                else -> {}
            }
        }
    }

    override fun updateByteCount(download: Long, upload: Long, diffIn: Long, diffOut: Long) {}

    override fun connectivityTestPassed(ip: String) {
        super.connectivityTestPassed(ip)
    }

    override fun setConnectedVPN(uuid: String?) {
    }

    fun protect(fd: Int): Boolean {
        return service?.protect(fd) ?: false
    }
}
