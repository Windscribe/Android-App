/*
 * Copyright (c) 2021 Windscribe Limited.
 */
package com.windscribe.mobile.windscribe

import android.content.Intent
import com.windscribe.mobile.R
import com.windscribe.mobile.connectionmode.AllProtocolFailedFragment
import com.windscribe.mobile.connectionmode.ConnectionChangeFragment
import com.windscribe.mobile.connectionmode.ConnectionFailureFragment
import com.windscribe.mobile.connectionmode.DebugLogSentFragment
import com.windscribe.mobile.connectionmode.SetupPreferredProtocolFragment
import com.windscribe.mobile.upgradeactivity.UpgradeActivity
import com.windscribe.mobile.view.AppStartActivity
import com.windscribe.mobile.view.ui.FragmentView
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
        return if (activeActivity is AppStartActivity) {
            val fragment = when (fragmentType) {
                FragmentType.ConnectionFailure -> ConnectionFailureFragment.newInstance(
                    protocolInformationList, autoConnectionModeCallback
                )
                FragmentType.ConnectionChange -> ConnectionChangeFragment.newInstance(
                    protocolInformationList, autoConnectionModeCallback
                )
                FragmentType.SetupAsPreferredProtocol -> SetupPreferredProtocolFragment.newInstance(
                    protocolInformation, autoConnectionModeCallback
                )
                FragmentType.DebugLogSent -> DebugLogSentFragment.newInstance(
                    autoConnectionModeCallback
                )
                FragmentType.AllProtocolFailed -> AllProtocolFailedFragment.newInstance(
                    autoConnectionModeCallback
                )
            }
            (activeActivity as? AppStartActivity)?.presentDialog {
                FragmentView(fragment, activeActivity as AppStartActivity)
            }
            true
        } else {
            false
        }
    }

    override fun cancelDialog() {
        if (activeActivity is AppStartActivity) {
            (activeActivity as AppStartActivity).cancelDialog()
            activeActivity?.supportFragmentManager?.popBackStack()
        }
    }
}
