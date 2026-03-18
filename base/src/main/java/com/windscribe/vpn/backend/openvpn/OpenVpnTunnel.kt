/*
 * Copyright (c) 2021 Windscribe Limited.
 */

package com.windscribe.vpn.backend.openvpn

import com.windscribe.vpn.backend.VPNState
import de.blinkt.openvpn.core.ConnectionStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow

class OpenVpnTunnel {

    enum class State {
        DOWN,
        CONNECTING,
        CONNECTED,
        DISCONNECTING
    }

    data class ErrorState(
        val errorType: VPNState.ErrorType,
        val message: String,
        val vpnStatus: VPNState.Status? = null
    )

    private val internalStateFlow = MutableSharedFlow<State>(replay = 1, extraBufferCapacity = 5).apply {
        tryEmit(State.DOWN)
    }
    val stateFlow: Flow<State> = internalStateFlow

    private val internalErrorFlow = MutableSharedFlow<ErrorState>(replay = 0, extraBufferCapacity = 5)
    val errorFlow: Flow<ErrorState> = internalErrorFlow

    var state = State.DOWN
        private set

    fun onStateChange(newState: State) {
        this.state = newState
        internalStateFlow.tryEmit(newState)
    }

    fun onError(errorType: VPNState.ErrorType, message: String, vpnStatus: VPNState.Status? = null) {
        internalErrorFlow.tryEmit(ErrorState(errorType, message, vpnStatus))
    }

    fun onConnectionStatusChange(status: ConnectionStatus): State? {
        return when (status) {
            ConnectionStatus.LEVEL_START,
            ConnectionStatus.LEVEL_CONNECTING_NO_SERVER_REPLY_YET,
            ConnectionStatus.LEVEL_CONNECTING_SERVER_REPLIED -> State.CONNECTING

            ConnectionStatus.LEVEL_CONNECTED -> State.CONNECTED

            ConnectionStatus.LEVEL_NOTCONNECTED -> State.DOWN

            ConnectionStatus.LEVEL_AUTH_FAILED -> {
                onError(VPNState.ErrorType.AuthenticationError, "Authentication failed.")
                null
            }

            ConnectionStatus.LEVEL_MULTI_USER_PERMISSION -> {
                onError(VPNState.ErrorType.GenericError, "Multi-user permission error.", VPNState.Status.Disconnected)
                null
            }

            ConnectionStatus.LEVEL_WAITING_FOR_USER_INPUT -> {
                onError(VPNState.ErrorType.GenericError, "Waiting for user input.", VPNState.Status.RequiresUserInput)
                null
            }

            else -> null
        }
    }
}
