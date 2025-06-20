package com.windscribe.mobile.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.windscribe.vpn.backend.VPNState
import com.windscribe.vpn.backend.utils.WindVpnController
import com.windscribe.vpn.state.VPNConnectionStateManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.slf4j.LoggerFactory
import javax.inject.Inject

enum class EmergencyConnectUIState {
    Connecting,
    Connected,
    Disconnected
}
class EmergencyConnectViewModal @Inject constructor(
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
    val error = MutableSharedFlow<String>(replay = 0)

    init {
        observeConnectionState()
    }

    private fun observeConnectionState() {
        viewModelScope.launch {
            vpnConnectionStateManager.state.collectLatest {
                when (it.status) {
                    VPNState.Status.Connecting -> _uiState.emit(EmergencyConnectUIState.Connecting)
                    VPNState.Status.Connected -> _uiState.emit(EmergencyConnectUIState.Connected)
                    VPNState.Status.Disconnected -> _uiState.emit(EmergencyConnectUIState.Disconnected)
                    VPNState.Status.Disconnecting -> _uiState.emit(EmergencyConnectUIState.Disconnected)
                    VPNState.Status.RequiresUserInput -> _uiState.emit(EmergencyConnectUIState.Disconnected)
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
                error.emit(it.message ?: "Failed to connect using emergency vpn profile.")
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