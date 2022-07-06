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
import java.util.Locale

/**
 * Application context wrapper.
 * Sets app location.
 */
class WindContextWrapper(base: Context) : ContextWrapper(base) {

    companion object {

        @JvmStatic
        fun changeLocale(context: Context, locale: String) {
            val res = context.resources
            val configuration = Configuration(res.configuration)
            val newLocale = Locale(locale)
            if (VERSION.SDK_INT >= VERSION_CODES.N) {
                configuration.setLocale(newLocale)
                val localeList = LocaleList(newLocale)
                LocaleList.setDefault(localeList)
                configuration.setLocales(localeList)
            } else {
                configuration.setLocale(newLocale)
            }
            context.createConfigurationContext(configuration)
            res.updateConfiguration(configuration, res.displayMetrics)
            ContextWrapper(context)
        }

        fun setAppLocale(context: Context) {
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
            context.createConfigurationContext(configuration)
            res.updateConfiguration(configuration, res.displayMetrics)
        }
    }
}
