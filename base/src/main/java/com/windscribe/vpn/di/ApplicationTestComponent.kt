/*
 * Copyright (c) 2021 Windscribe Limited.
 */

package com.windscribe.vpn.di

import dagger.Component
import javax.inject.Singleton

@Singleton
@Component(modules = [ApplicationTestModule::class])
interface ApplicationTestComponent : ApplicationComponent