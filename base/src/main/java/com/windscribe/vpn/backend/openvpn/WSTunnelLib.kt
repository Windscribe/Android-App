package com.windscribe.vpn.backend.openvpn

import androidx.annotation.Keep
import com.windscribe.vpn.Windscribe.Companion.appContext
import com.wireguard.android.backend.GoBackend

/**
 * wstunnel.
 *
 * Constructing this loads libwg-go.so, so never do it on the main thread — see
 * [GoBackend.loadLibrary]. The proxy only ever runs under OpenVPN, which never resolves the
 * WireGuard backend, so the load cannot be left to it.
 */
class WSTunnelLib {
    init {
        GoBackend.loadLibrary(appContext)
    }

    external fun initialise(
        development: Boolean,
        logPath: String,
    )

    external fun startProxy(
        listenAddress: String,
        remoteAddress: String,
        tunnelType: Int,
        mtu: Long,
        extraPadding: Boolean,
        tlsServerName: String,
    )

    external fun socketFd(): Int

    external fun stop()

    companion object {
        @Volatile
        private var protector: ((Int) -> Boolean)? = null

        /** Set before [startProxy], cleared once the proxy is stopped. */
        fun setProtector(block: ((Int) -> Boolean)?) {
            protector = block
        }

        /**
         * Called from the Go dialer between `socket()` and `connect()`. Blocking the dialer until
         * this returns is the point: `protect()` does not reroute a socket that already connected.
         */
        @Keep
        @JvmStatic
        fun protectSocket(fd: Int): Boolean = protector?.invoke(fd) == true
    }
}
