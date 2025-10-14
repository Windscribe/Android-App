package com.windscribe.mobile.ui.preferences.debug

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.windscribe.vpn.Windscribe.Companion.appContext
import com.windscribe.vpn.constants.PreferencesKeyConstants
import com.windscribe.vpn.repository.AdvanceParameterRepository
import com.windscribe.vpn.repository.LogRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.io.File

abstract class DebugViewModel : ViewModel() {
    abstract val showProgress: StateFlow<Boolean>
    abstract val debugLog: StateFlow<List<String>>
}

class DebugViewModelImpl(val logRepository: LogRepository) :
    DebugViewModel() {
    private val _showProgress = MutableStateFlow(false)
    override val showProgress: StateFlow<Boolean> = _showProgress
    private val _debugLog = MutableStateFlow(emptyList<String>())
    override val debugLog: StateFlow<List<String>> = _debugLog

    init {
        load()
    }

    private fun load() {
        _showProgress.value = true
        viewModelScope.launch(Dispatchers.IO) {
            runCatching { logRepository.getPartialLog() }.onSuccess {
                _debugLog.emit(it)
                _showProgress.value = false
            }.onFailure {
                _showProgress.value = false
            }
        }
    }
}