/*
 * Copyright (c) 2021 Windscribe Limited.
 */
package com.windscribe.mobile.mainmenu

import android.content.Context

interface MainMenuPresenter {
    fun continueWithLogoutClicked()
    fun savedLocale(): String
    fun isHapticFeedbackEnabled(): Boolean
    fun onAboutClicked()
    fun onAccountSetUpClicked()
    fun onAddEmailClicked()
    fun onConfirmEmailClicked()
    fun onConnectionSettingsClicked()
    fun onDestroy()
    fun onGeneralSettingsClicked()
    fun onHelpMeClicked()
    fun onLanguageChanged()
    fun onLoginClicked()
    fun onMyAccountClicked()
    fun onRobertSettingsClicked()
    fun onSignOutClicked()
    fun onUpgradeClicked()
    fun setLayoutFromApiSession()
    fun observeUserChange(mainMenuActivity: MainMenuActivity)
    fun setTheme(context: Context)
    fun onReferForDataClick()
    fun advanceViewClick()
}