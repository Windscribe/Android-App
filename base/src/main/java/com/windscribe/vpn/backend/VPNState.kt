/*
 * Copyright (c) 2021 Windscribe Limited.
 */

package com.windscribe.vpn.backend

class VPNState(val status: Status, val error: Error? = null, val ip: String? = null) {
    enum class Status {
        Connecting, Connected, Disconnected, Disconnecting, RequiresUserInput, ProtocolSwitch, UnsecuredNetwork, InvalidSession
    }

    enum class Error {
        AuthenticationError, GenericError
    }
}
