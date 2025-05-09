/*
 * Copyright (c) 2021 Windscribe Limited.
 */
package com.windscribe.vpn.apppreference

import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.windscribe.vpn.Windscribe
import org.slf4j.LoggerFactory

class SecurePreferences(app: Windscribe) {
    private val logger = LoggerFactory.getLogger("basic")
    private val secureSharedPrefsFile = "windscribe_secure_prefs"
    private var sharedPreferences: SharedPreferences? = null
    fun clear() {
        sharedPreferences?.edit()?.clear()?.apply()
    }

    fun getString(key: String, defaultValue: String?): String? {
        return sharedPreferences?.let {
            return it.getString(key, defaultValue)
        }
    }

    fun putString(key: String, value: String?) {
        sharedPreferences?.edit()?.putString(key, value)?.apply()
    }

    init {
        try {
            val masterKey = MasterKey.Builder(app.applicationContext)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()
            sharedPreferences = EncryptedSharedPreferences.create(
                app.applicationContext,
                secureSharedPrefsFile,
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        } catch (e: Exception) {
            logger.debug("Failed to create EncryptedSharedPreferences ${e.localizedMessage}")
            // Some Chinese Android tv boxes may throw security exception.
            if (sharedPreferences == null) {
                sharedPreferences = app.getSharedPreferences("windscribe_unsecured_preferences", 0)
            }
        }
    }
}
