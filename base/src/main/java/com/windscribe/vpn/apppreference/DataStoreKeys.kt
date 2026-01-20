/*
 * Copyright (c) 2021 Windscribe Limited.
 */
package com.windscribe.vpn.apppreference

import androidx.datastore.preferences.core.*
import com.windscribe.vpn.constants.BillingConstants
import com.windscribe.vpn.apppreference.PreferencesKeyConstants
import com.windscribe.vpn.constants.RateDialogConstants
import com.windscribe.vpn.constants.VpnPreferenceConstants

/**
 * Type-safe DataStore preference keys.
 * Maps from legacy Tray string keys to strongly-typed DataStore keys.
 */
object DataStoreKeys {

    // User & Session
    val USER_NAME = stringPreferencesKey(PreferencesKeyConstants.USER_NAME)
    val USER_STATUS = intPreferencesKey(PreferencesKeyConstants.USER_STATUS)
    val LOGIN_TIME = longPreferencesKey(PreferencesKeyConstants.LOGIN_TIME)
    val SESSION_HASH = stringPreferencesKey(SecurePreferencesKeys.SESSION_HASH)

    // VPN Connection
    val SELECTED_CITY_ID = intPreferencesKey(PreferencesKeyConstants.SELECTED_CITY_ID)
    val SELECTED_PROTOCOL = stringPreferencesKey(PreferencesKeyConstants.SELECTED_PROTOCOL)
    val SELECTED_PORT = stringPreferencesKey(PreferencesKeyConstants.SELECTED_PORT)
    val SELECTED_IP = stringPreferencesKey(PreferencesKeyConstants.SELECTED_IP)
    val SELECTED_PROTOCOL_TYPE =
        stringPreferencesKey(PreferencesKeyConstants.SELECTED_PROTOCOL_TYPE)
    val CONNECTION_STATUS = stringPreferencesKey(PreferencesKeyConstants.CONNECTION_STATUS)
    val CONNECTION_ATTEMPT = stringPreferencesKey(PreferencesKeyConstants.CONNECTION_ATTEMPT)

    // Protocol Ports
    val SAVED_IKev2_PORT = stringPreferencesKey(PreferencesKeyConstants.SAVED_IKev2_PORT)
    val SAVED_TCP_PORT = stringPreferencesKey(PreferencesKeyConstants.SAVED_TCP_PORT)
    val SAVED_UDP_PORT = stringPreferencesKey(PreferencesKeyConstants.SAVED_UDP_PORT)
    val SAVED_STEALTH_PORT = stringPreferencesKey(PreferencesKeyConstants.SAVED_STEALTH_PORT)
    val SAVED_WS_TUNNEL_PORT = stringPreferencesKey(PreferencesKeyConstants.SAVED_WS_TUNNEL_PORT)
    val SAVED_WIRE_GUARD_PORT = stringPreferencesKey(PreferencesKeyConstants.SAVED_WIRE_GUARD_PORT)

    // App Settings
    val AUTO_START_ON_BOOT = booleanPreferencesKey(PreferencesKeyConstants.AUTO_START_ON_BOOT)
    val SELECTED_THEME = stringPreferencesKey(PreferencesKeyConstants.SELECTED_THEME)
    val USER_LANGUAGE = stringPreferencesKey(PreferencesKeyConstants.USER_LANGUAGE)
    val NOTIFICATION_STAT = booleanPreferencesKey(PreferencesKeyConstants.NOTIFICATION_STAT)
    val HAPTIC_FEEDBACK = booleanPreferencesKey(PreferencesKeyConstants.HAPTIC_FEEDBACK)
    val SHOW_LATENCY_IN_MS = booleanPreferencesKey(PreferencesKeyConstants.SHOW_LATENCY_IN_MS)

    // VPN Features
    val SPLIT_TUNNEL_TOGGLE = booleanPreferencesKey(PreferencesKeyConstants.SPLIT_TUNNEL_TOGGLE)
    val SPLIT_ROUTING_MODE = stringPreferencesKey(PreferencesKeyConstants.SPLIT_ROUTING_MODE)
    val LAN_BY_PASS = booleanPreferencesKey(PreferencesKeyConstants.LAN_BY_PASS)
    val KEEP_ALIVE = stringPreferencesKey(PreferencesKeyConstants.KEEP_ALIVE)
    val KEEP_ALIVE_MODE_AUTO = booleanPreferencesKey(PreferencesKeyConstants.KEEP_ALIVE_MODE_AUTO)
    val LAST_MTU_VALUE = intPreferencesKey(PreferencesKeyConstants.LAST_MTU_VALUE)
    val AUTO_MTU_MODE_KEY = booleanPreferencesKey(PreferencesKeyConstants.AUTO_MTU_MODE_KEY)

