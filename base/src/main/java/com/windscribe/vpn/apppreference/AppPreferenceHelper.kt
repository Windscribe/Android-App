/*
 * Copyright (c) 2021 Windscribe Limited.
 */
package com.windscribe.vpn.apppreference

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.windscribe.vpn.Windscribe.Companion.appContext
import com.windscribe.vpn.api.response.ServerCredentialsResponse
import com.windscribe.vpn.autoconnection.ProtocolConnectionStatus
import com.windscribe.vpn.constants.PreferencesKeyConstants
import com.windscribe.vpn.constants.PreferencesKeyConstants.ADVANCE_PARAM_TEXT
import com.windscribe.vpn.constants.PreferencesKeyConstants.ALREADY_SHOWN_SHARE_APP_LINK
import com.windscribe.vpn.constants.PreferencesKeyConstants.ANTI_CENSORSHIP
import com.windscribe.vpn.constants.PreferencesKeyConstants.AUTO_CONNECT
import com.windscribe.vpn.constants.PreferencesKeyConstants.AUTO_SECURE_NEW_NETWORKS
import com.windscribe.vpn.constants.PreferencesKeyConstants.CUSTOM_DNS_ADDRESS
import com.windscribe.vpn.constants.PreferencesKeyConstants.DECOY_TRAFFIC
import com.windscribe.vpn.constants.PreferencesKeyConstants.DEFAULT_IKEV2_PORT
import com.windscribe.vpn.constants.PreferencesKeyConstants.DEFAULT_WIRE_GUARD_PORT
import com.windscribe.vpn.constants.PreferencesKeyConstants.DNS_MODE
import com.windscribe.vpn.constants.PreferencesKeyConstants.DNS_MODE_ROBERT
import com.windscribe.vpn.constants.PreferencesKeyConstants.FAKE_TRAFFIC_VOLUME
import com.windscribe.vpn.constants.PreferencesKeyConstants.LOCATION_HASH
import com.windscribe.vpn.constants.PreferencesKeyConstants.SUGGESTED_PORT
import com.windscribe.vpn.constants.PreferencesKeyConstants.SUGGESTED_PROTOCOL
import com.windscribe.vpn.constants.PreferencesKeyConstants.WG_CONNECT_API_FAIL_OVER_STATE
import com.windscribe.vpn.constants.PreferencesKeyConstants.WG_LOCAL_PARAMS
import com.windscribe.vpn.constants.PreferencesKeyConstants.WHITELISTED_NETWORK
import com.windscribe.vpn.constants.PreferencesKeyConstants.WS_NET_SETTINGS
import com.windscribe.vpn.constants.VpnPreferenceConstants
import com.windscribe.vpn.decoytraffic.FakeTrafficVolume
import com.windscribe.vpn.localdatabase.tables.NetworkInfo
import com.windscribe.vpn.repository.WgLocalParams
import io.reactivex.Single
import net.grandcentrix.tray.AppPreferences
import net.grandcentrix.tray.core.OnTrayPreferenceChangeListener
import java.util.Date
import javax.inject.Singleton

