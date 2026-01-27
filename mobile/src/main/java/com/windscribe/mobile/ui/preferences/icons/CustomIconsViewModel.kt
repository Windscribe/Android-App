package com.windscribe.mobile.ui.preferences.icons

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

abstract class CustomIconsViewModel : ViewModel() {
    abstract val icons: StateFlow<Map<String, AppIcon>>
    abstract fun selectIcon(appIcon: AppIcon)
}

class CustomIconsViewModelImpl(val appIconManager: AppIconManager) : CustomIconsViewModel() {
    val _icons: MutableStateFlow<Map<String, AppIcon>> = MutableStateFlow(mapOf())
    override val icons: StateFlow<Map<String, AppIcon>> = _icons

    init {
        viewModelScope.launch {
            _icons.value = appIconManager.appIcons
        }
    }

    override fun selectIcon(appIcon: AppIcon) {
        viewModelScope.launch {
            appIconManager.setEnable(appIcon.name)
            _icons.value = appIconManager.appIcons
        }
    }
}