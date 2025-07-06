/*
 * Copyright (c) 2021 Windscribe Limited.
 */
package com.windscribe.mobile.generalsettings

import android.app.Activity
import android.app.TaskStackBuilder
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.documentfile.provider.DocumentFile
import butterknife.BindView
import butterknife.OnClick
import com.windscribe.mobile.R
import com.windscribe.mobile.base.BaseActivity
import com.windscribe.mobile.custom_view.CustomDialog
import com.windscribe.mobile.custom_view.preferences.AppBackgroundView
import com.windscribe.mobile.custom_view.preferences.DropDownView
import com.windscribe.mobile.custom_view.preferences.ToggleView
import com.windscribe.mobile.di.ActivityModule
import com.windscribe.mobile.mainmenu.MainMenuActivity
import com.windscribe.mobile.windscribe.WindscribeActivity
import com.windscribe.vpn.state.PreferenceChangeObserver
import org.apache.commons.io.IOUtils
import org.slf4j.LoggerFactory
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import javax.inject.Inject

class GeneralSettingsActivity : BaseActivity(), GeneralSettingsView {

    private val logger = LoggerFactory.getLogger("basic")

    @BindView(R.id.cl_app_background_settings)
    lateinit var appBackgroundDropDown: AppBackgroundView

    @BindView(R.id.cl_selection_settings)
    lateinit var locationSelectionDropDown: DropDownView

    @BindView(R.id.cl_language_settings)
    lateinit var languageDropDown: DropDownView

    @BindView(R.id.cl_latency_settings)
    lateinit var latencyDropDown: DropDownView

    @BindView(R.id.cl_theme_settings)
    lateinit var themeDropDown: DropDownView

    @BindView(R.id.cl_notification_settings)
    lateinit var notificationToggle: ToggleView

