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
import com.windscribe.vpn.ActivityInteractor
import com.windscribe.vpn.ActivityInteractorImpl
import com.windscribe.vpn.api.IApiCallManager
import com.windscribe.vpn.apppreference.PreferencesHelper
import com.windscribe.vpn.autoconnection.AutoConnectionManager
import com.windscribe.vpn.backend.TrafficCounter
import com.windscribe.vpn.backend.utils.WindVpnController
import com.windscribe.vpn.decoytraffic.DecoyTrafficController
import com.windscribe.vpn.di.PerActivity
import com.windscribe.vpn.localdatabase.LocalDbInterface
import com.windscribe.vpn.repository.AdvanceParameterRepository
import com.windscribe.vpn.repository.ConnectionDataRepository
import com.windscribe.vpn.repository.LatencyRepository
import com.windscribe.vpn.repository.LocationRepository
import com.windscribe.vpn.repository.NotificationRepository
import com.windscribe.vpn.repository.ServerListRepository
import com.windscribe.vpn.repository.StaticIpRepository
import com.windscribe.vpn.repository.UserRepository
import com.windscribe.vpn.services.FirebaseManager
import com.windscribe.vpn.services.ReceiptValidator
import com.windscribe.vpn.state.NetworkInfoManager
import com.windscribe.vpn.state.PreferenceChangeObserver
import com.windscribe.vpn.state.VPNConnectionStateManager
import com.windscribe.vpn.workers.WindScribeWorkManager
import dagger.Module
import dagger.Provides
import kotlinx.coroutines.CoroutineScope

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
        activityInteractor: ActivityInteractor
    ): UpgradePresenter {
        return UpgradePresenterImpl(upgradeView, activityInteractor)
    }

    @Provides
    fun provideUpgradeView(): UpgradeView {
        return upgradeView
    }

    @Provides
    fun providesActivityScope(): LifecycleCoroutineScope {
        return activity.lifecycleScope
    }

    @Provides
    @PerActivity
    fun provideActivityInteractor(
        activityScope: LifecycleCoroutineScope,
        coroutineScope: CoroutineScope,
        prefHelper: PreferencesHelper,
        apiCallManager: IApiCallManager,
        localDbInterface: LocalDbInterface,
        vpnConnectionStateManager: VPNConnectionStateManager,
        userRepository: UserRepository,
        networkInfoManager: NetworkInfoManager,
        locationRepository: LocationRepository,
        vpnController: WindVpnController,
        connectionDataRepository: ConnectionDataRepository,
        serverListRepository: ServerListRepository,
        staticListUpdate: StaticIpRepository,
        preferenceChangeObserver: PreferenceChangeObserver,
        notificationRepository: NotificationRepository,
        workManager: WindScribeWorkManager,
        decoyTrafficController: DecoyTrafficController,
        trafficCounter: TrafficCounter,
        autoConnectionManager: AutoConnectionManager,
        latencyRepository: LatencyRepository,
        receiptValidator: ReceiptValidator,
        firebaseManager: FirebaseManager,
        advanceParameterRepository: AdvanceParameterRepository
    ): ActivityInteractor {
        return ActivityInteractorImpl(
            activityScope,
            coroutineScope,
            prefHelper,
            apiCallManager,
            localDbInterface,
            vpnConnectionStateManager,
            userRepository,
            networkInfoManager,
            locationRepository,
            vpnController,
            connectionDataRepository,
            serverListRepository,
            staticListUpdate,
            preferenceChangeObserver,
            notificationRepository,
            workManager,
            decoyTrafficController,
            trafficCounter,
            autoConnectionManager, latencyRepository, receiptValidator,
            firebaseManager,
            advanceParameterRepository
        )
    }
}
