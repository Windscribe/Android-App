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
import android.util.Log
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.runner.AndroidJUnitRunner
import androidx.work.testing.SynchronousExecutor
import androidx.work.testing.WorkManagerTestInitHelper
import com.google.gson.Gson
import com.windscribe.test.TestApplication
import com.windscribe.vpn.di.TestConfiguration
import java.util.Locale

open class CustomRunner : AndroidJUnitRunner() {
    private var testConfiguration: TestConfiguration? = null
    override fun onCreate(arguments: Bundle?) {
        StrictMode.setThreadPolicy(StrictMode.ThreadPolicy.Builder().permitAll().build())
        targetContext.assets.open("testConfiguration.json").bufferedReader().readText().let {
            testConfiguration = Gson().fromJson(it, TestConfiguration::class.java)
        }
        super.onCreate(arguments)
    }

    override fun newApplication(cl: ClassLoader, className: String, context: Context): Application {
        return super.newApplication(cl, TestApplication::class.java.name, context)
    }

    override fun callApplicationOnCreate(app: Application) {
        if (app is TestApplication) {
            app.testConfiguration = testConfiguration
            setLanguage(app)
            val context = InstrumentationRegistry.getInstrumentation().targetContext
            val config = androidx.work.Configuration.Builder().setMinimumLoggingLevel(Log.VERBOSE)
                .setExecutor(SynchronousExecutor()).build()
            WorkManagerTestInitHelper.initializeTestWorkManager(context, config)
            super.callApplicationOnCreate(app)
            app.applicationComponent.userRepository.synchronizedReload()
        } else {
            super.callApplicationOnCreate(app)
        }
    }

    private fun setLanguage(app: Application) {
        testConfiguration?.language?.let {
            val locale = Locale(it)
            Locale.setDefault(locale)
            val res: Resources = app.resources
            val config: Configuration = res.configuration
            config.setLocales(LocaleList(locale))
            res.updateConfiguration(config, context.resources.displayMetrics)
        }
    }
}