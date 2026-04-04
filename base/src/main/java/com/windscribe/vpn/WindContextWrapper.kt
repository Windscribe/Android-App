/*
 * Copyright (c) 2021 Windscribe Limited.
 */

package com.windscribe.vpn

import android.content.Context
import android.content.ContextWrapper
import android.content.res.Configuration
import android.os.Build.VERSION
import android.os.Build.VERSION_CODES
import android.os.LocaleList
import com.windscribe.vpn.Windscribe.Companion.appContext

/**
 * Application context wrapper.
 * Sets app location.
 */
class WindContextWrapper(base: Context) : ContextWrapper(base) {

    companion object {

        fun setAppLocale(context: Context) {
            try {
                val res = context.resources
                val configuration = Configuration(res.configuration)
                val newLocale = appContext.getSavedLocale()
                if (VERSION.SDK_INT >= VERSION_CODES.N) {
                    configuration.setLocale(newLocale)
                    val localeList = LocaleList(newLocale)
                    LocaleList.setDefault(localeList)
                    configuration.setLocales(localeList)
                } else {
                    configuration.setLocale(newLocale)
                }
                // Removed: context.createConfigurationContext(configuration) - wasteful ANR cause
                @Suppress("DEPRECATION")
                res.updateConfiguration(configuration, res.displayMetrics)
            } catch (_: Exception) {
                // Silently fail locale change to prevent ANR - app will use system locale
            }
        }
    }
}
