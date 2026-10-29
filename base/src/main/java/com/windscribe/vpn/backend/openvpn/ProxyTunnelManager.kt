package com.windscribe.vpn.backend.openvpn

import com.windscribe.vpn.BuildConfig
import com.windscribe.vpn.Windscribe.Companion.appContext
import com.windscribe.vpn.apppreference.isProtocolTweaksEnabled
import com.windscribe.vpn.commonutils.Ext.maskMiddle
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.File

class ProxyTunnelManager(
    val scope: CoroutineScope,
    val vpnBackend: OpenVPNBackend,
) {
    var logger: Logger = LoggerFactory.getLogger("vpn")
    val running: Boolean
        get() = proxyJob?.isActive == true
    private var proxyJob: Job? = null

    /** Lazy so libwg-go loads on the proxy coroutine rather than when the backend is built. */
    private val tunnelLib by lazy { WSTunnelLib() }

    fun startProxyTunnel(
        ip: String,
        port: String,
        localPort: Int = PROXY_TUNNEL_PORT,
        isWSTunnel: Boolean = true,
    ) {
        scope.launch {
            if (proxyJob?.isActive == true) {
                logger.debug("Previous Stunnel job is still running. Waiting for it to finish.")
                proxyJob?.join()
            }
            WSTunnelLib.setProtector { fd ->
                vpnBackend.protect(fd).also {
                    logger.debug("Protecting WSTunnel socket fd $fd: $it")
                }
            }
            proxyJob =
                scope.launch {
                    val mtu =
                        if (appContext.preference.isPackageSizeModeAuto) {
                            1500
                        } else {
                            appContext.preference.packetSize.toLong()
                        }
                    val logFile = File(appContext.filesDir, PROXY_LOG).path
                    tunnelLib.initialise(BuildConfig.DEV, logFile)
                    val protocolTweaksEnabled = appContext.preference.isProtocolTweaksEnabled
                    val tlsServerName = appContext.preference.tlsServerName
                    logger.debug(
                        "Running proxy on local port: $localPort Protocol Tweaks: $protocolTweaksEnabled SNI: ${tlsServerName.maskMiddle(
                            4,
                            4,
                        )}",
                    )
                    if (isWSTunnel) {
                        val remote =
                            "wss://$ip:$port/$PROXY_TUNNEL_PROTOCOL/$PROXY_TUNNEL_ADDRESS/$WS_TUNNEL_PORT"
                        tunnelLib.startProxy("127.0.0.1:$localPort", remote, 1, mtu, protocolTweaksEnabled, tlsServerName)
                    } else {
                        val remote = "https://$ip:$port"
                        tunnelLib.startProxy("127.0.0.1:$localPort", remote, 2, mtu, protocolTweaksEnabled, tlsServerName)
                    }
                    logger.debug("Exiting tunnel proxy.")
                }
        }
    }

    /**
     * Stops the proxy without waiting for it.
     *
     * `tunnelLib.stop()` blocks until the Go `startProxy` goroutine returns, which for a WSTunnel
     * proxy that never established takes ~90s. It is called inside the connect path's `withTimeout`,
     * which cannot interrupt a blocking native call.
     *
     * A stopgap, not a fix: the next start still serialises on [proxyJob]'s `join()`, so a slow stop
     * delays the proxy replacing it. The connect fails quickly rather than hanging. The real fix is
     * for `WSTunnelLib.stop()` to return promptly.
     */
    fun stopProxyTunnel() {
        logger.debug("Stopping proxy.")
        WSTunnelLib.setProtector(null)
        scope.launch(Dispatchers.IO) {
            runCatching { tunnelLib.stop() }
                .onFailure { logger.debug("Proxy stop failed: ${it.message}") }
            logger.debug("Proxy stop returned.")
        }
    }

    companion object {
        const val WS_TUNNEL_PORT = "1194"
        const val PROXY_LOG = "proxy.txt"
        const val PROXY_TUNNEL_ADDRESS = "127.0.0.1"
        const val PROXY_TUNNEL_PROTOCOL = "tcp"
        const val PROXY_TUNNEL_PORT = 65480
    }
}