    @BindView(R.id.cl_show_health)
    lateinit var locationLoadToggle: ToggleView

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

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (resultCode == Activity.RESULT_OK && data != null) {
            val fileUri = data.data
            logger.info(String.format("Received file uri  %s", fileUri.toString()))
            val file = fileUri?.let { uriToFile(it) }
            if (file != null) {
                logger.info("Converted uri to file")
                try {
                    contentResolver.openInputStream(fileUri).use { inputStream ->
                        FileOutputStream(file).use { outputStream ->
                            if (isTypeImage(fileUri)) {
                                presenter.resizeAndSaveBitmap(inputStream!!, outputStream)
                            } else {
                                IOUtils.copy(inputStream, outputStream)
                            }
                            val path = file.absolutePath
                            logger.info(String.format("Saved file to %s", path))
                            if (requestCode == DISCONNECTED_FLAG_PATH_PICK_REQUEST) {
                                presenter.onDisConnectedFlagPathPicked(path)
                            } else if (requestCode == CONNECTED_FLAG_PATH_PICK_REQUEST) {
                                presenter.onConnectedFlagPathPicked(path)
                            }
                        }
                    }
                } catch (e: IOException) {
                    logger.info("Error copying file from input stream")
                    showToast("Error copying image to app's internal storage")
                }
            } else {
                logger.info("Invalid file type")
                showToast("Invalid file.")
            }
        }
        super.onActivityResult(requestCode, resultCode, data)
    }

    override fun onDestroy() {
        presenter.onDestroy()
        super.onDestroy()
    }

    override val orderList: Array<String>
        get() = resources.getStringArray(R.array.order_list)
    override val themeList: Array<String>
        get() = resources.getStringArray(R.array.theme_list)

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
        latencyDropDown.delegate = object : DropDownView.Delegate {
            override fun onItemSelect(value: String) {
                presenter.onLatencyTypeSelected(value)
            }

            override fun onExplainClick() {}
        }
        languageDropDown.delegate = object : DropDownView.Delegate {
            override fun onItemSelect(value: String) {
                presenter.onLanguageSelected(value)
            }

            override fun onExplainClick() {}
        }
        themeDropDown.delegate = object : DropDownView.Delegate {
            override fun onItemSelect(value: String) {
                presenter.onThemeSelected(value)
            }

            override fun onExplainClick() {}
        }
        notificationToggle.delegate = object : ToggleView.Delegate {
            override fun onToggleClick() {
                presenter.onNotificationToggleButtonClicked()
            }

            override fun onExplainClick() {}
        }
        locationLoadToggle.delegate = object : ToggleView.Delegate {
            override fun onToggleClick() {
                presenter.onShowHealthToggleClicked()
            }

            override fun onExplainClick() {}
        }
        hapticToggle.delegate = object : ToggleView.Delegate {
            override fun onToggleClick() {
                presenter.onHapticToggleButtonClicked()
            }

            override fun onExplainClick() {}
        }
        appBackgroundDropDown.delegate = object : AppBackgroundView.Delegate {
            override fun onItemSelect(value: String) {
                presenter.onAppBackgroundValueChanged(value)
            }

            override fun onFirstRightIconClick() {
                logger.info("User clicked on disconnected flag edit button...")
                presenter.onDisconnectedFlagEditClicked(
                    DISCONNECTED_FLAG_PATH_PICK_REQUEST
                )
            }

            override fun onSecondRightIconClick() {
                logger.info("User clicked on connected flag edit button...")
                presenter.onConnectedFlagEditClicked(CONNECTED_FLAG_PATH_PICK_REQUEST)
            }
        }
    }

    override fun openFileChooser(requestCode: Int) {
        val pickIntent = Intent(Intent.ACTION_OPEN_DOCUMENT)
        pickIntent.type = "*/*"
        logger.info(String.format("Creating pick intent for %s", requestCode))
        if (pickIntent.resolveActivity(packageManager) != null) {
            startActivityForResult(pickIntent, requestCode)
        } else {
            logger.info("Pick intent did not resolve to any activity.")
            showToast("No File manager found.")
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
        latencyDropDown.setTitle(latencyDisplay)
        languageDropDown.setTitle(language)
        themeDropDown.setTitle(appearance)
        notificationToggle.setTitle(notificationState)
        hapticToggle.setTitle(hapticFeedback)
        tvVersionLabel.text = version
        appBackgroundDropDown.setTitle(appBackground)
        appBackgroundDropDown.setFirstItemDescription(disconnected)
        appBackgroundDropDown.setSecondItemDescription(connected)
    }

    override fun setActivityTitle(activityTitle: String) {
        tvActivityTitle.text = activityTitle
    }

    override fun setAppVersionText(versionText: String) {
        versionSelection.text = versionText
    }

    override fun setConnectedFlagPath(path: String) {
        appBackgroundDropDown.setSecondItemDescription(path)
    }

    override fun setDisconnectedFlagPath(path: String) {
        appBackgroundDropDown.setFirstItemDescription(path)
    }

    override fun setFlagSizeLabel(label: String) {
        appBackgroundDropDown.setFirstItemTitle(label)
        appBackgroundDropDown.setSecondItemTitle(label)
    }

    override fun setLanguageTextView(language: String) {
        languageDropDown.setCurrentValue(language)
        reloadApp()
    }

    override fun setLatencyType(latencyType: String) {
        latencyDropDown.setCurrentValue(latencyType)
    }

    override fun setSelectionTextView(selection: String) {
        locationSelectionDropDown.setCurrentValue(selection)
    }

    override fun setupCustomFlagAdapter(
        localiseValues: Array<String>,
        selectedKey: String,
        keys: Array<String>
    ) {
        appBackgroundDropDown.setAdapter(localiseValues, selectedKey, keys)
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

    override fun setupLatencyAdapter(
        localiseValues: Array<String>,
        selelctedKey: String,
        keys: Array<String>
    ) {
        latencyDropDown.setAdapter(localiseValues, selelctedKey, keys)
    }

    override fun setupLocationHealthToggleImage(image: Int) {
        locationLoadToggle.setToggleImage(image)
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

    override fun setupThemeAdapter(
        localiseValues: Array<String>,
        selectedKey: String,
        keys: Array<String>
    ) {
        themeDropDown.setAdapter(localiseValues, selectedKey, keys)
    }

    fun showToast(toastString: String) {
        Toast.makeText(this, toastString, Toast.LENGTH_SHORT).show()
    }

    private fun getFileName(fileUri: Uri): String? {
        val fileDocument = DocumentFile.fromSingleUri(this, fileUri)
        return fileDocument?.name
    }

    private fun isTypeImage(fileUri: Uri): Boolean {
        val fileName = getFileName(fileUri)
        return if (fileName != null) {
            fileName.endsWith(".jpeg") or fileName.endsWith(".jpg") or fileName.endsWith(".png")
        } else false
    }

    override fun reloadApp() {
        TaskStackBuilder.create(this).addNextIntent(WindscribeActivity.getStartIntent(this))
            .addNextIntent(MainMenuActivity.getStartIntent(this))
            .addNextIntentWithParentStack(intent).startActivities()
    }

    private fun uriToFile(fileUri: Uri): File? {
        val fileName = getFileName(fileUri)
        if (fileName != null) {
            if (fileName.endsWith(".jpeg") or fileName.endsWith(".jpg") or fileName.endsWith(".png") or fileName
                    .endsWith(".gif")
            ) {
                return File(filesDir, fileName)
            }
        }
        return null
    }

    companion object {
        fun getStartIntent(context: Context?): Intent {
            return Intent(context, GeneralSettingsActivity::class.java)
        }
    }
}