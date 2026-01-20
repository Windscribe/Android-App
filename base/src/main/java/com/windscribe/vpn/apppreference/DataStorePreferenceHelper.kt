/*
 * Copyright (c) 2021 Windscribe Limited.
 */
package com.windscribe.vpn.apppreference

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.windscribe.vpn.Windscribe.Companion.appContext
import com.windscribe.vpn.api.response.ServerCredentialsResponse
import com.windscribe.vpn.autoconnection.ProtocolConnectionStatus
import com.windscribe.vpn.apppreference.PreferencesKeyConstants
import com.windscribe.vpn.apppreference.PreferencesKeyConstants.DEFAULT_IKEV2_PORT
import com.windscribe.vpn.apppreference.PreferencesKeyConstants.DEFAULT_WIRE_GUARD_PORT
import com.windscribe.vpn.apppreference.PreferencesKeyConstants.DNS_MODE_ROBERT
import com.windscribe.vpn.decoytraffic.FakeTrafficVolume
import com.windscribe.vpn.localdatabase.tables.NetworkInfo
import com.windscribe.vpn.repository.WgLocalParams
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.util.Date
import javax.inject.Singleton

/**
 * DataStore-based implementation of PreferencesHelper.
 *
 * Migration Strategy:
 * - One-time bulk migration from Tray database on app startup (see TrayToDataStoreMigration)
 * - All reads/writes go directly to DataStore
 * - Tray dependencies completely removed
 */
