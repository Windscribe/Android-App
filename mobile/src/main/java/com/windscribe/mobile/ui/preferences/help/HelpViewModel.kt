package com.windscribe.mobile.ui.preferences.help

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.windscribe.vpn.repository.CallResult
import com.windscribe.vpn.repository.LogRepository
import com.windscribe.vpn.repository.UserRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

abstract class HelpViewModel : ViewModel() {
    abstract val showProgress: StateFlow<Boolean>
    abstract val sendLogState: StateFlow<SendLogState>
    abstract fun sendLogClicked()
    abstract val isUserPro: StateFlow<Boolean>
}

sealed class SendLogState {
    object Idle : SendLogState()
    object Loading : SendLogState()
    object Success : SendLogState()
    object Failure : SendLogState()
}

class HelpViewModelImpl(
    val userRepository: UserRepository,
    val logRepository: LogRepository
) : HelpViewModel() {
    private val _showProgress = MutableStateFlow(false)
    override val showProgress: StateFlow<Boolean> = _showProgress
    private val _sendLogState = MutableStateFlow<SendLogState>(SendLogState.Idle)
    override val sendLogState: StateFlow<SendLogState> = _sendLogState
    private val _isUserPro = MutableStateFlow(false)
    override val isUserPro: StateFlow<Boolean> = _isUserPro

    init {
        viewModelScope.launch(Dispatchers.IO) {
            userRepository.userInfo.collectLatest {
                _isUserPro.emit(it.isPro)
            }
        }
    }

    override fun sendLogClicked() {
        viewModelScope.launch(Dispatchers.IO) {
            _sendLogState.emit(SendLogState.Loading)
            val username = userRepository.user.value?.userName
            if (username.isNullOrEmpty()) {
                _sendLogState.emit(SendLogState.Failure)
                return@launch
            }
            try {
                val result = logRepository.onSendLog()
                when (result) {
                    is CallResult.Error -> {
                        _sendLogState.emit(SendLogState.Failure)
                    }

                    is CallResult.Success -> {
                        _sendLogState.emit(SendLogState.Success)
                    }
                }
            } catch (_: Exception) {
                _sendLogState.emit(SendLogState.Failure)
            }
        }
    }
}