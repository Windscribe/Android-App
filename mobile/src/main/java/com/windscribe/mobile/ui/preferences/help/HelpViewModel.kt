package com.windscribe.mobile.ui.preferences.help

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.windscribe.mobile.ui.preferences.account.AccountViewModel
import com.windscribe.mobile.ui.preferences.main.MainMenuViewModel
import com.windscribe.vpn.repository.UserRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

abstract class HelpViewModel : ViewModel() {
    abstract val showProgress: StateFlow<Boolean>
}

class HelpViewModelImpl(val userRepository: UserRepository) : HelpViewModel() {
    private val _showProgress = MutableStateFlow(false)
    override val showProgress: StateFlow<Boolean> = _showProgress
}