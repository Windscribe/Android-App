package com.windscribe.mobile.ui.helper

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.windscribe.vpn.apppreference.PreferencesHelper
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable.invokeOnCompletion
import kotlinx.coroutines.launch
import net.grandcentrix.tray.core.OnTrayPreferenceChangeListener

fun PreferencesHelper.onChanged(
    viewModel: ViewModel,
    onChange: () -> Unit
): Job {
    val listener = OnTrayPreferenceChangeListener {
        onChange()
    }
    return viewModel.viewModelScope.launch {
        addObserver(listener)
        invokeOnCompletion {
            removeObserver(listener)
        }
    }
}