    // Network & Location
    val GLOBAL_CONNECTION_PREFERENCE =
        booleanPreferencesKey(PreferencesKeyConstants.GLOBAL_CONNECTION_PREFERENCE)
    val SHOW_LOCATION_HEALTH = booleanPreferencesKey(PreferencesKeyConstants.SHOW_LOCATION_HEALTH)
    val LOWEST_PING_ID = intPreferencesKey(PreferencesKeyConstants.LOWEST_PING_ID)
    val PING_UPDATE_REQUIRED = booleanPreferencesKey(PreferencesKeyConstants.PING_UPDATE_REQUIRED)
    val LAST_SELECTED_SERVER_TAB =
        intPreferencesKey(PreferencesKeyConstants.LAST_SELECTED_SERVER_TAB)
    val SELECTION_KEY = stringPreferencesKey(PreferencesKeyConstants.SELECTION_KEY)
    val PROTOCOL_KEY = stringPreferencesKey(PreferencesKeyConstants.PROTOCOL_KEY)

    // Connection State
    val IS_CONNECTING_TO_CONFIGURED_IP =
        booleanPreferencesKey(PreferencesKeyConstants.IS_CONNECTING_TO_CONFIGURED_IP)
    val IS_CONNECTING_TO_STATIC_IP =
        booleanPreferencesKey(PreferencesKeyConstants.IS_CONNECTING_TO_STATIC_IP)
    val CONNECTION_RETRY_ENABLED =
        booleanPreferencesKey(PreferencesKeyConstants.CONNECTION_RETRY_ENABLED)
    val STARTED_BY_ALWAYS_ON = booleanPreferencesKey(PreferencesKeyConstants.STARTED_BY_ALWAYS_ON)
    val LAST_CONNECTION_USING_SPLIT =
        booleanPreferencesKey(PreferencesKeyConstants.LAST_CONNECTION_USING_SPLIT)

    // UI State
    val FLAG_VIEW_WIDTH = intPreferencesKey(PreferencesKeyConstants.FLAG_VIEW_WIDTH)
    val FLAG_VIEW_HEIGHT = intPreferencesKey(PreferencesKeyConstants.FLAG_VIEW_HEIGHT)
    val CUSTOM_FLAG_BACKGROUND =
        booleanPreferencesKey(PreferencesKeyConstants.CUSTOM_FLAG_BACKGROUND)
    val CONNECTED_FLAG_PATH = stringPreferencesKey(PreferencesKeyConstants.CONNECTED_FLAG_PATH)
    val DISCONNECTED_FLAG_PATH =
        stringPreferencesKey(PreferencesKeyConstants.DISCONNECTED_FLAG_PATH)
    val BLUR_IP = booleanPreferencesKey(PreferencesKeyConstants.BLUR_IP)
    val BLUR_NETWORK_NAME = booleanPreferencesKey(PreferencesKeyConstants.BLUR_NETWORK_NAME)

