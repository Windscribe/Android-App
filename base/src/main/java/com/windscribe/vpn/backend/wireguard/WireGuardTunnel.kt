/*
 * Copyright (c) 2021 Windscribe Limited.
 */

package com.windscribe.vpn.backend.wireguard

import androidx.databinding.BaseObservable
import com.wireguard.android.backend.Tunnel
import com.wireguard.config.Config
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow

class WireGuardTunnel internal constructor(
        private var name: String,
        config: Config?,
        state: Tunnel.State
) : BaseObservable(), Tunnel {

    override fun getName() = name

    private val internalStateFlow = MutableSharedFlow<Tunnel.State>(replay = 1, extraBufferCapacity = 5).apply {
        tryEmit(Tunnel.State.DOWN)
    }
    val stateFlow: Flow<Tunnel.State> = internalStateFlow

    var state = state
        private set

    override fun onStateChange(newState: Tunnel.State) {
        setupInternalState(newState)
    }

    private fun setupInternalState(state: Tunnel.State): Tunnel.State {
        this.state = state
        internalStateFlow.tryEmit(state)
        return state
    }

    var config = config
        private set
}
