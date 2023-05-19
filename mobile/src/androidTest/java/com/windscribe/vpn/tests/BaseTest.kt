/*
 * Copyright (c) 2021 Windscribe Limited.
 */

package com.windscribe.vpn.tests

import androidx.test.espresso.IdlingRegistry
import androidx.test.espresso.idling.CountingIdlingResource
import androidx.test.espresso.intent.Intents
import com.windscribe.test.dispatcher.MockServerDispatcher
import com.windscribe.vpn.Windscribe
import com.windscribe.vpn.di.TestConfiguration
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Before

open class BaseTest {

    private var mockServer: MockWebServer? = null
    private var app = Windscribe.appContext.applicationComponent
    var countingIdlingResource = CountingIdlingResource("countingResource")

    @Before
    open fun setup() {
        Intents.init()
        IdlingRegistry.getInstance().register(countingIdlingResource)
        mockServer = MockWebServer()
        mockServer?.dispatcher = MockServerDispatcher.ResponseDispatcher()
        mockServer?.start(8080)
    }

    @After
    open fun tearDown() {
        Intents.release()
        IdlingRegistry.getInstance().unregister(countingIdlingResource)
        mockServer?.shutdown()
    }

    fun updatedUserConfiguration(testConfiguration: TestConfiguration = TestConfiguration()) {
        testConfiguration.update(app.preferencesHelper)
        app.userRepository.synchronizedReload()
    }
}