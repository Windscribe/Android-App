package com.windscribe.tv.di

import android.animation.ArgbEvaluator
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.LifecycleCoroutineScope
import androidx.lifecycle.lifecycleScope
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
import com.windscribe.vpn.autoconnection.AutoConnectionManager
import com.windscribe.vpn.backend.TrafficCounter
import com.windscribe.vpn.backend.utils.WindVpnController
import com.windscribe.vpn.decoytraffic.DecoyTrafficController
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
open class BaseActivityModule {
    lateinit var confirmEmailView: ConfirmEmailView
    lateinit var activity: AppCompatActivity
    lateinit var emailView: AddEmailView
    lateinit var newsFeedView: NewsFeedView
    lateinit var splashView: SplashView
    lateinit var windscribeView: WindscribeView
    lateinit var detailView: DetailView
    lateinit var overlayView: OverlayView
    lateinit var rateView: RateView
    lateinit var settingView: SettingView
    lateinit var welcomeView: WelcomeView

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
                autoConnectionManager,
                latencyRepository,
                receiptValidator,
                firebaseManager,
                advanceParameterRepository
        )
    }
}