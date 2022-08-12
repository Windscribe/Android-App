/*
 * Copyright (c) 2021 Windscribe Limited.
 */
package com.windscribe.tv.di

import android.animation.ArgbEvaluator
import androidx.appcompat.app.AppCompatActivity
import com.windscribe.tv.confirmemail.ConfirmEmailPresenter
import com.windscribe.tv.confirmemail.ConfirmEmailPresenterImp
import com.windscribe.tv.confirmemail.ConfirmEmailView
import com.windscribe.tv.customview.CustomDialog
import com.windscribe.tv.email.AddEmailPresenter
import com.windscribe.tv.email.AddEmailPresenterImpl
import com.windscribe.tv.email.AddEmailView
import com.windscribe.tv.news.NewsFeedPresenter
import com.windscribe.tv.news.NewsFeedPresenterImpl
import com.windscribe.tv.news.NewsFeedView
import com.windscribe.tv.rate.RateMyAppPresenter
import com.windscribe.tv.rate.RateMyAppPresenterImp
import com.windscribe.tv.rate.RateView
import com.windscribe.tv.serverlist.detail.DetailPresenter
import com.windscribe.tv.serverlist.detail.DetailView
import com.windscribe.tv.serverlist.detail.DetailsPresenterImp
import com.windscribe.tv.serverlist.overlay.OverlayPresenter
import com.windscribe.tv.serverlist.overlay.OverlayPresenterImp
import com.windscribe.tv.serverlist.overlay.OverlayView
import com.windscribe.tv.settings.SettingView
import com.windscribe.tv.settings.SettingsPresenter
import com.windscribe.tv.settings.SettingsPresenterImp
import com.windscribe.tv.splash.SplashPresenter
import com.windscribe.tv.splash.SplashPresenterImpl
import com.windscribe.tv.splash.SplashView
import com.windscribe.tv.upgrade.UpgradePresenter
import com.windscribe.tv.upgrade.UpgradePresenterImpl
import com.windscribe.tv.upgrade.UpgradeView
import com.windscribe.tv.welcome.WelcomePresenter
import com.windscribe.tv.welcome.WelcomePresenterImpl
import com.windscribe.tv.welcome.WelcomeView
import com.windscribe.tv.windscribe.WindscribePresenter
import com.windscribe.tv.windscribe.WindscribePresenterImpl
import com.windscribe.tv.windscribe.WindscribeView
import com.windscribe.vpn.ActivityInteractor
import com.windscribe.vpn.ActivityInteractorImpl
import com.windscribe.vpn.api.IApiCallManager
import com.windscribe.vpn.apppreference.PreferencesHelper
import com.windscribe.vpn.backend.TrafficCounter
import com.windscribe.vpn.backend.utils.ProtocolManager
import com.windscribe.vpn.backend.utils.WindVpnController
import com.windscribe.vpn.decoytraffic.DecoyTrafficController
import com.windscribe.vpn.localdatabase.LocalDbInterface
import com.windscribe.vpn.repository.*
import com.windscribe.vpn.state.NetworkInfoManager
import com.windscribe.vpn.state.PreferenceChangeObserver
import com.windscribe.vpn.state.VPNConnectionStateManager
import com.windscribe.vpn.workers.WindScribeWorkManager
import dagger.Module
import dagger.Provides
import kotlinx.coroutines.CoroutineScope

@Module
class ActivityModule {
    private lateinit var confirmEmailView: ConfirmEmailView
    private val activity: AppCompatActivity
    private lateinit var emailView: AddEmailView
    private lateinit var newsFeedView: NewsFeedView
    private lateinit var splashView: SplashView
    private lateinit var upgradeView: UpgradeView
    private lateinit var windscribeView: WindscribeView
    private lateinit var detailView: DetailView
    private lateinit var overlayView: OverlayView
    private lateinit var rateView: RateView
    private lateinit var settingView: SettingView
    private lateinit var welcomeView: WelcomeView

    constructor(activity: AppCompatActivity, confirmEmailView: ConfirmEmailView) {
        this.activity = activity
        this.confirmEmailView = confirmEmailView
    }

    constructor(activity: AppCompatActivity, rateView: RateView) {
        this.activity = activity
        this.rateView = rateView
    }

    constructor(activity: AppCompatActivity, welcomeView: WelcomeView) {
        this.activity = activity
        this.welcomeView = welcomeView
    }

    constructor(activity: AppCompatActivity) {
        this.activity = activity
    }

    constructor(activity: AppCompatActivity, splashView: SplashView) {
        this.activity = activity
        this.splashView = splashView
    }

    constructor(activity: AppCompatActivity, overlayView: OverlayView) {
        this.activity = activity
        this.overlayView = overlayView
    }

    constructor(activity: AppCompatActivity, settingView: SettingView) {
        this.activity = activity
        this.settingView = settingView
    }

    constructor(activity: AppCompatActivity, windscribeView: WindscribeView) {
        this.activity = activity
        this.windscribeView = windscribeView
    }

    constructor(activity: AppCompatActivity, upgradeView: UpgradeView) {
        this.activity = activity
        this.upgradeView = upgradeView
    }

