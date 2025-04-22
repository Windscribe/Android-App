/*
 * Copyright (c) 2021 Windscribe Limited.
 */
package com.windscribe.mobile.generalsettings

interface GeneralSettingsView {
    val orderList: Array<String>
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
    fun setLanguageTextView(language: String)
    fun setSelectionTextView(selection: String)
    fun reloadApp()

    fun setupHapticToggleImage(ic_toggle_button_off: Int)
    fun setupLanguageAdapter(
        localiseValues: Array<String>,
        selectedKey: String,
        keys: Array<String>
    )
    fun setupNotificationToggleImage(ic_toggle_button_off: Int)
    fun setupSelectionAdapter(
        localiseValues: Array<String>,
        selectedKey: String,
        keys: Array<String>
    )
}