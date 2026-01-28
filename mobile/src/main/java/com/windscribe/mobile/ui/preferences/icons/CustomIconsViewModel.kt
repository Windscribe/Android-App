package com.windscribe.mobile.ui.preferences.icons

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

abstract class CustomIconsViewModel : ViewModel() {
    abstract val icons: StateFlow<Map<String, AppIcon>>
    abstract val showConfirmDialog: StateFlow<AppIcon?>
    abstract fun selectIcon(appIcon: AppIcon)
    abstract fun confirmIconChange()
    abstract fun dismissDialog()
}

class CustomIconsViewModelImpl(val appIconManager: AppIconManager) : CustomIconsViewModel() {
    val _icons: MutableStateFlow<Map<String, AppIcon>> = MutableStateFlow(mapOf())
    override val icons: StateFlow<Map<String, AppIcon>> = _icons

    private val _showConfirmDialog = MutableStateFlow<AppIcon?>(null)
    override val showConfirmDialog: StateFlow<AppIcon?> = _showConfirmDialog

    init {
        viewModelScope.launch {
            _icons.value = appIconManager.appIcons
        }
    }

    override fun selectIcon(appIcon: AppIcon) {
        // Show confirmation dialog
        _showConfirmDialog.value = appIcon
    }

    override fun confirmIconChange() {
        val appIcon = _showConfirmDialog.value ?: return
        viewModelScope.launch {
            appIconManager.setEnable(appIcon.name)
            _icons.value = appIconManager.appIcons
            _showConfirmDialog.value = null
        }
    }

    override fun dismissDialog() {
        _showConfirmDialog.value = null
    }
}