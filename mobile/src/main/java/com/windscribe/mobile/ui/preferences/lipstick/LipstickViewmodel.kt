package com.windscribe.mobile.ui.preferences.lipstick

import android.content.Context
import android.graphics.drawable.Drawable
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.windscribe.mobile.R
import com.windscribe.mobile.ui.connection.ToastMessage
import com.windscribe.mobile.ui.model.DropDownItem
import com.windscribe.mobile.ui.model.ThemeItem
import com.windscribe.mobile.ui.preferences.icons.AppIconManager
import com.windscribe.mobile.ui.preferences.lipstick.LookAndFeelHelper.getAspectRatioOptions
import com.windscribe.mobile.ui.preferences.lipstick.LookAndFeelHelper.getBackgroundOptions
import com.windscribe.mobile.ui.preferences.lipstick.LookAndFeelHelper.getBundledBackgroundOptions
import com.windscribe.mobile.ui.preferences.lipstick.LookAndFeelHelper.getBundledSoundOptions
import com.windscribe.mobile.ui.preferences.lipstick.LookAndFeelHelper.getSoundFile
import com.windscribe.mobile.ui.preferences.lipstick.LookAndFeelHelper.getSoundOptions
import com.windscribe.vpn.Windscribe.Companion.appContext
import com.windscribe.vpn.apppreference.PreferencesHelper
import com.windscribe.vpn.repository.ServerListRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.slf4j.LoggerFactory
import java.io.FileOutputStream
import java.io.IOException
import javax.inject.Inject


abstract class LipstickViewmodel : ViewModel() {
    abstract val whenDisconnectedBackgroundItem: StateFlow<DropDownItem>
    abstract val whenConnectedBackgroundItem: StateFlow<DropDownItem>
    abstract val whenDisconnectedSoundItem: StateFlow<DropDownItem>
    abstract val whenConnectedSoundItem: StateFlow<DropDownItem>
    abstract val aspectRatioItem: StateFlow<DropDownItem>
    abstract val bundledDisconnectedBackgroundItem: StateFlow<DropDownItem>
    abstract val bundledConnectedBackgroundItem: StateFlow<DropDownItem>
    abstract val bundledDisconnectedSoundItem: StateFlow<DropDownItem>
    abstract val bundledConnectedSoundItem: StateFlow<DropDownItem>
    abstract val customDisconnectedBackgroundItem: StateFlow<String?>
    abstract val customConnectedBackgroundItem: StateFlow<String?>
    abstract val customDisconnectedSoundItem: StateFlow<String?>
    abstract val customConnectedSoundItem: StateFlow<String?>
    abstract fun onWhenDisconnectedBackgroundItemSelected(item: DropDownItem)
    abstract fun onWhenConnectedBackgroundItemSelected(item: DropDownItem)
    abstract fun onAspectRatioItemSelected(item: DropDownItem)
    abstract fun onWhenDisconnectedSoundItemSelected(item: DropDownItem)
    abstract fun onWhenConnectedSoundItemSelected(item: DropDownItem)
    abstract fun onDisconnectedBundledBackgroundItemSelected(item: DropDownItem)
    abstract fun onConnectedBundledBackgroundItemSelected(item: DropDownItem)
    abstract fun onDisconnectedBundledSoundItemSelected(item: DropDownItem)
    abstract fun onConnectedBundledSoundItemSelected(item: DropDownItem)
    abstract fun loadConnectedCustomBackground(context: Context, fileUri: Uri)
    abstract fun loadDisconnectedCustomBackground(context: Context, fileUri: Uri)
    abstract fun loadConnectedCustomSound(context: Context, fileUri: Uri)
    abstract fun loadDisconnectedCustomSound(context: Context, fileUri: Uri)
    abstract fun loadServerListFile(context: Context, fileUri: Uri)
    abstract fun onResetClick()
    abstract fun exportServerListFile(context: Context, uri: Uri)
    abstract val toastMessage: StateFlow<ToastMessage>
    abstract fun clearToast()
    abstract fun onThemeItemSelected(theme: ThemeItem)
    abstract val themeItem: StateFlow<ThemeItem>
    abstract val selectedAppIcon: StateFlow<Int>
}

