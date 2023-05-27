/*
 * Copyright (c) 2021 Windscribe Limited.
 */

package com.windscribe.vpn.di

import com.windscribe.vpn.Windscribe
import dagger.Component
import javax.inject.Singleton

@Singleton
@Component(modules = [ApplicationModule::class, TestNetworkModule::class, TestVPNModule::class, TestPersistentModule::class])
interface ApplicationTestComponent : ApplicationComponent {
    override fun inject(app: Windscribe)
}