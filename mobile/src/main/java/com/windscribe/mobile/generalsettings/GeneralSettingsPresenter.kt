/*
 * Copyright (c) 2021 Windscribe Limited.
 */
package com.windscribe.mobile.generalsettings

interface GeneralSettingsPresenter {
    val savedLocale: String
    fun onDestroy()
    fun onHapticToggleButtonClicked()
    fun onLanguageChanged()
    fun onLanguageSelected(selectedLanguage: String)
    fun onNotificationToggleButtonClicked()
    fun onSelectionSelected(selection: String)
    fun setupInitialLayout()
}