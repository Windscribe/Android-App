/*
 * Copyright (c) 2021 Windscribe Limited.
 */
package com.windscribe.mobile.generalsettings

import com.windscribe.mobile.R
import com.windscribe.vpn.ActivityInteractor
import com.windscribe.vpn.Windscribe.Companion.appContext
import com.windscribe.vpn.commonutils.WindUtilities
import org.slf4j.LoggerFactory
import javax.inject.Inject

class GeneralSettingsPresenterImpl @Inject constructor(
    private var settingsView: GeneralSettingsView, private var interactor: ActivityInteractor
) : GeneralSettingsPresenter {

    private val logger = LoggerFactory.getLogger("basic")

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
            interactor.getServerListUpdater().invalidateServerListUI()
        }
    }

    override fun setupInitialLayout() {
        val savedLanguage = interactor.getSavedLanguage()

        //Setup language settings
        settingsView.setupLanguageAdapter(
            interactor.getStringArray(R.array.language),
            appContext.getLanguageCode(savedLanguage),
            interactor.getStringArray(R.array.language_codes)
        )

        // Setup notification stats toggle
        settingsView.setupNotificationToggleImage(
            if (interactor.getAppPreferenceInterface().notificationStat) R.drawable.ic_toggle_button_on else R.drawable.ic_toggle_button_off
        )
        // Setup Haptic toggle
        settingsView.setupHapticToggleImage(
            if (interactor.getAppPreferenceInterface().isHapticFeedbackEnabled) R.drawable.ic_toggle_button_on else R.drawable.ic_toggle_button_off
        )
        val savedSelection = interactor.getAppPreferenceInterface().selection
        settingsView.setupSelectionAdapter(
            interactor.getStringArray(R.array.order_list),
            savedSelection,
            interactor.getStringArray(R.array.order_list_keys)
        )
        settingsView.setAppVersionText(WindUtilities.getVersionName())
        settingsView.setActivityTitle(interactor.getResourceString(R.string.general))
        settingsView.registerLocaleChangeListener()
    }
}