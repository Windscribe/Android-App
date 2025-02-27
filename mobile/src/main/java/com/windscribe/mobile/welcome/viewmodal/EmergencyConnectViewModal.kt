package com.windscribe.mobile.welcome.viewmodal

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.windscribe.mobile.welcome.state.EmergencyConnectUIState
import com.windscribe.vpn.backend.VPNState.Status.*
import com.windscribe.vpn.backend.utils.WindVpnController
import com.windscribe.vpn.state.VPNConnectionStateManager
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import org.slf4j.LoggerFactory

class EmergencyConnectViewModal(
    private val scope: CoroutineScope,
    private val windVpnController: WindVpnController,
    private val vpnConnectionStateManager: VPNConnectionStateManager
) : ViewModel() {
    private val logger = LoggerFactory.getLogger("emergency_connect")
    private var _uiState = MutableStateFlow(EmergencyConnectUIState.Disconnected)
    val uiState: StateFlow<EmergencyConnectUIState> = _uiState
    private var _connectionProgressText = MutableStateFlow("Resolving e-connect domain..")
    val connectionProgressText: StateFlow<String> = _connectionProgressText
    private var connectingJob: Job? = null

    init {
        observeConnectionState()
    }

    private fun observeConnectionState() {
        viewModelScope.launch {
            vpnConnectionStateManager.state.collectLatest {
                when (it.status) {
                    Connecting -> _uiState.emit(EmergencyConnectUIState.Connecting)
                    Connected -> _uiState.emit(EmergencyConnectUIState.Connected)
                    Disconnected -> _uiState.emit(EmergencyConnectUIState.Disconnected)
                    Disconnecting -> _uiState.emit(EmergencyConnectUIState.Disconnected)
                    RequiresUserInput -> _uiState.emit(EmergencyConnectUIState.Disconnected)
                    else -> {}
                }
            }
        }
    }

    fun connectButtonClick() {
        logger.info("User clicked connect button with current state: ${uiState.value}")
        if (uiState.value == EmergencyConnectUIState.Connected || uiState.value == EmergencyConnectUIState.Connecting) {
            disconnect()
        } else {
            connect()
        }
    }

    fun disconnect() {
        scope.launch {
            connectingJob?.cancel()
            windVpnController.disconnectAsync()
        }
    }

    private fun connect() {
        connectingJob = scope.launch {
            _uiState.emit(EmergencyConnectUIState.Connecting)
            windVpnController.connectUsingEmergencyProfile { progress ->
                _connectionProgressText.value = progress
            }.onSuccess {
                logger.info("Successfully connected to emergency server.")
            }.onFailure {
                logger.error("Failure to connect using emergency vpn profiles: $it")
                _uiState.emit(EmergencyConnectUIState.Disconnected)
            }
        }
    }

    companion object {
        fun provideFactory(
            scope: CoroutineScope,
            windVpnController: WindVpnController,
            vpnConnectionStateManager: VPNConnectionStateManager
        ) = object : ViewModelProvider.NewInstanceFactory() {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                if (modelClass.isAssignableFrom(EmergencyConnectViewModal::class.java)) {
                    return EmergencyConnectViewModal(
                        scope, windVpnController, vpnConnectionStateManager
                    ) as T
                }
                return super.create(modelClass)
            }
        }
    }
}