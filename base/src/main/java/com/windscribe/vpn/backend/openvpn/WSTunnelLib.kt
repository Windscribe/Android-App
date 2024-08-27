package com.windscribe.vpn.backend.openvpn
class WSTunnelLib {
    external fun initialise(development: Boolean, logPath: String)
    external fun startProxy(listenAddress: String, remoteAddress: String, tunnelType: Int, mtu: Long, extraPadding: Boolean)
    external fun socketFd(): Int
    external fun stop()
}