/*
 * Copyright (c) 2021 Windscribe Limited.
 */
package com.windscribe.mobile.generalsettings

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import com.windscribe.mobile.R
import com.windscribe.vpn.ActivityInteractor
import com.windscribe.vpn.Windscribe.Companion.appContext
import com.windscribe.vpn.commonutils.WindUtilities
import com.windscribe.vpn.constants.PreferencesKeyConstants
import org.slf4j.LoggerFactory
import java.io.File
import java.io.InputStream
import java.io.OutputStream
import javax.inject.Inject

class GeneralSettingsPresenterImpl @Inject constructor(
        private var settingsView: GeneralSettingsView, private var interactor: ActivityInteractor
) : GeneralSettingsPresenter {

    private val logger = LoggerFactory.getLogger("gen_settings_p")

    override fun onDestroy() {
        if (interactor.getCompositeDisposable().isDisposed.not()) {
            interactor.getPreferenceChangeObserver().postLocationSettingsChange()
            logger.info("Disposing observer...")
            interactor.getCompositeDisposable().dispose()
        }
    }

    override val savedLocale: String
        get() {
            val selectedLanguage = interactor.getAppPreferenceInterface().savedLanguage
            return selectedLanguage.substring(
                    selectedLanguage.indexOf("(") + 1, selectedLanguage.indexOf(")")
            )
        }

    override fun onConnectedFlagEditClicked(requestCode: Int) {
        settingsView.openFileChooser(requestCode)
    }

    override fun onConnectedFlagPathPicked(path: String) {
        val lastPath = interactor.getAppPreferenceInterface().connectedFlagPath
        if (lastPath != null) {
            val file = File(appContext.filesDir, lastPath)
            if (file.exists()) {
                val success = file.delete()
                if (success) {
                    interactor.getAppPreferenceInterface().connectedFlagPath = null
                }
            }
        }
        interactor.getAppPreferenceInterface().connectedFlagPath = path
        settingsView.setConnectedFlagPath(path)
    }

    override fun onDisConnectedFlagPathPicked(path: String) {
        val lastPath = interactor.getAppPreferenceInterface().disConnectedFlagPath
        if (lastPath != null) {
            val file = File(appContext.filesDir, lastPath)
            if (file.exists()) {
                val success = file.delete()
                if (success) {
                    interactor.getAppPreferenceInterface().setDisconnectedFlagPath(null)
                }
            }
        }
        interactor.getAppPreferenceInterface().setDisconnectedFlagPath(path)
        settingsView.setDisconnectedFlagPath(path)
    }

    override fun onDisconnectedFlagEditClicked(requestCode: Int) {
        settingsView.openFileChooser(requestCode)
    }

    override fun onHapticToggleButtonClicked() {
        if (interactor.getAppPreferenceInterface().isHapticFeedbackEnabled) {
            logger.info("Previous  haptic Toggle Settings: True")
            interactor.getAppPreferenceInterface().setHapticFeedbackEnabled(false)
            settingsView.setupHapticToggleImage(R.drawable.ic_toggle_button_off)
        } else {
            logger.info("Previous haptic Toggle Settings: False")
            interactor.getAppPreferenceInterface().setHapticFeedbackEnabled(true)
            settingsView.setupHapticToggleImage(R.drawable.ic_toggle_button_on)
        }
    }

    override fun onLanguageChanged() {
        settingsView.resetTextResources(
                interactor.getResourceString(R.string.general),
                interactor.getResourceString(R.string.sort_by),
                interactor.getResourceString(R.string.display_latency),
                interactor.getResourceString(R.string.preferred_language),
                interactor.getResourceString(R.string.theme),
                interactor.getResourceString(R.string.show_timer_in_notifications),
                interactor.getResourceString(R.string.haptic_setting_label),
                interactor.getResourceString(R.string.version),
                interactor.getResourceString(R.string.connected_lower_case),
                interactor.getResourceString(R.string.disconnected_lower_case),
                interactor.getResourceString(R.string.app_background)
        )
    }

    override fun onLanguageSelected(selectedKey: String) {
        //Save the selected language
        val savedLanguage = interactor.getSavedLanguage()
        val selectedIndex = interactor.getStringArray(R.array.language_codes).indexOf(selectedKey)
        val selectedLanguage = interactor.getStringArray(R.array.language)[selectedIndex]
        if (savedLanguage == selectedLanguage) {
            logger.info("Language selected is same as saved. No action taken...")
        } else {
            interactor.saveSelectedLanguage(selectedLanguage)
            settingsView.setLanguageTextView(selectedLanguage)
            interactor.getPreferenceChangeObserver().postLanguageChange(selectedLanguage)
        }
    }

    override fun onLatencyTypeSelected(latencyType: String) {
        val savedLatencyType = interactor.getAppPreferenceInterface().latencyType
        if (savedLatencyType == latencyType) {
            logger.info("Same latency selected as saved.")
        } else {
            logger.info("Saving selected latency type")
            interactor.getAppPreferenceInterface().latencyType = latencyType
            updateServerList()
        }
    }

    override fun onNotificationToggleButtonClicked() {
        if (interactor.getAppPreferenceInterface().notificationStat) {
            logger.info("Previous  notification Toggle Settings: True")
            interactor.getAppPreferenceInterface().notificationStat = false
            settingsView.setupNotificationToggleImage(R.drawable.ic_toggle_button_off)
            interactor.getTrafficCounter().reset(false)
        } else {
            logger.info("Previous Notification Toggle Settings: False")
            interactor.getAppPreferenceInterface().notificationStat = true
            settingsView.setupNotificationToggleImage(R.drawable.ic_toggle_button_on)
            interactor.getTrafficCounter().reset(true)
        }
    }

    override fun onSelectionSelected(selection: String) {
        val savedSelection = interactor.getSavedSelection()
        if (savedSelection == selection) {
            logger.info("List selection selected is same as saved. No action taken...")
        } else {
            interactor.saveSelection(selection)
            updateServerList()
        }
    }

    override fun onShowHealthToggleClicked() {
        if (interactor.getAppPreferenceInterface().isShowLocationHealthEnabled) {
            logger.info("Previous show location health Toggle Settings: True")
            interactor.getAppPreferenceInterface().isShowLocationHealthEnabled = false
            settingsView.setupLocationHealthToggleImage(R.drawable.ic_toggle_button_off)
        } else {
            logger.info("Previous show location health Toggle Settings: False")
            interactor.getAppPreferenceInterface().isShowLocationHealthEnabled = true
            settingsView.setupLocationHealthToggleImage(R.drawable.ic_toggle_button_on)
        }
        interactor.getPreferenceChangeObserver().postShowLocationHealthChange()
    }

    override fun onThemeSelected(theme: String) {
        val savedTheme = interactor.getAppPreferenceInterface().selectedTheme
        if (savedTheme == theme) {
            logger.info("Same theme selected as saved.")
        } else {
            logger.info("Saving selected theme")
            interactor.getAppPreferenceInterface().selectedTheme = theme
            appContext.applicationInterface.setTheme()
            settingsView.reloadApp()
        }
    }

    override fun resizeAndSaveBitmap(inputStream: InputStream, outputStream: OutputStream) {
        val bitmap = BitmapFactory.decodeStream(inputStream)
        val requiredHeight = interactor.getAppPreferenceInterface().flagViewHeight
        if (bitmap.height > requiredHeight) {
            val customBitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, requiredHeight)
            customBitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream)
        } else {
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream)
        }
    }

    override fun setTheme(context: Context) {
        val savedThem = interactor.getAppPreferenceInterface().selectedTheme
        logger.debug("Setting theme to $savedThem")
        if (savedThem == PreferencesKeyConstants.DARK_THEME) {
            context.setTheme(R.style.DarkTheme)
        } else {
            context.setTheme(R.style.LightTheme)
        }
    }

    override fun setupInitialLayout() {
        val savedLanguage = interactor.getSavedLanguage()

        //Setup language settings
        settingsView.setupLanguageAdapter(
                interactor.getStringArray(R.array.language), appContext.getLanguageCode(savedLanguage), interactor.getStringArray(R.array.language_codes)
        )

        // Setup notification stats toggle
        settingsView.setupNotificationToggleImage(
                if (interactor.getAppPreferenceInterface().notificationStat) R.drawable.ic_toggle_button_on else R.drawable.ic_toggle_button_off
        )

        // Setup Haptic toggle
        settingsView.setupHapticToggleImage(
                if (interactor.getAppPreferenceInterface().isHapticFeedbackEnabled) R.drawable.ic_toggle_button_on else R.drawable.ic_toggle_button_off
        )

        // Setup Show Location health
        settingsView.setupLocationHealthToggleImage(
                if (interactor.getAppPreferenceInterface().isShowLocationHealthEnabled) R.drawable.ic_toggle_button_on else R.drawable.ic_toggle_button_off
        )

        // Setup selection settings
        val savedSelection = interactor.getAppPreferenceInterface().selection
        settingsView.setupSelectionAdapter(
                interactor.getStringArray(R.array.order_list),
                savedSelection,
                interactor.getStringArray(R.array.order_list_keys)
        )

        // Setup theme
        val savedTheme = interactor.getAppPreferenceInterface().selectedTheme
        settingsView.setupThemeAdapter(
                interactor.getStringArray(R.array.theme_list),
                savedTheme,
                interactor.getStringArray(R.array.theme_list_keys)
        )

        // Setup latency settings
        val savedLatencyType = interactor.getAppPreferenceInterface().latencyType
        settingsView.setupLatencyAdapter(
                interactor.getStringArray(R.array.latency_selection),
                savedLatencyType,
                interactor.getStringArray(R.array.latency_selection_keys)
        )
        settingsView.setAppVersionText(WindUtilities.getVersionName())
        settingsView.setActivityTitle(interactor.getResourceString(R.string.general))
        settingsView.registerLocaleChangeListener()
        setupAppBackgroundAdapter(interactor.getAppPreferenceInterface().isCustomBackground)
        val expandedFlagWidth = interactor.getAppPreferenceInterface().flagViewWidth
        val expandedFlagHeight = interactor.getAppPreferenceInterface().flagViewHeight
        val flagDimensionsText = String.format("%sx%s", expandedFlagWidth, expandedFlagHeight)
        settingsView.setFlagSizeLabel(flagDimensionsText)
    }

    private fun setupAppBackgroundAdapter(custom: Boolean) {
        if (custom) {
            interactor.getAppPreferenceInterface().isCustomBackground = true
            settingsView.setupCustomFlagAdapter(
                    interactor.getStringArray(R.array.background_list),
                    interactor.getStringArray(R.array.background_list_keys)[1],
                    interactor.getStringArray(R.array.background_list_keys)
            )
        } else {
            interactor.getAppPreferenceInterface().isCustomBackground = false
            settingsView.setupCustomFlagAdapter(
                    interactor.getStringArray(R.array.background_list),
                    interactor.getStringArray(R.array.background_list_keys)[0],
                    interactor.getStringArray(R.array.background_list_keys)
            )
        }
        setAppBackgroundPaths()
    }

    override fun onAppBackgroundValueChanged(value: String) {
        val newValue = value == interactor.getStringArray(R.array.background_list_keys)[1]
        if (newValue != interactor.getAppPreferenceInterface().isCustomBackground) {
            interactor.getAppPreferenceInterface().isCustomBackground = newValue
            setAppBackgroundPaths()
        }
    }

    private fun setAppBackgroundPaths() {
        val disconnectedFlagPath = interactor.getAppPreferenceInterface().disConnectedFlagPath
        val connectedFlagPath = interactor.getAppPreferenceInterface().connectedFlagPath
        settingsView.setDisconnectedFlagPath(
                (if (disconnectedFlagPath != null) Uri.parse(
                        disconnectedFlagPath
                ).path else "")!!
        )
        settingsView.setConnectedFlagPath(
                (if (connectedFlagPath != null) Uri.parse(
                        connectedFlagPath
                ).path else "")!!
        )
    }

    private fun updateServerList() {
        interactor.getServerListUpdater().load()
        interactor.getStaticListUpdater().load()
    }
}