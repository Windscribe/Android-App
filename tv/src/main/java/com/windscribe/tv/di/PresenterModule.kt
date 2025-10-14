package com.windscribe.tv.di

import com.windscribe.tv.confirmemail.ConfirmEmailPresenter
import com.windscribe.tv.confirmemail.ConfirmEmailPresenterImp
import com.windscribe.tv.confirmemail.ConfirmEmailView
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
import com.windscribe.vpn.api.IApiCallManager
import com.windscribe.vpn.apppreference.PreferencesHelper
import com.windscribe.vpn.autoconnection.AutoConnectionManager
import com.windscribe.vpn.backend.ProxyDNSManager
import com.windscribe.vpn.commonutils.ResourceHelper
import com.windscribe.vpn.localdatabase.LocalDbInterface
import com.windscribe.vpn.repository.ConnectionDataRepository
import com.windscribe.vpn.repository.LatencyRepository
import com.windscribe.vpn.repository.LocationRepository
import com.windscribe.vpn.repository.LogRepository
import com.windscribe.vpn.repository.ServerListRepository
import com.windscribe.vpn.repository.StaticIpRepository
import com.windscribe.vpn.repository.UserRepository
import com.windscribe.vpn.state.PreferenceChangeObserver
import com.windscribe.vpn.state.VPNConnectionStateManager
import com.windscribe.vpn.workers.WindScribeWorkManager
import dagger.Module
import dagger.Provides
import kotlinx.coroutines.CoroutineScope

@Module
abstract class PresenterModule {

