/*
 * Copyright (c) 2021 Windscribe Limited.
 */
package com.windscribe.mobile.connectionsettings

import android.os.Build
import androidx.annotation.RequiresApi

interface ConnectionSettingsView {
    fun gotoSplitTunnelingSettings()
    fun goToNetworkSecurity()
    fun openGpsSpoofSettings()
    fun packetSizeDetectionProgress(progress: Boolean)
    fun setAutoStartOnBootToggle(toggleDrawable: Int)
    fun setGpsSpoofingToggle(toggleDrawable: Int)
    fun setKeepAlive(keepAlive: String)
    fun setLanBypassToggle(toggleDrawable: Int)
    fun setPacketSize(size: String)
    fun setSplitTunnelText(onOff: String, color: Int)
    fun setupConnectionModeAdapter(savedValue: String, connectionModes: Array<String>)
    fun setupPacketSizeModeAdapter(savedValue: String, types: Array<String>)
    fun setKeepAliveModeAdapter(savedValue: String, types: Array<String>)
    fun setupPortMapAdapter(port: String, portMap: List<String>)
    fun setupProtocolAdapter(protocol: String, protocols: Array<String>)
    fun showGpsSpoofing()
    fun showToast(toastString: String)
    fun setDecoyTrafficToggle(toggleDrawable: Int)
    fun showExtraDataUseWarning()
    fun setupFakeTrafficVolumeAdapter(selectedValue: String, values: Array<String>)
    fun setPotentialTrafficUse(value: String)
    fun showAutoStartOnBoot()
    fun setKeepAliveContainerVisibility(isAutoKeepAlive: Boolean)
    fun setAntiCensorshipToggle(toggleDrawable: Int)
    fun setAutoConnectToggle(toggleDrawable: Int)
    fun setupDNSModeAdapter(savedValue: String, dnsModes: Array<String>)
    fun setCustomDnsAddress(dnsAddress: String)
    fun setPowerWhitelistToggle(toggleDrawable: Int)
    @RequiresApi(Build.VERSION_CODES.M)
    fun launchBatteryOptimizationActivity()
}