/*
 * Copyright (c) 2021 Windscribe Limited.
 */
package com.windscribe.tv.di

import com.windscribe.tv.confirmemail.ConfirmActivity
import com.windscribe.tv.email.AddEmailActivity
import com.windscribe.tv.news.NewsFeedActivity
import com.windscribe.tv.rate.RateMyAppActivity
import com.windscribe.tv.serverlist.detail.DetailActivity
import com.windscribe.tv.serverlist.overlay.OverlayActivity
import com.windscribe.tv.settings.SettingActivity
import com.windscribe.tv.splash.SplashActivity
import com.windscribe.tv.upgrade.UpgradeActivity
import com.windscribe.tv.welcome.WelcomeActivity
import com.windscribe.tv.windscribe.WindscribeActivity
import com.windscribe.vpn.di.ApplicationComponent
import dagger.Component

@PerActivity
@Component(dependencies = [ApplicationComponent::class], modules = [ActivityModule::class, PresenterModule::class])
interface ActivityComponent {
    fun inject(confirmActivity: ConfirmActivity)
    fun inject(splashActivity: SplashActivity)
    fun inject(welcomeActivity: WelcomeActivity)
    fun inject(windscribeActivity: WindscribeActivity)
    fun inject(upgradeActivity: UpgradeActivity)
    fun inject(newsFeedActivity: NewsFeedActivity)
    fun inject(addEmailActivity: AddEmailActivity)
    fun inject(settingActivity: SettingActivity)
    fun inject(overlayActivity: OverlayActivity)
    fun inject(detailActivity: DetailActivity)
    fun inject(rateMyAppActivity: RateMyAppActivity)
}
