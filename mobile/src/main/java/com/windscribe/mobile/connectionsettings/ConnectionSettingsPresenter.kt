/*
 * Copyright (c) 2021 Windscribe Limited.
 */
package com.windscribe.mobile.connectionsettings

import android.content.Context

interface ConnectionSettingsPresenter {
    fun init()
    fun onAllowLanClicked()
    fun onAutoFillPacketSizeClicked()
    fun onAutoStartOnBootClick()
    fun onConnectionModeAutoClicked()
    fun onConnectionModeManualClicked()
    fun onDestroy()
    fun onGpsSpoofingClick()
    fun onHotStart()
    fun onKeepAliveAutoModeClicked()
    fun onKeepAliveManualModeClicked()
    fun onManualLayoutSetupCompleted()
    fun onPacketSizeAutoModeClicked()
    fun onPacketSizeManualModeClicked()
    fun onPermissionProvided()
    fun onPortSelected(heading: String, port: String)
    fun onProtocolSelected(heading: String)
    fun onSplitTunnelingOptionClicked()
    fun onStart()
    fun saveKeepAlive(keepAlive: String)
    fun setKeepAlive(keepAlive: String)
    fun setPacketSize(size: String)
    fun setTheme(context: Context)
    fun onDecoyTrafficClick()
    fun turnOnDecoyTraffic()
    fun onFakeTrafficVolumeSelected(label: String)
    fun onNetworkOptionsClick()
    fun onAntiCensorshipClick()
}