@Singleton
class AppPreferenceHelper(
    private val preference: AppPreferences,
    private val securePreferences: SecurePreferences
) : PreferencesHelper {
    override fun clearAllData() {
        val installation = getResponseString(PreferencesKeyConstants.NEW_INSTALLATION)
        preference.clear()
        securePreferences.clear()
        if (PreferencesKeyConstants.I_OLD == installation) {
           saveResponseStringData(PreferencesKeyConstants.NEW_INSTALLATION, PreferencesKeyConstants.I_OLD)
        }
    }

    override fun clearOldSessionAuth() {
        preference.remove(PreferencesKeyConstants.SESSION_HASH)
    }

    override fun getAccessIp(key: String): String? {
        return preference.getString(key, null)
    }

    override var alcListString: String?
        get() = preference.getString(PreferencesKeyConstants.ALC_LIST, null)
        set(alcListString) {
            preference.put(PreferencesKeyConstants.ALC_LIST, alcListString)
        }
    override var autoStartOnBoot: Boolean
        get() = preference.getBoolean(PreferencesKeyConstants.AUTO_START_ON_BOOT, false)
        set(autoStartOnBoot) {
            preference.put(PreferencesKeyConstants.AUTO_START_ON_BOOT, autoStartOnBoot)
        }
    private val bestLocationIp2: String?
        get() = preference.getString(PreferencesKeyConstants.BEST_LOCATION_IP_2, null)
    override var blurIp: Boolean
        get() = preference.getBoolean(PreferencesKeyConstants.BLUR_IP, false)
        set(blurIp) {
            preference.put(PreferencesKeyConstants.BLUR_IP, blurIp)
        }
    override var blurNetworkName: Boolean
        get() = preference.getBoolean(PreferencesKeyConstants.BLUR_NETWORK_NAME, false)
        set(blurNetworkName) {
            preference.put(PreferencesKeyConstants.BLUR_NETWORK_NAME, blurNetworkName)
        }
    override var connectedFlagPath: String?
        get() = preference.getString(PreferencesKeyConstants.CONNECTED_FLAG_PATH, null)
        set(path) {
            preference.put(PreferencesKeyConstants.CONNECTED_FLAG_PATH, path)
        }
    override var connectionStatus: String?
        get() = preference.getString(
            PreferencesKeyConstants.CONNECTION_STATUS,
            PreferencesKeyConstants.VPN_DISCONNECTED
        )
        set(connectionStatus) {
            preference.put(PreferencesKeyConstants.CONNECTION_STATUS, connectionStatus)
        }
    override val currentConnectionAttemptTag: String?
        get() = preference.getString(PreferencesKeyConstants.CONNECTION_ATTEMPT, null)

    override fun getDeviceUUID(): String? {
        return preference.getString(PreferencesKeyConstants.DEVICE_ID, null)
    }

    override val disConnectedFlagPath: String?
        get() = preference.getString(PreferencesKeyConstants.DISCONNECTED_FLAG_PATH, null)

    override var flagViewHeight: Int
        get() = preference.getInt(PreferencesKeyConstants.FLAG_VIEW_HEIGHT, 745)
        set(height) {
            preference.put(PreferencesKeyConstants.FLAG_VIEW_HEIGHT, height)
        }
    override var flagViewWidth: Int
        get() = preference.getInt(PreferencesKeyConstants.FLAG_VIEW_WIDTH, 1080)
        set(width) {
            preference.put(PreferencesKeyConstants.FLAG_VIEW_WIDTH, width)
        }
    override var globalUserConnectionPreference: Boolean
        get() = preference.getBoolean(
            PreferencesKeyConstants.GLOBAL_CONNECTION_PREFERENCE,
            false
        )
        set(connectionPreference) {
            preference.put(
                PreferencesKeyConstants.GLOBAL_CONNECTION_PREFERENCE,
                connectionPreference
            )
        }
    override val iKEv2Port: String
        get() = preference.getString(PreferencesKeyConstants.SAVED_IKev2_PORT, DEFAULT_IKEV2_PORT)
            ?: DEFAULT_IKEV2_PORT
    override val installedApps: Single<List<String>>
        get() {
            val jsonString =
                preference.getString(PreferencesKeyConstants.INSTALLED_APPS_DATA, null)
            return if (jsonString != null) {
                Single.fromCallable {
                    Gson().fromJson(
                        jsonString,
                        object :
                            TypeToken<List<String?>?>() {}.type
                    )
                }
            } else {
                Single.fromCallable { ArrayList() }
            }
        }
    override var keepAlive: String
        get() = preference.getString(PreferencesKeyConstants.KEEP_ALIVE, "20") ?: "20"
        set(keepAlive) {
            preference.put(PreferencesKeyConstants.KEEP_ALIVE, keepAlive)
        }
    override var lanByPass: Boolean
        get() = preference.getBoolean(PreferencesKeyConstants.LAN_BY_PASS, false)
        set(bypass) {
            preference.put(PreferencesKeyConstants.LAN_BY_PASS, bypass)
        }
    override var lastConnectedUsingSplit: Boolean
        get() = preference.getBoolean(
            PreferencesKeyConstants.LAST_CONNECTION_USING_SPLIT,
            false
        )
        set(isSplit) {
            preference.put(PreferencesKeyConstants.LAST_CONNECTION_USING_SPLIT, isSplit)
        }
    override val lastSelectedTabIndex: Int
        get() = preference.getInt(PreferencesKeyConstants.LAST_SELECTED_SERVER_TAB, 0)
    override var latencyType: String
        get() = if (showLatencyInMS) "Ms" else "Bars"
        set(latencyType) {
            val showInMs = latencyType == "Ms"
            showLatencyInMS = showInMs
        }

    override var loginTime: Date?
        get() {
            val time = preference.getLong(PreferencesKeyConstants.LOGIN_TIME, -1L)
            return if (time == -1L) {
                null
            } else {
                Date(time)
            }
        }
        set(value) {
            if (value != null) {
                preference.put(PreferencesKeyConstants.LOGIN_TIME, value.time)
            }
        }
    override var lowestPingId: Int
        get() = preference.getInt(PreferencesKeyConstants.LOWEST_PING_ID, -1)
        set(lowestPingId) {
            preference.put(PreferencesKeyConstants.LOWEST_PING_ID, lowestPingId)
        }

    override var migrationRequired: Boolean
        get() = preference.getBoolean("migration_required", true)
        set(required) {
            preference.put("migration_required", required)
        }
    override val multipleTunnel: Boolean
        get() = preference.getBoolean(PreferencesKeyConstants.MULTIPLE_TUNNELS, false)
    override var notificationStat: Boolean
        get() = preference.getBoolean(PreferencesKeyConstants.NOTIFICATION_STAT, false)
        set(show) {
            preference.put(PreferencesKeyConstants.NOTIFICATION_STAT, show)
        }
    override val oldSessionAuth: String?
        get() = preference.getString(PreferencesKeyConstants.SESSION_HASH, null)
    override var packetSize: Int
        get() = preference.getInt(PreferencesKeyConstants.LAST_MTU_VALUE, 1500)
        set(mtuValue) {
            preference.put(PreferencesKeyConstants.LAST_MTU_VALUE, mtuValue)
        }
    override var pingTestRequired: Boolean
        get() = preference.getBoolean(PreferencesKeyConstants.PING_UPDATE_REQUIRED, true)
        set(required) {
            preference.put(PreferencesKeyConstants.PING_UPDATE_REQUIRED, required)
        }
    override val portMapVersion: Int
        get() = preference.getInt(PreferencesKeyConstants.PORT_MAP_VERSION, 0)

    override fun getPreviousAccountStatus(userNameKey: String): Int {
        return preference.getInt(
            userNameKey + PreferencesKeyConstants.PREVIOUS_ACCOUNT_STATUS,
            -1
        )
    }

    override fun getPreviousUserStatus(userNameKey: String): Int {
        return preference.getInt(userNameKey + PreferencesKeyConstants.PREVIOUS_USER_STATUS, -1)
    }

    override val purchaseFlowState: String
        get() = preference.getString(
            PreferencesKeyConstants.PURCHASE_FLOW_STATE_KEY, "FINISHED"
        ) ?: "FINISHED"

    override fun getResponseInt(key: String, defaultValue: Int): Int {
        return preference.getInt(key, defaultValue)
    }

    override fun getResponseString(key: String): String? {
        return if (key == PreferencesKeyConstants.CONNECTION_MODE_KEY) {
            preference.getString(key, PreferencesKeyConstants.CONNECTION_MODE_AUTO)
        } else preference.getString(key, null)
    }

    override val savedLanguage: String
        get() = preference.getString(
            PreferencesKeyConstants.USER_LANGUAGE,
            appContext.getAppSupportedSystemLanguage()
        )
            ?: appContext.getAppSupportedSystemLanguage()
    override val savedProtocol: String
        get() = preference.getString(
            PreferencesKeyConstants.PROTOCOL_KEY,
            getDefaultProtoInfo().first
        ) ?: getDefaultProtoInfo().first
    override val savedSTEALTHPort: String
        get() = preference.getString(
            PreferencesKeyConstants.SAVED_STEALTH_PORT,
            PreferencesKeyConstants.DEFAULT_STEALTH_LEGACY_PORT
        )
            ?: PreferencesKeyConstants.DEFAULT_STEALTH_LEGACY_PORT
    override val savedWSTunnelPort: String
        get() = preference.getString(
            PreferencesKeyConstants.SAVED_WS_TUNNEL_PORT,
            PreferencesKeyConstants.DEFAULT_WS_TUNNEL_LEGACY_PORT
        )
            ?: PreferencesKeyConstants.DEFAULT_WS_TUNNEL_LEGACY_PORT
    override val savedTCPPort: String
        get() = preference.getString(
            PreferencesKeyConstants.SAVED_TCP_PORT,
            PreferencesKeyConstants.DEFAULT_TCP_LEGACY_PORT
        )
            ?: PreferencesKeyConstants.DEFAULT_TCP_LEGACY_PORT
    override val savedUDPPort: String
        get() = preference.getString(
            PreferencesKeyConstants.SAVED_UDP_PORT,
            PreferencesKeyConstants.DEFAULT_UDP_LEGACY_PORT
        )
            ?: PreferencesKeyConstants.DEFAULT_UDP_LEGACY_PORT
    override var selectedCity: Int
        get() = preference.getInt(PreferencesKeyConstants.SELECTED_CITY_ID, -1)
        set(cityId) {
            preference.put(PreferencesKeyConstants.SELECTED_CITY_ID, cityId)
        }
    override var selectedIp: String?
        get() = preference.getString(VpnPreferenceConstants.SELECTED_IP, bestLocationIp2)
        set(selectedIp) {
            preference.put(VpnPreferenceConstants.SELECTED_IP, selectedIp)
        }
    override var selectedPort: String
        get() = preference.getString(VpnPreferenceConstants.SELECTED_PORT, DEFAULT_IKEV2_PORT)
            ?: DEFAULT_IKEV2_PORT
        set(selectedPort) {
            preference.put(VpnPreferenceConstants.SELECTED_PORT, selectedPort)
        }
    override var selectedProtocol: String
        get() = preference.getString(
            VpnPreferenceConstants.SELECTED_PROTOCOL,
            getDefaultProtoInfo().first
        ) ?: getDefaultProtoInfo().first
        set(selectedProtocol) {
            preference.put(VpnPreferenceConstants.SELECTED_PROTOCOL, selectedProtocol)
        }
    override var selectedProtocolType: ProtocolConnectionStatus
        get() = preference.getString(
            VpnPreferenceConstants.SELECTED_PROTOCOL_TYPE,
            ProtocolConnectionStatus.Disconnected.name
        )?.let {
            ProtocolConnectionStatus.valueOf(
                it
            )
        } ?: ProtocolConnectionStatus.Disconnected
        set(type) {
            preference.put(VpnPreferenceConstants.SELECTED_PROTOCOL_TYPE, type.name)
        }
    override var selectedTheme: String
        get() = preference.getString(
            PreferencesKeyConstants.SELECTED_THEME,
            PreferencesKeyConstants.DARK_THEME
        )
            ?: PreferencesKeyConstants.DARK_THEME
        set(theme) {
            preference.put(PreferencesKeyConstants.SELECTED_THEME, theme)
        }
    override val selection: String
        get() = preference.getString(
            PreferencesKeyConstants.SELECTION_KEY,
            PreferencesKeyConstants.DEFAULT_LIST_SELECTION_MODE
        )
            ?: PreferencesKeyConstants.DEFAULT_LIST_SELECTION_MODE
    override var sessionHash: String?
        get() = securePreferences.getString(PreferencesKeyConstants.SESSION_HASH, null)
        set(sessionHash) {
            securePreferences.putString(PreferencesKeyConstants.SESSION_HASH, sessionHash)
        }
    override var showLatencyInMS: Boolean
        get() = preference.getBoolean(PreferencesKeyConstants.SHOW_LATENCY_IN_MS, false)
        set(showLatencyInMS) {
            preference.put(PreferencesKeyConstants.SHOW_LATENCY_IN_MS, showLatencyInMS)
        }

    override val splitRoutingMode: String
        get() = preference.getString(
            PreferencesKeyConstants.SPLIT_ROUTING_MODE,
            PreferencesKeyConstants.EXCLUSIVE_MODE
        )
            ?: PreferencesKeyConstants.EXCLUSIVE_MODE
    override var splitTunnelToggle: Boolean
        get() = preference.getBoolean(PreferencesKeyConstants.SPLIT_TUNNEL_TOGGLE, false)
        set(toggle) {
            preference.put(PreferencesKeyConstants.SPLIT_TUNNEL_TOGGLE, toggle)
        }
    override var userName: String
        get() = preference.getString(PreferencesKeyConstants.USER_NAME, "na") ?: "na"
        set(userName) {
            preference.put(PreferencesKeyConstants.USER_NAME, userName)
        }
    override var userStatus: Int
        get() {
            val status = preference.getString(PreferencesKeyConstants.USER_STATUS, null)
            return status?.toInt() ?: 0
        }
        set(userStatus) {
            preference.put(PreferencesKeyConstants.USER_STATUS, userStatus)
        }
    override var whitelistOverride: Boolean
        get() = preference.getBoolean(PreferencesKeyConstants.WHITELIST_OVERRIDE, false)
        set(whitelistOverride) {
            preference.put(PreferencesKeyConstants.WHITELIST_OVERRIDE, whitelistOverride)
        }
    override val wireGuardPort: String
        get() = preference.getString(
            PreferencesKeyConstants.SAVED_WIRE_GUARD_PORT,
            DEFAULT_WIRE_GUARD_PORT
        )
            ?: DEFAULT_WIRE_GUARD_PORT

    override fun isConnectingToConfiguredLocation(): Boolean {
        return preference.getBoolean(
            PreferencesKeyConstants.IS_CONNECTING_TO_CONFIGURED_IP,
            false
        )
    }

    override val isConnectingToStaticIp: Boolean
        get() = preference.getBoolean(PreferencesKeyConstants.IS_CONNECTING_TO_STATIC_IP, false)
    override var isCustomBackground: Boolean
        get() = preference.getBoolean(PreferencesKeyConstants.CUSTOM_FLAG_BACKGROUND, false)
        set(customFlag) {
            preference.put(PreferencesKeyConstants.CUSTOM_FLAG_BACKGROUND, customFlag)
        }
    override val isGpsSpoofingOn: Boolean
        get() = preference.getBoolean(PreferencesKeyConstants.GPS_SPOOF_SETTING, false)
    override val isHapticFeedbackEnabled: Boolean
        get() = preference.getBoolean(PreferencesKeyConstants.HAPTIC_FEEDBACK, true)
    override var isKeepAliveModeAuto: Boolean
        get() = preference.getBoolean(PreferencesKeyConstants.KEEP_ALIVE_MODE_AUTO, true)
        set(auto) {
            preference.put(PreferencesKeyConstants.KEEP_ALIVE_MODE_AUTO, auto)
        }
    override val isKernelModuleDisabled: Boolean
        get() = preference.getBoolean(PreferencesKeyConstants.DISABLE_KERNEL_MODULE, false)
    override var isNewApplicationInstance: Boolean
        get() = preference.getBoolean(PreferencesKeyConstants.ON_CREATE_APPLICATION, true)
        set(instance) {
            preference.put(PreferencesKeyConstants.ON_CREATE_APPLICATION, instance)
        }

    override fun isNotificationAlreadyShown(notificationId: String): Boolean {
        return preference.getBoolean(notificationId, false)
    }

    override val isPackageSizeModeAuto: Boolean
        get() = preference.getBoolean(PreferencesKeyConstants.AUTO_MTU_MODE_KEY, true)
    override var isReconnecting: Boolean
        get() = preference.getBoolean(PreferencesKeyConstants.CONNECTION_RETRY_ENABLED, false)
        set(mode) {
            preference.put(PreferencesKeyConstants.CONNECTION_RETRY_ENABLED, mode)
        }
    override var isShowLocationHealthEnabled: Boolean
        get() = preference.getBoolean(PreferencesKeyConstants.SHOW_LOCATION_HEALTH, false)
        set(enabled) {
            preference.put(PreferencesKeyConstants.SHOW_LOCATION_HEALTH, enabled)
        }
    override var isStartedByAlwaysOn: Boolean
        get() = preference.getBoolean(PreferencesKeyConstants.STARTED_BY_ALWAYS_ON, false)
        set(alwaysOn) {
            preference.put(PreferencesKeyConstants.STARTED_BY_ALWAYS_ON, alwaysOn)
        }

    override fun nextProtocol(protocol: String?) {
        preference.put(PreferencesKeyConstants.CHOSEN_PROTOCOL, protocol)
    }

    override fun removeResponseData(key: String?) {
        preference.remove(key!!)
    }

    override fun requiredReconnect(): Boolean {
        return preference.getBoolean(PreferencesKeyConstants.RECONNECT_REQUIRED, false)
    }

    override fun saveIKEv2Port(port: String?) {
        preference.put(PreferencesKeyConstants.SAVED_IKev2_PORT, port)
    }

    override fun saveInstalledApps(installedAppsSaved: List<String>) {
        preference.put(
            PreferencesKeyConstants.INSTALLED_APPS_DATA,
            Gson().toJson(installedAppsSaved)
        )
    }

    override fun saveLastSelectedServerTabIndex(index: Int) {
        preference.put(PreferencesKeyConstants.LAST_SELECTED_SERVER_TAB, index)
    }

    override fun saveNotificationId(notificationId: String) {
        preference.put(notificationId, true)
    }

    override fun savePortMapVersion(version: Int) {
        preference.put(PreferencesKeyConstants.PORT_MAP_VERSION, version)
    }

    override fun savePurchaseFlowState(state: String?) {
        preference.put(PreferencesKeyConstants.PURCHASE_FLOW_STATE_KEY, state)
    }

    override fun saveResponseIntegerData(key: String, value: Int) {
        preference.put(key, value)
    }

    override fun saveResponseStringData(key: String, value: String) {
        preference.put(key, value)
    }

    override fun saveCredentials(key: String, value: ServerCredentialsResponse) {
        securePreferences.putString(key, Gson().toJson(value))
    }

    override fun getCredentials(key: String): ServerCredentialsResponse? {
        val json = securePreferences.getString(key, null) ?: return null
        return Gson().fromJson(json, ServerCredentialsResponse::class.java)
    }

    override fun saveOpenVPNServerConfig(value: String) {
        securePreferences.putString(PreferencesKeyConstants.OPEN_VPN_SERVER_CONFIG, value)
    }

    override fun getOpenVPNServerConfig(): String? {
        return securePreferences.getString(PreferencesKeyConstants.OPEN_VPN_SERVER_CONFIG, null)
    }

    override fun saveSelection(selection: String?) {
        preference.put(PreferencesKeyConstants.SELECTION_KEY, selection)
    }

    override fun saveSplitRoutingMode(mode: String?) {
        preference.put(PreferencesKeyConstants.SPLIT_ROUTING_MODE, mode)
    }

    override fun saveWireGuardPort(port: String?) {
        preference.put(PreferencesKeyConstants.SAVED_WIRE_GUARD_PORT, port)
    }

    override fun setAlwaysOn(status: Boolean) {
        preference.put(PreferencesKeyConstants.ALWAYS_ON, status)
    }

    override fun setAuthFailedConnectionAttemptCount(numberOfAttempts: Int?) {
        preference.put(
            PreferencesKeyConstants.AUTH_RECONNECT_ATTEMPT_COUNT_KEY,
            numberOfAttempts!!
        )
    }

    override fun setChosenProtocol(protocol: String?) {
        preference.put(PreferencesKeyConstants.CHOSEN_PROTOCOL, protocol)
    }

    override fun setConnectingToConfiguredLocation(connectingToConfiguredLocation: Boolean) {
        preference.put(
            PreferencesKeyConstants.IS_CONNECTING_TO_CONFIGURED_IP,
            connectingToConfiguredLocation
        )
    }

    override fun setConnectingToStaticIP(connectingToStaticIP: Boolean) {
        preference.put(PreferencesKeyConstants.IS_CONNECTING_TO_STATIC_IP, connectingToStaticIP)
    }

    override fun setConnectionAttemptTag() {
        preference.put(
            PreferencesKeyConstants.CONNECTION_ATTEMPT,
            System.currentTimeMillis().toString()
        )
    }

    override fun setDeviceUUID(deviceUUID: String?) {
        preference.put(PreferencesKeyConstants.DEVICE_ID, deviceUUID)
    }

    override fun setDisconnectedFlagPath(path: String?) {
        preference.put(PreferencesKeyConstants.DISCONNECTED_FLAG_PATH, path)
    }

    override fun setFutureSelectCity(cityId: Int) {
        preference.put(PreferencesKeyConstants.FUTURE_SELECTED_CITY, cityId)
    }

    override fun setGpsSpoofing(spoof: Boolean) {
        preference.put(PreferencesKeyConstants.GPS_SPOOF_SETTING, spoof)
    }

    override fun setHapticFeedbackEnabled(hapticFeedbackEnabled: Boolean) {
        preference.put(PreferencesKeyConstants.HAPTIC_FEEDBACK, hapticFeedbackEnabled)
    }

    override fun setOurIp(ip: Int) {
        preference.put(PreferencesKeyConstants.OUR_IP, ip)
    }

    override fun setPacketSizeModeToAuto(auto: Boolean) {
        preference.put(PreferencesKeyConstants.AUTO_MTU_MODE_KEY, auto)
    }

    override fun setPreviousAccountStatus(userNameKey: String, userAccountStatus: Int) {
        preference.put(
            userNameKey + PreferencesKeyConstants.PREVIOUS_ACCOUNT_STATUS,
            userAccountStatus
        )
    }

    override fun setPreviousUserStatus(userNameKey: String, userStatus: Int) {
        preference.put(userNameKey + PreferencesKeyConstants.PREVIOUS_USER_STATUS, userStatus)
    }

    override fun setReconnectRequired(required: Boolean) {
        preference.put(PreferencesKeyConstants.RECONNECT_REQUIRED, required)
    }

    override fun setShowNewsFeedAlert(showAlert: Boolean) {
        preference.put(PreferencesKeyConstants.NEWS_FEED_ALERT, showAlert)
    }

    override fun setStaticAccessIp(key: String, staticAccessIp: String?) {
        preference.put(key, staticAccessIp)
    }

    override fun setUserAccountUpdateRequired(required: Boolean) {
        preference.put(PreferencesKeyConstants.USER_ACCOUNT_UPDATE_REQUIRED, required)
    }

    override fun setUserIntendedDisconnect(userIntendedDisconnect: Boolean) {
        preference.put(PreferencesKeyConstants.USER_INTENDED_DISCONNECT, userIntendedDisconnect)
    }

    override fun userIsInGhostMode(): Boolean {
        return userName == "na"
    }

    override fun installedApps(): List<String> {
        val jsonString = preference.getString(PreferencesKeyConstants.INSTALLED_APPS_DATA, null)
            ?: return emptyList()
        return Gson().fromJson(jsonString, object : TypeToken<List<String>>() {}.type)
    }

    override var wgLocalParams: WgLocalParams?
        get() {
            val jsonString = securePreferences.getString(WG_LOCAL_PARAMS, null) ?: return null
            return Gson().fromJson(jsonString, WgLocalParams::class.java)
        }
        set(value) {
            securePreferences.putString(WG_LOCAL_PARAMS, Gson().toJson(value))
        }

    override var isDecoyTrafficOn: Boolean
        get() = preference.getBoolean(DECOY_TRAFFIC, false)
        set(value) {
            preference.put(DECOY_TRAFFIC, value)
        }
    override var isAntiCensorshipOn: Boolean
        get() = preference.getBoolean(ANTI_CENSORSHIP, appContext.isRegionRestricted)
        set(value) {
            preference.put(ANTI_CENSORSHIP, value)
        }

    override var isAutoSecureOn: Boolean
        get() = preference.getBoolean(AUTO_SECURE_NEW_NETWORKS, true)
        set(value) {
            preference.put(AUTO_SECURE_NEW_NETWORKS, value)
        }

    override var fakeTrafficVolume: FakeTrafficVolume
        get() {
            val value = preference.getString(FAKE_TRAFFIC_VOLUME, FakeTrafficVolume.High.name)
                ?: FakeTrafficVolume.High.name
            return FakeTrafficVolume.valueOf(value)
        }
        set(value) {
            preference.put(FAKE_TRAFFIC_VOLUME, value.name)
        }

    override var wgConnectApiFailOverState: Map<String, Boolean>
        get() {
            return preference.getString(WG_CONNECT_API_FAIL_OVER_STATE, null)?.let {
                val type = object : TypeToken<Map<String, Boolean>>() {}.type
                return@let Gson().fromJson(it, type)
            } ?: mapOf()
        }
        set(value) {
            preference.put(WG_CONNECT_API_FAIL_OVER_STATE, Gson().toJson(value))
        }

    override var alreadyShownShareAppLink: Boolean
        get() = preference.getBoolean(ALREADY_SHOWN_SHARE_APP_LINK, false)
        set(value) {
            preference.put(ALREADY_SHOWN_SHARE_APP_LINK, value)
        }

    override var autoConnect: Boolean
        get() = preference.getBoolean(AUTO_CONNECT, false)
        set(value) {
            preference.put(AUTO_CONNECT, value)
        }
    override var advanceParamText: String
        get() = preference.getString(ADVANCE_PARAM_TEXT, "") ?: ""
        set(value) {
            preference.put(ADVANCE_PARAM_TEXT, value)
        }
    override var dnsMode: String
        get() = preference.getString(DNS_MODE, DNS_MODE_ROBERT) ?: DNS_MODE_ROBERT
        set(value) {
            preference.put(DNS_MODE, value)
        }
    override var dnsAddress: String?
        get() = preference.getString(CUSTOM_DNS_ADDRESS, null)
        set(value) {
            preference.put(CUSTOM_DNS_ADDRESS, value)
        }
    override var wsNetSettings: String
        get() = preference.getString(WS_NET_SETTINGS, "") ?: ""
        set(value) {
            preference.put(WS_NET_SETTINGS, value)
        }

    override fun isSuggested(): Boolean {
        return suggestedProtocol != null && suggestedPort != null
    }

    override var suggestedProtocol: String?
        get() = preference.getString(SUGGESTED_PROTOCOL, null)
        set(value) {
            preference.put(SUGGESTED_PROTOCOL, value)
        }
    override var suggestedPort: String?
        get() = preference.getString(SUGGESTED_PORT, null)
        set(value) {
            preference.put(SUGGESTED_PORT, value)
        }

    override fun getDefaultProtoInfo(): Pair<String, String> {
        if (isSuggested()) {
            return Pair(suggestedProtocol!!, suggestedPort!!)
        }
        return Pair(PreferencesKeyConstants.PROTO_IKev2, DEFAULT_IKEV2_PORT)
    }

    override fun getDefaultNetworkInfo(networkName: String): NetworkInfo {
        val proto = getDefaultProtoInfo()
        return NetworkInfo(networkName, isAutoSecureOn, false, proto.first, proto.second)
    }

    override var whiteListedNetwork: String?
        get() = preference.getString(WHITELISTED_NETWORK, null)
        set(value) {
            preference.put(WHITELISTED_NETWORK, value)
        }

    override var locationHash: String?
        get() = preference.getString(LOCATION_HASH, null)
        set(value) {
            preference.put(LOCATION_HASH, value)
        }

    override fun increaseConnectionCount() {
        val connectionCount = preference.getInt(PreferencesKeyConstants.CONNECTION_COUNT, 0)
        preference.put(PreferencesKeyConstants.CONNECTION_COUNT, connectionCount + 1)
    }

    override fun getConnectionCount(): Int {
        return preference.getInt(PreferencesKeyConstants.CONNECTION_COUNT, 0)
    }

    override fun getPowerWhiteListDialogCount(): Int {
        return preference.getInt(PreferencesKeyConstants.POWER_WHITE_LIST_POPUP_SHOW_COUNT, 0)
    }

    override fun setPowerWhiteListDialogCount(count: Int) {
        preference.put(PreferencesKeyConstants.POWER_WHITE_LIST_POPUP_SHOW_COUNT, count)
    }

    override fun addObserver(listener: OnTrayPreferenceChangeListener) {
        preference.registerOnTrayPreferenceChangeListener(listener)
    }

    override fun removeObserver(listener: OnTrayPreferenceChangeListener) {
        preference.unregisterOnTrayPreferenceChangeListener(listener)
    }
    override var whenDisconnectedBackgroundOption: Int
        get() = preference.getInt(PreferencesKeyConstants.WHEN_DISCONNECTED_BACKGROUND_OPTION, 1)
        set(value) {
            preference.put(PreferencesKeyConstants.WHEN_DISCONNECTED_BACKGROUND_OPTION, value)
        }
    override var whenConnectedBackgroundOption: Int
        get() = preference.getInt(PreferencesKeyConstants.WHEN_CONNECTED_BACKGROUND_OPTION, 1)
        set(value) {
            preference.put(PreferencesKeyConstants.WHEN_CONNECTED_BACKGROUND_OPTION, value)
        }
    override var backgroundAspectRatioOption: Int
        get() = preference.getInt(PreferencesKeyConstants.ASPECT_RATIO_BACKGROUND_OPTION, 1)
        set(value) {
            preference.put(PreferencesKeyConstants.ASPECT_RATIO_BACKGROUND_OPTION, value)
        }
    override var disconnectedBundleBackgroundOption: Int
        get() = preference.getInt(PreferencesKeyConstants.DISCONNECTED_BUNDLE_BACKGROUND_OPTION, 1)
        set(value) {
            preference.put(PreferencesKeyConstants.DISCONNECTED_BUNDLE_BACKGROUND_OPTION, value)
        }
    override var connectedBundleBackgroundOption: Int
        get() = preference.getInt(PreferencesKeyConstants.CONNECTED_BUNDLE_BACKGROUND_OPTION, 1)
        set(value) {
            preference.put(PreferencesKeyConstants.CONNECTED_BUNDLE_BACKGROUND_OPTION, value)
        }
    override var customDisconnectedBackground: String?
        get() = preference.getString(PreferencesKeyConstants.DISCONNECTED_CUSTOM_BACKGROUND, null)
        set(value) {
            preference.put(PreferencesKeyConstants.DISCONNECTED_CUSTOM_BACKGROUND, value)
        }
    override var customConnectedBackground: String?
        get() = preference.getString(PreferencesKeyConstants.CONNECTED_CUSTOM_BACKGROUND, null)
        set(value) {
            preference.put(PreferencesKeyConstants.CONNECTED_CUSTOM_BACKGROUND, value)
        }
    override var whenDisconnectedSoundOption: Int
        get() = preference.getInt(PreferencesKeyConstants.WHEN_DISCONNECTED_SOUND_OPTION, 1)
        set(value) {
            preference.put(PreferencesKeyConstants.WHEN_DISCONNECTED_SOUND_OPTION, value)
        }
    override var whenConnectedSoundOption: Int
        get() = preference.getInt(PreferencesKeyConstants.WHEN_CONNECTED_SOUND_OPTION, 1)
        set(value) {
            preference.put(PreferencesKeyConstants.WHEN_CONNECTED_SOUND_OPTION, value)
        }
    override var disconnectedBundleSoundOption: Int
        get() = preference.getInt(PreferencesKeyConstants.DISCONNECTED_BUNDLE_SOUND_OPTION, 1)
        set(value) {
            preference.put(PreferencesKeyConstants.DISCONNECTED_BUNDLE_SOUND_OPTION, value)
        }
    override var connectedBundleSoundOption: Int
        get() = preference.getInt(PreferencesKeyConstants.CONNECTED_BUNDLE_SOUND_OPTION, 1)
        set(value) {
            preference.put(PreferencesKeyConstants.CONNECTED_BUNDLE_SOUND_OPTION, value)
        }
    override var customDisconnectedSound: String?
        get() = preference.getString(PreferencesKeyConstants.DISCONNECTED_CUSTOM_SOUND, null)
        set(value) {
            preference.put(PreferencesKeyConstants.DISCONNECTED_CUSTOM_SOUND, value)
        }
    override var customConnectedSound: String?
        get() = preference.getString(PreferencesKeyConstants.CONNECTED_CUSTOM_SOUND, null)
        set(value) {
            preference.put(PreferencesKeyConstants.CONNECTED_CUSTOM_SOUND, value)
        }
}