class LipstickViewmodelImpl @Inject constructor(
    private val preferenceHelper: PreferencesHelper,
    private val serverListRepository: ServerListRepository,
    private val appIconManager: AppIconManager
) :
    LipstickViewmodel() {
    private val _whenDisconnectedBackgroundItem = MutableStateFlow(getBackgroundOptions().first())
    override val whenDisconnectedBackgroundItem: StateFlow<DropDownItem> =
        _whenDisconnectedBackgroundItem
    private val _whenConnectedBackgroundItem = MutableStateFlow(getBackgroundOptions().first())
    override val whenConnectedBackgroundItem: StateFlow<DropDownItem> = _whenConnectedBackgroundItem
    private val _aspectRatioItem = MutableStateFlow(getAspectRatioOptions().first())
    override val aspectRatioItem: StateFlow<DropDownItem> = _aspectRatioItem
    private val _bundledDisconnectedBackgroundItem =
        MutableStateFlow(getBundledBackgroundOptions().first())
    override val bundledDisconnectedBackgroundItem: StateFlow<DropDownItem> =
        _bundledDisconnectedBackgroundItem
    private val _bundledConnectedBackgroundItem =
        MutableStateFlow(getBundledBackgroundOptions().first())
    override val bundledConnectedBackgroundItem: StateFlow<DropDownItem> =
        _bundledConnectedBackgroundItem
    private val _customDisconnectedBackgroundItem = MutableStateFlow<String?>(null)
    override val customDisconnectedBackgroundItem: StateFlow<String?> =
        _customDisconnectedBackgroundItem
    private val _customConnectedBackgroundItem = MutableStateFlow<String?>(null)
    override val customConnectedBackgroundItem: StateFlow<String?> = _customConnectedBackgroundItem
    private val _whenDisconnectedSoundItem = MutableStateFlow(getBackgroundOptions().first())
    override val whenDisconnectedSoundItem: StateFlow<DropDownItem> = _whenDisconnectedSoundItem
    private val _whenConnectedSoundItem = MutableStateFlow(getBackgroundOptions().first())
    override val whenConnectedSoundItem: StateFlow<DropDownItem> = _whenConnectedSoundItem
    private val _customDisconnectedSoundItem = MutableStateFlow<String?>(null)
    override val customDisconnectedSoundItem: StateFlow<String?> = _customDisconnectedSoundItem
    private val _customConnectedSoundItem = MutableStateFlow<String?>(null)
    override val customConnectedSoundItem: StateFlow<String?> = _customConnectedSoundItem
    private val _bundledDisconnectedSoundItem = MutableStateFlow(getSoundOptions().first())
    override val bundledDisconnectedSoundItem: StateFlow<DropDownItem> =
        _bundledDisconnectedSoundItem
    private val _bundledConnectedSoundItem = MutableStateFlow(getSoundOptions().first())
    override val bundledConnectedSoundItem: StateFlow<DropDownItem> = _bundledConnectedSoundItem
    private val _toastMessage = MutableStateFlow<ToastMessage>(ToastMessage.None)
    override val toastMessage: StateFlow<ToastMessage> = _toastMessage
    private val _themeItem = MutableStateFlow(LookAndFeelHelper.getThemeOptions().first())
    override val themeItem: StateFlow<ThemeItem> = _themeItem
    private val _selectedAppIcon = MutableStateFlow(com.windscribe.vpn.R.mipmap.ws_launcher)
    override val selectedAppIcon: StateFlow<Int> = _selectedAppIcon
    private val logger = LoggerFactory.getLogger("LipstickViewmodel")

    init {
        loadBackgroundPreferences()
        loadSoundPreferences()
        loadThemePreferences()
        observeSelectedAppIcon()
    }

    private fun loadBackgroundPreferences() {
        _whenDisconnectedBackgroundItem.value =
            getBackgroundOptions().firstOrNull { it.id == preferenceHelper.whenDisconnectedBackgroundOption }
                ?: getBackgroundOptions().first()
        _whenConnectedBackgroundItem.value =
            getBackgroundOptions().firstOrNull { it.id == preferenceHelper.whenConnectedBackgroundOption }
                ?: getBackgroundOptions().first()
        _aspectRatioItem.value =
            getAspectRatioOptions().firstOrNull { it.id == preferenceHelper.backgroundAspectRatioOption }
                ?: getAspectRatioOptions().first()
        _bundledDisconnectedBackgroundItem.value =
            getBundledBackgroundOptions().firstOrNull { it.id == preferenceHelper.disconnectedBundleBackgroundOption }
                ?: getBundledBackgroundOptions().first()
        _bundledConnectedBackgroundItem.value =
            getBundledBackgroundOptions().firstOrNull { it.id == preferenceHelper.connectedBundleBackgroundOption }
                ?: getBundledBackgroundOptions().first()
        _customDisconnectedBackgroundItem.value = preferenceHelper.customDisconnectedBackground
        _customConnectedBackgroundItem.value = preferenceHelper.customConnectedBackground
    }

    private fun loadSoundPreferences() {
        _whenDisconnectedSoundItem.value =
            getSoundOptions().firstOrNull { it.id == preferenceHelper.whenDisconnectedSoundOption }
                ?: getSoundOptions().first()
        _whenConnectedSoundItem.value =
            getSoundOptions().firstOrNull { it.id == preferenceHelper.whenConnectedSoundOption }
                ?: getSoundOptions().first()
        _bundledDisconnectedSoundItem.value =
            getBundledSoundOptions().firstOrNull { it.id == preferenceHelper.disconnectedBundleSoundOption }
                ?: getBundledSoundOptions().first()
        _bundledConnectedSoundItem.value =
            getBundledSoundOptions().firstOrNull { it.id == preferenceHelper.connectedBundleSoundOption }
                ?: getBundledSoundOptions().first()
        _customDisconnectedSoundItem.value = preferenceHelper.customDisconnectedSound
        _customConnectedSoundItem.value = preferenceHelper.customConnectedSound
    }

    private fun loadThemePreferences() {
        _themeItem.value = LookAndFeelHelper.getThemeOptions()
            .firstOrNull { it.id == preferenceHelper.selectedTheme }
            ?: LookAndFeelHelper.getThemeOptions().first()
        preferenceHelper.selectedTheme
    }

    override fun onWhenDisconnectedBackgroundItemSelected(item: DropDownItem) {
        _whenDisconnectedBackgroundItem.value = item
        preferenceHelper.whenDisconnectedBackgroundOption = item.id
    }

    override fun onWhenConnectedBackgroundItemSelected(item: DropDownItem) {
        _whenConnectedBackgroundItem.value = item
        preferenceHelper.whenConnectedBackgroundOption = item.id
    }

    override fun onAspectRatioItemSelected(item: DropDownItem) {
        _aspectRatioItem.value = item
        preferenceHelper.backgroundAspectRatioOption = item.id
    }

    override fun onDisconnectedBundledBackgroundItemSelected(item: DropDownItem) {
        _bundledDisconnectedBackgroundItem.value = item
        preferenceHelper.disconnectedBundleBackgroundOption = item.id
    }

    override fun onConnectedBundledBackgroundItemSelected(item: DropDownItem) {
        _bundledConnectedBackgroundItem.value = item
        preferenceHelper.connectedBundleBackgroundOption = item.id
    }

    override fun onDisconnectedBundledSoundItemSelected(item: DropDownItem) {
        _bundledDisconnectedSoundItem.value = item
        preferenceHelper.disconnectedBundleSoundOption = item.id
    }

    override fun onConnectedBundledSoundItemSelected(item: DropDownItem) {
        _bundledConnectedSoundItem.value = item
        preferenceHelper.connectedBundleSoundOption = item.id
    }

    override fun loadDisconnectedCustomBackground(context: Context, fileUri: Uri) {
        try {
            val inputStream = context.contentResolver.openInputStream(fileUri)
            inputStream?.use {
                val documentFile = DocumentFile.fromSingleUri(context, fileUri)
                val output =
                    context.openFileOutput("disconnected_background.png", Context.MODE_PRIVATE)
                it.copyTo(output)
                output.close()
                _customDisconnectedBackgroundItem.value = documentFile?.name
                preferenceHelper.customDisconnectedBackground = documentFile?.name
            }
        } catch (e: IOException) {
            logger.error("Error reading disconnected custom background image file: ${e.message}")
        }
    }

    override fun loadConnectedCustomBackground(context: Context, fileUri: Uri) {
        try {
            val inputStream = context.contentResolver.openInputStream(fileUri)
            inputStream?.use {
                val documentFile = DocumentFile.fromSingleUri(context, fileUri)
                val output =
                    context.openFileOutput("connected_background.png", Context.MODE_PRIVATE)
                it.copyTo(output)
                output.close()
                _customConnectedBackgroundItem.value = documentFile?.name
                preferenceHelper.customConnectedBackground = documentFile?.name
            }
        } catch (e: IOException) {
            logger.error("Error reading connected custom background image file: ${e.message}")
        }
    }

    override fun loadDisconnectedCustomSound(context: Context, fileUri: Uri) {
        try {
            val inputStream = context.contentResolver.openInputStream(fileUri)
            inputStream?.use {
                val documentFile = DocumentFile.fromSingleUri(context, fileUri)
                if (documentFile?.name == null) {
                    return
                }
                val outputFile = getSoundFile(context, false, documentFile.name!!)
                val output = FileOutputStream(outputFile)
                it.copyTo(output)
                output.close()
                _customDisconnectedSoundItem.value = outputFile.name
                preferenceHelper.customDisconnectedSound = outputFile.name
            }
        } catch (e: IOException) {
            logger.error("Error reading disconnected custom sound file: ${e.message}")
        }
    }

    override fun loadConnectedCustomSound(context: Context, fileUri: Uri) {
        try {
            val inputStream = context.contentResolver.openInputStream(fileUri)
            inputStream?.use {
                val documentFile = DocumentFile.fromSingleUri(context, fileUri)
                if (documentFile?.name == null) {
                    return
                }
                val outputFile = getSoundFile(context, true, documentFile.name!!)
                val output = FileOutputStream(outputFile)
                it.copyTo(output)
                output.close()
                _customConnectedSoundItem.value = outputFile.name
                preferenceHelper.customConnectedSound = outputFile.name
            }
        } catch (e: IOException) {
            logger.error("Error reading connected custom sound file: ${e.message}")
        }
    }

    override fun onWhenConnectedSoundItemSelected(item: DropDownItem) {
        _whenConnectedSoundItem.value = item
        preferenceHelper.whenConnectedSoundOption = item.id
    }

    override fun onWhenDisconnectedSoundItemSelected(item: DropDownItem) {
        _whenDisconnectedSoundItem.value = item
        preferenceHelper.whenDisconnectedSoundOption = item.id
    }

    override fun loadServerListFile(context: Context, fileUri: Uri) {
        try {
            val inputStream = context.contentResolver.openInputStream(fileUri)
            val jsonString = inputStream?.bufferedReader().use { it?.readText() }
            if (jsonString != null) {
                serverListRepository.saveLocationsJson(jsonString)
                showToast("Imported successfully!")
            } else {
                showToast("Failed to read file")
            }
        } catch (e: Exception) {
            showToast("Error: ${e.message}")
        }
    }

    override fun exportServerListFile(context: Context, uri: Uri) {
        val jsonData = serverListRepository.locationJsonToExport.value
        if (jsonData.isEmpty()) {
            return
        }
        try {
            context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                outputStream.write(jsonData.toByteArray())
                showToast("Exported successfully!")
            }
        } catch (e: Exception) {
            showToast("Failed to export: ${e.message}")
        }
    }

    override fun onResetClick() {
        serverListRepository.deleteLocationsJson()
        showToast("Server list reset successfully!")
    }

    private fun observeSelectedAppIcon() {
        viewModelScope.launch {
            appIconManager.selectedAppIcon.collect { appIcon ->
                if (appIcon != null) {
                    _selectedAppIcon.value = appIcon.icon
                }
            }
        }
    }

    private fun showToast(message: String) {
        viewModelScope.launch {
            _toastMessage.emit(ToastMessage.Raw(message))
        }
    }

    override fun clearToast() {
        _toastMessage.value = ToastMessage.None
    }

    override fun onThemeItemSelected(theme: ThemeItem) {
        _themeItem.value = theme
        preferenceHelper.selectedTheme = theme.id
        appContext.applicationInterface.setTheme()
    }
}