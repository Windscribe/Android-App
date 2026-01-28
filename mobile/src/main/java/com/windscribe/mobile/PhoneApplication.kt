package com.windscribe.mobile

import android.content.Intent
import com.windscribe.mobile.ui.AppStartActivity
import com.windscribe.mobile.ui.nav.Screen
import com.windscribe.mobile.ui.preferences.icons.AppIconManager
import com.windscribe.mobile.upgradeactivity.UpgradeActivity
import com.windscribe.vpn.Windscribe
import com.windscribe.vpn.autoconnection.AutoConnectionModeCallback
import com.windscribe.vpn.autoconnection.FragmentType
import com.windscribe.vpn.autoconnection.ProtocolInformation
import com.windscribe.vpn.apppreference.PreferencesKeyConstants

class PhoneApplication : Windscribe(), Windscribe.ApplicationInterface {
    override fun onCreate() {
        applicationInterface = this
        super.onCreate()
        setTheme()
    }

    /**
     * Creates an Intent for the active launcher component.
     * This respects the user's selected app icon (activity-alias).
     * Uses AppIconManager.getComponentName() to keep icon mapping centralized.
     */
    private fun getActiveLauncherIntent(): Intent {
        val selectedIcon = preference.customIcon
        val activityClassName = AppIconManager.getActivityClassName(selectedIcon)
        return Intent().apply {
            setClassName(appContext.packageName, activityClassName)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
    }

    override val homeIntent: Intent
        get() = getActiveLauncherIntent()
    override val splashIntent: Intent
        get() = getActiveLauncherIntent()
    override val upgradeIntent: Intent
        get() = Intent(appContext, UpgradeActivity::class.java)
    override val welcomeIntent: Intent
        get() = getActiveLauncherIntent()
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
                    FragmentType.ManualModeFailed -> activity.navController.navigate(Screen.ManualModeFailed.route)
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