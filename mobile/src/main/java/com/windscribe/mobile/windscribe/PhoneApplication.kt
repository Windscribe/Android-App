/*
 * Copyright (c) 2021 Windscribe Limited.
 */
package com.windscribe.mobile.windscribe

import android.content.Intent
import androidx.compose.ui.input.key.Key.Companion.W
import com.windscribe.mobile.R
import com.windscribe.mobile.upgradeactivity.UpgradeActivity
import com.windscribe.mobile.ui.AppStartActivity
import com.windscribe.mobile.ui.nav.Screen
import com.windscribe.vpn.Windscribe
import com.windscribe.vpn.Windscribe.ApplicationInterface
import com.windscribe.vpn.autoconnection.AutoConnectionModeCallback
import com.windscribe.vpn.autoconnection.FragmentType
import com.windscribe.vpn.autoconnection.ProtocolInformation
import com.windscribe.vpn.constants.PreferencesKeyConstants
import kotlin.jvm.java

class PhoneApplication : Windscribe(), ApplicationInterface {
    override fun onCreate() {
        applicationInterface = this
        super.onCreate()
        setTheme()
    }

    override val homeIntent: Intent
        get() = Intent(appContext, AppStartActivity::class.java)
    override val splashIntent: Intent
        get() = Intent(appContext, AppStartActivity::class.java)
    override val upgradeIntent: Intent
        get() = Intent(appContext, UpgradeActivity::class.java)
    override val welcomeIntent: Intent
        get() = Intent(appContext, AppStartActivity::class.java)
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
        if (activeActivity is AppStartActivity) {
            val activity = activeActivity as AppStartActivity
            activity.viewmodel.setConnectionCallback(protocolInformationList, autoConnectionModeCallback, protocolInformation)
            activity.runOnUiThread {
                when (fragmentType) {
                    FragmentType.ConnectionFailure -> activity.navController.navigate(Screen.ConnectionFailure.route)
                    FragmentType.ConnectionChange -> activity.navController.navigate(Screen.ConnectionChange.route)
                    FragmentType.SetupAsPreferredProtocol -> activity.navController.navigate(Screen.SetupPreferredProtocol.route)
                    FragmentType.DebugLogSent -> activity.navController.navigate(Screen.DebugLogSent.route)
                    FragmentType.AllProtocolFailed -> activity.navController.navigate(Screen.AllProtocolFailed.route)
                }
            }
            return true
        }
        return false
    }

    override fun cancelDialog() {
        if (activeActivity is AppStartActivity) {
            activeActivity?.supportFragmentManager?.popBackStack()
        }
    }
}
