package com.windscribe.mobile.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.windscribe.vpn.apppreference.PreferencesHelper
import com.windscribe.vpn.backend.VPNState.Status.Connected
import com.windscribe.vpn.state.VPNConnectionStateManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch


class AppStartViewModel @javax.inject.Inject constructor(
    private val preferencesHelper: PreferencesHelper,
    private val vpnConnectionStateManager: VPNConnectionStateManager
) :
    ViewModel() {
    private val _isConnected = MutableStateFlow(false)
    val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()
    val loggedIn = preferencesHelper.sessionHash != null

    init {
        observeConnectionState()
    }

    private fun observeConnectionState() {
        viewModelScope.launch {
            vpnConnectionStateManager.state.collectLatest {
                when (it.status) {
                    Connected -> {
                        _isConnected.emit(true)
                    }

                    else -> {
                        _isConnected.emit(false)
                    }
                }
            }
        }
    }
}