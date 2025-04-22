/*
 * Copyright (c) 2021 Windscribe Limited.
 */
package com.windscribe.mobile.generalsettings

import android.app.TaskStackBuilder
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import butterknife.BindView
import butterknife.OnClick
import com.windscribe.mobile.R
import com.windscribe.mobile.base.BaseActivity
import com.windscribe.mobile.custom_view.CustomDialog
import com.windscribe.mobile.custom_view.preferences.DropDownView
import com.windscribe.mobile.custom_view.preferences.ToggleView
import com.windscribe.mobile.di.ActivityModule
import com.windscribe.mobile.mainmenu.MainMenuActivity
import com.windscribe.mobile.view.AppStartActivity
import com.windscribe.vpn.state.PreferenceChangeObserver
import org.slf4j.LoggerFactory
import javax.inject.Inject

class GeneralSettingsActivity : BaseActivity(), GeneralSettingsView {

    private val logger = LoggerFactory.getLogger("basic")

    @BindView(R.id.cl_selection_settings)
    lateinit var locationSelectionDropDown: DropDownView

    @BindView(R.id.cl_language_settings)
    lateinit var languageDropDown: DropDownView

    @BindView(R.id.cl_notification_settings)
    lateinit var notificationToggle: ToggleView

    @BindView(R.id.cl_haptic_settings)
    lateinit var hapticToggle: ToggleView

    @BindView(R.id.nav_button)
    lateinit var imgGeneralBackButton: ImageView

    @BindView(R.id.nav_title)
    lateinit var tvActivityTitle: TextView

    @BindView(R.id.tv_version_label)
    lateinit var tvVersionLabel: TextView

    @BindView(R.id.tv_version_selection)
    lateinit var versionSelection: TextView

    @Inject
    lateinit var presenter: GeneralSettingsPresenter

    @Inject
    lateinit var preferenceChangeObserver: PreferenceChangeObserver

    @Inject
    lateinit var sendDebugDialog: CustomDialog

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setActivityModule(ActivityModule(this, this)).inject(this)
        setContentLayout(R.layout.activity_general_settings, true)
        setUpCustomViewDelegates()
        logger.info("Setting up layout based on saved mode settings...")
        presenter.setupInitialLayout()
    }

    override fun onDestroy() {
        presenter.onDestroy()
        super.onDestroy()
    }

    override val orderList: Array<String>
        get() = resources.getStringArray(R.array.order_list)

    @OnClick(R.id.nav_button)
    fun onBackButtonClicked() {
        logger.info("User clicked on back arrow ...")
        onBackPressed()
    }

    private fun setUpCustomViewDelegates() {
        locationSelectionDropDown.delegate = object : DropDownView.Delegate {
            override fun onItemSelect(value: String) {
                presenter.onSelectionSelected(value)
            }

            override fun onExplainClick() {}
        }

        languageDropDown.delegate = object : DropDownView.Delegate {
            override fun onItemSelect(value: String) {
                presenter.onLanguageSelected(value)
            }

            override fun onExplainClick() {}
        }

        notificationToggle.delegate = object : ToggleView.Delegate {
            override fun onToggleClick() {
                presenter.onNotificationToggleButtonClicked()
            }

            override fun onExplainClick() {}
        }

        hapticToggle.delegate = object : ToggleView.Delegate {
            override fun onToggleClick() {
                presenter.onHapticToggleButtonClicked()
            }

            override fun onExplainClick() {}
        }
    }

    override fun registerLocaleChangeListener() {
        preferenceChangeObserver.addLanguageChangeObserver(this) {
            setLanguage()
            presenter.onLanguageChanged()
        }
    }

    override fun resetTextResources(
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
    ) {
        tvActivityTitle.text = title
        locationSelectionDropDown.setTitle(sortBy)
        languageDropDown.setTitle(language)
        notificationToggle.setTitle(notificationState)
        hapticToggle.setTitle(hapticFeedback)
        tvVersionLabel.text = version
    }

    override fun setActivityTitle(activityTitle: String) {
        tvActivityTitle.text = activityTitle
    }

    override fun setAppVersionText(versionText: String) {
        versionSelection.text = versionText
    }

    override fun setLanguageTextView(language: String) {
        languageDropDown.setCurrentValue(language)
        reloadApp()
    }

    override fun setSelectionTextView(selection: String) {
        locationSelectionDropDown.setCurrentValue(selection)
    }

    override fun setupHapticToggleImage(ic_toggle_button_off: Int) {
        hapticToggle.setToggleImage(ic_toggle_button_off)
    }

    override fun setupLanguageAdapter(
        localiseValues: Array<String>,
        selectedKey: String,
        keys: Array<String>
    ) {
        languageDropDown.setAdapter(localiseValues, selectedKey, keys)
    }

    override fun setupNotificationToggleImage(ic_toggle_button_off: Int) {
        notificationToggle.setToggleImage(ic_toggle_button_off)
    }

    override fun setupSelectionAdapter(
        localiseValues: Array<String>,
        selectedKey: String,
        keys: Array<String>
    ) {
        locationSelectionDropDown.setAdapter(localiseValues, selectedKey, keys)
    }

    fun showToast(toastString: String) {
        Toast.makeText(this, toastString, Toast.LENGTH_SHORT).show()
    }

    override fun reloadApp() {
        TaskStackBuilder.create(this).addNextIntent(Intent(this, AppStartActivity::class.java))
            .addNextIntent(MainMenuActivity.getStartIntent(this))
            .addNextIntentWithParentStack(intent).startActivities()
    }


    companion object {
        fun getStartIntent(context: Context?): Intent {
            return Intent(context, GeneralSettingsActivity::class.java)
        }
    }
}