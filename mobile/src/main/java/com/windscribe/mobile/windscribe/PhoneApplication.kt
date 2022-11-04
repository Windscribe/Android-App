/*
 * Copyright (c) 2021 Windscribe Limited.
 */
package com.windscribe.mobile.windscribe

import android.content.Intent
import android.view.ViewGroup
import androidx.core.view.children
import androidx.fragment.app.FragmentTransaction.TRANSIT_FRAGMENT_FADE
import com.windscribe.mobile.R
import com.windscribe.mobile.connectionmode.*
import com.windscribe.mobile.splash.SplashActivity
import com.windscribe.mobile.upgradeactivity.UpgradeActivity
import com.windscribe.mobile.welcome.WelcomeActivity
import com.windscribe.vpn.Windscribe
import com.windscribe.vpn.Windscribe.ApplicationInterface
import com.windscribe.vpn.autoconnection.AutoConnectionModeCallback
import com.windscribe.vpn.autoconnection.FragmentType
import com.windscribe.vpn.autoconnection.ProtocolInformation
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

    override fun launchFragment(
        protocolInformationList: List<ProtocolInformation>,
        fragmentType: FragmentType,
        autoConnectionModeCallback: AutoConnectionModeCallback,
        protocolInformation: ProtocolInformation?
    ): Boolean {
        return if (activeActivity == null) {
            false
        } else {
            val viewGroup: ViewGroup =
                activeActivity?.findViewById(android.R.id.content) as ViewGroup
            if (viewGroup.children.count() > 0 && viewGroup.children.first().id != -1) {
                val fragment = when (fragmentType) {
                    FragmentType.ConnectionFailure -> ConnectionFailureFragment(
                        protocolInformationList,
                        autoConnectionModeCallback
                    )
                    FragmentType.ConnectionChange -> ConnectionChangeFragment(
                        protocolInformationList,
                        autoConnectionModeCallback
                    )
                    FragmentType.SetupAsPreferredProtocol -> SetupPreferredProtocolFragment(
                        protocolInformation,
                        autoConnectionModeCallback
                    )
                    FragmentType.DebugLogSent -> DebugLogSentFragment(autoConnectionModeCallback)
                    FragmentType.AllProtocolFailed -> AllProtocolFailedFragment(
                        autoConnectionModeCallback
                    )
                }
                activeActivity?.supportFragmentManager?.beginTransaction()
                    ?.setTransition(TRANSIT_FRAGMENT_FADE)
                    ?.add(viewGroup.children.first().id, fragment, "")?.commit()
            }
            true
        }
    }
}
