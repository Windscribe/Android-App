/*
 * Copyright (c) 2021 Windscribe Limited.
 */
package com.windscribe.vpn.apppreference

import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.windscribe.vpn.Windscribe
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.slf4j.LoggerFactory
import java.io.IOException
import java.security.GeneralSecurityException

class SecurePreferences(
    private val app: Windscribe,
) {
    private val logger = LoggerFactory.getLogger("secure_prefs")
    private val secureSharedPrefsFile = "windscribe_secure_prefs"
    private val fallbackSharedPrefsFile = "windscribe_unsecured_preferences"
    private val _encryptionAvailable = MutableStateFlow<Boolean?>(null)
    val encryptionAvailable: StateFlow<Boolean?> = _encryptionAvailable.asStateFlow()

    // DEBUG: Set to true to force encryption failure for testing
    private val FORCE_ENCRYPTION_FAILURE = false

    // Lazy initialization - only creates prefs on first access
    private val sharedPreferences: SharedPreferences by lazy {
        if (FORCE_ENCRYPTION_FAILURE) {
            _encryptionAvailable.value = false
            logger.error("DEBUG: Forcing encryption failure for testing")
            logger.error("SECURITY WARNING: Encrypted storage initialization failed - DEBUG MODE")
            logger.error("Sensitive credentials will be stored without encryption")
            return@lazy createFallbackPreferences()
        }

        try {
            val masterKey =
                MasterKey
                    .Builder(app.applicationContext)
                    .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                    .build()
            val prefs =
                EncryptedSharedPreferences.create(
                    app.applicationContext,
                    secureSharedPrefsFile,
                    masterKey,
                    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
                )
            _encryptionAvailable.value = true
            logger.info("Encrypted storage initialized successfully")
            prefs
        } catch (e: GeneralSecurityException) {
            _encryptionAvailable.value = false
            logger.error("SECURITY WARNING: Encrypted storage initialization failed - Keystore error: ${e.message}")
            logger.error("Sensitive credentials will be stored without encryption")
            createFallbackPreferences()
        } catch (e: IOException) {
            _encryptionAvailable.value = false
            logger.error("SECURITY WARNING: Encrypted storage initialization failed - I/O error: ${e.message}")
            logger.error("Sensitive credentials will be stored without encryption")
            createFallbackPreferences()
        }
    }

    private fun createFallbackPreferences(): SharedPreferences {
        logger.warn("Using unencrypted fallback storage: $fallbackSharedPrefsFile")
        return app.getSharedPreferences(fallbackSharedPrefsFile, 0)
    }

    fun clear() {
        sharedPreferences.edit().clear().apply()
    }

    fun getString(
        key: String,
        defaultValue: String?,
    ): String? = sharedPreferences.getString(key, defaultValue)

    fun putStringSync(
        key: String,
        value: String?,
    ) {
        sharedPreferences.edit().putString(key, value).commit()
    }
}
