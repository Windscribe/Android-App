package com.windscribe.mobile.ui.preferences.general

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.windscribe.mobile.ui.model.DropDownStringItem
import com.windscribe.vpn.R
import com.windscribe.vpn.Windscribe.Companion.appContext
import com.windscribe.vpn.apppreference.PreferencesHelper
import com.windscribe.vpn.commonutils.WindUtilities
import com.windscribe.vpn.apppreference.PreferencesKeyConstants
import com.windscribe.vpn.repository.UserRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

abstract class GeneralViewModel : ViewModel() {
    abstract val orderByItems: List<DropDownStringItem>
    abstract fun onOrderByItemSelected(item: DropDownStringItem)
    abstract val selectedOrderBy: String

    abstract val languageItems: List<DropDownStringItem>
    abstract fun onLanguageItemSelected(item: DropDownStringItem)
    abstract val selectedLanguage: String

    abstract val reloadApp: SharedFlow<Boolean>
    abstract val isHapticEnabled: StateFlow<Boolean>
    abstract fun onHapticToggleButtonClicked()

    abstract fun onLocationLoadItemClicked()
    abstract val isLocationLoadEnabled: StateFlow<Boolean>

    abstract val isNotificationStatEnabled: StateFlow<Boolean>
    abstract fun onNotificationStatEnabledClick()
    abstract val versionName: String
}

class GeneralViewModelImpl(
    private val preferenceHelper: PreferencesHelper, private val userRepository: UserRepository
) : GeneralViewModel() {

    private val _reloadApp = MutableSharedFlow<Boolean>()
    override val reloadApp: SharedFlow<Boolean> = _reloadApp

    override val orderByItems: List<DropDownStringItem>
        get() = with(appContext.resources) {
            val values = getStringArray(R.array.order_list)
            val keys = getStringArray(R.array.order_list_keys)
            keys.zip(values) { key, value -> DropDownStringItem(key, value) }
        }

    override val selectedOrderBy: String
        get() = preferenceHelper.selection

    override fun onOrderByItemSelected(item: DropDownStringItem) {
        preferenceHelper.selection = item.key
    }

    override val languageItems: List<DropDownStringItem>
        get() = with(appContext.resources) {
            val entries = getStringArray(R.array.language)
            entries.zip(entries) { key, value -> DropDownStringItem(key, value) }
        }

    override val selectedLanguage: String
        get() = preferenceHelper.savedLanguage

    override fun onLanguageItemSelected(item: DropDownStringItem) {
        viewModelScope.launch {
            preferenceHelper.savedLanguage = item.key
            _reloadApp.emit(true)
        }
    }

    private val _isLocationLoadEnabled = MutableStateFlow(preferenceHelper.isShowLocationHealthEnabled)
    override val isLocationLoadEnabled: StateFlow<Boolean> = _isLocationLoadEnabled
    override fun onLocationLoadItemClicked() {
        viewModelScope.launch {
            val updatedValue = !_isLocationLoadEnabled.value
            preferenceHelper.isShowLocationHealthEnabled = updatedValue
            _isLocationLoadEnabled.value = updatedValue
        }
    }

    private val _isHapticEnabled = MutableStateFlow(preferenceHelper.isHapticFeedbackEnabled)
    override val isHapticEnabled: StateFlow<Boolean> = _isHapticEnabled

    override fun onHapticToggleButtonClicked() {
        val newValue = !_isHapticEnabled.value
        preferenceHelper.isHapticFeedbackEnabled = newValue
        _isHapticEnabled.value = newValue
    }

    private val _isNotificationStatEnabled = MutableStateFlow(preferenceHelper.notificationStat)
    override val isNotificationStatEnabled: StateFlow<Boolean> = _isNotificationStatEnabled

    override fun onNotificationStatEnabledClick() {
        val newValue = !_isNotificationStatEnabled.value
        preferenceHelper.notificationStat = newValue
        _isNotificationStatEnabled.value = newValue
    }

    override val versionName: String
        get() = "v${WindUtilities.getVersionName()}"
}