    // Background & Sound Options
    val WHEN_DISCONNECTED_BACKGROUND_OPTION =
        intPreferencesKey(PreferencesKeyConstants.WHEN_DISCONNECTED_BACKGROUND_OPTION)
    val WHEN_CONNECTED_BACKGROUND_OPTION =
        intPreferencesKey(PreferencesKeyConstants.WHEN_CONNECTED_BACKGROUND_OPTION)
    val ASPECT_RATIO_BACKGROUND_OPTION =
        intPreferencesKey(PreferencesKeyConstants.ASPECT_RATIO_BACKGROUND_OPTION)
    val DISCONNECTED_BUNDLE_BACKGROUND_OPTION =
        intPreferencesKey(PreferencesKeyConstants.DISCONNECTED_BUNDLE_BACKGROUND_OPTION)
    val CONNECTED_BUNDLE_BACKGROUND_OPTION =
        intPreferencesKey(PreferencesKeyConstants.CONNECTED_BUNDLE_BACKGROUND_OPTION)
    val DISCONNECTED_CUSTOM_BACKGROUND =
        stringPreferencesKey(PreferencesKeyConstants.DISCONNECTED_CUSTOM_BACKGROUND)
    val CONNECTED_CUSTOM_BACKGROUND =
        stringPreferencesKey(PreferencesKeyConstants.CONNECTED_CUSTOM_BACKGROUND)
    val WHEN_DISCONNECTED_SOUND_OPTION =
        intPreferencesKey(PreferencesKeyConstants.WHEN_DISCONNECTED_SOUND_OPTION)
    val WHEN_CONNECTED_SOUND_OPTION =
        intPreferencesKey(PreferencesKeyConstants.WHEN_CONNECTED_SOUND_OPTION)
    val DISCONNECTED_BUNDLE_SOUND_OPTION =
        intPreferencesKey(PreferencesKeyConstants.DISCONNECTED_BUNDLE_SOUND_OPTION)
    val CONNECTED_BUNDLE_SOUND_OPTION =
        intPreferencesKey(PreferencesKeyConstants.CONNECTED_BUNDLE_SOUND_OPTION)
    val DISCONNECTED_CUSTOM_SOUND =
        stringPreferencesKey(PreferencesKeyConstants.DISCONNECTED_CUSTOM_SOUND)
    val CONNECTED_CUSTOM_SOUND =
        stringPreferencesKey(PreferencesKeyConstants.CONNECTED_CUSTOM_SOUND)

    // Advanced Features
    val DECOY_TRAFFIC = booleanPreferencesKey(PreferencesKeyConstants.DECOY_TRAFFIC)
    val FAKE_TRAFFIC_VOLUME = stringPreferencesKey(PreferencesKeyConstants.FAKE_TRAFFIC_VOLUME)
    val ANTI_CENSORSHIP = booleanPreferencesKey(PreferencesKeyConstants.ANTI_CENSORSHIP)
    val AUTO_SECURE_NEW_NETWORKS =
        booleanPreferencesKey(PreferencesKeyConstants.AUTO_SECURE_NEW_NETWORKS)
    val GPS_SPOOF_SETTING = booleanPreferencesKey(PreferencesKeyConstants.GPS_SPOOF_SETTING)
    val DISABLE_KERNEL_MODULE = booleanPreferencesKey(PreferencesKeyConstants.DISABLE_KERNEL_MODULE)
    val MULTIPLE_TUNNELS = booleanPreferencesKey(PreferencesKeyConstants.MULTIPLE_TUNNELS)

    // App Lifecycle
    val ON_CREATE_APPLICATION = booleanPreferencesKey(PreferencesKeyConstants.ON_CREATE_APPLICATION)
    val MIGRATION_REQUIRED = booleanPreferencesKey("migration_required")
    val CONNECTION_COUNT = intPreferencesKey(PreferencesKeyConstants.CONNECTION_COUNT)
    val POWER_WHITE_LIST_POPUP_SHOW_COUNT =
        intPreferencesKey(PreferencesKeyConstants.POWER_WHITE_LIST_POPUP_SHOW_COUNT)

    // Purchase & Review
    val PURCHASE_FLOW_STATE_KEY =
        stringPreferencesKey(PreferencesKeyConstants.PURCHASE_FLOW_STATE_KEY)
    val ALREADY_SHOWN_SHARE_APP_LINK =
        booleanPreferencesKey(PreferencesKeyConstants.ALREADY_SHOWN_SHARE_APP_LINK)
    val PURCHASED_ITEM = stringPreferencesKey(PreferencesKeyConstants.PURCHASED_ITEM)
    val AMAZON_PURCHASED_ITEM = stringPreferencesKey(PreferencesKeyConstants.AMAZON_PURCHASED_ITEM)
    val CURRENT_STATUS_KEY = intPreferencesKey(PreferencesKeyConstants.CURRENT_STATUS_KEY)
    val LAST_UPDATE_TIME = stringPreferencesKey(PreferencesKeyConstants.LAST_UPDATE_TIME)

