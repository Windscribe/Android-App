/*
 * Copyright (c) 2021 Windscribe Limited.
 */

package com.windscribe.vpn.backend.utils

class VPNParameters(
    val ikev2Ip: String,
    val udpIp: String,
    val tcpIp: String,
    val stealthIp: String,
    val hostName: String,
    val publicKey: String,
    val ovpnX509: String,
    val supportsV6: Boolean = false,
    /** Server list node id, 0 when the node has no id (static IP or custom config). */
    val nodeId: Int = 0,
) {
    override fun toString(): String =
        "VPNParameters(ikev2Ip='$ikev2Ip', udpIp='$udpIp', tcpIp='$tcpIp', stealthIp='$stealthIp', hostName='$hostName', publicKey='$publicKey', ovpnX509='$ovpnX509', nodeId=$nodeId)"
}
