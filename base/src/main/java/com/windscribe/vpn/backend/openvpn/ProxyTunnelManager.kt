package com.windscribe.vpn.backend.openvpn

import com.windscribe.vpn.BuildConfig
import com.windscribe.vpn.Windscribe.Companion.appContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.File

class ProxyTunnelManager(val scope: CoroutineScope, val vpnBackend: OpenVPNBackend) {
    var logger: Logger = LoggerFactory.getLogger("proxy")
    val running: Boolean
        get() = proxyJob?.isActive == true
    private var proxyJob: Job? = null
    private var tunnelLib = WSTunnelLib()
    private var protectJob: Job? = null

    fun startProxyTunnel(ip: String, port: String, isWSTunnel: Boolean = true) {
        proxyJob = scope.launch {
            val mtu = if (appContext.preference.isPackageSizeModeAuto) {
                1500
            } else {
                appContext.preference.packetSize.toLong()
            }
            val logFile = File(appContext.filesDir, PROXY_LOG).path
            tunnelLib.initialise(BuildConfig.DEV, logFile)
            if (isWSTunnel) {
                val remote =
                    "wss://$ip:$port/$PROXY_TUNNEL_PROTOCOL/$PROXY_TUNNEL_ADDRESS/$WS_TUNNEL_PORT"
                tunnelLib.startProxy(":$PROXY_TUNNEL_PORT", remote, 1, mtu, false)
            } else {
                val remote = "https://$ip:$port"
                val antiCensorship = appContext.preference.isAntiCensorshipOn
                tunnelLib.startProxy(":$PROXY_TUNNEL_PORT", remote, 2, mtu, antiCensorship)
            }
            logger.debug("Exiting tunnel proxy.")
        }
        protectJob = scope.launch {
            delay(1500)
            while (tunnelLib.socketFd() != -1) {
                Thread.sleep(100)
            }
            val socketFd = tunnelLib.socketFd()
            logger.debug("Protecting WSTunnel Socket Fd: $socketFd")
            vpnBackend.protect(socketFd)
        }
    }

    fun stopProxyTunnel() {
        logger.debug("Stopping proxy.")
        protectJob?.cancel()
        proxyJob?.cancel()
        tunnelLib.stop()
    }

    companion object {
        const val WS_TUNNEL_PORT = "1194"
        const val PROXY_LOG = "proxy.txt"
        const val PROXY_TUNNEL_ADDRESS = "127.0.0.1"
        const val PROXY_TUNNEL_PROTOCOL = "tcp"
        const val PROXY_TUNNEL_PORT = 65479
    }
}