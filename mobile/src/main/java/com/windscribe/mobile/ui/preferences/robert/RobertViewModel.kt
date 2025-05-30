package com.windscribe.mobile.ui.preferences.robert

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.windscribe.mobile.ui.preferences.account.AccountViewModel
import com.windscribe.mobile.ui.preferences.main.MainMenuViewModel
import com.windscribe.vpn.repository.UserRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

abstract class RobertViewModel : ViewModel() {
    abstract val showProgress: StateFlow<Boolean>
}

class RobertViewModelImpl(val userRepository: UserRepository) : RobertViewModel() {
    private val _showProgress = MutableStateFlow(false)
    override val showProgress: StateFlow<Boolean> = _showProgress
}