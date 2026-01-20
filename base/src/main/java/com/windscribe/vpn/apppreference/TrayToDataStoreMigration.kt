/*
 * Copyright (c) 2021 Windscribe Limited.
 */
package com.windscribe.vpn.apppreference

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.windscribe.vpn.localdatabase.LocalDbInterface
import kotlinx.coroutines.flow.first
import org.slf4j.LoggerFactory

/**
 * One-time migration utility to migrate all preferences from Tray database (tray.db) to DataStore.
 * This migration reads directly from the SQLite database file without requiring the Tray library.
 */
class TrayToDataStoreMigration(private val context: Context, private val dataStore: DataStore<Preferences>) {
    private val logger = LoggerFactory.getLogger("migration")

    companion object {
        private const val MIGRATION_COMPLETE_KEY = "tray_migration_completed"
        private const val TRAY_DB_NAME = "tray.db"
        private const val TRAY_TABLE_NAME = "TrayPreferences"

        // Long keys
        private val LONG_KEYS = setOf(
            "login_time"
        )

        // Int keys (from DataStoreKeys.kt)
        private val INT_KEYS = setOf(
            "aspect_ratio_background_option",
            "connected_bundle_background_option",
            "connected_bundle_sound_option",
            "connection_attempt",
            "connection_count",
            "disconnected_bundle_background_option",
            "disconnected_bundle_sound_option",
            "flag_view_height",
            "flag_view_width",
            "future_selected_city",
            "is_premium_user",
            "last_mtu_value",
            "last_selected_server_tab",
            "lowest_ping_id",
            "our_ip",
            "port_map_version",
            "power_white_list_popup_show_count",
            "rate_dialog_key",
            "selected_city_id",
            "when_connected_background_option",
            "when_connected_sound_option",
            "when_disconnected_background_option",
            "when_disconnected_sound_option"
        )

        // Boolean keys (from DataStoreKeys.kt)
        private val BOOLEAN_KEYS = setOf(
            "already_shown_share_app_link",
            "always_on",
            "anti_censorship",
            "auto_connect",
            "auto_mtu_mode",
            "auto_secure_new_networks",
            "auto_start_boot",
            "blur_ip",
            "blur_network_name",
            "connection_retry_enabled",
            "custom_flag_background",
            "decoy_traffic",
            "disable_kernel_module",
            "global_preference",
            "gps_spoof_setting",
            "haptic_feedback",
            "is_connecting_configured",
            "is_connecting_static",
            "is_sso_login",
            "keep_alive_mode_auto",
            "lan_by_pass",
            "last_connection_using_split",
            "migration_required",
            "multiple_tunnels",
            "news_feed_alert",
            "notification_stat",
            "on_create_application",
            "ping_update_required",
            "reconnect_required",
            "show_latency_in_ms",
            "show_location_health",
            "show_system_apps",
            "started_by_always_on",
            "tunnel_toggle",
            "user_account_update_required",
            "user_intended_disconnect",
            "whitelist_override"
        )

        // String keys (from DataStoreKeys.kt + generic response strings)
        private val STRING_KEYS = setOf(
            // From DataStoreKeys.kt
            "advance_params_text",
            "alc_list",
            "chosen_protocol",
            "connected_custom_background",
            "connected_custom_sound",
            "connected_flag_path",
            "connection_status",
            "current_tag",
            "custom_dns_address",
            "device_id",
            "disconnected_custom_background",
            "disconnected_custom_sound",
            "disconnected_flag_path",
            "dns_mode",
            "fake_traffic_volume",
            "installed_app_data",
            "keep_alive",
            "list_selection_key",
            "loc_hash",
            "locale",
            "protocol",
            "purchase_flow_state",
            "saved_IkEv2_port",
            "saved_stealth_port",
            "saved_tcp_port",
            "saved_udp_port",
            "saved_wire_guard_port",
            "saved_ws_tunnel_port",
            "selected_theme",
            "split_routing_mode",
            "suggested_port",
            "suggested_protocol",
            "user_name",
            "wg_connect_api_fail_over_state",
            "whitelisted_network",
            "ws_net_settings",
            // From VpnPreferenceConstants (used with getResponseString)
            "selected_protocol",
            "selected_port",
            "selected_ip",
            "selected_protocol_type",
            // Generic response string keys (stored via saveResponseStringData)
            "access_api_ip_1",
            "access_api_ip_2",
            "amazon_purchase_item",
            "best_location_ip_2",
            "connection_mode",
            "data_left",
            "data_max",
            "data_used",
            "email_address",
            "favorite_server_list",
            "get_session_data",
            "IKev2_server_credentials",
            "last_time",
            "loc_rev",
            "new_installation",
            "port_map_data",
            "purchased_item",
            "robert_filters",
            "robert_settings",
            "server_config",
            "server_credentials",
            "session_auth_hash",
            "static_ip_credentials",
            "user_ip",
            "user_reg_data",
            "user_session_data",
            "wg_local_params",
            "wire_guard_config"
        )
    }

    /**
     * Check if migration has already been completed.
     */
    suspend fun isMigrationComplete(): Boolean {
        val prefs = dataStore.data.first()
        return prefs[stringPreferencesKey(MIGRATION_COMPLETE_KEY)] == "true"
    }

