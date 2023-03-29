/*
 * Copyright (c) 2021 Windscribe Limited.
 */

package com.windscribe.vpn

import android.app.Application
import android.content.Context
import android.content.res.Configuration
import android.content.res.Resources
import android.os.Bundle
import android.os.LocaleList
import android.os.StrictMode
import androidx.test.runner.AndroidJUnitRunner
import com.squareup.rx2.idler.Rx2Idler
import com.windscribe.test.TestApplication
import io.reactivex.plugins.RxJavaPlugins
import java.util.*

open class CustomRunner : AndroidJUnitRunner() {
    private var language = "en"
    override fun onCreate(arguments: Bundle?) {
        StrictMode.setThreadPolicy(StrictMode.ThreadPolicy.Builder().permitAll().build())
        arguments?.getString("language")?.let {
            language = it
        }
        super.onCreate(arguments)
    }

    override fun newApplication(cl: ClassLoader, className: String, context: Context): Application {
        return super.newApplication(cl, TestApplication::class.java.name, context)
    }

    override fun callApplicationOnCreate(app: Application) {
        setLanguage(app)
        super.callApplicationOnCreate(app)
    }

    override fun onStart() {
        RxJavaPlugins.setInitIoSchedulerHandler(
            Rx2Idler.create("IO-Scheduler")
        )
        super.onStart()
    }

    private fun setLanguage(app: Application) {
        val locale = Locale(language)
        Locale.setDefault(locale)
        val res: Resources = app.resources
        val config: Configuration = res.configuration
        config.setLocales(LocaleList(locale))
        res.updateConfiguration(config, context.resources.displayMetrics)
    }
}