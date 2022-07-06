/*
 * Copyright (c) 2021 Windscribe Limited.
 */
package com.windscribe.mobile.di

import com.windscribe.mobile.about.AboutActivity
import com.windscribe.mobile.account.AccountActivity
import com.windscribe.mobile.confirmemail.ConfirmActivity
import com.windscribe.mobile.connectionsettings.ConnectionSettingsActivity
import com.windscribe.mobile.debug.DebugViewActivity
import com.windscribe.mobile.email.AddEmailActivity
import com.windscribe.mobile.fragments.ServerListFragment
import com.windscribe.mobile.generalsettings.GeneralSettingsActivity
import com.windscribe.mobile.gpsspoofing.GpsSpoofingSettingsActivity
import com.windscribe.mobile.help.HelpActivity
import com.windscribe.mobile.mainmenu.MainMenuActivity
import com.windscribe.mobile.networksecurity.NetworkSecurityActivity
import com.windscribe.mobile.networksecurity.networkdetails.NetworkDetailsActivity
import com.windscribe.mobile.newsfeedactivity.NewsFeedActivity
import com.windscribe.mobile.robert.RobertSettingsActivity
import com.windscribe.mobile.splash.SplashActivity
import com.windscribe.mobile.splittunneling.SplitTunnelingActivity
import com.windscribe.mobile.ticket.SendTicketActivity
import com.windscribe.mobile.upgradeactivity.UpgradeActivity
import com.windscribe.mobile.welcome.WelcomeActivity
import com.windscribe.mobile.windscribe.WindscribeActivity
import com.windscribe.vpn.di.ActivityComponent
import com.windscribe.vpn.di.ApplicationComponent
import dagger.Component

@PerActivity
@Component(dependencies = [ApplicationComponent::class], modules = [ActivityModule::class])
interface ActivityComponent {
    fun inject(sendTicketActivity: SendTicketActivity)
    fun inject(helpActivity: HelpActivity)
    fun inject(confirmActivity: ConfirmActivity)
    fun inject(splashActivity: SplashActivity)
    fun inject(welcomeActivity: WelcomeActivity)
    fun inject(networkDetailsActivity: NetworkDetailsActivity)
    fun inject(windscribeActivity: WindscribeActivity)
    fun inject(upgradeActivity: UpgradeActivity)
    fun inject(mainMenuActivity: MainMenuActivity)
    fun inject(generalSettingsActivity: GeneralSettingsActivity)
    fun inject(networkSecurityActivity: NetworkSecurityActivity)
    fun inject(accountActivity: AccountActivity)
    fun inject(newsFeedActivity: NewsFeedActivity)
    fun inject(addEmailActivity: AddEmailActivity)
    fun inject(settingsActivity: ConnectionSettingsActivity)
    fun inject(splitTunnelingActivity: SplitTunnelingActivity)
    fun inject(serverListFragment: ServerListFragment)
    fun inject(gpsSpoofingSettingsActivity: GpsSpoofingSettingsActivity)
    fun inject(aboutActivity: AboutActivity)
    fun inject(robertSettingsActivity: RobertSettingsActivity)
    fun inject(debugViewActivity: DebugViewActivity)
}
