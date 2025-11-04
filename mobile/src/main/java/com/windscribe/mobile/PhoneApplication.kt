package com.windscribe.mobile

import android.content.Intent
import com.windscribe.mobile.ui.AppStartActivity
import com.windscribe.mobile.ui.nav.Screen
import com.windscribe.mobile.upgradeactivity.UpgradeActivity
import com.windscribe.vpn.Windscribe
import com.windscribe.vpn.autoconnection.AutoConnectionModeCallback
import com.windscribe.vpn.autoconnection.FragmentType
import com.windscribe.vpn.autoconnection.ProtocolInformation
import com.windscribe.vpn.constants.PreferencesKeyConstants

class PhoneApplication : Windscribe(), Windscribe.ApplicationInterface {
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

    override fun showPinnedNodeErrorDialog(title: String, description: String) {
        if (activeActivity is AppStartActivity) {
            val activity = activeActivity as AppStartActivity
            activity.runOnUiThread {
                activity.navController.currentBackStackEntry?.savedStateHandle?.apply {
                    set("message", title)
                    set("description", description)
                }
                activity.navController.navigate(Screen.IpActionResult.route)
            }
        }
    }
}