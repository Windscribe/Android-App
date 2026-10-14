/*
 * Copyright (c) 2021 Windscribe Limited.
 */

package com.windscribe.vpn.state

import com.windscribe.vpn.apppreference.PreferencesHelper
import com.windscribe.vpn.autoconnection.AutoConnectionManager
import com.windscribe.vpn.backend.VPNState
import com.windscribe.vpn.backend.VPNState.Status.Connected
import com.windscribe.vpn.backend.VPNState.Status.Disconnected
import com.windscribe.vpn.commonutils.ConnectionLogContext
import com.windscribe.vpn.repository.UserRepository
import com.windscribe.vpn.wsnet.WSNetWrapper
import dagger.Lazy
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.slf4j.LoggerFactory
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Singleton

@Singleton
class VPNConnectionStateManager(
    val scope: CoroutineScope,
    val autoConnectionManager: AutoConnectionManager,
    val preferencesHelper: PreferencesHelper,
    val userRepository: Lazy<UserRepository>,
    private val wsNetWrapper: WSNetWrapper,
) {
    private val logger = LoggerFactory.getLogger("vpn")

    private val _events = MutableStateFlow(VPNState(Disconnected))
    val state: StateFlow<VPNState> = _events

    private val _serviceMode = MutableStateFlow<ConnectionPolicy.ServiceMode?>(null)

    /**
     * What the service is doing while there is no tunnel — paused, or watching — and null otherwise.
     *
     * A second flow rather than a field on [VPNState]: both are written at nearly the same moment on
     * a disconnect, and the backend's carries a connectionId so it does not dedupe against the
     * service's. On one field, whichever landed second would win.
     */
    val serviceMode: StateFlow<ConnectionPolicy.ServiceMode?> = _serviceMode

    fun setServiceMode(mode: ConnectionPolicy.ServiceMode?) {
        _serviceMode.value = mode
    }

    private val _connectionCount = MutableSharedFlow<Int>(preferencesHelper.getConnectionCount())
    val connectionCount: SharedFlow<Int> = _connectionCount

    /** "Has a decision been made?". Callers asking "is there a tunnel?" want [isTunnelUp]. */
    fun isVPNActive(): Boolean = state.value.status != Disconnected

    /** True only while a tunnel exists or is being built. */
    fun isTunnelUp(): Boolean =
        when (state.value.status) {
            Connected,
            VPNState.Status.Connecting,
            VPNState.Status.Disconnecting,
            VPNState.Status.ProtocolSwitch,
            -> true

            else -> false
        }

    fun isVPNConnected(): Boolean = state.value.status == Connected

    fun setState(
        newState: VPNState,
        force: Boolean = false,
    ) {
        // Clearing here rather than at each call site is what stops a stale Paused surviving a connect.
        if (newState.status != Disconnected) {
            _serviceMode.value = null
        }
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
        val start = AtomicBoolean(false)
        scope.launch(Dispatchers.IO) {
            state.collectLatest {
                if (start.getAndSet(true)) {
                    // Named only when it is not the attempt the line is already stamped with: such a
                    // state came from a superseded engine that is only now dying, and nothing else
                    // in the log tells the two apart.
                    val attempt = ConnectionLogContext.differing(it.connectionId)?.let { id -> " from attempt $id" } ?: ""
                    logger.info("VPN state changed to ${it.status}$attempt (error=${it.error?.error})")
                    wsNetWrapper.withWSNet { wsNet ->
                        wsNet.setIsConnectedToVpnState(isVPNConnected())
                        if (!isVPNConnected()) {
                            wsNetWrapper.safeBridgeAPI()?.setConnectedState(false)
                        }
                    }
                } else {
                    // Nothing logged: the first emission is always the Disconnected the flow was
                    // constructed with, and "Application startup finished." already marks the process.
                    if (autoConnectionManager.listOfProtocols.isEmpty()) {
                        autoConnectionManager.reset()
                    }
                }
                if (it.status == VPNState.Status.Connected) {
                    preferencesHelper.increaseConnectionCount()
                    _connectionCount.emit(preferencesHelper.getConnectionCount())
                }
            }
        }
    }
}
