package com.windscribe.mobile.ui

import androidx.annotation.DrawableRes
import androidx.lifecycle.ViewModel
import com.windscribe.vpn.autoconnection.AutoConnectionModeCallback
import com.windscribe.vpn.autoconnection.ProtocolInformation

abstract class DialogCallback {
    abstract fun onDismiss()
    abstract fun onConfirm()
}

data class DialogData(@DrawableRes val icon: Int, val title: String, val description: String, val okLabel: String)

abstract class AppStartActivityViewModel : ViewModel() {
    var protocolInformationList: List<ProtocolInformation>? = null
    var autoConnectionModeCallback: AutoConnectionModeCallback? = null
    var protocolInformation: ProtocolInformation? = null
    var dialogCallback: DialogCallback? = null
    var dialogData: DialogData? = null
    abstract fun setConnectionCallback(
        protocolInformationList: List<ProtocolInformation>,
        autoConnectionModeCallback: AutoConnectionModeCallback,
        protocolInformation: ProtocolInformation?
    )

    abstract fun setDialogCallback(data: DialogData, dialogCallback: DialogCallback)
}

class AppStartActivityViewModelImpl : AppStartActivityViewModel() {
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
}