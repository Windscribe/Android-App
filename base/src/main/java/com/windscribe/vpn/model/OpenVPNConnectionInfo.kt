package com.windscribe.vpn.model

import com.windscribe.vpn.autoconnection.ProtocolConnectionStatus
import com.windscribe.vpn.autoconnection.ProtocolInformation
import com.windscribe.vpn.constants.PreferencesKeyConstants

data class OpenVPNConnectionInfo(
    val serverConfig: String,
    val ip: String,
    val port: String,
    val protocol: String,
    val username: String,
    val password: String
) {
    val base64EncodedServerConfig =
        String(com.windscribe.vpn.encoding.encoders.Base64.encode(serverConfig.toByteArray()))

    fun getProtocolInformation(): ProtocolInformation {
        var protocol = PreferencesKeyConstants.PROTO_TCP
        if (protocol == "udp") {
            protocol = PreferencesKeyConstants.PROTO_UDP
        }
        return ProtocolInformation(
            protocol, port, "", type = ProtocolConnectionStatus.NextUp
        )
    }

    override fun toString(): String {
        return "$ip:$port $protocol"
    }
}