package com.windscribe.mobile.ui.preferences.debug

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.windscribe.vpn.commonutils.LogLevel
import com.windscribe.vpn.commonutils.LogRecord
import com.windscribe.vpn.commonutils.matches
import com.windscribe.vpn.commonutils.parseLogLines
import com.windscribe.vpn.repository.LogRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

abstract class DebugViewModel : ViewModel() {
    abstract val showProgress: StateFlow<Boolean>
    abstract val debugLog: StateFlow<List<LogRecord>>
    abstract val minLevel: StateFlow<LogLevel>
    abstract val query: StateFlow<String>

    abstract val levelsAvailable: StateFlow<Boolean>

    abstract fun onMinLevelChanged(level: LogLevel)

    abstract fun onQueryChanged(query: String)
}

@HiltViewModel
class DebugViewModelImpl
    @Inject
    constructor(
        val logRepository: LogRepository,
    ) : DebugViewModel() {
        private val _showProgress = MutableStateFlow(false)
        override val showProgress: StateFlow<Boolean> = _showProgress
        private val _records = MutableStateFlow(emptyList<LogRecord>())
        private val _minLevel = MutableStateFlow(LogLevel.DEBUG)
        override val minLevel: StateFlow<LogLevel> = _minLevel
        private val _query = MutableStateFlow("")
        override val query: StateFlow<String> = _query

        override val levelsAvailable: StateFlow<Boolean> =
            _records
                .map { records -> records.any { it.raw == null } }
                .stateIn(viewModelScope, SharingStarted.Eagerly, false)

        override val debugLog: StateFlow<List<LogRecord>> =
            combine(_records, _minLevel, _query) { records, level, query ->
                records.filter { it.matches(level, query) }
            }.flowOn(Dispatchers.Default)
                .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

        init {
            load()
        }

        override fun onMinLevelChanged(level: LogLevel) {
            _minLevel.value = level
        }

        override fun onQueryChanged(query: String) {
            _query.value = query
        }

        private fun load() {
            _showProgress.value = true
            viewModelScope.launch(Dispatchers.IO) {
                runCatching { parseLogLines(logRepository.getPartialLog()) }
                    .onSuccess {
                        _records.emit(it)
                        _showProgress.value = false
                    }.onFailure {
                        _showProgress.value = false
                    }
            }
        }
    }