    /**
     * Perform one-time migration from Tray database to DataStore.
     * This reads directly from tray.db SQLite file.
     */
    suspend fun migrate(): MigrationResult {
        try {
            // Check if migration already completed
            if (isMigrationComplete()) {
                return MigrationResult.AlreadyCompleted
            }

            logger.info("Starting Tray to DataStore migration...")

            // Locate tray.db file in /data/data/com.windscribe.vpn/databases/
            val trayDbFile = context.getDatabasePath(TRAY_DB_NAME)
            logger.info("Looking for Tray database at: ${trayDbFile.absolutePath}")

            if (!trayDbFile.exists()) {
                logger.info("No Tray database found (fresh install)")
                markMigrationComplete()
                return MigrationResult.NoTrayData
            }

            // Open SQLite database directly
            val db = try {
                SQLiteDatabase.openDatabase(
                    trayDbFile.absolutePath,
                    null,
                    SQLiteDatabase.OPEN_READONLY
                )
            } catch (e: Exception) {
                logger.warn("Failed to open Tray database: ${e.message}")
                markMigrationComplete()
                return MigrationResult.NoTrayData
            }

            var successCount = 0
            var errorCount = 0
            val notificationIdsToMarkRead = mutableListOf<Int>()

            // Store all keys/values in a map first
            val allKeys = mutableMapOf<String, String>()

            try {
                // Query all preferences from Tray database
                // Tray stores preferences in a table with columns: KEY, VALUE, MODULE, CREATED, UPDATED
                // Note: Columns are UPPERCASE, and module is "com.windscribe.vpn"
                val cursor = db.rawQuery(
                    "SELECT KEY, VALUE FROM $TRAY_TABLE_NAME WHERE MODULE = ?",
                    arrayOf("com.windscribe.vpn")
                )

                if (cursor.count == 0) {
                    logger.info("No Tray data to migrate")
                    cursor.close()
                    db.close()
                    markMigrationComplete()
                    return MigrationResult.NoTrayData
                }

                logger.info("Found ${cursor.count} items to migrate from Tray")

                // First pass: collect all keys and values
                cursor.use { c ->
                    while (c.moveToNext()) {
                        val key = c.getString(0)
                        val value = c.getString(1)
                        if (key != null && value != null) {
                            allKeys[key] = value
                        } else if (key != null) {
                            logger.info("Skipping key with null value: $key")
                        }
                    }
                }

                // Migrate all items to DataStore with proper type conversion
                // Use iterator to safely remove items during iteration
                val keysToRemove = mutableListOf<String>()

                dataStore.edit { preferences ->
                    allKeys.forEach { (key, value) ->
                        try {
                            var migrated = false
                            when {
                                key in LONG_KEYS -> {
                                    value.toLongOrNull()?.let {
                                        preferences[longPreferencesKey(key)] = it
                                        successCount++
                                        migrated = true
                                    }
                                }

                                key in INT_KEYS -> {
                                    value.toIntOrNull()?.let {
                                        preferences[intPreferencesKey(key)] = it
                                        successCount++
                                        migrated = true
                                    }
                                }

                                key in BOOLEAN_KEYS -> {
                                    val boolValue = when (value.lowercase()) {
                                        "true", "1" -> true
                                        "false", "0" -> false
                                        else -> null
                                    }
                                    boolValue?.let {
                                        preferences[booleanPreferencesKey(key)] = it
                                        successCount++
                                        migrated = true
                                    }
                                }

                                key in STRING_KEYS -> {
                                    preferences[stringPreferencesKey(key)] = value
                                    successCount++
                                    migrated = true
                                }
                                // Dynamic keys
                                key.endsWith("previous_account_status") || key.endsWith("previous_user_status") -> {
                                    value.toIntOrNull()?.let {
                                        preferences[intPreferencesKey(key)] = it
                                        successCount++
                                        migrated = true
                                    }
                                }
                                // Check if this is a notification ID (numeric key + boolean value)
                                else -> {
                                    val notificationId = key.toIntOrNull()
                                    val isRead = when (value.lowercase()) {
                                        "true", "1" -> true
                                        "false", "0" -> false
                                        else -> null
                                    }

                                    if (notificationId != null && isRead != null && isRead) {
                                        notificationIdsToMarkRead.add(notificationId)
                                        successCount++
                                        migrated = true
                                    }
                                }
                            }

                            // Track successfully migrated keys for removal
                            if (migrated) {
                                keysToRemove.add(key)
                            }
                        } catch (e: Exception) {
                            logger.error("Failed to migrate item $key: ${e.message}")
                            errorCount++
                        }
                    }
                }

                // Remove migrated keys after iteration completes
                keysToRemove.forEach { allKeys.remove(it) }

                logger.info("Migration complete: $successCount items migrated, $errorCount errors")

                // Log unmigrated keys summary (whatever remains in allKeys)
                if (allKeys.isNotEmpty()) {
                    logger.info("=== UNMIGRATED KEYS (${allKeys.size}) ===")
                    allKeys.forEach { (key, value) ->
                        logger.info("  $key = $value")
                    }
                    logger.info("=== END UNMIGRATED KEYS ===")
                }

            } finally {
                db.close()
            }

            markMigrationComplete()
            return MigrationResult.Success(successCount, errorCount, allKeys.size, notificationIdsToMarkRead)

        } catch (e: Exception) {
            logger.error("Migration failed with exception: ${e.message}", e)
            return MigrationResult.Error(e.message ?: "Unknown error")
        }
    }

    private suspend fun markMigrationComplete() {
        dataStore.edit { preferences ->
            preferences[stringPreferencesKey(MIGRATION_COMPLETE_KEY)] = "true"
        }
    }
}

sealed class MigrationResult {
    data class Success(
        val migratedCount: Int,
        val errorCount: Int,
        val unmigratedCount: Int,
        val notificationIdsToMarkRead: List<Int> = emptyList()
    ) : MigrationResult()
    object AlreadyCompleted : MigrationResult()
    object NoTrayData : MigrationResult()
    data class Error(val message: String) : MigrationResult()
}
