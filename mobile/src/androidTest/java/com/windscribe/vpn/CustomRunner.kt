/*
 * Copyright (c) 2021 Windscribe Limited.
 */

package com.windscribe.vpn

import android.app.Application
import android.content.Context
import android.os.Bundle
import android.os.StrictMode
import androidx.test.runner.AndroidJUnitRunner
import com.squareup.rx2.idler.Rx2Idler
import com.windscribe.test.TestApplication
import io.reactivex.plugins.RxJavaPlugins

open class CustomRunner : AndroidJUnitRunner() {

    override fun onCreate(arguments: Bundle?) {
        StrictMode.setThreadPolicy(StrictMode.ThreadPolicy.Builder().permitAll().build())
        super.onCreate(arguments)
    }

    override fun newApplication(cl: ClassLoader, className: String, context: Context): Application {
        return super.newApplication(cl, TestApplication::class.java.name, context)
    }

    override fun onStart() {
        RxJavaPlugins.setInitIoSchedulerHandler(
                Rx2Idler.create("IO-Scheduler"))
        super.onStart()
    }
}