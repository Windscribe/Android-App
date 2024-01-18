/*
 * Copyright (c) 2021 Windscribe Limited.
 */
package com.windscribe.tv.base

import android.content.res.Configuration
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import butterknife.ButterKnife
import com.windscribe.tv.di.ActivityComponent
import com.windscribe.tv.di.ActivityModule
import com.windscribe.tv.di.DaggerActivityComponent
import com.windscribe.vpn.Windscribe.Companion.appContext
import java.util.Locale
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

abstract class BaseActivity : AppCompatActivity() {
    val coldLoad = AtomicBoolean()

    protected fun setActivityModule(activityModule: ActivityModule?): ActivityComponent {
        return DaggerActivityComponent.builder().activityModule(activityModule)
            .applicationComponent(
                appContext
                    .applicationComponent
            ).build()
    }

    protected fun setContentLayout(layoutID: Int) {
        coldLoad.set(true)
        updateLanguage()
        setContentView(layoutID)
        ButterKnife.bind(this)
    }

    protected fun updateLanguage() {
        val newLocale = appContext.getSavedLocale()
        Locale.setDefault(newLocale)
        val config = Configuration()
        config.locale = newLocale
        appContext.resources.updateConfiguration(config, baseContext.resources.displayMetrics)
        resources.updateConfiguration(config, baseContext.resources.displayMetrics)
    }

    fun activityScope(block: suspend CoroutineScope.() -> Unit) {
        lifecycleScope.launch {
            lifecycle.repeatOnLifecycle(Lifecycle.State.CREATED, block)
        }
    }
}
