/*
 * Copyright (c) 2021 Windscribe Limited.
 */
package com.windscribe.vpn.apppreference

import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.windscribe.vpn.Windscribe
import org.slf4j.LoggerFactory

class SecurePreferences(private val app: Windscribe) {
    private val logger = LoggerFactory.getLogger("basic")
    private val secureSharedPrefsFile = "windscribe_secure_prefs"

    // Lazy initialization - only creates encrypted prefs on first access
    private val sharedPreferences: SharedPreferences by lazy {
        try {
            val masterKey = MasterKey.Builder(app.applicationContext)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()
            EncryptedSharedPreferences.create(
                app.applicationContext,
                secureSharedPrefsFile,
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        } catch (e: Exception) {
            logger.debug("Failed to create EncryptedSharedPreferences ${e.localizedMessage}")
            // Some Chinese Android tv boxes may throw security exception.
            app.getSharedPreferences("windscribe_unsecured_preferences", 0)
        }
    }

    fun clear() {
        sharedPreferences.edit().clear().apply()
    }

    fun getString(key: String, defaultValue: String?): String? {
        return sharedPreferences.getString(key, defaultValue)
    }

    fun putString(key: String, value: String?) {
        sharedPreferences.edit().putString(key, value).apply()  // ASYNC
    }

    fun putStringSync(key: String, value: String?) {
        sharedPreferences.edit().putString(key, value).commit()  // SYNC - blocks until saved
    }
}
