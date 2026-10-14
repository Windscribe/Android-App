/*
 * Copyright (c) 2021 Windscribe Limited.
 */

package com.windscribe.vpn.backend

import com.windscribe.vpn.autoconnection.ProtocolInformation
import java.util.UUID

class VPNState(
    val status: Status,
    var error: Error? = null,
    val ip: String? = null,
    var protocolInformation: ProtocolInformation? = null,
    var connectionId: UUID? = null,
) {
    enum class Status {
        Connecting,
        Connected,
        Disconnected,
        Disconnecting,
        RequiresUserInput,
        ProtocolSwitch,
        UnsecuredNetwork,
        InvalidSession,
    }

    enum class ErrorType {
        UserReconnect,
        UserDisconnect,
        UserPause,
        UserTurnOff,

        /** VPN consent was revoked. Terminal: nothing can connect without it. */
        Revoked,
        AuthenticationError,
        WireguardAuthenticationError,
        GenericError,
        TimeoutError,
        WireguardApiError,
        ConnectivityTestFailed,
        BrokenTunnel,
    }

    data class Error(
        val error: ErrorType,
        val message: String = "Unknown",
        val showError: Boolean = false,
    )
}
