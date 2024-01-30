package com.windscribe.mobile.di

import android.animation.ArgbEvaluator
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.LifecycleCoroutineScope
import androidx.lifecycle.lifecycleScope
import com.windscribe.mobile.about.AboutPresenter
import com.windscribe.mobile.about.AboutPresenterImpl
import com.windscribe.mobile.about.AboutView
import com.windscribe.mobile.account.AccountPresenter
import com.windscribe.mobile.account.AccountPresenterImpl
import com.windscribe.mobile.account.AccountView
import com.windscribe.mobile.advance.AdvanceParamPresenter
import com.windscribe.mobile.advance.AdvanceParamView
import com.windscribe.mobile.advance.AdvanceParamsPresenterImpl
import com.windscribe.mobile.confirmemail.ConfirmEmailPresenter
import com.windscribe.mobile.confirmemail.ConfirmEmailPresenterImp
import com.windscribe.mobile.confirmemail.ConfirmEmailView
import com.windscribe.mobile.connectionsettings.ConnectionSettingsPresenter
import com.windscribe.mobile.connectionsettings.ConnectionSettingsPresenterImpl
import com.windscribe.mobile.connectionsettings.ConnectionSettingsView
import com.windscribe.mobile.custom_view.CustomDialog
import com.windscribe.mobile.debug.DebugPresenter
import com.windscribe.mobile.debug.DebugPresenterImpl
import com.windscribe.mobile.debug.DebugView
import com.windscribe.mobile.email.AddEmailPresenter
import com.windscribe.mobile.email.AddEmailPresenterImpl
import com.windscribe.mobile.email.AddEmailView
import com.windscribe.mobile.fragments.ServerListFragment
import com.windscribe.mobile.generalsettings.GeneralSettingsPresenter
import com.windscribe.mobile.generalsettings.GeneralSettingsPresenterImpl
import com.windscribe.mobile.generalsettings.GeneralSettingsView
import com.windscribe.mobile.gpsspoofing.GpsSpoofingPresenter
import com.windscribe.mobile.gpsspoofing.GpsSpoofingPresenterImp
import com.windscribe.mobile.gpsspoofing.GpsSpoofingSettingView
import com.windscribe.mobile.help.HelpPresenter
import com.windscribe.mobile.help.HelpPresenterImpl
import com.windscribe.mobile.help.HelpView
import com.windscribe.mobile.mainmenu.MainMenuPresenter
import com.windscribe.mobile.mainmenu.MainMenuPresenterImpl
import com.windscribe.mobile.mainmenu.MainMenuView
import com.windscribe.mobile.networksecurity.NetworkSecurityPresenter
import com.windscribe.mobile.networksecurity.NetworkSecurityPresenterImpl
import com.windscribe.mobile.networksecurity.NetworkSecurityView
import com.windscribe.mobile.networksecurity.networkdetails.NetworkDetailPresenter
import com.windscribe.mobile.networksecurity.networkdetails.NetworkDetailPresenterImp
import com.windscribe.mobile.networksecurity.networkdetails.NetworkDetailView
import com.windscribe.mobile.newsfeedactivity.NewsFeedPresenter
import com.windscribe.mobile.newsfeedactivity.NewsFeedPresenterImpl
import com.windscribe.mobile.newsfeedactivity.NewsFeedView
import com.windscribe.mobile.robert.RobertSettingsPresenter
import com.windscribe.mobile.robert.RobertSettingsPresenterImpl
import com.windscribe.mobile.robert.RobertSettingsView
import com.windscribe.mobile.splash.SplashPresenter
import com.windscribe.mobile.splash.SplashPresenterImpl
import com.windscribe.mobile.splash.SplashView
import com.windscribe.mobile.splittunneling.SplitTunnelingPresenter
import com.windscribe.mobile.splittunneling.SplitTunnelingPresenterImpl
import com.windscribe.mobile.splittunneling.SplitTunnelingView
import com.windscribe.mobile.ticket.SendTicketPresenter
import com.windscribe.mobile.ticket.SendTicketPresenterImpl
import com.windscribe.mobile.ticket.SendTicketView
import com.windscribe.mobile.utils.PermissionManager
import com.windscribe.mobile.utils.PermissionManagerImpl
import com.windscribe.mobile.welcome.WelcomePresenter
import com.windscribe.mobile.welcome.WelcomePresenterImpl
import com.windscribe.mobile.welcome.WelcomeView
import com.windscribe.mobile.welcome.viewmodal.EmergencyConnectViewModal
import com.windscribe.mobile.windscribe.WindscribePresenter
import com.windscribe.mobile.windscribe.WindscribePresenterImpl
import com.windscribe.mobile.windscribe.WindscribeView
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
import com.windscribe.vpn.state.NetworkInfoManager
import com.windscribe.vpn.state.PreferenceChangeObserver
import com.windscribe.vpn.state.VPNConnectionStateManager
import com.windscribe.vpn.services.ReceiptValidator
import com.windscribe.vpn.workers.WindScribeWorkManager
import dagger.Module
import dagger.Provides
import kotlinx.coroutines.CoroutineScope
import javax.inject.Named
@Module
open class BaseActivityModule {
    lateinit var activity: AppCompatActivity
    lateinit var confirmEmailView: ConfirmEmailView
    lateinit var helpView: HelpView
    lateinit var aboutView: AboutView
    lateinit var accountView: AccountView
    lateinit var connectionSettingsView: ConnectionSettingsView
    lateinit var emailView: AddEmailView
    lateinit var generalSettingsView: GeneralSettingsView
    lateinit var gpsSpoofingSettingView: GpsSpoofingSettingView
    lateinit var mainMenuView: MainMenuView
    lateinit var networkDetailView: NetworkDetailView
    lateinit var networkSecurityView: NetworkSecurityView
    lateinit var newsFeedView: NewsFeedView
    lateinit var robertSettingsView: RobertSettingsView
    lateinit var splashView: SplashView
    lateinit var splitTunnelingView: SplitTunnelingView
    lateinit var windscribeView: WindscribeView
    lateinit var sendTicketView: SendTicketView
    lateinit var welcomeView: WelcomeView
    lateinit var debugView: DebugView
    lateinit var advanceParamView: AdvanceParamView

