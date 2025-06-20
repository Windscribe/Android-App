/*
 * Copyright (c) 2021 Windscribe Limited.
 */
package com.windscribe.vpn

import com.windscribe.vpn.di.ApplicationComponent
import com.windscribe.vpn.di.PerActivity
import com.windscribe.vpn.tests.BaseTest
import dagger.Component

@PerActivity
@Component(dependencies = [ApplicationComponent::class])
interface ActivityTestComponent {
    fun inject(test: BaseTest)
}
