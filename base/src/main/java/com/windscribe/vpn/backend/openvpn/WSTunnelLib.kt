package com.windscribe.vpn.backend.openvpn

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
}
