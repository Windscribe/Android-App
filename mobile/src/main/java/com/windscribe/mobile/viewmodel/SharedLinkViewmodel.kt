package com.windscribe.mobile.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.windscribe.vpn.repository.UserRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

abstract class SharedLinkViewmodel : ViewModel() {
    abstract fun exit()

    abstract val shouldExit: StateFlow<Boolean>
    abstract val userName: StateFlow<String>
}

class SharedLinkViewmodelImpl(private val userRepository: UserRepository) :
    SharedLinkViewmodel() {
    private val _shouldExit = MutableStateFlow(false)
    override val shouldExit = _shouldExit.asStateFlow()
    private val _userName = MutableStateFlow("")
    override val userName: StateFlow<String> = _userName

    init {
        fetchUserState()
    }

    private fun fetchUserState() {
        viewModelScope.launch {
            userRepository.userInfo.collect {
                _userName.emit(it.userName)
            }
        }
    }

    override fun exit() {
        viewModelScope.launch {
            _shouldExit.emit(true)
        }
    }
}