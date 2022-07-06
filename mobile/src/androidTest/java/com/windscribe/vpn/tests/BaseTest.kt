/*
 * Copyright (c) 2021 Windscribe Limited.
 */

package com.windscribe.vpn.tests

import com.windscribe.test.BaseRobot
import com.windscribe.test.dispatcher.MockServerDispatcher
import com.windscribe.vpn.robots.HomeRobot
import com.windscribe.vpn.robots.LoginRobot
import com.windscribe.vpn.robots.SignUpRobot
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Before
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

    @Before
    fun setUp() {
        mockServer = MockWebServer()
        mockServer.dispatcher = MockServerDispatcher.ResponseDispatcher()
        mockServer.start(8080)
    }

    @After
    fun tearDown() = mockServer.shutdown()
}