    @Provides
    fun provideAboutPresenter(activityInteractor: ActivityInteractor): AboutPresenter {
        return AboutPresenterImpl(aboutView, activityInteractor)
    }

    @Provides
    fun provideDebugPresenter(activityInteractor: ActivityInteractor): DebugPresenter {
        return DebugPresenterImpl(debugView, activityInteractor)
    }

    @Provides
    fun provideAccountPresenter(activityInteractor: ActivityInteractor): AccountPresenter {
        return AccountPresenterImpl(accountView, activityInteractor)
    }

    @Provides
    fun provideAccountView(): AccountView {
        return accountView
    }

    @Provides
    fun provideDebugView(): DebugView {
        return debugView
    }

    @Provides
    fun provideAdvanceParamsView(): AdvanceParamView {
        return advanceParamView
    }

    @Provides
    fun provideActivity(): AppCompatActivity {
        return activity
    }

    @Provides
    fun provideAddEmailPresenter(activityInteractor: ActivityInteractor): AddEmailPresenter {
        return AddEmailPresenterImpl(emailView, activityInteractor)
    }

    @Provides
    fun provideConfirmEmailPresenter(
            confirmEmailView: ConfirmEmailView, activityInteractor: ActivityInteractor
    ): ConfirmEmailPresenter {
        return ConfirmEmailPresenterImp(confirmEmailView, activityInteractor)
    }

    @Provides
    fun provideConfirmEmailView(): ConfirmEmailView {
        return confirmEmailView
    }

    @Provides
    fun provideConnectionPresenter(
            activityInteractor: ActivityInteractor,
            permissionManager: PermissionManager
    ): ConnectionSettingsPresenter {
        return ConnectionSettingsPresenterImpl(connectionSettingsView, activityInteractor, permissionManager)
    }

    @Provides
    fun provideConnectionSettingsView(): ConnectionSettingsView {
        return connectionSettingsView
    }

    @Provides
    fun provideCustomDialog(): CustomDialog {
        return CustomDialog(activity)
    }

    @Provides
    fun provideEmailView(): AddEmailView {
        return emailView
    }

    @Provides
    fun provideGeneralSettingsPresenter(
            activityInteractor: ActivityInteractor
    ): GeneralSettingsPresenter {
        return GeneralSettingsPresenterImpl(generalSettingsView, activityInteractor)
    }

    @Provides
    fun provideGeneralSettingsView(): GeneralSettingsView {
        return generalSettingsView
    }

    @Provides
    fun provideGpsSpoofingPresenter(activityInteractor: ActivityInteractor): GpsSpoofingPresenter {
        return GpsSpoofingPresenterImp(gpsSpoofingSettingView, activityInteractor)
    }

    @Provides
    fun provideGpsSpoofingView(): GpsSpoofingSettingView {
        return gpsSpoofingSettingView
    }

