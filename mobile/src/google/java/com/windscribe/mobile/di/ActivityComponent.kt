/*
 * Copyright (c) 2021 Windscribe Limited.
 */
package com.windscribe.mobile.di

import com.windscribe.mobile.upgradeactivity.UpgradeActivity
import com.windscribe.vpn.di.ApplicationComponent
import com.windscribe.vpn.di.PerActivity
import dagger.Component

@PerActivity
@Component(dependencies = [ApplicationComponent::class], modules = [ActivityModule::class])
interface ActivityComponent  {
    fun inject(upgradeActivity: UpgradeActivity)
}