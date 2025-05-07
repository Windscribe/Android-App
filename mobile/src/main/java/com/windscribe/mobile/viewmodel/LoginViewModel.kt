package com.windscribe.mobile.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import com.windscribe.mobile.R
import com.windscribe.vpn.api.IApiCallManager
import com.windscribe.vpn.api.response.AuthToken
import com.windscribe.vpn.apppreference.PreferencesHelper
import com.windscribe.vpn.commonutils.Ext.toResult
import com.windscribe.vpn.commonutils.WindUtilities
import com.windscribe.vpn.constants.NetworkErrorCodes
import com.windscribe.vpn.errormodel.SessionErrorHandler
import com.windscribe.vpn.exceptions.ApiFailure
import com.windscribe.vpn.exceptions.WSNetException
import com.windscribe.vpn.repository.UserDataState
import com.windscribe.vpn.repository.UserRepository
import com.windscribe.vpn.services.FirebaseManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.slf4j.LoggerFactory
import javax.inject.Inject

sealed class LoginState {
    object Idle : LoginState()
    data class LoggingIn(val message: String) : LoginState()
    data class Captcha(val request: CaptchaRequest) : LoginState()
    object Success : LoginState()
    data class Error(val errorType: AuthError) : LoginState()
}

data class CaptchaRequest(
    val background: String,
    val top: Int,
    val slider: String,
    val secureToken: String
)

data class CaptchaSolution(
    val leftOffset: Float,
    val trail: Map<String, List<Float>>,
    val token: String
)