    @Provides
    fun provideHelpPresenter(activityInteractor: ActivityInteractor): HelpPresenter {
        return HelpPresenterImpl(helpView, activityInteractor)
    }

    @Provides
    fun provideHelpView(): HelpView {
        return helpView
    }

    @Provides
    fun provideMainMenuView(): MainMenuView {
        return mainMenuView
    }

    @Provides
    fun provideMenuPresenter(
            activityInteractor: ActivityInteractor
    ): MainMenuPresenter {
        return MainMenuPresenterImpl(mainMenuView, activityInteractor)
    }

    @Provides
    fun provideNetworkDetailPresenter(activityInteractor: ActivityInteractor): NetworkDetailPresenter {
        return NetworkDetailPresenterImp(networkDetailView, activityInteractor)
    }

    @Provides
    fun provideAdvanceParamsPresenter(preferencesHelper: PreferencesHelper, advanceParameterRepository: AdvanceParameterRepository): AdvanceParamPresenter {
        return AdvanceParamsPresenterImpl(advanceParamView, preferencesHelper, advanceParameterRepository)
    }

    @Provides
    fun provideNetworkDetailView(): NetworkDetailView {
        return networkDetailView
    }

    @Provides
    fun provideNewsPresenter(activityInteractor: ActivityInteractor): NewsFeedPresenter {
        return NewsFeedPresenterImpl(newsFeedView, activityInteractor)
    }

    @Provides
    fun provideRobertSettingsPresenter(activityInteractor: ActivityInteractor): RobertSettingsPresenter {
        return RobertSettingsPresenterImpl(robertSettingsView, activityInteractor)
    }

    @Provides
    fun provideSecurityPresenter(activityInteractor: ActivityInteractor): NetworkSecurityPresenter {
        return NetworkSecurityPresenterImpl(networkSecurityView, activityInteractor)
    }

    @Provides
    fun provideSecurityView(): NetworkSecurityView {
        return networkSecurityView
    }

    @Provides
    fun provideSendTicketPresenter(activityInteractor: ActivityInteractor): SendTicketPresenter {
        return SendTicketPresenterImpl(sendTicketView, activityInteractor)
    }

    @Provides
    fun provideSendTicketView(): SendTicketView {
        return sendTicketView
    }

    @Named("serverListFragments")
    @Provides
    fun provideServerListFragments(): List<ServerListFragment> {
        val serverListFragments: MutableList<ServerListFragment> = ArrayList()
        for (counter in 0..4) {
            serverListFragments.add(counter, ServerListFragment.newInstance(counter))
        }
        return serverListFragments
    }

    @Provides
    fun provideSplashPresenter(activityInteractor: ActivityInteractor): SplashPresenter {
        return SplashPresenterImpl(splashView, activityInteractor)
    }

    @Provides
    fun provideSplashView(): SplashView {
        return splashView
    }

    @Provides
    fun provideSplitPresenter(
            activityInteractor: ActivityInteractor
    ): SplitTunnelingPresenter {
        return SplitTunnelingPresenterImpl(splitTunnelingView, activityInteractor)
    }

    @Provides
    fun provideSplitTunnelingView(): SplitTunnelingView {
        return splitTunnelingView
    }

    @Provides
    fun provideWelcomePresenter(activityInteractor: ActivityInteractor): WelcomePresenter {
        return WelcomePresenterImpl(welcomeView, activityInteractor)
    }

    @Provides
    fun provideWelcomeView(): WelcomeView {
        return welcomeView
    }

    @Provides
    fun provideWindscribePresenter(
            activityInteractor: ActivityInteractor,
            permissionManager: PermissionManager
    ): WindscribePresenter {
        return WindscribePresenterImpl(windscribeView, activityInteractor, permissionManager)
    }

    @Provides
    fun provideWindscribeView(): WindscribeView {
        return windscribeView
    }

    @Provides
    fun providesArgbEvaluator(): ArgbEvaluator {
        return ArgbEvaluator()
    }

    @Provides
    fun providesActivityScope(): LifecycleCoroutineScope {
        return activity.lifecycleScope
    }

    @Provides
    @PerActivity
    fun providesPermissionManager(): PermissionManager {
        return PermissionManagerImpl(activity)
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

    @Provides
    fun providesEmergencyConnectViewModal(
            scope: CoroutineScope,
            windVpnController: WindVpnController,
            vpnConnectionStateManager: VPNConnectionStateManager
    ): Lazy<EmergencyConnectViewModal> {
        return activity.viewModels {
            return@viewModels EmergencyConnectViewModal.provideFactory(
                    scope, windVpnController, vpnConnectionStateManager
            )
        }
    }
}