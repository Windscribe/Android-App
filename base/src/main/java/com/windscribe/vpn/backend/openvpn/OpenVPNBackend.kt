/*
 * Copyright (c) 2021 Windscribe Limited.
 */

package com.windscribe.vpn.backend.openvpn

import android.content.Intent
import android.os.Build
import com.windscribe.vpn.ServiceInteractor
import com.windscribe.vpn.Windscribe
import com.windscribe.vpn.backend.TrafficCounter
import com.windscribe.vpn.backend.VPNState
import com.windscribe.vpn.backend.VpnBackend
import com.windscribe.vpn.backend.utils.ProtocolManager
import com.windscribe.vpn.decoytraffic.DecoyTrafficController
import com.windscribe.vpn.state.NetworkInfoManager
import com.windscribe.vpn.state.VPNConnectionStateManager
import com.wireguard.android.backend.GoBackend
import de.blinkt.openvpn.core.ConnectionStatus
import de.blinkt.openvpn.core.OpenVPNService
import de.blinkt.openvpn.core.VpnStatus
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay

@Singleton
class OpenVPNBackend(
        var backend: GoBackend,
        var scope: CoroutineScope,
        var trafficCounter: TrafficCounter,
        var networkInfoManager: NetworkInfoManager,
        vpnStateManager: VPNConnectionStateManager,
        var serviceInteractor: ServiceInteractor,
        var protocolManager: ProtocolManager,
) : VpnBackend(scope, vpnStateManager, serviceInteractor, protocolManager),
        VpnStatus.StateListener,
        VpnStatus.ByteCountListener {

    override var active = false
    override fun activate() {
        VpnStatus.addStateListener(this)
        VpnStatus.addLogListener {
            vpnLogger.debug(it.toString())
            if (it.toString() == "AUTH: Received control message: AUTH_FAILED") {
                authFailure = true
            }
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

    override fun connect() {
        vpnLogger.debug("Starting Open VPN Service.")
        startConnectionJob()
        startOpenVPN(null)
    }

    override suspend fun disconnect() {
        vpnLogger.debug("Stopping Open VPN Service.")
        connectionJob?.cancel()
        trafficCounter.stop()
        startOpenVPN(OpenVPNService.PAUSE_VPN)
        delay(DISCONNECT_DELAY)
        deactivate()
    }

    private fun startOpenVPN(action: String?) {
        vpnLogger.debug("Launching open VPN Service")
        val ovpnService = Intent(Windscribe.appContext, OpenVPNWrapperService::class.java)
        if (action != null)
            ovpnService.action = action
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
            // if (!active)return@let
            vpnLogger.debug("Open VPN Connection State: ${level.name}")
            when (it) {
                ConnectionStatus.LEVEL_START, ConnectionStatus.LEVEL_CONNECTING_NO_SERVER_REPLY_YET,
                ConnectionStatus.LEVEL_CONNECTING_SERVER_REPLIED -> {
                    updateState(VPNState(VPNState.Status.Connecting))
                }
                ConnectionStatus.LEVEL_NOTCONNECTED -> {
                    connectionJob?.cancel()
                    updateState(VPNState(VPNState.Status.Disconnected))
                }
                ConnectionStatus.LEVEL_CONNECTED -> {
                    testConnectivity()
                }
                ConnectionStatus.LEVEL_MULTI_USER_PERMISSION -> {
                    updateState(VPNState(VPNState.Status.Disconnected, VPNState.Error.GenericError))
                }
                ConnectionStatus.LEVEL_AUTH_FAILED -> {
                    serviceInteractor.preferenceHelper.isReconnecting = false
                    updateState(VPNState(VPNState.Status.Disconnected, VPNState.Error.AuthenticationError))
                }
                ConnectionStatus.LEVEL_WAITING_FOR_USER_INPUT -> {
                    updateState(VPNState(VPNState.Status.RequiresUserInput, VPNState.Error.GenericError))
                }
                else -> {}
            }
        }
    }

    override fun updateByteCount(download: Long, upload: Long, diffIn: Long, diffOut: Long) {
        trafficCounter.update(download, upload)
    }

    override fun connectivityTestPassed(ip: String) {
        if (vpnServiceInteractor.preferenceHelper.notificationStat) {
            trafficCounter.start()
        }
        super.connectivityTestPassed(ip)
    }

    override fun setConnectedVPN(uuid: String?) {
    }
}
