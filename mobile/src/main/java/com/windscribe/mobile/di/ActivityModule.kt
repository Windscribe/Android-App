/*
 * Copyright (c) 2021 Windscribe Limited.
 */
package com.windscribe.mobile.di

import android.animation.ArgbEvaluator
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentFactory
import com.windscribe.mobile.about.AboutPresenter
import com.windscribe.mobile.about.AboutPresenterImpl
import com.windscribe.mobile.about.AboutView
import com.windscribe.mobile.account.AccountPresenter
import com.windscribe.mobile.account.AccountPresenterImpl
import com.windscribe.mobile.account.AccountView
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
import com.windscribe.mobile.share.ShareAppLink
import com.windscribe.mobile.splash.SplashPresenter
import com.windscribe.mobile.splash.SplashPresenterImpl
import com.windscribe.mobile.splash.SplashView
import com.windscribe.mobile.splittunneling.SplitTunnelingPresenter
import com.windscribe.mobile.splittunneling.SplitTunnelingPresenterImpl
import com.windscribe.mobile.splittunneling.SplitTunnelingView
import com.windscribe.mobile.ticket.SendTicketPresenter
import com.windscribe.mobile.ticket.SendTicketPresenterImpl
import com.windscribe.mobile.ticket.SendTicketView
import com.windscribe.mobile.upgradeactivity.UpgradePresenter
import com.windscribe.mobile.upgradeactivity.UpgradePresenterImpl
import com.windscribe.mobile.upgradeactivity.UpgradeView
import com.windscribe.mobile.welcome.WelcomePresenter
import com.windscribe.mobile.welcome.WelcomePresenterImpl
import com.windscribe.mobile.welcome.WelcomeView
import com.windscribe.mobile.windscribe.WindscribePresenter
import com.windscribe.mobile.windscribe.WindscribePresenterImpl
import com.windscribe.mobile.windscribe.WindscribeView
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
import javax.inject.Inject
import javax.inject.Named

@Module
class ActivityModule {
    private lateinit var activity: AppCompatActivity
    private lateinit var confirmEmailView: ConfirmEmailView
    private lateinit var helpView: HelpView
    private lateinit var aboutView: AboutView
    private lateinit var accountView: AccountView
    private lateinit var connectionSettingsView: ConnectionSettingsView
    private lateinit var emailView: AddEmailView
    private lateinit var generalSettingsView: GeneralSettingsView
    private lateinit var gpsSpoofingSettingView: GpsSpoofingSettingView
    private lateinit var mainMenuView: MainMenuView
    private lateinit var networkDetailView: NetworkDetailView
    private lateinit var networkSecurityView: NetworkSecurityView
    private lateinit var newsFeedView: NewsFeedView
    private lateinit var robertSettingsView: RobertSettingsView
    private lateinit var splashView: SplashView
    private lateinit var splitTunnelingView: SplitTunnelingView
    private lateinit var upgradeView: UpgradeView
    private lateinit var windscribeView: WindscribeView
    private lateinit var sendTicketView: SendTicketView
    private lateinit var welcomeView: WelcomeView
    private lateinit var debugView: DebugView

    constructor()
    constructor(activity: AppCompatActivity) {
        this.activity = activity
    }

    constructor(mActivity: AppCompatActivity, robertSettingsView: RobertSettingsView) {
        this.activity = mActivity
        this.robertSettingsView = robertSettingsView
    }

    constructor(mActivity: AppCompatActivity, confirmEmailView: ConfirmEmailView) {
        this.activity = mActivity
        this.confirmEmailView = confirmEmailView
    }

    constructor(mActivity: AppCompatActivity, helpView: HelpView) {
        this.activity = mActivity
        this.helpView = helpView
    }

    constructor(mActivity: AppCompatActivity, sendTicketView: SendTicketView) {
        this.activity = mActivity
        this.sendTicketView = sendTicketView
    }

    constructor(activity: AppCompatActivity, welcomeView: WelcomeView) {
        this.activity = activity
        this.welcomeView = welcomeView
    }

