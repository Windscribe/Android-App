package com.windscribe.mobile.di

import com.windscribe.mobile.upgradeactivity.UpgradePresenter
import com.windscribe.mobile.upgradeactivity.UpgradePresenterImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ActivityComponent
import dagger.hilt.android.scopes.ActivityScoped

@Module
@InstallIn(ActivityComponent::class)
abstract class MobileUpgradeModule {
    @Binds
    @ActivityScoped
    abstract fun bindUpgradePresenter(impl: UpgradePresenterImpl): UpgradePresenter
}
