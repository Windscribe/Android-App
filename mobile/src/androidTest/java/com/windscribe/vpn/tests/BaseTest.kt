/*
 * Copyright (c) 2021 Windscribe Limited.
 */

package com.windscribe.vpn.tests

import android.app.Activity
import android.graphics.Bitmap
import android.view.View
import android.view.ViewGroup
import androidx.test.espresso.Espresso
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.runner.screenshot.BasicScreenCaptureProcessor
import androidx.test.runner.screenshot.ScreenCaptureProcessor
import androidx.test.runner.screenshot.Screenshot
import com.windscribe.test.BaseRobot
import com.windscribe.test.dispatcher.MockServerDispatcher
import com.windscribe.vpn.DaggerActivityTestComponent
import com.windscribe.vpn.Windscribe
import com.windscribe.vpn.apppreference.PreferencesHelper
import com.windscribe.vpn.localdatabase.LocalDbInterface
import com.windscribe.vpn.robots.HomeRobot
import com.windscribe.vpn.robots.LoginRobot
import com.windscribe.vpn.robots.SignUpRobot
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Before
import javax.inject.Inject
import kotlin.reflect.KClass
import kotlin.reflect.full.createInstance

open class BaseTest {

    val buildLoginRobot = robotRunner(LoginRobot::class)
    val buildSignUpRobot = robotRunner(SignUpRobot::class)
    val buildHomeRobot = robotRunner(HomeRobot::class)

    private fun <T : BaseRobot> robotRunner(cls: KClass<T>) = { func: T.() -> Unit ->
        cls.createInstance().apply {
            func()
        }
    }

    lateinit var mockServer: MockWebServer

    @Inject
    lateinit var preferencesHelper: PreferencesHelper

    @Inject
    lateinit var localDbInterface: LocalDbInterface

    @Before
    fun setUp() {
        DaggerActivityTestComponent.builder()
            .applicationComponent(Windscribe.appContext.applicationComponent).build().inject(this)
        preferencesHelper.clearAllData()
        localDbInterface.clearAllTables()
        mockServer = MockWebServer()
        mockServer.dispatcher = MockServerDispatcher.ResponseDispatcher()
        mockServer.start(8080)
    }

    @After
    fun tearDown() {
        preferencesHelper.clearAllData()
        localDbInterface.clearAllTables()
        mockServer.shutdown()
    }

    private fun getCurrentActivity(): Activity? {
        val activity: Array<Activity?> = arrayOfNulls(1)
        Espresso.onView(ViewMatchers.isRoot()).check { view, _ ->
            var checkedView: View = view
            while (checkedView is ViewGroup && checkedView.childCount > 0) {
                checkedView = checkedView.getChildAt(0)
                if (checkedView.context is Activity) {
                    activity[0] = checkedView.context as Activity
                    return@check
                }
            }
        }
        return activity[0]
    }

    fun captureScreenshot(name: String) {
        try {
            val capture = getCurrentActivity()?.let { Screenshot.capture(it) }
            capture?.format = Bitmap.CompressFormat.PNG
            capture?.name = name
            val processors = HashSet<ScreenCaptureProcessor>()
            processors.add(BasicScreenCaptureProcessor())
            capture?.process(processors)
        } catch (e: Exception) {
            throw IllegalStateException(e)
        }
    }
}