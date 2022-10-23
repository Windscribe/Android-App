package com.windscribe.vpn.backend.openvpn

import com.windscribe.vpn.BuildConfig
import com.windscribe.vpn.Windscribe.Companion.appContext
import com.windscribe.websockettunnel.wstunnel.TunnelCallBack
import com.windscribe.websockettunnel.wstunnel.Wstunnel.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.File

class WsTunnelManager(val scope: CoroutineScope, val vpnBackend: OpenVPNBackend) {
    var logger: Logger = LoggerFactory.getLogger("ws_tunnel")
    val running: Boolean
        get() = wsTunnelJob?.isActive == true
    private var wsTunnelJob: Job? = null

    fun startWsTunnel(ip: String, port: String) {
        wsTunnelJob = scope.launch {
            val logFile = File(appContext.filesDir, WS_TUNNEL_LOG_FILE).path
            initialise(BuildConfig.DEV, logFile)
            val remote = "wss://$ip:$port/$WS_TUNNEL_PROTOCOL/$WS_TUNNEL_ADDRESS/$WS_TUNNEL_PORT"
            logger.debug("Starting ws tunnel with arguments: $remote")
            registerTunnelCallback(callback)
            startWSTunnel(":$WS_TUNNEL_PORT", remote)
            logger.debug("Exiting ws tunnel.")
        }
    }

    fun stopWsTunnel() {
        logger.debug("Stopping ws tunnel.")
        wsTunnelJob?.cancel()
        registerTunnelCallback(null)
    }

    private var callback = TunnelCallBack { fd ->
        vpnBackend.service?.protect(fd.toInt())
        logger.debug("Protecting ws socket $fd.")
    }

    companion object {
        const val WS_TUNNEL_PORT = "1194"
        const val WS_TUNNEL_ADDRESS = "127.0.0.1"
        const val WS_TUNNEL_PROTOCOL = "tcp"
        const val WS_TUNNEL_LOG_FILE = "ws_tunnel_log.txt"
    }
}