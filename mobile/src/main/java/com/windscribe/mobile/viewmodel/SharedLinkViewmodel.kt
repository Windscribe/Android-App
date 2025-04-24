package com.windscribe.mobile.viewmodel

import androidx.lifecycle.ViewModel
import com.windscribe.vpn.apppreference.PreferencesHelper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.slf4j.LoggerFactory

abstract class SharedLinkViewmodel : ViewModel() {
    abstract val shouldExit: StateFlow<Boolean>
}

class SharedLinkViewmodelImpl(private val preferenceHelper: PreferencesHelper) :
    SharedLinkViewmodel() {
    private val _shouldExit = MutableStateFlow(false)
    override val shouldExit = _shouldExit.asStateFlow()
    private val logger = LoggerFactory.getLogger("basic")
}