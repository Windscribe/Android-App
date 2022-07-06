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
        val ovpnX509: String
) {
    override fun toString(): String {
        return "VPNParameters(ikev2Ip='$ikev2Ip', udpIp='$udpIp', tcpIp='$tcpIp', stealthIp='$stealthIp', hostName='$hostName', publicKey='$publicKey', ovpnX509='$ovpnX509')"
    }
}