    constructor(mActivity: AppCompatActivity, splashView: SplashView) {
        this.activity = mActivity
        this.splashView = splashView
    }

    constructor(mActivity: AppCompatActivity, windscribeView: WindscribeView) {
        this.activity = mActivity
        this.windscribeView = windscribeView
    }

    constructor(mActivity: AppCompatActivity, networkDetailView: NetworkDetailView) {
        this.activity = mActivity
        this.networkDetailView = networkDetailView
    }

    constructor(mActivity: AppCompatActivity, mainMenuView: MainMenuView) {
        this.activity = mActivity
        this.mainMenuView = mainMenuView
    }

    constructor(mActivity: AppCompatActivity, generalSettingsView: GeneralSettingsView) {
        this.activity = mActivity
        this.generalSettingsView = generalSettingsView
    }

    constructor(mActivity: AppCompatActivity, upgradeView: UpgradeView) {
        this.activity = mActivity
        this.upgradeView = upgradeView
    }

    constructor(mActivity: AppCompatActivity, networkSecurityView: NetworkSecurityView) {
        this.activity = mActivity
        this.networkSecurityView = networkSecurityView
    }

    constructor(mActivity: AppCompatActivity, accountView: AccountView) {
        this.activity = mActivity
        this.accountView = accountView
    }

    constructor(mActivity: AppCompatActivity, newsFeedView: NewsFeedView) {
        this.activity = mActivity
        this.newsFeedView = newsFeedView
    }

    constructor(mActivity: AppCompatActivity, emailView: AddEmailView) {
        this.activity = mActivity
        this.emailView = emailView
    }

    constructor(mActivity: AppCompatActivity, connectionSettingsView: ConnectionSettingsView) {
        this.activity = mActivity
        this.connectionSettingsView = connectionSettingsView
    }

    constructor(mActivity: AppCompatActivity, splitTunnelingView: SplitTunnelingView) {
        this.activity = mActivity
        this.splitTunnelingView = splitTunnelingView
    }

    constructor(mActivity: AppCompatActivity, gpsSpoofingSettingView: GpsSpoofingSettingView) {
        this.activity = mActivity
        this.gpsSpoofingSettingView = gpsSpoofingSettingView
    }

    constructor(mActivity: AppCompatActivity, debugView: DebugView) {
        this.activity = mActivity
        this.debugView = debugView
    }

    constructor(mActivity: AppCompatActivity, aboutView: AboutView) {
        this.activity = mActivity
        this.aboutView = aboutView
    }

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
    fun provideActivity(): AppCompatActivity {
        return activity
    }

    @Provides
    fun provideAddEmailPresenter(activityInteractor: ActivityInteractor): AddEmailPresenter {
        return AddEmailPresenterImpl(emailView, activityInteractor)
    }

    @Provides
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
    fun provideConnectionPresenter(
        activityInteractor: ActivityInteractor
    ): ConnectionSettingsPresenter {
        return ConnectionSettingsPresenterImpl(connectionSettingsView, activityInteractor)
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
    fun provideWelcomePresenter(activityInteractor: ActivityInteractor): WelcomePresenter {
        return WelcomePresenterImpl(welcomeView, activityInteractor)
    }

    @Provides
    fun provideWelcomeView(): WelcomeView {
        return welcomeView
    }

    @Provides
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
            notificationRepository,
            workManager,
            decoyTrafficController,
            trafficCounter
        )
    }

    @Provides
    @PerActivity
    fun providesCustomFragmentFactory(activityInteractor: ActivityInteractor): CustomFragmentFactory {
        return CustomFragmentFactory(activityInteractor)
    }

    @PerActivity
    class CustomFragmentFactory @Inject constructor(private val activityInteractor: ActivityInteractor) :
        FragmentFactory() {
        override fun instantiate(classLoader: ClassLoader, className: String): Fragment {
            return when (loadFragmentClass(classLoader, className)) {
                ShareAppLink::class.java -> ShareAppLink(activityInteractor)
                else -> super.instantiate(classLoader, className)
            }
        }
    }
}
