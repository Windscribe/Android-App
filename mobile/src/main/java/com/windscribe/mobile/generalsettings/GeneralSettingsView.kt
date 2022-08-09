/*
 * Copyright (c) 2021 Windscribe Limited.
 */
package com.windscribe.mobile.generalsettings

interface GeneralSettingsView {
    val orderList: Array<String>
    val themeList: Array<String>
    fun openFileChooser(requestCode: Int)
    fun registerLocaleChangeListener()
    fun resetTextResources(
        title: String,
        sortBy: String,
        latencyDisplay: String,
        language: String,
        appearance: String,
        notificationState: String,
        hapticFeedback: String,
        version: String,
        connected: String,
        disconnected: String,
        appBackground: String
    )

    fun setActivityTitle(activityTitle: String)
    fun setAppVersionText(versionText: String)
    fun setConnectedFlagPath(path: String)
    fun setDisconnectedFlagPath(path: String)
    fun setFlagSizeLabel(label: String)
    fun setLanguageTextView(language: String)
    fun setLatencyType(latencyType: String)
    fun setSelectionTextView(selection: String)
    fun setThemeTextView(theme: String)
    fun setupCustomFlagAdapter(saved: String, options: Array<String>)
    fun setupHapticToggleImage(ic_toggle_button_off: Int)
    fun setupLanguageAdapter(savedLanguage: String, language: Array<String>)
    fun setupLatencyAdapter(savedLatency: String, latencyTypes: Array<String>)
    fun setupLocationHealthToggleImage(image: Int)
    fun setupNotificationToggleImage(ic_toggle_button_off: Int)
    fun setupSelectionAdapter(savedSelection: String, selections: Array<String>)
    fun setupThemeAdapter(savedTheme: String, themeList: Array<String>)
}