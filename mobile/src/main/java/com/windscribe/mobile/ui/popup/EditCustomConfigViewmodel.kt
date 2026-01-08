package com.windscribe.mobile.ui.popup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.windscribe.vpn.backend.utils.WindVpnController
import com.windscribe.vpn.commonutils.WindUtilities
import com.windscribe.vpn.localdatabase.LocalDbInterface
import com.windscribe.vpn.serverlist.entity.ConfigFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.slf4j.LoggerFactory

abstract class EditCustomConfigViewmodel : ViewModel() {
    abstract fun load(id: Int, connect: Boolean)
    abstract val name: StateFlow<String>
    abstract val username: StateFlow<String>
    abstract val password: StateFlow<String>
    abstract val isRemember: StateFlow<Boolean>
    abstract val isOpenVPN: StateFlow<Boolean>
    abstract val connect: StateFlow<Boolean>
    abstract fun onNameChange(name: String)
    abstract fun onUsernameChange(username: String)
    abstract fun onPasswordChange(password: String)
    abstract fun onToggleIsRemember()
    abstract fun onSaveClick()
    abstract val shouldExit: StateFlow<Boolean>
}

sealed class EditConfigState {
    object Loading : EditConfigState()
    data class Success(val file: ConfigFile) : EditConfigState()
    object Error : EditConfigState()
}

class EditCustomConfigViewmodelImpl(
    private val localDbInterface: LocalDbInterface,
    private val vpnController: WindVpnController
) :
    EditCustomConfigViewmodel() {
    private val _shouldExit = MutableStateFlow(false)
    override val shouldExit = _shouldExit.asStateFlow()
    private val _configState = MutableStateFlow<EditConfigState>(EditConfigState.Loading)
    private val configState: StateFlow<EditConfigState> = _configState
    private val _name = MutableStateFlow("")
    override val name: StateFlow<String> = _name
    private val _username = MutableStateFlow("")
    override val username: StateFlow<String> = _username
    private val _password = MutableStateFlow("")
    override val password: StateFlow<String> = _password
    private val _isRemember = MutableStateFlow(true)
    override val isRemember: StateFlow<Boolean> = _isRemember
    private val _isOpenVPN = MutableStateFlow(false)
    override val isOpenVPN: StateFlow<Boolean> = _isOpenVPN
    private val _connect = MutableStateFlow(false)
    override val connect: StateFlow<Boolean> = _connect
    private val logger = LoggerFactory.getLogger("basic")


    override fun load(id: Int, connect: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val config = localDbInterface.getConfigFileAsync(id)
                _configState.emit(EditConfigState.Success(config))
                _name.emit(config.name)
                _username.emit(config.username ?: "")
                _password.emit(config.password ?: "")
                _isRemember.emit(config.isRemember)
                _connect.emit(connect)
                val configType = WindUtilities.getConfigType(config.content)
                _isOpenVPN.emit(configType == WindUtilities.ConfigType.OpenVPN)
            } catch (e: Exception) {
                logger.error(e.toString())
                _configState.emit(EditConfigState.Error)
            }
        }
    }

    override fun onNameChange(name: String) {
        viewModelScope.launch {
            _name.emit(name)
        }
    }

    override fun onUsernameChange(username: String) {
        viewModelScope.launch {
            _username.emit(username)
        }
    }

    override fun onPasswordChange(password: String) {
        viewModelScope.launch {
            _password.emit(password)
        }
    }

    override fun onToggleIsRemember() {
        viewModelScope.launch {
            _isRemember.emit(isRemember.value.not())
        }
    }

    override fun onSaveClick() {
        viewModelScope.launch(Dispatchers.IO) {
            if (configState.value is EditConfigState.Success) {
                val config = (configState.value as EditConfigState.Success).file
                config.name = name.value
                config.username = username.value
                config.password = password.value
                config.isRemember = isRemember.value
                localDbInterface.addConfigSync(config)
                if (connect.value && !config.username.isNullOrEmpty() && !config.password.isNullOrEmpty()) {
                    vpnController.connectAsync()
                }
                _shouldExit.emit(true)
            }
        }
    }
}