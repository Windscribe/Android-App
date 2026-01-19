/*
 * Copyright (c) 2021 Windscribe Limited.
 */
package com.windscribe.vpn.apppreference

import com.windscribe.vpn.api.response.ServerCredentialsResponse
import com.windscribe.vpn.autoconnection.ProtocolConnectionStatus
import com.windscribe.vpn.decoytraffic.FakeTrafficVolume
import com.windscribe.vpn.localdatabase.tables.NetworkInfo
import com.windscribe.vpn.repository.WgLocalParams
import java.util.Date
import javax.inject.Singleton

@Singleton
interface PreferencesHelper {
    fun clearAllData()
    fun clearOldSessionAuth()
    fun getAccessIp(key: String): String?
    val isHapticFeedbackEnabled: Boolean
    var alcListString: String?
    var autoStartOnBoot: Boolean
    var blurIp: Boolean
    var blurNetworkName: Boolean
    var connectedFlagPath: String?
    var connectionStatus: String?
    val currentConnectionAttemptTag: String?
    fun getDeviceUUID(): String?
    val disConnectedFlagPath: String?
    var flagViewHeight: Int
    var flagViewWidth: Int
    var globalUserConnectionPreference: Boolean
    val iKEv2Port: String
    fun installedApps(): List<String>
    var wgConnectApiFailOverState: Map<String, Boolean>
    var keepAlive: String
    var lanByPass: Boolean
    var lastConnectedUsingSplit: Boolean
    val lastSelectedTabIndex: Int
    var latencyType: String
    var loginTime: Date?
    var lowestPingId: Int
    var migrationRequired: Boolean
    val multipleTunnel: Boolean
    var notificationStat: Boolean
    val oldSessionAuth: String?
    var packetSize: Int
    var pingTestRequired: Boolean
    val portMapVersion: Int
    fun getPreviousAccountStatus(userNameKey: String): Int
    fun getPreviousUserStatus(userNameKey: String): Int
    val purchaseFlowState: String
    val savedLanguage: String
    val savedProtocol: String
    val savedSTEALTHPort: String
    val savedWSTunnelPort: String
    val savedTCPPort: String
    val savedUDPPort: String
    var selectedCity: Int
    var selectedIp: String?
    var selectedPort: String
    var selectedProtocol: String
    var selectedProtocolType: ProtocolConnectionStatus
    var selectedTheme: String
    val selection: String
    var sessionHash: String?
    val showLatencyInMS: Boolean
    val splitRoutingMode: String
    var splitTunnelToggle: Boolean
    var showSystemApps: Boolean
    var userName: String
    var userStatus: Int
    var whitelistOverride: Boolean
    var whiteListedNetwork: String?
    val wireGuardPort: String
    var isAutoSecureOn : Boolean
    fun isConnectingToConfiguredLocation(): Boolean
    val isConnectingToStaticIp: Boolean
    var isCustomBackground: Boolean
    val isGpsSpoofingOn: Boolean
    var isKeepAliveModeAuto: Boolean
    val isKernelModuleDisabled: Boolean
    var isNewApplicationInstance: Boolean
    val isPackageSizeModeAuto: Boolean
    var isReconnecting: Boolean
    var isShowLocationHealthEnabled: Boolean
    var isStartedByAlwaysOn: Boolean
    var isDecoyTrafficOn: Boolean
    var isAntiCensorshipOn: Boolean
    var wgLocalParams: WgLocalParams?
    var autoConnect: Boolean
    var advanceParamText: String
    var wsNetSettings: String
    fun nextProtocol(protocol: String?)
    fun requiredReconnect(): Boolean
    fun saveIKEv2Port(port: String?)
    fun saveTCPPort(port: String?)
    fun saveUDPPort(port: String?)
    fun saveStealthPort(port: String?)
    fun saveWSTunnelPort(port: String?)
    fun saveInstalledApps(installedAppsSaved: List<String>)
    fun saveLastSelectedServerTabIndex(index: Int)
    fun savePortMapVersion(version: Int)
    fun savePurchaseFlowState(state: String?)
    fun saveCredentials(key: String, value: ServerCredentialsResponse)
    fun getCredentials(key: String): ServerCredentialsResponse?
    fun saveOpenVPNServerConfig(value: String)
    fun getOpenVPNServerConfig():String?
    fun saveSelection(selection: String?)
    fun saveSplitRoutingMode(mode: String?)
    fun saveProtocol(protocol: String?)
    fun saveWireGuardPort(port: String?)
    fun setAlwaysOn(status: Boolean)
    fun setAuthFailedConnectionAttemptCount(numberOfAttempts: Int?)
    fun setChosenProtocol(protocol: String?)
    fun setConnectingToConfiguredLocation(connectingToConfiguredLocation: Boolean)
    fun setConnectingToStaticIP(connectingToStaticIP: Boolean)
    fun setConnectionAttemptTag()
    fun setDeviceUUID(deviceUUID: String?)
    fun setDisconnectedFlagPath(path: String?)
    fun setFutureSelectCity(cityId: Int)
    fun setGpsSpoofing(spoof: Boolean)
    fun setHapticFeedbackEnabled(hapticFeedbackEnabled: Boolean)
    fun setOurIp(ip: Int)
    fun setPacketSizeModeToAuto(auto: Boolean)
    fun setPreviousAccountStatus(userNameKey: String, userAccountStatus: Int)
    fun setPreviousUserStatus(userNameKey: String, userStatus: Int)
    fun setReconnectRequired(required: Boolean)
    fun setShowNewsFeedAlert(showAlert: Boolean)
    fun setUserAccountUpdateRequired(required: Boolean)
    fun setUserIntendedDisconnect(userIntendedDisconnect: Boolean)
    fun userIsInGhostMode(): Boolean
    fun increaseConnectionCount()
    fun getConnectionCount(): Int
    fun getPowerWhiteListDialogCount(): Int
    fun setPowerWhiteListDialogCount(count: Int)

    var alreadyShownShareAppLink: Boolean
    var fakeTrafficVolume:FakeTrafficVolume
    var dnsMode: String
    var dnsAddress: String?
    var suggestedProtocol: String?
    var suggestedPort: String?
    fun getDefaultProtoInfo(): Pair<String, String>
    fun getDefaultNetworkInfo(networkName: String): NetworkInfo
    fun isSuggested(): Boolean
    var locationHash: String?
    fun addObserver(listener: OnPreferenceChangeListener)
    fun removeObserver(listener: OnPreferenceChangeListener)
    var whenDisconnectedBackgroundOption: Int
    var whenConnectedBackgroundOption: Int
    var backgroundAspectRatioOption: Int
    var disconnectedBundleBackgroundOption: Int
    var connectedBundleBackgroundOption: Int
    var customDisconnectedBackground: String?
    var customConnectedBackground: String?
    var whenDisconnectedSoundOption: Int
    var whenConnectedSoundOption: Int
    var disconnectedBundleSoundOption: Int
    var connectedBundleSoundOption: Int
    var customDisconnectedSound: String?
    var customConnectedSound: String?
    var isSsoLogin: Boolean
    var userIP: String?
    var connectionMode: String?
    var newInstallation: String?
    var getSession: String?
    var portMap: String?
    var robertFilters: String?
    var favoriteServerList: String?
    var purchasedItem: String?
    var amazonPurchasedItem: String?
    var rateDialogStatus: Int
    var rateDialogLastUpdateTime: String?
    fun setUserLanguage(language: String)
}
