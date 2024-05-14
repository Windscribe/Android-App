/*
 * Copyright (c) 2021 Windscribe Limited.
 */
package com.windscribe.vpn.apppreference

import com.windscribe.vpn.api.response.NewsFeedNotification
import com.windscribe.vpn.api.response.ServerCredentialsResponse
import com.windscribe.vpn.autoconnection.ProtocolConnectionStatus
import com.windscribe.vpn.decoytraffic.FakeTrafficVolume
import com.windscribe.vpn.repository.WgLocalParams
import io.reactivex.Single
import java.util.*
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
    fun getDeviceUUID(username: String): String?
    val disConnectedFlagPath: String?
    var flagViewHeight: Int
    var flagViewWidth: Int
    var globalUserConnectionPreference: Boolean
    val iKEv2Port: String
    val installedApps: Single<List<String>>
    fun installedApps(): List<String>
    var wgConnectApiFailOverState: Map<String, Boolean>
    var keepAlive: String
    var lanByPass: Boolean
    var lastConnectedUsingSplit: Boolean
    val lastSelectedTabIndex: Int
    var latencyType: String
    val loginTime: Date
    var lowestPingId: Int
    var migrationRequired: Boolean
    val multipleTunnel: Boolean
    var notificationStat: Boolean
    val notifications: Single<NewsFeedNotification>
    val oldSessionAuth: String?
    var packetSize: Int
    var pingTestRequired: Boolean
    val portMapVersion: Int
    fun getPreviousAccountStatus(userNameKey: String): Int
    fun getPreviousUserStatus(userNameKey: String): Int
    val purchaseFlowState: String
    fun getResponseInt(key: String, defaultValue: Int): Int
    fun getResponseString(key: String): String?
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
    var userName: String
    var userStatus: Int
    var whitelistOverride: Boolean
    val wireGuardPort: String
    var isAutoSecureOn : Boolean
    fun isConnectingToConfiguredLocation(): Boolean
    val isConnectingToStaticIp: Boolean
    var isCustomBackground: Boolean
    val isGpsSpoofingOn: Boolean
    var isKeepAliveModeAuto: Boolean
    val isKernelModuleDisabled: Boolean
    var isNewApplicationInstance: Boolean
    fun isNotificationAlreadyShown(notificationId: String): Boolean
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
    fun removeResponseData(key: String?)
    fun requiredReconnect(): Boolean
    fun saveIKEv2Port(port: String?)
    fun saveInstalledApps(installedAppsSaved: List<String>)
    fun saveLastSelectedServerTabIndex(index: Int)
    fun saveNotificationId(notificationId: String)
    fun savePortMapVersion(version: Int)
    fun savePurchaseFlowState(state: String?)
    fun saveResponseIntegerData(key: String, value: Int)
    fun saveResponseStringData(key: String, value: String)
    fun saveCredentials(key: String, value: ServerCredentialsResponse)
    fun getCredentials(key: String): ServerCredentialsResponse?
    fun saveOpenVPNServerConfig(value: String)
    fun getOpenVPNServerConfig():String?
    fun saveSelection(selection: String?)
    fun saveSplitRoutingMode(mode: String?)
    fun saveWireGuardPort(port: String?)
    fun setAlwaysOn(status: Boolean)
    fun setAuthFailedConnectionAttemptCount(numberOfAttempts: Int?)
    fun setChosenProtocol(protocol: String?)
    fun setConnectingToConfiguredLocation(connectingToConfiguredLocation: Boolean)
    fun setConnectingToStaticIP(connectingToStaticIP: Boolean)
    fun setConnectionAttemptTag()
    fun setDeviceUUID(userName: String, deviceUUID: String?)
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
    fun setStaticAccessIp(key: String, staticAccessIp: String?)
    fun setUserAccountUpdateRequired(required: Boolean)
    fun setUserIntendedDisconnect(userIntendedDisconnect: Boolean)
    fun userIsInGhostMode(): Boolean
    var alreadyShownShareAppLink: Boolean
    var fakeTrafficVolume:FakeTrafficVolume
    var dnsMode: String
    var dnsAddress: String?
}
