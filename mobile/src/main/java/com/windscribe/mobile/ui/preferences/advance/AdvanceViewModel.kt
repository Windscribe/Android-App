package com.windscribe.mobile.ui.preferences.advance

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.windscribe.mobile.advance.AdvanceParamView
import com.windscribe.vpn.apppreference.PreferencesHelper
import com.windscribe.vpn.repository.AdvanceParameterRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

abstract class AdvanceViewModel : ViewModel() {
    abstract val showProgress: StateFlow<Boolean>
    abstract val parameters: StateFlow<String>
    abstract val toastMessage: SharedFlow<String>
    abstract fun saveAdvanceParams(text: String)
    abstract fun clearAdvanceParams()
    abstract fun updateParams(params: String)
}

class AdvanceViewModelImpl(
    val preferencesHelper: PreferencesHelper,
    val advanceParameterRepository: AdvanceParameterRepository
) : AdvanceViewModel() {
    private val _showProgress = MutableStateFlow(false)
    override val showProgress: StateFlow<Boolean> = _showProgress
    private val _parameters = MutableStateFlow(preferencesHelper.advanceParamText)
    override val parameters: StateFlow<String> = _parameters
    private val _toastMessage = MutableSharedFlow<String>(replay = 0)
    override val toastMessage: SharedFlow<String> = _toastMessage


    init {
        viewModelScope.launch(Dispatchers.IO) {
            _parameters.emit(preferencesHelper.advanceParamText)
        }
    }

    override fun saveAdvanceParams(text: String) {
        viewModelScope.launch {
            val lineCount = text.split("\n").count { it.isNotEmpty() }
            if (lineCount == 0 || text.isEmpty()) {
                _toastMessage.emit("Nothing to save!! Please add at least 1 key=value pair.")
                return@launch
            }
            val lines = text.split("\n").filter { it.isNotEmpty() }
            val invalidLines = lines.filter {
                val kv = it.split("=")
                kv.count() != 2 || (kv.count() == 2 && (kv[0].isEmpty() || kv[1].isEmpty()))
            }
            if (invalidLines.isNotEmpty()) {
                val error =
                    invalidLines.joinToString(prefix = "Invalid key/value: ", separator = ",")
                _toastMessage.emit(error)
                return@launch
            }
            preferencesHelper.advanceParamText = text
            _parameters.emit(text)
            _toastMessage.emit("Saved successfully.")
            advanceParameterRepository.reload()
        }
    }

    override fun clearAdvanceParams() {
        viewModelScope.launch(Dispatchers.IO) {
            preferencesHelper.advanceParamText = ""
            _parameters.emit("")
            advanceParameterRepository.reload()
        }
    }

    override fun updateParams(params: String) {
        viewModelScope.launch(Dispatchers.IO) {
            _parameters.emit(params)
        }
    }
}