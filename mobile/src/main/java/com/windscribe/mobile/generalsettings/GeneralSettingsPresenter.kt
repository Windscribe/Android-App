/*
 * Copyright (c) 2021 Windscribe Limited.
 */
package com.windscribe.mobile.generalsettings

import android.content.Context
import java.io.InputStream
import java.io.OutputStream

interface GeneralSettingsPresenter {
    val savedLocale: String
    fun onConnectedFlagEditClicked(requestCode: Int)
    fun onConnectedFlagPathPicked(path: String)
    fun onDestroy()
    fun onDisConnectedFlagPathPicked(path: String)
    fun onDisconnectedFlagEditClicked(requestCode: Int)
    fun onHapticToggleButtonClicked()
    fun onLanguageChanged()
    fun onLanguageSelected(selectedLanguage: String)
    fun onLatencyTypeSelected(latencyType: String)
    fun onNotificationToggleButtonClicked()
    fun onSelectionSelected(selection: String)
    fun onShowHealthToggleClicked()
    fun onThemeSelected(theme: String)
    fun resizeAndSaveBitmap(inputStream: InputStream, outputStream: OutputStream)
    fun setTheme(context: Context)
    fun setupInitialLayout()
    fun onAppBackgroundValueChanged(value: String)
}