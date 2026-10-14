package com.windscribe.vpn.autoconnection

import java.io.Serializable

data class ProtocolInformation(
    var protocol: String,
    var port: String,
    val description: String,
    var type: ProtocolConnectionStatus,
    var autoConnectTimeLeft: Int = 10,
    val error: String = "failed",
) : Serializable {
    // Without the port's colon reading as a separator, and without [type] — the failover slot this
    // entry sits in, which in a connect line reads as the state of the attempt.
    override fun toString(): String = "$protocol/$port"
}
