/*
 * Copyright (c) 2021 Windscribe Limited.
 */
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
import com.windscribe.vpn.repository.*
import com.windscribe.vpn.state.NetworkInfoManager
import com.windscribe.vpn.state.PreferenceChangeObserver
import com.windscribe.vpn.state.VPNConnectionStateManager
import com.windscribe.vpn.workers.WindScribeWorkManager
import dagger.Module
import dagger.Provides
import kotlinx.coroutines.CoroutineScope
import javax.inject.Named

@Module
open class ActivityModule: BaseActivityModule {
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
}