    constructor(activity: AppCompatActivity, newsFeedView: NewsFeedView) {
        this.activity = activity
        this.newsFeedView = newsFeedView
    }

    constructor(activity: AppCompatActivity, emailView: AddEmailView) {
        this.activity = activity
        this.emailView = emailView
    }

    constructor(activity: AppCompatActivity, detailView: DetailView) {
        this.activity = activity
        this.detailView = detailView
    }

    @Provides
    fun provideActivity(): AppCompatActivity {
        return activity
    }

    @Provides
    @PerActivity
    fun provideAddEmailPresenter(activityInteractor: ActivityInteractor): AddEmailPresenter {
        return AddEmailPresenterImpl(emailView, activityInteractor)
    }

    @Provides
    @PerActivity
    fun provideConfirmEmailPresenter(
        confirmEmailView: ConfirmEmailView,
        activityInteractor: ActivityInteractor
    ): ConfirmEmailPresenter {
        return ConfirmEmailPresenterImp(confirmEmailView, activityInteractor)
    }

    @Provides
    fun provideConfirmEmailView(): ConfirmEmailView {
        return confirmEmailView
    }

    @Provides
    fun provideDetailView(): DetailView {
        return detailView
    }

    @Provides
    fun provideOverlayView(): OverlayView {
        return overlayView
    }

    @Provides
    fun provideRateView(): RateView {
        return rateView
    }

    @Provides
    fun provideSettingView(): SettingView {
        return settingView
    }

    @Provides
    @PerActivity
    fun provideCustomDialog(): CustomDialog {
        return CustomDialog(activity)
    }

    @Provides
    @PerActivity
    fun provideDetailPresenter(activityInteractor: ActivityInteractor): DetailPresenter {
        return DetailsPresenterImp(detailView, activityInteractor)
    }

    @Provides
    fun provideEmailView(): AddEmailView {
        return emailView
    }

    @Provides
    @PerActivity
    fun provideNewsPresenter(activityInteractor: ActivityInteractor): NewsFeedPresenter {
        return NewsFeedPresenterImpl(newsFeedView, activityInteractor)
    }

    @Provides
    @PerActivity
    fun provideOverlayPresenter(
        activityInteractor: ActivityInteractor
    ): OverlayPresenter {
        return OverlayPresenterImp(overlayView, activityInteractor)
    }

    @Provides
    @PerActivity
    fun provideRateMyAppPresenter(activityInteractor: ActivityInteractor): RateMyAppPresenter {
        return RateMyAppPresenterImp(rateView, activityInteractor)
    }

    @Provides
    @PerActivity
    fun provideSettingsPresenter(activityInteractor: ActivityInteractor): SettingsPresenter {
        return SettingsPresenterImp(settingView, activityInteractor)
    }

    @Provides
    @PerActivity
    fun provideSplashPresenter(
        activityInteractor: ActivityInteractor
    ): SplashPresenter {
        return SplashPresenterImpl(splashView, activityInteractor)
    }

    @Provides
    fun provideSplashView(): SplashView {
        return splashView
    }

    @Provides
    @PerActivity
    fun provideUpgradePresenter(activityInteractor: ActivityInteractor): UpgradePresenter {
        return UpgradePresenterImpl(upgradeView, activityInteractor)
    }

    @Provides
    fun provideUpgradeView(): UpgradeView {
        return upgradeView
    }

    @Provides
    @PerActivity
    fun provideWelcomePresenter(
        activityInteractor: ActivityInteractor
    ): WelcomePresenter {
        return WelcomePresenterImpl(welcomeView, activityInteractor)
    }

    @Provides
    fun provideWelcomeView(): WelcomeView {
        return welcomeView
    }

    @Provides
    @PerActivity
    fun provideWindscribePresenter(
        activityInteractor: ActivityInteractor
    ): WindscribePresenter {
        return WindscribePresenterImpl(windscribeView, activityInteractor)
    }

    @Provides
    fun provideWindscribeView(): WindscribeView {
        return windscribeView
    }

    @Provides
    @PerActivity
    fun providesArgbEvaluator(): ArgbEvaluator {
        return ArgbEvaluator()
    }

    @Provides
    @PerActivity
    fun provideActivityInteractor(
        coroutineScope: CoroutineScope,
        prefHelper: PreferencesHelper,
        apiCallManager: IApiCallManager,
        localDbInterface: LocalDbInterface,
        vpnConnectionStateManager: VPNConnectionStateManager,
        userRepository: UserRepository,
        protocolManager: ProtocolManager,
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
        trafficCounter: TrafficCounter
    ): ActivityInteractor {
        return ActivityInteractorImpl(
            coroutineScope,
            prefHelper, apiCallManager, localDbInterface, vpnConnectionStateManager,
            userRepository, protocolManager, networkInfoManager, locationRepository, vpnController,
            connectionDataRepository,
            serverListRepository,
            staticListUpdate,
            preferenceChangeObserver,
            notificationRepository, workManager, decoyTrafficController,
            trafficCounter

        )
    }
}
