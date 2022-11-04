package com.windscribe.vpn.autoconnection

interface AutoConnectionModeCallback {
    fun onCancel()
    fun onProtocolSelect(protocolInformation: ProtocolInformation) {}
    fun onSendLogClicked() {}
    fun onContactSupportClick() {}
    fun onSetAsPreferredClicked() {}
}