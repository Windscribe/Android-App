package com.windscribe.mobile.ui

import androidx.annotation.DrawableRes
import androidx.lifecycle.ViewModel
import com.windscribe.vpn.apppreference.PreferencesHelper
import com.windscribe.vpn.autoconnection.AutoConnectionModeCallback
import com.windscribe.vpn.autoconnection.ProtocolInformation
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import net.grandcentrix.tray.core.OnTrayPreferenceChangeListener

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

class AppStartActivityViewModelImpl(val preferencesHelper: PreferencesHelper) :
    AppStartActivityViewModel() {
    private var _hapticFeedback = MutableStateFlow(preferencesHelper.isHapticFeedbackEnabled)
    override val hapticFeedback: StateFlow<Boolean> = _hapticFeedback

    private val trayPreferenceChangeListener = OnTrayPreferenceChangeListener {
        _hapticFeedback.value = preferencesHelper.isHapticFeedbackEnabled
    }

    init {
        preferencesHelper.addObserver(trayPreferenceChangeListener)
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
        preferencesHelper.setGpsSpoofing(true)
    }

    override fun onCleared() {
        preferencesHelper.removeObserver(trayPreferenceChangeListener)
        super.onCleared()
    }
}