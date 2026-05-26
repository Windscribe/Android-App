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
import com.windscribe.vpn.autoconnection.AutoConnectionModeCallback
import com.windscribe.vpn.autoconnection.FragmentType
import com.windscribe.vpn.autoconnection.ProtocolConnectionStatus
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.windscribe.vpn.autoconnection.ProtocolInformation
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class TVApplication : Windscribe(), ApplicationInterface, Configuration.Provider {

    @Inject
    lateinit var hiltWorkerFactory: HiltWorkerFactory

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(hiltWorkerFactory)
            .build()


    override fun onCreate() {
        // Set static appContext BEFORE super.onCreate(); see PhoneApplication for rationale.
        appContext = this
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
    override fun launchFragment(
        protocolInformationList: List<ProtocolInformation>,
        fragmentType: FragmentType,
        autoConnectionModeCallback: AutoConnectionModeCallback,
        protocolInformation: ProtocolInformation?
    ): Boolean {
        val nextUp = protocolInformationList.find { it.type == ProtocolConnectionStatus.NextUp }
        return if (nextUp != null && fragmentType == FragmentType.ConnectionFailure) {
            autoConnectionModeCallback.onProtocolSelect(nextUp)
            true
        } else {
            false
        }
    }
}