    @Module
    companion object {

        @Provides
        @PerActivity
        @JvmStatic
        fun provideWindscribePresenter(
            windscribeView: WindscribeView,
            activityScope: CoroutineScope,
            preferencesHelper: PreferencesHelper,
            apiCallManager: IApiCallManager,
            localDbInterface: LocalDbInterface,
            userRepository: UserRepository,
            serverListRepository: ServerListRepository,
            locationRepository: LocationRepository,
            autoConnectionManager: AutoConnectionManager,
            vpnConnectionStateManager: VPNConnectionStateManager,
            vpnController: com.windscribe.vpn.backend.utils.WindVpnController,
            workManager: WindScribeWorkManager,
            resourceHelper: ResourceHelper
        ): WindscribePresenter {
            return WindscribePresenterImpl(
                windscribeView,
                activityScope,
                preferencesHelper,
                apiCallManager,
                localDbInterface,
                userRepository,
                serverListRepository,
                locationRepository,
                autoConnectionManager,
                vpnConnectionStateManager,
                vpnController,
                workManager,
                resourceHelper
            )
        }

        @Provides
        @PerActivity
        @JvmStatic
        fun provideAddEmailPresenter(
            emailView: AddEmailView,
            activityScope: CoroutineScope,
            apiCallManager: IApiCallManager,
            resourceHelper: ResourceHelper
        ): AddEmailPresenter {
            return AddEmailPresenterImpl(emailView, activityScope, apiCallManager, resourceHelper)
        }

        @Provides
        @PerActivity
        @JvmStatic
        fun provideConfirmEmailPresenter(
            confirmEmailView: ConfirmEmailView,
            activityScope: CoroutineScope,
            preferencesHelper: PreferencesHelper,
            apiCallManager: IApiCallManager,
            resourceHelper: ResourceHelper
        ): ConfirmEmailPresenter {
            return ConfirmEmailPresenterImp(
                confirmEmailView,
                activityScope,
                preferencesHelper,
                apiCallManager,
                resourceHelper
            )
        }

        @Provides
        @PerActivity
        @JvmStatic
        fun provideDetailPresenter(
            detailView: DetailView,
            activityScope: CoroutineScope,
            localDbInterface: LocalDbInterface,
            preferencesHelper: PreferencesHelper,
            resourceHelper: ResourceHelper,
            latencyRepository: LatencyRepository
        ): DetailPresenter {
            return DetailsPresenterImp(
                detailView,
                activityScope,
                localDbInterface,
                preferencesHelper,
                resourceHelper,
                latencyRepository
            )
        }

        @Provides
        @PerActivity
        @JvmStatic
        fun provideNewsFeedPresenter(
            newsFeedView: NewsFeedView,
            activityScope: CoroutineScope,
            preferencesHelper: PreferencesHelper,
            notificationRepository: com.windscribe.vpn.repository.NotificationRepository
        ): NewsFeedPresenter {
            return NewsFeedPresenterImpl(
                newsFeedView,
                activityScope,
                preferencesHelper,
                notificationRepository
            )
        }

        @Provides
        @PerActivity
        @JvmStatic
        fun provideOverlayPresenter(
            overlayView: OverlayView,
            activityScope: CoroutineScope,
            localDbInterface: LocalDbInterface,
            preferencesHelper: PreferencesHelper,
            resourceHelper: ResourceHelper,
            locationRepository: LocationRepository,
            serverListRepository: ServerListRepository,
            staticIpRepository: StaticIpRepository,
            latencyRepository: LatencyRepository
        ): OverlayPresenter {
            return OverlayPresenterImp(
                overlayView,
                activityScope,
                localDbInterface,
                preferencesHelper,
                resourceHelper,
                locationRepository,
                serverListRepository,
                staticIpRepository,
                latencyRepository
            )
        }

        @Provides
        @PerActivity
        @JvmStatic
        fun provideRateMyAppPresenter(
            rateView: RateView,
            preferencesHelper: PreferencesHelper,
            firebaseManager: com.windscribe.vpn.services.FirebaseManager
        ): RateMyAppPresenter {
            return RateMyAppPresenterImp(rateView, preferencesHelper, firebaseManager)
        }

        @Provides
        @PerActivity
        @JvmStatic
        fun provideSettingsPresenter(
            settingView: SettingView,
            activityScope: CoroutineScope,
            preferencesHelper: PreferencesHelper,
            resourceHelper: ResourceHelper,
            apiCallManager: IApiCallManager,
            userRepository: UserRepository,
            localDbInterface: LocalDbInterface,
            autoConnectionManager: com.windscribe.vpn.autoconnection.AutoConnectionManager,
            vpnConnectionStateManager: VPNConnectionStateManager,
            logRepository: LogRepository,
            proxyDNSManager: ProxyDNSManager,
            portMapRepository: com.windscribe.vpn.repository.PortMapRepository
        ): SettingsPresenter {
            return SettingsPresenterImp(
                settingView,
                activityScope,
                preferencesHelper,
                resourceHelper,
                apiCallManager,
                userRepository,
                localDbInterface,
                autoConnectionManager,
                vpnConnectionStateManager,
                logRepository,
                proxyDNSManager,
                portMapRepository
            )
        }

        @Provides
        @PerActivity
        @JvmStatic
        fun provideSplashPresenter(
            splashView: SplashView,
            activityScope: CoroutineScope,
            preferencesHelper: PreferencesHelper,
            apiCallManager: IApiCallManager,
            localDbInterface: LocalDbInterface,
            autoConnectionManager: AutoConnectionManager,
            workManager: WindScribeWorkManager,
            receiptValidator: com.windscribe.vpn.services.ReceiptValidator,
            userRepository: UserRepository,
            serverListRepository: ServerListRepository,
            staticIpRepository: StaticIpRepository
        ): SplashPresenter {
            return SplashPresenterImpl(
                splashView,
                activityScope,
                preferencesHelper,
                apiCallManager,
                localDbInterface,
                autoConnectionManager,
                workManager,
                receiptValidator,
                userRepository,
                serverListRepository,
                staticIpRepository
            )
        }

        @Provides
        @PerActivity
        @JvmStatic
        fun provideWelcomePresenter(
            welcomeView: WelcomeView,
            activityScope: CoroutineScope,
            apiCallManager: IApiCallManager,
            preferencesHelper: PreferencesHelper,
            userRepository: UserRepository,
            staticIpRepository: StaticIpRepository,
            connectionDataRepository: ConnectionDataRepository,
            serverListRepository: ServerListRepository,
            preferenceChangeObserver: PreferenceChangeObserver,
            workManager: WindScribeWorkManager,
            resourceHelper: ResourceHelper,
            logRepository: LogRepository,
            firebaseManager: com.windscribe.vpn.services.FirebaseManager
        ): WelcomePresenter {
            return WelcomePresenterImpl(
                welcomeView,
                activityScope,
                preferencesHelper,
                apiCallManager,
                firebaseManager,
                userRepository,
                staticIpRepository,
                connectionDataRepository,
                serverListRepository,
                preferenceChangeObserver,
                workManager,
                resourceHelper,
                logRepository
            )
        }
    }
}

// Note: UpgradePresenter is provided by flavor-specific ActivityModule (Google flavor)
