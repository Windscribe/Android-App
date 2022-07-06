/*
 * Copyright (c) 2021 Windscribe Limited.
 */

package com.windscribe.vpn.state

import com.windscribe.vpn.Windscribe.Companion.appContext
import com.windscribe.vpn.apppreference.PreferencesHelper
import com.windscribe.vpn.backend.VPNState
import com.windscribe.vpn.backend.VPNState.Status.Connected
import com.windscribe.vpn.backend.VPNState.Status.Disconnected
import com.windscribe.vpn.repository.ConnectionDataRepository
import com.windscribe.vpn.repository.UserRepository
import dagger.Lazy
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.rx2.await
import org.slf4j.LoggerFactory

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

    fun setState(newState: VPNState) {
        scope.launch {
            if(AppLifeCycleObserver.isInForeground.not() && newState.status == Disconnected){
               preferencesHelper.isReconnecting = false
            }
            _events.emit(newState)
        }
    }

    init {
        scope.launch {
            state.collectLatest {
                logger.debug("VPN Connection State: ${it.status}")
                handleErrors(it.error)
            }
        }
    }

    private suspend fun handleErrors(error: VPNState.Error?) {
        error?.let {
            logger.debug("VPN Connection Error: ${it.name}")
            if (it == VPNState.Error.AuthenticationError) {
                try {
                    if(preferencesHelper.globalUserConnectionPreference && userRepository.get().accountStatusOkay()) {
                        connectionDataRepository.update().retry(1).await()
                        if(preferencesHelper.globalUserConnectionPreference){
                            appContext.vpnController.connect()
                        }
                    }
                } catch (e: Exception) {
                    logger.debug("Failed to update connection data.")
                }
            }
        }
    }
}
