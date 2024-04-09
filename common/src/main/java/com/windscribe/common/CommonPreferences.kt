package com.windscribe.common

import android.content.Context

object CommonPreferences {
    private const val PREFERENCES = "common_preferences";
    fun saveBootstrapIp(context: Context, url: String, ip: String?) {
        val prefs = context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)
        prefs.edit().putString(url, ip).apply()
    }
    fun getBootstrapIp(context: Context, url: String): String? {
        return context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE).getString(url, null)
    }
}