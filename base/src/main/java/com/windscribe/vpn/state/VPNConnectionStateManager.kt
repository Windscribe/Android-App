/*
 * Copyright (c) 2021 Windscribe Limited.
 */

package com.windscribe.vpn.state

import android.os.Build
import com.windscribe.vpn.R
import com.windscribe.vpn.Windscribe
import com.windscribe.vpn.apppreference.PreferencesHelper
import com.windscribe.vpn.autoconnection.AutoConnectionManager
import com.windscribe.vpn.backend.VPNState
import com.windscribe.vpn.backend.VPNState.Status.Connected
import com.windscribe.vpn.backend.VPNState.Status.Disconnected
import com.windscribe.vpn.commonutils.WindUtilities
import com.windscribe.vpn.repository.UserRepository
import com.wsnet.lib.WSNet
import dagger.Lazy
import kotlinx.coroutines.CoroutineScope
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
class VPNConnectionStateManager(val scope: CoroutineScope, val autoConnectionManager: AutoConnectionManager, val preferencesHelper: PreferencesHelper, val userRepository: Lazy<UserRepository>) {
    private val logger = LoggerFactory.getLogger("vpn")

    private val _events = MutableStateFlow(VPNState(Disconnected))
    val state: StateFlow<VPNState> = _events

    private val _connectionCount = MutableSharedFlow<Int>(preferencesHelper.getConnectionCount())
    val connectionCount: SharedFlow<Int> = _connectionCount
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
        val start = AtomicBoolean(false)
        scope.launch {
            state.collectLatest {
                WSNet.instance().setIsConnectedToVpnState(isVPNConnected())
                if (start.getAndSet(true)) {
                    logger.debug("VPN state changed to ${it.status}")
                } else {
                    val logFile = Windscribe.appContext.resources.getString(
                        R.string.log_file_header,
                        Build.VERSION.SDK_INT,
                        Build.BRAND,
                        Build.DEVICE,
                        Build.MODEL,
                        Build.MANUFACTURER,
                        Build.VERSION.RELEASE,
                        WindUtilities.getVersionCode()
                    )
                    logger.info(logFile)
                    logger.debug("VPN state initialized with ${it.status}")
                    if (autoConnectionManager.listOfProtocols.isEmpty()){
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
