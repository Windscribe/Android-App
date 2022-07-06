/*
 * Copyright (c) 2021 Windscribe Limited.
 */

package com.windscribe.tv.windscribe

import android.content.Intent
import com.windscribe.tv.splash.SplashActivity
import com.windscribe.tv.upgrade.UpgradeActivity
import com.windscribe.tv.welcome.WelcomeActivity
import com.windscribe.vpn.Windscribe
import com.windscribe.vpn.Windscribe.ApplicationInterface

class TVApplication : Windscribe(), ApplicationInterface {

    override fun onCreate() {
        applicationInterface = this
        super.onCreate()
        setTheme()
    }

    override val homeIntent: Intent
        get() = Intent(appContext, WindscribeActivity::class.java)
    override val splashIntent: Intent
        get() = Intent(appContext, SplashActivity::class.java)
    override val welcomeIntent: Intent
        get() = Intent(appContext, WelcomeActivity::class.java)
    override val upgradeIntent: Intent
        get() = Intent(appContext, UpgradeActivity::class.java)
    override val isTV: Boolean
        get() = true

    override fun setTheme() {}
}
