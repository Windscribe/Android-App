/*
 * Copyright (c) 2021 Windscribe Limited.
 */
package com.windscribe.vpn.apppreference

import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.windscribe.vpn.Windscribe
import org.slf4j.LoggerFactory
import java.io.IOException
import java.security.GeneralSecurityException

class SecurePreferences(app: Windscribe) {
    private val logger = LoggerFactory.getLogger("secure_p")
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
        var masterKey: MasterKey? = null
        try {
            masterKey = MasterKey.Builder(app.applicationContext)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()
        } catch (e: GeneralSecurityException) {
            logger.debug("Failed to create master key:", e)
        } catch (e: IOException) {
            logger.debug("Failed to create master key:", e)
        }
        try {
            masterKey?.let {
                sharedPreferences = EncryptedSharedPreferences.create(
                    app.applicationContext,
                    secureSharedPrefsFile,
                    it,
                    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
                )
            }
        } catch (e: GeneralSecurityException) {
            logger.error("Failed to create Encrypted preferences:", e)
        } catch (e: IOException) {
            logger.error("Failed to create Encrypted preferences:", e)
        }
    }
}
