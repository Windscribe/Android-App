package com.windscribe.vpn.autoconnection

import java.io.Serializable

data class ProtocolInformation(
    var protocol: String,
    var port: String,
    val description: String,
    var type: ProtocolConnectionStatus,
    var autoConnectTimeLeft: Int = 10,
    val error: String = "failed"
) : Serializable {
    override fun toString(): String {
        return "$protocol:$port:${type.name}"
    }
}

