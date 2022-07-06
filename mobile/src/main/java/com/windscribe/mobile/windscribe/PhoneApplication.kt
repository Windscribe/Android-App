/*
 * Copyright (c) 2021 Windscribe Limited.
 */
package com.windscribe.mobile.windscribe

import android.content.Intent
import com.windscribe.mobile.R
import com.windscribe.mobile.splash.SplashActivity
import com.windscribe.mobile.upgradeactivity.UpgradeActivity
import com.windscribe.mobile.welcome.WelcomeActivity
import com.windscribe.vpn.Windscribe
import com.windscribe.vpn.Windscribe.ApplicationInterface
import com.windscribe.vpn.constants.PreferencesKeyConstants

class PhoneApplication : Windscribe(), ApplicationInterface {
    override fun onCreate() {
        applicationInterface = this
        super.onCreate()
        setTheme()
    }

    override val homeIntent: Intent
        get() = Intent(appContext, WindscribeActivity::class.java)
    override val splashIntent: Intent
        get() = Intent(appContext, SplashActivity::class.java)
    override val upgradeIntent: Intent
        get() = Intent(appContext, UpgradeActivity::class.java)
    override val welcomeIntent: Intent
        get() = Intent(appContext, WelcomeActivity::class.java)
    override val isTV: Boolean
        get() = false

    override fun setTheme() {
        val savedThem = preference.selectedTheme
        if (savedThem == PreferencesKeyConstants.DARK_THEME) {
            setTheme(R.style.DarkTheme)
        } else {
            setTheme(R.style.LightTheme)
        }
    }
}
