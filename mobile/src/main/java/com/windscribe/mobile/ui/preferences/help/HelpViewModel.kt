package com.windscribe.mobile.ui.preferences.help

import android.os.Build
import android.os.Build.VERSION
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.windscribe.mobile.ui.preferences.account.AccountViewModel
import com.windscribe.mobile.ui.preferences.main.MainMenuViewModel
import com.windscribe.vpn.R.string
import com.windscribe.vpn.Windscribe
import com.windscribe.vpn.Windscribe.Companion.appContext
import com.windscribe.vpn.api.IApiCallManager
import com.windscribe.vpn.backend.openvpn.ProxyTunnelManager
import com.windscribe.vpn.commonutils.Ext.toResult
import com.windscribe.vpn.commonutils.WindUtilities
import com.windscribe.vpn.constants.PreferencesKeyConstants
import com.windscribe.vpn.encoding.encoders.Base64
import com.windscribe.vpn.repository.AdvanceParameterRepository
import com.windscribe.vpn.repository.UserRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.io.BufferedReader
import java.io.File
import java.io.FileReader
import java.nio.charset.Charset

abstract class HelpViewModel : ViewModel() {
    abstract val showProgress: StateFlow<Boolean>
    abstract val sendLogState: StateFlow<SendLogState>
    abstract fun sendLogClicked()
}
sealed class SendLogState {
    object Idle : SendLogState()
    object Loading : SendLogState()
    object Success : SendLogState()
    object Failure : SendLogState()
}

class HelpViewModelImpl(
    val userRepository: UserRepository,
    val advanceParameterRepository: AdvanceParameterRepository,
    val api: IApiCallManager
) : HelpViewModel() {
    private val _showProgress = MutableStateFlow(false)
    override val showProgress: StateFlow<Boolean> = _showProgress
    private val _sendLogState = MutableStateFlow<SendLogState>(SendLogState.Idle)
    override val sendLogState: StateFlow<SendLogState> = _sendLogState

    override fun sendLogClicked() {
        viewModelScope.launch(Dispatchers.IO) {
            _sendLogState.emit(SendLogState.Loading)
            val username = userRepository.user.value?.userName
            if (username.isNullOrEmpty()) {
                _sendLogState.emit(SendLogState.Failure)
                return@launch
            }
            getEncodedLog().runCatching {
                api.postDebugLog(username, this).toResult().onSuccess {
                    _sendLogState.emit(SendLogState.Success)
                }.onFailure {
                    _sendLogState.emit(SendLogState.Failure)
                }
            }
        }
    }

    @Throws(Exception::class)
    private fun getEncodedLog(): String {
        var logLine: String?
        val debugFilePath = advanceParameterRepository.getDebugFilePath()
        val logFile = Windscribe.appContext.resources.getString(
            string.log_file_header,
            VERSION.SDK_INT, Build.BRAND, Build.DEVICE, Build.MODEL, Build.MANUFACTURER,
            VERSION.RELEASE, WindUtilities.getVersionCode()
        )
        val builder = StringBuilder()
        builder.append(logFile)
        val file = File(debugFilePath)
        val bufferedReader = BufferedReader(FileReader(file))
        while (bufferedReader.readLine().also { logLine = it } != null) {
            builder.append(logLine)
            builder.append("\n")
        }
        val wsTunnelLog = File(appContext.filesDir, ProxyTunnelManager.PROXY_LOG)
        if (wsTunnelLog.exists()) {
            wsTunnelLog.bufferedReader().use { builder.append(it.readText()) }
        }
        bufferedReader.close()
        return String(Base64.encode(builder.toString().toByteArray(Charset.defaultCharset())))
    }
}