/*
 * Copyright (c) 2021 Windscribe Limited.
 */

package com.windscribe.vpn.backend.ikev2

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow

/**
 * Simple tunnel state tracker for IKEv2, similar to WireGuardTunnel.
 * Uses StateFlow pattern to emit connection state changes from CharonVpnService.
 */
class IKev2Tunnel {
    enum class State {
        DOWN,
        CONNECTING,
        CONNECTED,
        DISCONNECTING,
    }

    private val internalStateFlow =
        MutableSharedFlow<State>(replay = 1, extraBufferCapacity = 5).apply {
            tryEmit(State.DOWN)
        }
    val stateFlow: Flow<State> = internalStateFlow

    var state = State.DOWN
        private set

    /**
     * Called by CharonVpnService when tunnel state changes.
     */
    fun onStateChange(newState: State) {
        this.state = newState
        internalStateFlow.tryEmit(newState)
    }
}
