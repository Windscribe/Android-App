/*
 * Copyright (c) 2021 Windscribe Limited.
 */

package com.windscribe.test

import com.windscribe.vpn.Windscribe
import com.windscribe.vpn.di.ApplicationComponent
import com.windscribe.vpn.di.ApplicationModule
import com.windscribe.vpn.di.DaggerApplicationTestComponent
import com.windscribe.vpn.di.TestConfiguration
import com.windscribe.vpn.di.TestPersistentModule

class TestApplication : Windscribe() {
    var testConfiguration: TestConfiguration? = null
    override fun getApplicationModuleComponent(): ApplicationComponent {
        val persistentModule = TestPersistentModule()
        persistentModule.testConfiguration = testConfiguration
        return DaggerApplicationTestComponent.builder().applicationModule(ApplicationModule(this))
            .testPersistentModule(persistentModule).build()
    }
}