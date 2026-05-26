/*
 * Copyright (c) 2021 Windscribe Limited.
 */
package com.windscribe.tv.base

import android.content.res.Configuration
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.windscribe.vpn.Windscribe.Companion.appContext
import java.util.Locale
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

abstract class BaseActivity : AppCompatActivity() {
    val coldLoad = AtomicBoolean()

    protected fun setContentLayout(layoutID: Int) {
        coldLoad.set(true)
        applyAppLocale()
        setContentView(layoutID)
    }

    protected fun onActivityLaunch() {
        coldLoad.set(true)
        applyAppLocale()
    }

    fun activityScope(block: suspend CoroutineScope.() -> Unit) {
        lifecycleScope.launch {
            lifecycle.repeatOnLifecycle(Lifecycle.State.CREATED, block)
        }
    }
}

/**
 * Extension function for AppCompatActivity to set app locale before setContentView.
 * Use this in any TV activity's onCreate before calling setContentView.
 */
fun AppCompatActivity.applyAppLocale() {
    val newLocale = appContext.getSavedLocale()
    Locale.setDefault(newLocale)
    val config = Configuration()
    @Suppress("DEPRECATION")
    config.locale = newLocale
    @Suppress("DEPRECATION")
    appContext.resources.updateConfiguration(config, resources.displayMetrics)
    @Suppress("DEPRECATION")
    resources.updateConfiguration(config, resources.displayMetrics)
}

