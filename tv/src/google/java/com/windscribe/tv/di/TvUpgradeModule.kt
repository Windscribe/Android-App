package com.windscribe.tv.di

import com.windscribe.tv.upgrade.UpgradePresenter
import com.windscribe.tv.upgrade.UpgradePresenterImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ActivityComponent
import dagger.hilt.android.scopes.ActivityScoped

@Module
@InstallIn(ActivityComponent::class)
abstract class TvUpgradeModule {
    @Binds
    @ActivityScoped
    abstract fun bindUpgradePresenter(impl: UpgradePresenterImpl): UpgradePresenter
}
