package com.windscribe.mobile.ui.preferences.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.windscribe.vpn.repository.UserRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

abstract class MainMenuViewModel : ViewModel() {
    abstract val showReferData: Boolean
    abstract fun logout()
    abstract val showProgress: StateFlow<Boolean>
}

class MainMenuViewModelImpl(val userRepository: UserRepository) : MainMenuViewModel() {
    override val showReferData: Boolean
        get() = userRepository.user.value?.isPro == false

    private val _showProgress = MutableStateFlow(false)
    override val showProgress: StateFlow<Boolean> = _showProgress
    override fun logout() {
        viewModelScope.launch {
            _showProgress.value = true
            userRepository.logout()
            _showProgress.value = false
        }
    }
}