class LoginViewModel @Inject constructor(
    private val apiCallManager: IApiCallManager,
    private val preferenceHelper: PreferencesHelper,
    private val firebaseManager: FirebaseManager,
    private val userRepository: UserRepository
) : ViewModel() {

    private val _loginState = MutableStateFlow<LoginState>(LoginState.Idle)
    val loginState: StateFlow<LoginState> = _loginState.asStateFlow()

    private val _loginButtonEnabled = MutableStateFlow(false)
    val loginButtonEnabled: StateFlow<Boolean> = _loginButtonEnabled.asStateFlow()

    private val _twoFactorEnabled = MutableStateFlow(false)
    val twoFactorEnabled: StateFlow<Boolean> = _twoFactorEnabled.asStateFlow()

    private val _showAllBackupFailedDialog = MutableSharedFlow<Boolean>()
    val showAllBackupFailedDialog: SharedFlow<Boolean> = _showAllBackupFailedDialog

    private var username = ""
    private var password = ""
    private var twoFactorCode = ""
    private val logger = LoggerFactory.getLogger("LoginScreen")

    fun onUsernameChanged(username: String) {
        this.username = username
        validateInput()
    }

    fun onPasswordChanged(password: String) {
        this.password = password
        validateInput()
    }

    fun onTwoFactorChanged(twoFactorCode: String) {
        this.twoFactorCode = twoFactorCode
    }

    fun onCaptchaSolutionReceived(solution: CaptchaSolution) {
        viewModelScope.launch(Dispatchers.IO) {
            updateState(LoginState.LoggingIn("Logging in..."))
            _loginButtonEnabled.emit(false)
            loginWithCaptcha(solution)
        }
    }

    fun onTwoFactorHintClicked() = toggleTwoFactor()

    fun loginButtonClick() {
        if (_loginState.value is LoginState.LoggingIn) return

        viewModelScope.launch {
            updateState(LoginState.LoggingIn("Logging in..."))
            _loginButtonEnabled.emit(false)

            if (!WindUtilities.isOnline()) {
                logger.info("User is not connected to the internet.")
                _loginButtonEnabled.emit(true)
                updateState(LoginState.Error(AuthError.LocalizedInputError(R.string.no_internet)))
                return@launch
            }

            startLoginProcess()
        }
    }

    private fun toggleTwoFactor() {
        viewModelScope.launch { _twoFactorEnabled.emit(!_twoFactorEnabled.value) }
    }

    private fun isValidUsername() = username.length >= 2
    private fun isValidPassword() = password.length >= 2

    private fun validateInput() {
        viewModelScope.launch {
            updateState(LoginState.Idle)
            _loginButtonEnabled.emit(isValidUsername() && isValidPassword())
        }
    }

    private fun saveCaptcha(captchaRequest: CaptchaRequest) {
        viewModelScope.launch(Dispatchers.IO) {
            val gson = Gson()
            val data = gson.toJson(captchaRequest)
            preferenceHelper.saveResponseStringData("captcha", data)
        }
    }

    private suspend fun loginWithCaptcha(captchaSolution: CaptchaSolution) {
        val trailX = captchaSolution.trail["x"]?.toFloatArray() ?: floatArrayOf()
        val trailY = captchaSolution.trail["y"]?.toFloatArray() ?: floatArrayOf()
        val result = apiCallManager.logUserIn(
            username,
            password,
            twoFactorCode,
            captchaSolution.token,
            "${captchaSolution.leftOffset}",
            trailX,
            trailY
        ).toResult()
        result.onFailure {
            if (it is WSNetException) {
                handleNetworkError(it.getType())
            }
        }
        result.onSuccess {
            if (it.dataClass != null) {
                logger.info("User signup in successfully.")
                handleSuccessfulLogin(it.dataClass!!.sessionAuthHash)
            }
            if (it.errorClass != null) {
                _loginButtonEnabled.emit(true)
                handleApiError(it.errorClass!!.errorCode, it.errorClass!!.errorMessage)
            }
        }
    }

    private suspend fun startLoginProcess() {
        logger.info("Trying to log in with provided credentials...")
        val authResult = apiCallManager.authTokenLogin().toResult()
        authResult.onSuccess {
            if (it.errorClass != null) {
                logger.info("Error login: ${it.errorClass!!.errorMessage}")
                _loginButtonEnabled.emit(true)
                handleApiError(it.errorClass!!.errorCode, it.errorClass!!.errorMessage)
            }
            if (it.dataClass != null) {
                logger.info("Received auth token: ${it.dataClass!!.token}")
                handleAuthToken(it.dataClass!!)
            }
        }
        authResult.onFailure {
            if (it is WSNetException) {
                handleNetworkError(it.getType())
            }
        }
    }

    private suspend fun handleAuthToken(authToken: AuthToken) {
        val captcha = authToken.captcha
        val token = authToken.token
        if (captcha != null) {
            logger.info("Received captcha: ${captcha.top}")
            val request = CaptchaRequest(
                captcha.background,
                captcha.top,
                captcha.slider,
                authToken.token
            )
            saveCaptcha(request)
            updateState(LoginState.Captcha(request))
        } else {
            val result =
                apiCallManager.logUserIn(
                    username,
                    password,
                    twoFactorCode,
                    token,
                    null,
                    floatArrayOf(),
                    floatArrayOf()
                )
                    .toResult()
            result.onFailure {
                if (it is WSNetException) {
                    handleNetworkError(it.getType())
                }
            }
            result.onSuccess {
                if (it.dataClass != null) {
                    logger.info("User signup in successfully.")
                    handleSuccessfulLogin(it.dataClass!!.sessionAuthHash)
                }
                if (it.errorClass != null) {
                    logger.info("Error login: ${it.errorClass!!.errorMessage}")
                    _loginButtonEnabled.emit(true)
                    handleApiError(it.errorClass!!.errorCode, it.errorClass!!.errorMessage)
                }
            }
        }
    }

    private fun handleSuccessfulLogin(sessionAuthHash: String) {
        preferenceHelper.sessionHash = sessionAuthHash
        firebaseManager.getFirebaseToken { firebaseToken ->
            viewModelScope.launch(Dispatchers.IO) {
                userRepository.prepareDashboard(firebaseToken).collect {
                    when (it) {
                        is UserDataState.Error -> {
                            updateState(
                                LoginState.Error(
                                    AuthError.InputError(it.error)
                                )
                            )
                        }

                        is UserDataState.Loading -> {
                            updateState(LoginState.LoggingIn(it.status))
                        }

                        is UserDataState.Success -> {
                            updateState(LoginState.Success)
                        }
                    }
                }
            }
        }
    }


    private fun handleNetworkError(failure: ApiFailure) {
        when (failure) {
            ApiFailure.AllFallbackFailed -> {
                updateState(LoginState.Idle)
                viewModelScope.launch {
                    _showAllBackupFailedDialog.emit(true)
                }
            }

            ApiFailure.IncorrectJsonError -> {
                updateState(LoginState.Error(AuthError.InputError("Incorrect JSON response received from server.")))
            }

            ApiFailure.Network -> {
                updateState(LoginState.Error(AuthError.InputError("Network error, unable to connect to server.")))
            }

            ApiFailure.NoNetwork -> {
                updateState(LoginState.Error(AuthError.LocalizedInputError(R.string.no_internet)))
            }
        }
    }

    private suspend fun handleApiError(errorCode: Int, error: String) {
        logger.debug("Error code: $errorCode, Error: $error")
        val errorMessage = SessionErrorHandler.instance.getErrorMessage(errorCode, error)

        when (errorCode) {
            NetworkErrorCodes.ERROR_2FA_REQUIRED, NetworkErrorCodes.ERROR_INVALID_2FA -> {
                _twoFactorEnabled.emit(true)
                updateState(
                    LoginState.Error(
                        AuthError.InputError(
                            errorMessage,
                            listOf(AuthInputFields.TwoFactor)
                        )
                    )
                )
            }

            else -> updateState(
                LoginState.Error(
                    AuthError.InputError(
                        errorMessage,
                        listOf(AuthInputFields.Username, AuthInputFields.Password)
                    )
                )
            )
        }
    }

    private fun updateState(state: LoginState) {
        viewModelScope.launch {
            _loginState.emit(state)
        }
    }
}

