/*
 * Copyright (c) 2021 Windscribe Limited.
 */
package com.windscribe.mobile.di

import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.LifecycleCoroutineScope
import androidx.lifecycle.lifecycleScope
import com.windscribe.mobile.upgradeactivity.UpgradePresenter
import com.windscribe.mobile.upgradeactivity.UpgradePresenterImpl
import com.windscribe.mobile.upgradeactivity.UpgradeView
import com.windscribe.vpn.api.IApiCallManager
import com.windscribe.vpn.apppreference.PreferencesHelper
import com.windscribe.vpn.di.PerActivity
import com.windscribe.vpn.repository.ConnectionDataRepository
import com.windscribe.vpn.repository.ServerListRepository
import com.windscribe.vpn.repository.UserRepository
import com.windscribe.vpn.services.ReceiptValidator
import dagger.Module
import dagger.Provides

@Module
open class ActivityModule {
    private var upgradeView: UpgradeView
    private var activity: AppCompatActivity

    constructor(mActivity: AppCompatActivity, upgradeView: UpgradeView) {
        this.activity = mActivity
        this.upgradeView = upgradeView
    }

    @Provides
    @PerActivity
    fun provideUpgradePresenter(
        activityScope: LifecycleCoroutineScope,
        preferencesHelper: PreferencesHelper,
        apiCallManager: IApiCallManager,
        userRepository: UserRepository,
        receiptValidator: ReceiptValidator,
        connectionDataRepository: ConnectionDataRepository,
        serverListRepository: ServerListRepository
    ): UpgradePresenter {
        return UpgradePresenterImpl(
            upgradeView,
            activityScope,
            preferencesHelper,
            apiCallManager,
            userRepository,
            receiptValidator,
            connectionDataRepository,
            serverListRepository
        )
    }

    @Provides
    fun provideUpgradeView(): UpgradeView {
        return upgradeView
    }

    @Provides
    fun providesActivityScope(): LifecycleCoroutineScope {
        return activity.lifecycleScope
    }
}