    // Network Configuration
    val ALC_LIST = stringPreferencesKey(PreferencesKeyConstants.ALC_LIST)
    val WHITELISTED_NETWORK = stringPreferencesKey(PreferencesKeyConstants.WHITELISTED_NETWORK)
    val WHITELIST_OVERRIDE = booleanPreferencesKey(PreferencesKeyConstants.WHITELIST_OVERRIDE)
    val SHOW_SYSTEM_APPS = booleanPreferencesKey(PreferencesKeyConstants.SHOW_SYSTEM_APPS)

    // DNS
    val DNS_MODE = stringPreferencesKey(PreferencesKeyConstants.DNS_MODE)
    val CUSTOM_DNS_ADDRESS = stringPreferencesKey(PreferencesKeyConstants.CUSTOM_DNS_ADDRESS)

    // Advanced Parameters
    val ADVANCE_PARAM_TEXT = stringPreferencesKey(PreferencesKeyConstants.ADVANCE_PARAM_TEXT)
    val WS_NET_SETTINGS = stringPreferencesKey(PreferencesKeyConstants.WS_NET_SETTINGS)
    val AUTO_CONNECT = booleanPreferencesKey(PreferencesKeyConstants.AUTO_CONNECT)
    val SUGGESTED_PROTOCOL = stringPreferencesKey(PreferencesKeyConstants.SUGGESTED_PROTOCOL)
    val SUGGESTED_PORT = stringPreferencesKey(PreferencesKeyConstants.SUGGESTED_PORT)
    val LOCATION_HASH = stringPreferencesKey(PreferencesKeyConstants.LOCATION_HASH)

    // System
    val DEVICE_ID = stringPreferencesKey(PreferencesKeyConstants.DEVICE_ID)
    val PORT_MAP_VERSION = intPreferencesKey(PreferencesKeyConstants.PORT_MAP_VERSION)

    // Complex types (JSON serialized)
    val INSTALLED_APPS_DATA = stringPreferencesKey(PreferencesKeyConstants.INSTALLED_APPS_DATA)
    val WG_CONNECT_API_FAIL_OVER_STATE =
        stringPreferencesKey(PreferencesKeyConstants.WG_CONNECT_API_FAIL_OVER_STATE)
    val USER_IP = stringPreferencesKey(PreferencesKeyConstants.USER_IP)
    val GET_SESSION = stringPreferencesKey(PreferencesKeyConstants.GET_SESSION)
    val PORT_MAP = stringPreferencesKey(PreferencesKeyConstants.PORT_MAP)
    val ROBERT_FILTERS = stringPreferencesKey(PreferencesKeyConstants.ROBERT_FILTERS)
    val FAVORITE_SERVER_LIST = stringPreferencesKey(PreferencesKeyConstants.FAVORITE_SERVER_LIST)

    // App Lifecycle & Installation
    val NEW_INSTALLATION = stringPreferencesKey(PreferencesKeyConstants.NEW_INSTALLATION)
    val CONNECTION_MODE_KEY = stringPreferencesKey(PreferencesKeyConstants.CONNECTION_MODE_KEY)

    // SSO
    val IS_SSO_LOGIN = booleanPreferencesKey(PreferencesKeyConstants.IS_SSO_LOGIN)

    // Credentials (stored as JSON strings in DataStore)
    val OPEN_VPN_CREDENTIALS = stringPreferencesKey(PreferencesKeyConstants.OPEN_VPN_CREDENTIALS)
    val IKEV2_CREDENTIALS = stringPreferencesKey(PreferencesKeyConstants.IKEV2_CREDENTIALS)
    val STATIC_IP_CREDENTIALS = stringPreferencesKey(PreferencesKeyConstants.STATIC_IP_CREDENTIAL)

    // OpenVPN Server Config (base64 encoded config)
    val OPEN_VPN_SERVER_CONFIG =
        stringPreferencesKey(PreferencesKeyConstants.OPEN_VPN_SERVER_CONFIG)

    // Dynamic keys (per-user, per-network, etc.)
    fun previousAccountStatus(userName: String) =
        intPreferencesKey("${userName}${PreferencesKeyConstants.PREVIOUS_ACCOUNT_STATUS}")

    fun previousUserStatus(userName: String) =
        intPreferencesKey("${userName}${PreferencesKeyConstants.PREVIOUS_USER_STATUS}")
}
