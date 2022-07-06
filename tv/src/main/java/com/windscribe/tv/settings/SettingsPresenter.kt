/*
 * Copyright (c) 2021 Windscribe Limited.
 */
package com.windscribe.tv.settings

interface SettingsPresenter {
    val isUserInGhostMode: Boolean
    val isUserPro: Boolean
    fun onAddEmailClicked()
    fun onAllowBootStartClick()
    fun onAllowLanClicked()
    fun onBlockBootStartClick()
    fun onBlockLanClicked()
    fun onConnectionModeAutoClicked()
    fun onConnectionModeManualClicked()
    fun onDestroy()
    fun onDisabledModeClick()
    fun onEmailResend()
    fun onExclusiveModeClick()
    fun onInclusiveModeClick()
    fun onLanguageSelected(selectedLanguage: String)
    fun onLoginAndClaimClick()
    fun onPortSelected(protocol: String, port: String)
    fun onProtocolSelected(protocol: String)
    fun onSendDebugClicked()
    fun onSignOutClicked()
    fun onSortSelected(newSort: String)
    fun onUpgradeClicked(textViewText: String)
    fun setUpTabMenu()
    fun setupLayoutBasedOnConnectionMode()
    fun setupLayoutForDebugTab()
    fun setupLayoutForGeneralTab()
    fun showLayoutBasedOnUserType()
    fun updateUserDataFromApi()
    fun observeUserData(settingsActivity: SettingActivity)
}
