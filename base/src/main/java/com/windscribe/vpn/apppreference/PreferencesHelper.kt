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
    fun addObserver(listener: OnPreferenceChangeListener)
    fun removeObserver(listener: OnPreferenceChangeListener)
    fun clearAllData()
    fun clearOldSessionAuth()
    fun getPreviousAccountStatus(userNameKey: String): Int
    fun getPreviousUserStatus(userNameKey: String): Int
    fun setPreviousAccountStatus(userNameKey: String, userAccountStatus: Int)
    fun setPreviousUserStatus(userNameKey: String, userStatus: Int)
    fun getDefaultProtoInfo(): Pair<String, String>
    fun getDefaultNetworkInfo(networkName: String): NetworkInfo
    fun isSuggested(): Boolean
    fun userIsInGhostMode(): Boolean
    fun increaseConnectionCount()
    fun getConnectionCount(): Int
    var isHapticFeedbackEnabled: Boolean
    var alcListString: String?
    var autoStartOnBoot: Boolean
    var blurIp: Boolean
    var blurNetworkName: Boolean
    var connectedFlagPath: String?
    var connectionStatus: String?
    val currentConnectionAttemptTag: String?
    val disConnectedFlagPath: String?
    var flagViewHeight: Int
    var flagViewWidth: Int
    var globalUserConnectionPreference: Boolean
    var iKEv2Port: String
    var installedApps: List<String>
    var wgConnectApiFailOverState: Map<String, Boolean>
    var keepAlive: String
    var lanByPass: Boolean
    var lastConnectedUsingSplit: Boolean
    var lastSelectedTabIndex: Int
    var latencyType: String
    var loginTime: Date?
    var lowestPingId: Int
    var migrationRequired: Boolean
    val multipleTunnel: Boolean
    var notificationStat: Boolean
    val oldSessionAuth: String?
    var packetSize: Int
    var pingTestRequired: Boolean
    var portMapVersion: Int
    var purchaseFlowState: String
    var savedLanguage: String
    var savedProtocol: String
    var savedSTEALTHPort: String
    var savedWSTunnelPort: String
    var savedTCPPort: String
    var savedUDPPort: String
    var selectedCity: Int
    var selectedIp: String?
    var selectedPort: String
    var selectedProtocol: String
    var selectedProtocolType: ProtocolConnectionStatus
    var selectedTheme: String
    var selection: String
    var sessionHash: String?
    val showLatencyInMS: Boolean
    var splitRoutingMode: String
    var splitTunnelToggle: Boolean
    var showSystemApps: Boolean
    var userName: String
    var userStatus: Int
    var whitelistOverride: Boolean
    var whiteListedNetwork: String?
    var wireGuardPort: String
    var deviceUuid: String?
    var powerWhiteListDialogCount: Int
    var isAutoSecureOn: Boolean
    var isConnectingToConfigured: Boolean
    var isConnectingToStaticIp: Boolean
    var isCustomBackground: Boolean
    var isGpsSpoofingOn: Boolean
    var isKeepAliveModeAuto: Boolean
    val isKernelModuleDisabled: Boolean
    var isNewApplicationInstance: Boolean
    var isPackageSizeModeAuto: Boolean
    var isReconnecting: Boolean
    var isShowLocationHealthEnabled: Boolean
    var isStartedByAlwaysOn: Boolean
    var isDecoyTrafficOn: Boolean
    var isAntiCensorshipOn: Boolean
    var wgLocalParams: WgLocalParams?
    var autoConnect: Boolean
    var advanceParamText: String
    var wsNetSettings: String

    // OpenVPN Credentials
    var openVpnCredentials: ServerCredentialsResponse?

    // IKEv2 Credentials
    var ikev2Credentials: ServerCredentialsResponse?

    // Static IP Credentials
    var staticIpCredentials: ServerCredentialsResponse?

    // OpenVPN Server Config (base64 encoded)
    var openVpnServerConfig: String?
    var alreadyShownShareAppLink: Boolean
    var fakeTrafficVolume: FakeTrafficVolume
    var dnsMode: String
    var dnsAddress: String?
    var suggestedProtocol: String?
    var suggestedPort: String?
    var locationHash: String?
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
    var customIcon: String
}
