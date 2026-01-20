package com.windscribe.mobile.ui.popup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.windscribe.vpn.apppreference.PreferencesHelper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.slf4j.LoggerFactory

abstract class PowerWhitelistViewmodel : ViewModel() {
    abstract fun onLaterClicked()
    abstract fun onNeverAskAgainClicked()
    abstract fun onPermissionResult(granted: Boolean)
    abstract val shouldExit: StateFlow<Boolean>
}

class PowerWhitelistViewmodelImpl(private val preferenceHelper: PreferencesHelper) :
    PowerWhitelistViewmodel() {
    private val _shouldExit = MutableStateFlow(false)
    override val shouldExit = _shouldExit.asStateFlow()
    private val logger = LoggerFactory.getLogger("basic")

    override fun onPermissionResult(granted: Boolean) {
        viewModelScope.launch {
            logger.info("PowerWhitelist permission result: $granted")
            _shouldExit.emit(true)
        }
    }

    override fun onLaterClicked() {
        viewModelScope.launch {
            val count = preferenceHelper.powerWhiteListDialogCount
            preferenceHelper.powerWhiteListDialogCount = count + 1
            _shouldExit.emit(true)
        }
    }

    override fun onNeverAskAgainClicked() {
        viewModelScope.launch {
            preferenceHelper.powerWhiteListDialogCount = 3
            _shouldExit.emit(true)
        }
    }
}