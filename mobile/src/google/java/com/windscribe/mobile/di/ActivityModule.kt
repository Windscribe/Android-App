/*
 * Copyright (c) 2021 Windscribe Limited.
 */
package com.windscribe.mobile.di

import androidx.appcompat.app.AppCompatActivity
import com.windscribe.mobile.about.AboutView
import com.windscribe.mobile.account.AccountView
import com.windscribe.mobile.confirmemail.ConfirmEmailView
import com.windscribe.mobile.connectionsettings.ConnectionSettingsView
import com.windscribe.mobile.debug.DebugView
import com.windscribe.mobile.email.AddEmailView
import com.windscribe.mobile.generalsettings.GeneralSettingsView
import com.windscribe.mobile.gpsspoofing.GpsSpoofingSettingView
import com.windscribe.mobile.help.HelpView
import com.windscribe.mobile.mainmenu.MainMenuView
import com.windscribe.mobile.networksecurity.NetworkSecurityView
import com.windscribe.mobile.networksecurity.networkdetails.NetworkDetailView
import com.windscribe.mobile.newsfeedactivity.NewsFeedView
import com.windscribe.mobile.robert.RobertSettingsView
import com.windscribe.mobile.splash.SplashView
import com.windscribe.mobile.splittunneling.SplitTunnelingView
import com.windscribe.mobile.ticket.SendTicketView
import com.windscribe.mobile.upgradeactivity.UpgradePresenter
import com.windscribe.mobile.upgradeactivity.UpgradePresenterImpl
import com.windscribe.mobile.upgradeactivity.UpgradeView
import com.windscribe.mobile.welcome.WelcomeView
import com.windscribe.mobile.windscribe.WindscribeView
import com.windscribe.vpn.ActivityInteractor
import dagger.Module
import dagger.Provides

@Module
open class ActivityModule : BaseActivityModule {
    private lateinit var upgradeView: UpgradeView
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
}
