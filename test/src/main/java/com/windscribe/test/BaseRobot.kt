/*
 * Copyright (c) 2021 Windscribe Limited.
 */

package com.windscribe.test

import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import com.google.common.truth.Truth.assertThat

open class BaseRobot {
    private val device: UiDevice =
        UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())

    fun pressDeviceBackButton() {
        device.pressBack()
    }

    fun pageLoadedInBrowser() {
        val currentPackage: String = device.currentPackageName
        assertThat(currentPackage).isAnyOf("com.android.browser", "com.android.chrome")
    }
}