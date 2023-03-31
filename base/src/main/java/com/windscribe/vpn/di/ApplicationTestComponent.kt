/*
 * Copyright (c) 2021 Windscribe Limited.
 */

package com.windscribe.vpn.di

import dagger.Component
import javax.inject.Singleton

@Singleton
@Component(modules = [ApplicationModule::class, TestNetworkModule::class, TestVPNModule::class])
interface ApplicationTestComponent : ApplicationComponent