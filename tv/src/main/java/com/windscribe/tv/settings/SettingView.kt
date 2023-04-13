/*
 * Copyright (c) 2021 Windscribe Limited.
 */
package com.windscribe.tv.settings

import com.windscribe.tv.adapter.InstalledAppsAdapter
import com.windscribe.tv.settings.fragment.AccountFragment

interface SettingView {
    fun getResourceString(id: Int): String
    fun goToClaimAccount()
    fun goToLogin()
    fun gotoLoginRegistrationActivity()
    fun hideProgress()
    fun openConfirmEmailActivity()
    fun openEmailActivity(email: String?)
    fun openUpgradeActivity()
    fun reloadApp()
    fun setBootStartMode(mode: String)
    fun setDebugLog(log: List<String>)
    fun setDebugLogProgress(progressText: String, error: String)
    fun setEmail(email: String)
    fun setEmailState(state: AccountFragment.Status?, email: String?)
    fun setLanTrafficMode(lanTrafficMode: String)
    fun setPlanName(planName: String)
    fun setResetDate(resetDateLabel: String, resetDate: String)
    fun setSplitRouteMode(mode: String)
    fun setUpTabLayoutForGhostMode()
    fun setUpTabLayoutForGhostModePro()
    fun setUpTabLayoutForLoggedInUser()
    fun setUsername(username: String)
    fun setupAppsAdapter(adapter: InstalledAppsAdapter)
    fun setupLanguageAdapter(savedLanguage: String, languageList: Array<String>)
    fun setupLayoutForAutoMode()
    fun setupLayoutForFreeUser(upgradeText: String)
    fun setupLayoutForManualMode()
    fun setupLayoutForPremiumUser(upgradeText: String)
    fun setupPortMapAdapter(savedPort: String, ports: List<String>)
    fun setupProtocolAdapter(protocol: String, protocols: List<String>)
    fun setupSortAdapter(localiseValues: Array<String>, selectedItem: String, values: Array<String>)
    fun showProgress(progressText: String)
    fun showToast(toastString: String)
    fun updateLocale()
}
