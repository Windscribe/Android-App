/*
 * Copyright (c) 2021 Windscribe Limited.
 */
package com.windscribe.mobile.di

import com.windscribe.mobile.upgradeactivity.UpgradePresenter
import com.windscribe.mobile.upgradeactivity.UpgradePresenterImpl
import com.windscribe.mobile.upgradeactivity.UpgradeView
import com.windscribe.vpn.ActivityInteractor
import com.windscribe.vpn.di.PerActivity
import dagger.Module
import dagger.Provides

@Module
open class ActivityModule  {
    private lateinit var upgradeView: UpgradeView

    @Provides
    @PerActivity
    fun provideUpgradePresenter(
            activityInteractor: ActivityInteractor
    ): UpgradePresenter {
        return UpgradePresenterImpl(upgradeView, activityInteractor)
    }

    @Provides
    fun provideUpgradeView(): UpgradeView {
        return upgradeView
    }
}
