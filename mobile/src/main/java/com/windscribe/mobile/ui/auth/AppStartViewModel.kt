package com.windscribe.mobile.ui.auth

import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.windscribe.vpn.api.IApiCallManager
import com.windscribe.vpn.api.response.SsoResponse
import com.windscribe.vpn.apppreference.PreferencesHelper
import com.windscribe.vpn.backend.VPNState.Status.Connected
import com.windscribe.vpn.commonutils.Ext.result
import com.windscribe.vpn.repository.CallResult
import com.windscribe.vpn.repository.UserDataState
import com.windscribe.vpn.repository.UserRepository
import com.windscribe.vpn.services.FirebaseManager
import com.windscribe.vpn.services.sso.GoogleSignInManager
import com.windscribe.vpn.state.VPNConnectionStateManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.slf4j.LoggerFactory
import javax.inject.Inject


sealed class SsoLoginState {
    object Idle : SsoLoginState()
    data class LoggingIn(val message: String) : SsoLoginState()
    object Success : SsoLoginState()
    data class Error(val error: String) : SsoLoginState()
}

abstract class AppStartViewModel : ViewModel() {
    abstract val loginState: StateFlow<SsoLoginState>
    abstract fun onSignIntentResult(data: Intent?)
    abstract val isConnected: StateFlow<Boolean>
    abstract val loggedIn: Boolean
    abstract val signInIntent: Intent?
    abstract fun clearLoginState()
    abstract fun onSignIntentLaunch()
}

class AppStartViewModelImpl @Inject constructor(
    private val preferencesHelper: PreferencesHelper,
    private val api: IApiCallManager,
    private val vpnConnectionStateManager: VPNConnectionStateManager,
    private val googleSignInManager: GoogleSignInManager,
    private val firebaseManager: FirebaseManager,
    private val userRepository: UserRepository,
) :
    AppStartViewModel() {
    private val _isConnected = MutableStateFlow(false)
    override val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()
    override val loggedIn = preferencesHelper.sessionHash != null
    override val signInIntent = googleSignInManager.getSignInIntent()
    private val logger = LoggerFactory.getLogger("AppStartViewModel")
    private val _loginState = MutableStateFlow<SsoLoginState>(SsoLoginState.Idle)
    override val loginState: StateFlow<SsoLoginState> = _loginState

    init {
        observeConnectionState()
    }

    override fun onSignIntentResult(data: Intent?) {
        _loginState.value = SsoLoginState.LoggingIn("Logging in")
        if (data == null) {
            logger.error("Received empty sign in intent.")
            updateState(SsoLoginState.Error("Sso login failed."))
            return
        }
        viewModelScope.launch {
            googleSignInManager.getToken(data) { token, error ->
                if (token != null) {
                    logger.debug("Received sso token: $token")
                    ssoLogin(token = token)
                } else if (error != null) {
                    logger.debug("Failed to get sso token from google: $error")
                    logger.debug(error)
                    updateState(SsoLoginState.Error(error))
                } else {
                    logger.error("Failed to get sso token from google.")
                    updateState(SsoLoginState.Error("Failed to get token."))
                }
            }
        }
    }

    private fun ssoLogin(token: String) {
        viewModelScope.launch {
            when (val result = api.sso("google", token).result<SsoResponse>()) {
                is CallResult.Error -> {
                    logger.error("Sso login failed with error: ${result.errorMessage}")
                    updateState(SsoLoginState.Error(result.errorMessage))
                }

                is CallResult.Success -> {
                    logger.info("Sso login successful: ${result.data}")
                    handleSuccessfulSsoLogin(result.data.sessionAuth)
                }
            }
        }
    }

    private fun handleSuccessfulSsoLogin(sessionAuthHash: String) {
        preferencesHelper.sessionHash = sessionAuthHash
        firebaseManager.getFirebaseToken { firebaseToken ->
            viewModelScope.launch(Dispatchers.IO) {
                userRepository.prepareDashboard(firebaseToken).collect {
                    when (it) {
                        is UserDataState.Error -> {
                            logger.error("Prepare dashboard failed with error: ${it.error}")
                            updateState(SsoLoginState.Error(it.error))
                        }

                        is UserDataState.Loading -> {
                            updateState(SsoLoginState.LoggingIn(it.status))
                        }

                        is UserDataState.Success -> {
                            logger.info("Prepare dashboard successful.")
                            updateState(SsoLoginState.Success)
                        }
                    }
                }
            }
        }
    }

    private fun updateState(state: SsoLoginState) {
        viewModelScope.launch {
            _loginState.emit(state)
        }
    }

    override fun clearLoginState() {
        viewModelScope.launch {
            _loginState.emit(SsoLoginState.Idle)
        }
    }

    private fun observeConnectionState() {
        viewModelScope.launch {
            vpnConnectionStateManager.state.collectLatest {
                when (it.status) {
                    Connected -> {
                        _isConnected.emit(true)
                    }

                    else -> {
                        _isConnected.emit(false)
                    }
                }
            }
        }
    }

    override fun onSignIntentLaunch() {
        viewModelScope.launch {
            _loginState.emit(SsoLoginState.LoggingIn("Logging in"))
        }
    }
}