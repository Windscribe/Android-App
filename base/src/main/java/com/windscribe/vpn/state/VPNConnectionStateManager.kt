/*
 * Copyright (c) 2021 Windscribe Limited.
 */

package com.windscribe.vpn.state

import com.windscribe.vpn.apppreference.PreferencesHelper
import com.windscribe.vpn.backend.VPNState
import com.windscribe.vpn.backend.VPNState.Status.Connected
import com.windscribe.vpn.backend.VPNState.Status.Disconnected
import com.windscribe.vpn.repository.ConnectionDataRepository
import com.windscribe.vpn.repository.UserRepository
import dagger.Lazy
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.slf4j.LoggerFactory
import javax.inject.Singleton

@Singleton
class VPNConnectionStateManager(val scope: CoroutineScope, val connectionDataRepository: ConnectionDataRepository, val preferencesHelper: PreferencesHelper, val userRepository: Lazy<UserRepository>) {
    private val logger = LoggerFactory.getLogger("vpn_backend")

    private val _events = MutableStateFlow(VPNState(Disconnected))
    val state: StateFlow<VPNState> = _events

    fun isVPNActive(): Boolean {
        return state.value.status != Disconnected
    }

    fun isVPNConnected(): Boolean {
        return state.value.status == Connected
    }

    fun setState(newState: VPNState, force: Boolean = false) {
        scope.launch {
            if (AppLifeCycleObserver.isInForeground.not() && newState.status == Disconnected) {
                preferencesHelper.isReconnecting = false
            }
            val lastState = "${_events.value.status}${_events.value.connectionId}>"
            val state = "${newState.status}${newState.connectionId}>"
            if (lastState != state || force) {
                _events.emit(newState)
            }
        }
    }

    init {
        scope.launch {
            state.collectLatest {
                logger.debug("VPN connection state changed to ${it.status}")
            }
        }
    }
}
