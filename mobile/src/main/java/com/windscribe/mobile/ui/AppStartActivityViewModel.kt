package com.windscribe.mobile.ui

import androidx.annotation.DrawableRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.windscribe.vpn.api.ApiCallManager
import com.windscribe.vpn.api.IApiCallManager
import com.windscribe.vpn.apppreference.PreferencesHelper
import com.windscribe.vpn.autoconnection.AutoConnectionModeCallback
import com.windscribe.vpn.autoconnection.ProtocolInformation
import com.windscribe.vpn.commonutils.Ext.result
import com.windscribe.vpn.apppreference.PreferencesKeyConstants
import com.windscribe.vpn.repository.CallResult
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.collectLatest
import org.slf4j.LoggerFactory

abstract class DialogCallback {
    abstract fun onDismiss()
    abstract fun onConfirm()
}

data class DialogData(
    @DrawableRes val icon: Int,
    val title: String,
    val description: String,
    val okLabel: String
)

abstract class AppStartActivityViewModel : ViewModel() {
    var protocolInformationList: List<ProtocolInformation>? = null
    var autoConnectionModeCallback: AutoConnectionModeCallback? = null
    var protocolInformation: ProtocolInformation? = null
    var dialogCallback: DialogCallback? = null
    var dialogData: DialogData? = null

    abstract val hapticFeedback: StateFlow<Boolean>
    abstract fun enableDecoyTraffic()
    abstract fun enableGpsSpoofing()
    abstract fun setConnectionCallback(
        protocolInformationList: List<ProtocolInformation>,
        autoConnectionModeCallback: AutoConnectionModeCallback,
        protocolInformation: ProtocolInformation?
    )

    abstract fun setDialogCallback(data: DialogData, dialogCallback: DialogCallback)
}

class AppStartActivityViewModelImpl(val preferencesHelper: PreferencesHelper, val apiCalManager: IApiCallManager) :
    AppStartActivityViewModel() {
    private var _hapticFeedback = MutableStateFlow(preferencesHelper.isHapticFeedbackEnabled)
    override val hapticFeedback: StateFlow<Boolean> = _hapticFeedback
    private val logger = LoggerFactory.getLogger("ui")

    init {
        observePreferences()
        recordInstall()
    }

    private fun observePreferences() {
        viewModelScope.launch {
            preferencesHelper.isHapticFeedbackEnabledFlow.collectLatest { isEnabled ->
                _hapticFeedback.value = isEnabled
            }
        }
    }

    private fun recordInstall() {
       viewModelScope.launch {
           delay(500)
           if (preferencesHelper.isNewApplicationInstance) {
               val installation = preferencesHelper.newInstallation
               if (PreferencesKeyConstants.I_NEW == installation) {
                   val result = result<String?> { apiCalManager.recordAppInstall() }
                   when(result) {
                       is CallResult.Success -> {
                           logger.info("App install recorded successfully.")
                           preferencesHelper.isNewApplicationInstance = false
                           preferencesHelper.newInstallation = PreferencesKeyConstants.I_OLD
                       }
                       is CallResult.Error -> {
                           logger.error("Failed to record app install: ${result.errorMessage}")
                       }
                   }
               }
           }
       }
    }
    override fun setConnectionCallback(
        protocolInformationList: List<ProtocolInformation>,
        autoConnectionModeCallback: AutoConnectionModeCallback,
        protocolInformation: ProtocolInformation?
    ) {
        this.protocolInformationList = protocolInformationList
        this.autoConnectionModeCallback = autoConnectionModeCallback
        this.protocolInformation = protocolInformation
    }

    override fun setDialogCallback(data: DialogData, dialogCallback: DialogCallback) {
        this.dialogData = data
        this.dialogCallback = dialogCallback
    }

    override fun enableDecoyTraffic() {
        preferencesHelper.isDecoyTrafficOn = true
    }

    override fun enableGpsSpoofing() {
        preferencesHelper.isGpsSpoofingOn = true
    }
}