@Singleton
class DataStorePreferenceHelper(
    private val dataStore: DataStore<Preferences>,
    private val securePreferences: SecurePreferences,
    private val scope: CoroutineScope
) : PreferencesHelper {

    // ============================================================================
    // OBSERVER PATTERN (Flow-based)
    // ============================================================================

    private val listeners = mutableListOf<OnPreferenceChangeListener>()

    init {
        // Observe DataStore changes and notify all listeners
        scope.launch {
            dataStore.data
                .drop(1) // Skip initial value
                .distinctUntilChanged() // Only emit when preferences actually change
                .collect {
                    // Notify all registered listeners
                    listeners.forEach { listener ->
                        listener.onPreferenceChanged(null) // null = any preference changed
                    }
                }
        }
    }

    override fun addObserver(listener: OnPreferenceChangeListener) {
        if (!listeners.contains(listener)) {
            listeners.add(listener)
        }
    }

    override fun removeObserver(listener: OnPreferenceChangeListener) {
        listeners.remove(listener)
    }

    // ============================================================================
    // DATASTORE HELPER FUNCTIONS
    // ============================================================================

    /**
     * Get string from DataStore
     */
    private suspend fun getString(
        key: Preferences.Key<String>,
        default: String
    ): String {
        return dataStore.data.first()[key] ?: default
    }

    /**
     * Get int from DataStore
     */
    private suspend fun getInt(
        key: Preferences.Key<Int>,
        default: Int
    ): Int {
        return dataStore.data.first()[key] ?: default
    }

    /**
     * Get boolean from DataStore
     * Handles migration edge cases where value might be stored as String
     */
    private suspend fun getBoolean(
        key: Preferences.Key<Boolean>,
        default: Boolean
    ): Boolean {
        return try {
            dataStore.data.first()[key] ?: default
        } catch (_: ClassCastException) {
            // Handle migration case: value stored as String instead of Boolean
            // This can happen with dynamic keys (notifications, etc.) that weren't explicitly migrated
            val stringKey = stringPreferencesKey(key.name)
            val stringValue = dataStore.data.first()[stringKey]
            when (stringValue?.lowercase()) {
                "true", "1" -> true
                "false", "0" -> false
                else -> default
            }
        }
    }

    /**
     * Get long from DataStore
     */
    private suspend fun getLong(
        key: Preferences.Key<Long>,
        default: Long
    ): Long {
        return dataStore.data.first()[key] ?: default
    }

    /**
     * Set string (DataStore only) - ASYNC
     * Returns immediately, write happens in background
     */
    private fun setString(key: Preferences.Key<String>, value: String?) {
        scope.launch {
            dataStore.edit { preferences ->
                if (value != null) {
                    preferences[key] = value
                } else {
                    preferences.remove(key)
                }
            }
        }
    }

    /**
     * Set string synchronously - SYNC
     * Blocks until write completes (use for critical values)
     */
    private fun setStringSync(key: Preferences.Key<String>, value: String?) {
        runBlocking {
            dataStore.edit { preferences ->
                if (value != null) {
                    preferences[key] = value
                } else {
                    preferences.remove(key)
                }
            }
        }
    }

    /**
     * Set int (DataStore only) - ASYNC
     * Returns immediately, write happens in background
     */
    private fun setInt(key: Preferences.Key<Int>, value: Int) {
        scope.launch {
            dataStore.edit { it[key] = value }
        }
    }

    /**
     * Set int synchronously - SYNC
     * Blocks until write completes (use for critical values)
     */
    private fun setIntSync(key: Preferences.Key<Int>, value: Int) {
        runBlocking {
            dataStore.edit { it[key] = value }
        }
    }

    /**
     * Set boolean (DataStore only) - ASYNC
     * Returns immediately, write happens in background
     */
    private fun setBoolean(key: Preferences.Key<Boolean>, value: Boolean) {
        scope.launch {
            dataStore.edit { it[key] = value }
        }
    }

    /**
     * Set boolean synchronously - SYNC
     * Blocks until write completes (use for critical values)
     */
    private fun setBooleanSync(key: Preferences.Key<Boolean>, value: Boolean) {
        runBlocking {
            dataStore.edit { it[key] = value }
        }
    }

    /**
     * Set long (DataStore only)
     */
    private fun setLong(key: Preferences.Key<Long>, value: Long) {
        scope.launch {
            dataStore.edit { it[key] = value }
        }
    }

    // ============================================================================
    // PREFERENCE IMPLEMENTATIONS
    // ============================================================================

    override fun clearAllData() {
        val installation = newInstallation
        scope.launch {
            dataStore.edit { it.clear() }
        }
        securePreferences.clear()
        if (PreferencesKeyConstants.I_OLD == installation) {
            newInstallation = PreferencesKeyConstants.I_OLD
        }
    }

    override fun clearOldSessionAuth() {
        scope.launch {
            dataStore.edit { it.remove(DataStoreKeys.SESSION_HASH) }
        }
    }

    override var isHapticFeedbackEnabled: Boolean
        get() = runBlocking { getBoolean(DataStoreKeys.HAPTIC_FEEDBACK, true) }
        set(value) = setBoolean(DataStoreKeys.HAPTIC_FEEDBACK, value)


    // User & Session
    override var userName: String
        get() = runBlocking { getString(DataStoreKeys.USER_NAME, "na") }
        set(value) = setString(DataStoreKeys.USER_NAME, value)

    override var userStatus: Int
        get() = runBlocking { getInt(DataStoreKeys.USER_STATUS, 0) }
        set(value) = setInt(DataStoreKeys.USER_STATUS, value)

    override var sessionHash: String?
        get() = securePreferences.getString(SecurePreferencesKeys.SESSION_HASH, null)
        set(value) = securePreferences.putStringSync(
            SecurePreferencesKeys.SESSION_HASH,
            value
        )  // SYNC - critical auth token

    override var loginTime: Date?
        get() = runBlocking {
            val time = getLong(DataStoreKeys.LOGIN_TIME, -1L)
            if (time == -1L) null else Date(time)
        }
        set(value) {
            if (value != null) {
                setLong(DataStoreKeys.LOGIN_TIME, value.time)
            }
        }

    // VPN Connection (critical - use SYNC setters to ensure values are saved immediately)
    override var selectedCity: Int
        get() = runBlocking { getInt(DataStoreKeys.SELECTED_CITY_ID, -1) }
        set(value) = setIntSync(DataStoreKeys.SELECTED_CITY_ID, value)  // SYNC

    override var selectedProtocol: String
        get() = runBlocking {
            getString(
                DataStoreKeys.SELECTED_PROTOCOL,
                getDefaultProtoInfo().first
            )
        }
        set(value) = setStringSync(
            DataStoreKeys.SELECTED_PROTOCOL,
            value
        )  // SYNC - blocks until saved

    override var selectedPort: String
        get() = runBlocking {
            getString(
                DataStoreKeys.SELECTED_PORT,
                DEFAULT_IKEV2_PORT
            )
        }
        set(value) = setStringSync(DataStoreKeys.SELECTED_PORT, value)  // SYNC

    override var selectedIp: String?
        get() = runBlocking {
            val bestLocationIp2 =
                dataStore.data.first()[stringPreferencesKey(PreferencesKeyConstants.BEST_LOCATION_IP_2)]
            getString(DataStoreKeys.SELECTED_IP, bestLocationIp2 ?: "")
                .takeIf { it.isNotEmpty() }
        }
        set(value) = setStringSync(DataStoreKeys.SELECTED_IP, value)  // SYNC

    override var selectedProtocolType: ProtocolConnectionStatus
        get() = runBlocking {
            val typeName = getString(
                DataStoreKeys.SELECTED_PROTOCOL_TYPE,
                ProtocolConnectionStatus.Disconnected.name
            )
            ProtocolConnectionStatus.valueOf(typeName)
        }
        set(value) = setStringSync(DataStoreKeys.SELECTED_PROTOCOL_TYPE, value.name)  // SYNC

    override var connectionStatus: String?
        get() = runBlocking {
            getString(
                DataStoreKeys.CONNECTION_STATUS,
                PreferencesKeyConstants.VPN_DISCONNECTED
            )
        }
        set(value) = setStringSync(DataStoreKeys.CONNECTION_STATUS, value)  // SYNC

    override val currentConnectionAttemptTag: String?
        get() = runBlocking {
            getString(DataStoreKeys.CONNECTION_ATTEMPT, "")
                .takeIf { it.isNotEmpty() }
        }

    // Protocol Ports
    override var iKEv2Port: String
        get() = runBlocking {
            getString(DataStoreKeys.SAVED_IKev2_PORT, DEFAULT_IKEV2_PORT)
        }
        set(value) = setString(DataStoreKeys.SAVED_IKev2_PORT, value)

    override var wireGuardPort: String
        get() = runBlocking {
            getString(DataStoreKeys.SAVED_WIRE_GUARD_PORT, DEFAULT_WIRE_GUARD_PORT)
        }
        set(value) = setString(DataStoreKeys.SAVED_WIRE_GUARD_PORT, value)

    override var savedTCPPort: String
        get() = runBlocking {
            getString(
                DataStoreKeys.SAVED_TCP_PORT,
                PreferencesKeyConstants.DEFAULT_TCP_LEGACY_PORT
            )
        }
        set(value) = setString(DataStoreKeys.SAVED_TCP_PORT, value)

    override var savedUDPPort: String
        get() = runBlocking {
            getString(
                DataStoreKeys.SAVED_UDP_PORT,
                PreferencesKeyConstants.DEFAULT_UDP_LEGACY_PORT
            )
        }
        set(value) = setString(DataStoreKeys.SAVED_UDP_PORT, value)

    override var savedSTEALTHPort: String
        get() = runBlocking {
            getString(
                DataStoreKeys.SAVED_STEALTH_PORT,
                PreferencesKeyConstants.DEFAULT_STEALTH_LEGACY_PORT
            )
        }
        set(value) = setString(DataStoreKeys.SAVED_STEALTH_PORT, value)

    override var savedWSTunnelPort: String
        get() = runBlocking {
            getString(
                DataStoreKeys.SAVED_WS_TUNNEL_PORT,
                PreferencesKeyConstants.DEFAULT_WS_TUNNEL_LEGACY_PORT
            )
        }
        set(value) = setString(DataStoreKeys.SAVED_WS_TUNNEL_PORT, value)

    // App Settings
    override var autoStartOnBoot: Boolean
        get() = runBlocking { getBoolean(DataStoreKeys.AUTO_START_ON_BOOT, false) }
        set(value) = setBoolean(DataStoreKeys.AUTO_START_ON_BOOT, value)

    override var selectedTheme: String
        get() = runBlocking {
            getString(
                DataStoreKeys.SELECTED_THEME,
                PreferencesKeyConstants.DARK_THEME
            )
        }
        set(value) = setString(DataStoreKeys.SELECTED_THEME, value)

    override var savedLanguage: String
        get() = runBlocking {
            getString(
                DataStoreKeys.USER_LANGUAGE,
                appContext.getAppSupportedSystemLanguage()
            )
        }
        set(value) = setString(DataStoreKeys.USER_LANGUAGE, value)

    override var savedProtocol: String
        get() = runBlocking {
            getString(
                DataStoreKeys.PROTOCOL_KEY,
                getDefaultProtoInfo().first
            )
        }
        set(value) = setString(DataStoreKeys.PROTOCOL_KEY, value)

    override var notificationStat: Boolean
        get() = runBlocking { getBoolean(DataStoreKeys.NOTIFICATION_STAT, false) }
        set(value) = setBoolean(DataStoreKeys.NOTIFICATION_STAT, value)

    override var showLatencyInMS: Boolean
        get() = runBlocking { getBoolean(DataStoreKeys.SHOW_LATENCY_IN_MS, false) }
        set(value) = setBoolean(DataStoreKeys.SHOW_LATENCY_IN_MS, value)

    override var latencyType: String
        get() = if (showLatencyInMS) "Ms" else "Bars"
        set(value) {
            showLatencyInMS = value == "Ms"
        }

    // VPN Features
    override var splitTunnelToggle: Boolean
        get() = runBlocking { getBoolean(DataStoreKeys.SPLIT_TUNNEL_TOGGLE, false) }
        set(value) = setBoolean(DataStoreKeys.SPLIT_TUNNEL_TOGGLE, value)

    override var splitRoutingMode: String
        get() = runBlocking {
            getString(
                DataStoreKeys.SPLIT_ROUTING_MODE,
                PreferencesKeyConstants.EXCLUSIVE_MODE
            )
        }
        set(value) = setString(DataStoreKeys.SPLIT_ROUTING_MODE, value)

    override var lanByPass: Boolean
        get() = runBlocking { getBoolean(DataStoreKeys.LAN_BY_PASS, false) }
        set(value) = setBoolean(DataStoreKeys.LAN_BY_PASS, value)

    override var keepAlive: String
        get() = runBlocking { getString(DataStoreKeys.KEEP_ALIVE, "20") }
        set(value) = setString(DataStoreKeys.KEEP_ALIVE, value)

    override var isKeepAliveModeAuto: Boolean
        get() = runBlocking { getBoolean(DataStoreKeys.KEEP_ALIVE_MODE_AUTO, true) }
        set(value) = setBoolean(DataStoreKeys.KEEP_ALIVE_MODE_AUTO, value)

    override var packetSize: Int
        get() = runBlocking { getInt(DataStoreKeys.LAST_MTU_VALUE, 1500) }
        set(value) = setInt(DataStoreKeys.LAST_MTU_VALUE, value)

    override var isPackageSizeModeAuto: Boolean
        get() = runBlocking { getBoolean(DataStoreKeys.AUTO_MTU_MODE_KEY, true) }
        set(value) = setBoolean(DataStoreKeys.AUTO_MTU_MODE_KEY, value)

    override var showSystemApps: Boolean
        get() = runBlocking { getBoolean(DataStoreKeys.SHOW_SYSTEM_APPS, false) }
        set(value) = setBoolean(DataStoreKeys.SHOW_SYSTEM_APPS, value)

    // Network Configuration
    override var alcListString: String?
        get() = runBlocking { getString(DataStoreKeys.ALC_LIST, "").takeIf { it.isNotEmpty() } }
        set(value) = setString(DataStoreKeys.ALC_LIST, value)

    override var whiteListedNetwork: String?
        get() = runBlocking {
            getString(
                DataStoreKeys.WHITELISTED_NETWORK,
                ""
            ).takeIf { it.isNotEmpty() }
        }
        set(value) = setString(DataStoreKeys.WHITELISTED_NETWORK, value)

    override var whitelistOverride: Boolean
        get() = runBlocking { getBoolean(DataStoreKeys.WHITELIST_OVERRIDE, false) }
        set(value) = setBoolean(DataStoreKeys.WHITELIST_OVERRIDE, value)

    override var isAutoSecureOn: Boolean
        get() = runBlocking { getBoolean(DataStoreKeys.AUTO_SECURE_NEW_NETWORKS, true) }
        set(value) = setBoolean(DataStoreKeys.AUTO_SECURE_NEW_NETWORKS, value)

    // UI State & Flags
    override var blurIp: Boolean
        get() = runBlocking { getBoolean(DataStoreKeys.BLUR_IP, false) }
        set(value) = setBoolean(DataStoreKeys.BLUR_IP, value)

    override var blurNetworkName: Boolean
        get() = runBlocking { getBoolean(DataStoreKeys.BLUR_NETWORK_NAME, false) }
        set(value) = setBoolean(DataStoreKeys.BLUR_NETWORK_NAME, value)

    override var connectedFlagPath: String?
        get() = runBlocking {
            getString(
                DataStoreKeys.CONNECTED_FLAG_PATH,
                ""
            ).takeIf { it.isNotEmpty() }
        }
        set(value) = setString(DataStoreKeys.CONNECTED_FLAG_PATH, value)

    override val disConnectedFlagPath: String?
        get() = runBlocking {
            getString(
                DataStoreKeys.DISCONNECTED_FLAG_PATH,
                ""
            ).takeIf { it.isNotEmpty() }
        }

    override var flagViewHeight: Int
        get() = runBlocking { getInt(DataStoreKeys.FLAG_VIEW_HEIGHT, 745) }
        set(value) = setInt(DataStoreKeys.FLAG_VIEW_HEIGHT, value)

    override var flagViewWidth: Int
        get() = runBlocking { getInt(DataStoreKeys.FLAG_VIEW_WIDTH, 1080) }
        set(value) = setInt(DataStoreKeys.FLAG_VIEW_WIDTH, value)

    override var isCustomBackground: Boolean
        get() = runBlocking { getBoolean(DataStoreKeys.CUSTOM_FLAG_BACKGROUND, false) }
        set(value) = setBoolean(DataStoreKeys.CUSTOM_FLAG_BACKGROUND, value)

    // Connection State
    override var globalUserConnectionPreference: Boolean
        get() = runBlocking { getBoolean(DataStoreKeys.GLOBAL_CONNECTION_PREFERENCE, false) }
        set(value) = setBoolean(DataStoreKeys.GLOBAL_CONNECTION_PREFERENCE, value)

    override var lastConnectedUsingSplit: Boolean
        get() = runBlocking { getBoolean(DataStoreKeys.LAST_CONNECTION_USING_SPLIT, false) }
        set(value) = setBoolean(DataStoreKeys.LAST_CONNECTION_USING_SPLIT, value)

    override var lowestPingId: Int
        get() = runBlocking { getInt(DataStoreKeys.LOWEST_PING_ID, -1) }
        set(value) = setInt(DataStoreKeys.LOWEST_PING_ID, value)

    override var migrationRequired: Boolean
        get() = runBlocking { getBoolean(DataStoreKeys.MIGRATION_REQUIRED, true) }
        set(value) = setBoolean(DataStoreKeys.MIGRATION_REQUIRED, value)

    override var pingTestRequired: Boolean
        get() = runBlocking { getBoolean(DataStoreKeys.PING_UPDATE_REQUIRED, true) }
        set(value) = setBoolean(DataStoreKeys.PING_UPDATE_REQUIRED, value)

    override var isReconnecting: Boolean
        get() = runBlocking { getBoolean(DataStoreKeys.CONNECTION_RETRY_ENABLED, false) }
        set(value) = setBoolean(DataStoreKeys.CONNECTION_RETRY_ENABLED, value)

    override var isStartedByAlwaysOn: Boolean
        get() = runBlocking { getBoolean(DataStoreKeys.STARTED_BY_ALWAYS_ON, false) }
        set(value) = setBoolean(DataStoreKeys.STARTED_BY_ALWAYS_ON, value)

    override var isShowLocationHealthEnabled: Boolean
        get() = runBlocking { getBoolean(DataStoreKeys.SHOW_LOCATION_HEALTH, false) }
        set(value) = setBoolean(DataStoreKeys.SHOW_LOCATION_HEALTH, value)

    override var isConnectingToStaticIp: Boolean
        get() = runBlocking { getBoolean(DataStoreKeys.IS_CONNECTING_TO_STATIC_IP, false) }
        set(value) {
            setBooleanSync(DataStoreKeys.IS_CONNECTING_TO_STATIC_IP, value)
        }
    override var isConnectingToConfigured: Boolean
        get() = runBlocking { getBoolean(DataStoreKeys.IS_CONNECTING_TO_CONFIGURED_IP, false) }
        set(value) {
            setBooleanSync(DataStoreKeys.IS_CONNECTING_TO_CONFIGURED_IP, value)
        }

    // Advanced Features
    override val multipleTunnel: Boolean
        get() = runBlocking { getBoolean(DataStoreKeys.MULTIPLE_TUNNELS, false) }

    override var isGpsSpoofingOn: Boolean
        get() = runBlocking { getBoolean(DataStoreKeys.GPS_SPOOF_SETTING, false) }
        set(value) = setBoolean(DataStoreKeys.GPS_SPOOF_SETTING, value)

    override val isKernelModuleDisabled: Boolean
        get() = runBlocking { getBoolean(DataStoreKeys.DISABLE_KERNEL_MODULE, false) }

    override var isDecoyTrafficOn: Boolean
        get() = runBlocking { getBoolean(DataStoreKeys.DECOY_TRAFFIC, false) }
        set(value) = setBoolean(DataStoreKeys.DECOY_TRAFFIC, value)

    override var isAntiCensorshipOn: Boolean
        get() = runBlocking {
            getBoolean(
                DataStoreKeys.ANTI_CENSORSHIP,
                appContext.isRegionRestricted
            )
        }
        set(value) = setBoolean(DataStoreKeys.ANTI_CENSORSHIP, value)

    override var fakeTrafficVolume: FakeTrafficVolume
        get() = runBlocking {
            val value = getString(DataStoreKeys.FAKE_TRAFFIC_VOLUME, FakeTrafficVolume.High.name)
            FakeTrafficVolume.valueOf(value)
        }
        set(value) = setString(DataStoreKeys.FAKE_TRAFFIC_VOLUME, value.name)

    // App Lifecycle
    override var isNewApplicationInstance: Boolean
        get() = runBlocking { getBoolean(DataStoreKeys.ON_CREATE_APPLICATION, true) }
        set(value) = setBoolean(DataStoreKeys.ON_CREATE_APPLICATION, value)

    override var lastSelectedTabIndex: Int
        get() = runBlocking { getInt(DataStoreKeys.LAST_SELECTED_SERVER_TAB, 0) }
        set(value) = setInt(DataStoreKeys.LAST_SELECTED_SERVER_TAB, value)

    override var portMapVersion: Int
        get() = runBlocking { getInt(DataStoreKeys.PORT_MAP_VERSION, 0) }
        set(value) = setInt(DataStoreKeys.PORT_MAP_VERSION, value)

    override var purchaseFlowState: String
        get() = runBlocking { getString(DataStoreKeys.PURCHASE_FLOW_STATE_KEY, "FINISHED") }
        set(value) = setString(DataStoreKeys.PURCHASE_FLOW_STATE_KEY, value)

    override var selection: String
        get() = runBlocking {
            getString(
                DataStoreKeys.SELECTION_KEY,
                PreferencesKeyConstants.DEFAULT_LIST_SELECTION_MODE
            )
        }
        set(value) = setString(DataStoreKeys.SELECTION_KEY, value)

    override val oldSessionAuth: String?
        get() = runBlocking {
            getString(DataStoreKeys.SESSION_HASH, "")
                .takeIf { it.isNotEmpty() }
        }

    // Advanced Parameters
    override var autoConnect: Boolean
        get() = runBlocking { getBoolean(DataStoreKeys.AUTO_CONNECT, false) }
        set(value) = setBoolean(DataStoreKeys.AUTO_CONNECT, value)

    override var advanceParamText: String
        get() = runBlocking { getString(DataStoreKeys.ADVANCE_PARAM_TEXT, "") }
        set(value) = setString(DataStoreKeys.ADVANCE_PARAM_TEXT, value)

    override var wsNetSettings: String
        get() = runBlocking { getString(DataStoreKeys.WS_NET_SETTINGS, "") }
        set(value) = setString(DataStoreKeys.WS_NET_SETTINGS, value)

    // DNS
    override var dnsMode: String
        get() = runBlocking { getString(DataStoreKeys.DNS_MODE, DNS_MODE_ROBERT) }
        set(value) = setString(DataStoreKeys.DNS_MODE, value)

    override var dnsAddress: String?
        get() = runBlocking {
            getString(
                DataStoreKeys.CUSTOM_DNS_ADDRESS,
                ""
            ).takeIf { it.isNotEmpty() }
        }
        set(value) = setString(DataStoreKeys.CUSTOM_DNS_ADDRESS, value)

    override var suggestedProtocol: String?
        get() = runBlocking {
            getString(
                DataStoreKeys.SUGGESTED_PROTOCOL,
                ""
            ).takeIf { it.isNotEmpty() }
        }
        set(value) = setString(DataStoreKeys.SUGGESTED_PROTOCOL, value)

    override var suggestedPort: String?
        get() = runBlocking {
            getString(
                DataStoreKeys.SUGGESTED_PORT,
                ""
            ).takeIf { it.isNotEmpty() }
        }
        set(value) = setString(DataStoreKeys.SUGGESTED_PORT, value)

    override var locationHash: String?
        get() = runBlocking {
            getString(
                DataStoreKeys.LOCATION_HASH,
                ""
            ).takeIf { it.isNotEmpty() }
        }
        set(value) = setString(DataStoreKeys.LOCATION_HASH, value)

    override var alreadyShownShareAppLink: Boolean
        get() = runBlocking { getBoolean(DataStoreKeys.ALREADY_SHOWN_SHARE_APP_LINK, false) }
        set(value) = setBoolean(DataStoreKeys.ALREADY_SHOWN_SHARE_APP_LINK, value)

    override var deviceUuid: String?
        get() = runBlocking {
            getString(DataStoreKeys.DEVICE_ID, "")
                .takeIf { it.isNotEmpty() }
        }
        set(value) = setString(DataStoreKeys.DEVICE_ID, value)

    override var powerWhiteListDialogCount: Int
        get() = runBlocking { getInt(DataStoreKeys.POWER_WHITE_LIST_POPUP_SHOW_COUNT, 0) }
        set(value) = setInt(DataStoreKeys.POWER_WHITE_LIST_POPUP_SHOW_COUNT, value)

    // Complex Types (JSON serialized)
    override var installedApps: List<String>
        get() = runBlocking {
            val jsonString = getString(DataStoreKeys.INSTALLED_APPS_DATA, "")
            if (jsonString.isEmpty()) {
                emptyList()
            } else {
                Gson().fromJson(jsonString, object : TypeToken<List<String>>() {}.type)
            }
        }
        set(value) = setString(DataStoreKeys.INSTALLED_APPS_DATA, Gson().toJson(value))

    override var wgConnectApiFailOverState: Map<String, Boolean>
        get() = runBlocking {
            val jsonString = getString(DataStoreKeys.WG_CONNECT_API_FAIL_OVER_STATE, "")
            if (jsonString.isEmpty()) {
                mapOf()
            } else {
                val type = object : TypeToken<Map<String, Boolean>>() {}.type
                Gson().fromJson(jsonString, type)
            }
        }
        set(value) = setString(DataStoreKeys.WG_CONNECT_API_FAIL_OVER_STATE, Gson().toJson(value))

    // Secure Preferences (remain in EncryptedSharedPreferences)
    override var wgLocalParams: WgLocalParams?
        get() {
            val jsonString =
                securePreferences.getString(SecurePreferencesKeys.WG_LOCAL_PARAMS, null)
                    ?: return null
            return Gson().fromJson(jsonString, WgLocalParams::class.java)
        }
        set(value) {
            securePreferences.putStringSync(
                SecurePreferencesKeys.WG_LOCAL_PARAMS,
                Gson().toJson(value)
            )  // SYNC - critical WireGuard config
        }

    // OpenVPN Credentials (stored as JSON in DataStore)
    override var openVpnCredentials: ServerCredentialsResponse?
        get() = runBlocking {
            val json = getString(DataStoreKeys.OPEN_VPN_CREDENTIALS, "")
            if (json.isEmpty()) null else Gson().fromJson(
                json,
                ServerCredentialsResponse::class.java
            )
        }
        set(value) = setStringSync(
            DataStoreKeys.OPEN_VPN_CREDENTIALS,
            if (value != null) Gson().toJson(value) else ""
        )  // SYNC - critical for VPN connection

    // IKEv2 Credentials (stored as JSON in DataStore)
    override var ikev2Credentials: ServerCredentialsResponse?
        get() = runBlocking {
            val json = getString(DataStoreKeys.IKEV2_CREDENTIALS, "")
            if (json.isEmpty()) null else Gson().fromJson(
                json,
                ServerCredentialsResponse::class.java
            )
        }
        set(value) = setStringSync(
            DataStoreKeys.IKEV2_CREDENTIALS,
            if (value != null) Gson().toJson(value) else ""
        )  // SYNC - critical for VPN connection

    // Static IP Credentials (stored as JSON in DataStore)
    override var staticIpCredentials: ServerCredentialsResponse?
        get() = runBlocking {
            val json = getString(DataStoreKeys.STATIC_IP_CREDENTIALS, "")
            if (json.isEmpty()) null else Gson().fromJson(
                json,
                ServerCredentialsResponse::class.java
            )
        }
        set(value) = setStringSync(
            DataStoreKeys.STATIC_IP_CREDENTIALS,
            if (value != null) Gson().toJson(value) else ""
        )  // SYNC - critical for VPN connection

    // OpenVPN Server Config (base64 encoded server config)
    override var openVpnServerConfig: String?
        get() = runBlocking {
            getString(DataStoreKeys.OPEN_VPN_SERVER_CONFIG, "").takeIf { it.isNotEmpty() }
        }
        set(value) = setStringSync(
            DataStoreKeys.OPEN_VPN_SERVER_CONFIG,
            value ?: ""
        )  // SYNC - critical VPN config

    // Dynamic Keys (per-user, per-network, etc.)
    override fun getPreviousAccountStatus(userNameKey: String): Int = runBlocking {
        getInt(DataStoreKeys.previousAccountStatus(userNameKey), -1)
    }

    override fun getPreviousUserStatus(userNameKey: String): Int = runBlocking {
        getInt(DataStoreKeys.previousUserStatus(userNameKey), -1)
    }

    override fun setPreviousAccountStatus(userNameKey: String, userAccountStatus: Int) {
        setInt(DataStoreKeys.previousAccountStatus(userNameKey), userAccountStatus)
    }

    override fun setPreviousUserStatus(userNameKey: String, userStatus: Int) {
        setInt(DataStoreKeys.previousUserStatus(userNameKey), userStatus)
    }

    override fun userIsInGhostMode(): Boolean = userName == "na"

    override fun increaseConnectionCount() {
        val connectionCount = runBlocking {
            getInt(DataStoreKeys.CONNECTION_COUNT, 0)
        }
        setInt(DataStoreKeys.CONNECTION_COUNT, connectionCount + 1)
    }

    override fun getConnectionCount(): Int = runBlocking {
        getInt(DataStoreKeys.CONNECTION_COUNT, 0)
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

    override fun isSuggested(): Boolean {
        return suggestedProtocol != null && suggestedPort != null
    }

    // UI Customization - Background Options
    override var whenDisconnectedBackgroundOption: Int
        get() = runBlocking { getInt(DataStoreKeys.WHEN_DISCONNECTED_BACKGROUND_OPTION, 1) }
        set(value) = setInt(DataStoreKeys.WHEN_DISCONNECTED_BACKGROUND_OPTION, value)

    override var whenConnectedBackgroundOption: Int
        get() = runBlocking { getInt(DataStoreKeys.WHEN_CONNECTED_BACKGROUND_OPTION, 1) }
        set(value) = setInt(DataStoreKeys.WHEN_CONNECTED_BACKGROUND_OPTION, value)

    override var backgroundAspectRatioOption: Int
        get() = runBlocking { getInt(DataStoreKeys.ASPECT_RATIO_BACKGROUND_OPTION, 1) }
        set(value) = setInt(DataStoreKeys.ASPECT_RATIO_BACKGROUND_OPTION, value)

    override var disconnectedBundleBackgroundOption: Int
        get() = runBlocking { getInt(DataStoreKeys.DISCONNECTED_BUNDLE_BACKGROUND_OPTION, 1) }
        set(value) = setInt(DataStoreKeys.DISCONNECTED_BUNDLE_BACKGROUND_OPTION, value)

    override var connectedBundleBackgroundOption: Int
        get() = runBlocking { getInt(DataStoreKeys.CONNECTED_BUNDLE_BACKGROUND_OPTION, 1) }
        set(value) = setInt(DataStoreKeys.CONNECTED_BUNDLE_BACKGROUND_OPTION, value)

    override var customDisconnectedBackground: String?
        get() = runBlocking {
            getString(
                DataStoreKeys.DISCONNECTED_CUSTOM_BACKGROUND,
                ""
            ).takeIf { it.isNotEmpty() }
        }
        set(value) = setString(DataStoreKeys.DISCONNECTED_CUSTOM_BACKGROUND, value)

    override var customConnectedBackground: String?
        get() = runBlocking {
            getString(
                DataStoreKeys.CONNECTED_CUSTOM_BACKGROUND,
                ""
            ).takeIf { it.isNotEmpty() }
        }
        set(value) = setString(DataStoreKeys.CONNECTED_CUSTOM_BACKGROUND, value)

    // UI Customization - Sound Options
    override var whenDisconnectedSoundOption: Int
        get() = runBlocking { getInt(DataStoreKeys.WHEN_DISCONNECTED_SOUND_OPTION, 1) }
        set(value) = setInt(DataStoreKeys.WHEN_DISCONNECTED_SOUND_OPTION, value)

    override var whenConnectedSoundOption: Int
        get() = runBlocking { getInt(DataStoreKeys.WHEN_CONNECTED_SOUND_OPTION, 1) }
        set(value) = setInt(DataStoreKeys.WHEN_CONNECTED_SOUND_OPTION, value)

    override var disconnectedBundleSoundOption: Int
        get() = runBlocking { getInt(DataStoreKeys.DISCONNECTED_BUNDLE_SOUND_OPTION, 1) }
        set(value) = setInt(DataStoreKeys.DISCONNECTED_BUNDLE_SOUND_OPTION, value)

    override var connectedBundleSoundOption: Int
        get() = runBlocking { getInt(DataStoreKeys.CONNECTED_BUNDLE_SOUND_OPTION, 1) }
        set(value) = setInt(DataStoreKeys.CONNECTED_BUNDLE_SOUND_OPTION, value)

    override var customDisconnectedSound: String?
        get() = runBlocking {
            getString(
                DataStoreKeys.DISCONNECTED_CUSTOM_SOUND,
                ""
            ).takeIf { it.isNotEmpty() }
        }
        set(value) = setString(DataStoreKeys.DISCONNECTED_CUSTOM_SOUND, value)

    override var customConnectedSound: String?
        get() = runBlocking {
            getString(
                DataStoreKeys.CONNECTED_CUSTOM_SOUND,
                ""
            ).takeIf { it.isNotEmpty() }
        }
        set(value) = setString(DataStoreKeys.CONNECTED_CUSTOM_SOUND, value)

    // SSO
    override var isSsoLogin: Boolean
        get() = runBlocking { getBoolean(DataStoreKeys.IS_SSO_LOGIN, false) }
        set(value) = setBoolean(DataStoreKeys.IS_SSO_LOGIN, value)

    override var userIP: String?
        get() = runBlocking { getString(DataStoreKeys.USER_IP, "").takeIf { it.isNotEmpty() } }
        set(value) = setStringSync(DataStoreKeys.USER_IP, value)

    override var connectionMode: String?
        get() = runBlocking {
            getString(
                DataStoreKeys.CONNECTION_MODE_KEY,
                PreferencesKeyConstants.CONNECTION_MODE_AUTO
            )
                .takeIf { it.isNotEmpty() }
        }
        set(value) = setString(DataStoreKeys.CONNECTION_MODE_KEY, value)

    override var newInstallation: String?
        get() = runBlocking {
            getString(
                DataStoreKeys.NEW_INSTALLATION,
                ""
            ).takeIf { it.isNotEmpty() }
        }
        set(value) = setString(DataStoreKeys.NEW_INSTALLATION, value)

    override var getSession: String?
        get() = runBlocking { getString(DataStoreKeys.GET_SESSION, "").takeIf { it.isNotEmpty() } }
        set(value) = setString(DataStoreKeys.GET_SESSION, value)

    override var portMap: String?
        get() = runBlocking { getString(DataStoreKeys.PORT_MAP, "").takeIf { it.isNotEmpty() } }
        set(value) = setString(DataStoreKeys.PORT_MAP, value)

    override var robertFilters: String?
        get() = runBlocking {
            getString(
                DataStoreKeys.ROBERT_FILTERS,
                ""
            ).takeIf { it.isNotEmpty() }
        }
        set(value) = setString(DataStoreKeys.ROBERT_FILTERS, value)

    override var favoriteServerList: String?
        get() = runBlocking {
            getString(
                DataStoreKeys.FAVORITE_SERVER_LIST,
                ""
            ).takeIf { it.isNotEmpty() }
        }
        set(value) = setString(DataStoreKeys.FAVORITE_SERVER_LIST, value)

    override var purchasedItem: String?
        get() = runBlocking {
            getString(
                DataStoreKeys.PURCHASED_ITEM,
                ""
            ).takeIf { it.isNotEmpty() }
        }
        set(value) = setString(DataStoreKeys.PURCHASED_ITEM, value)

    override var amazonPurchasedItem: String?
        get() = runBlocking {
            getString(
                DataStoreKeys.AMAZON_PURCHASED_ITEM,
                ""
            ).takeIf { it.isNotEmpty() }
        }
        set(value) = setString(DataStoreKeys.AMAZON_PURCHASED_ITEM, value)

    override var rateDialogStatus: Int
        get() = runBlocking { getInt(DataStoreKeys.CURRENT_STATUS_KEY, 0) }
        set(value) = setInt(DataStoreKeys.CURRENT_STATUS_KEY, value)

    override var rateDialogLastUpdateTime: String?
        get() = runBlocking {
            getString(
                DataStoreKeys.LAST_UPDATE_TIME,
                ""
            ).takeIf { it.isNotEmpty() }
        }
        set(value) = setString(DataStoreKeys.LAST_UPDATE_TIME, value)
}
