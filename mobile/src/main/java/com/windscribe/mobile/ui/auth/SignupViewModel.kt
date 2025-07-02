package com.windscribe.mobile.ui.auth

import android.util.Patterns
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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

sealed class SignupState {
    object Idle : SignupState()
    data class Registering(val message: String) : SignupState()
    object Success : SignupState()
    data class Captcha(val request: CaptchaRequest) : SignupState()
    data class Error(val error: AuthError) : SignupState()
}

class SignupViewModel @Inject constructor(
    private val apiCallManager: IApiCallManager,
    private val preferenceHelper: PreferencesHelper,
    private val firebaseManager: FirebaseManager,
    private val userRepository: UserRepository
) : ViewModel() {

    private val _signupState = MutableStateFlow<SignupState>(SignupState.Idle)
    val signupState: StateFlow<SignupState> = _signupState.asStateFlow()

    private val _signupButtonEnabled = MutableStateFlow(false)
    val signupButtonEnabled: StateFlow<Boolean> = _signupButtonEnabled.asStateFlow()

    private val _showAllBackupFailedDialog = MutableSharedFlow<Boolean>()
    val showAllBackupFailedDialog: SharedFlow<Boolean> = _showAllBackupFailedDialog

    private var username = ""
    private var password = ""
    private var email = ""
    private var voucher = ""
    private var referralUsername = ""
    var isAccountClaim = false
    private val logger = LoggerFactory.getLogger("basic")

    fun onUsernameChanged(username: String) {
        this.username = username
        validateInput()
    }

    fun onPasswordChanged(password: String) {
        this.password = password
        validateInput()
    }

    fun onEmailChanged(email: String) {
        this.email = email
        validateInput()
    }

    fun onVoucherChanged(voucher: String) {
        this.voucher = voucher
        validateInput()
    }

    fun onReferralUsernameChanged(referralUsername: String) {
        this.referralUsername = referralUsername
        validateInput()
    }

    private fun isValidUsername() = username.length >= 2
    private fun isValidPassword() = password.length >= 2

    private fun validateInput() {
        viewModelScope.launch {
            _signupButtonEnabled.emit(isValidUsername() && isValidPassword())
            updateState(SignupState.Idle)
        }
    }

    fun signupButtonClick() {
        if (!WindUtilities.isOnline()) {
            logger.info("User is not connected to the internet.")
            viewModelScope.launch {
                _signupButtonEnabled.emit(true)
                updateState(
                    SignupState.Error(AuthError.LocalizedInputError(com.windscribe.vpn.R.string.no_internet))
                )
            }
            return
        }
        if (username.isEmpty()) {
            updateState(
                SignupState.Error(
                    AuthError.LocalizedInputError(
                        com.windscribe.vpn.R.string.username_empty,
                        listOf(AuthInputFields.Username)
                    )
                )
            )
            return
        }
        if (password.length < 8) {
            updateState(
                SignupState.Error(
                    AuthError.LocalizedInputError(
                        com.windscribe.vpn.R.string.password_too_short,
                        listOf(AuthInputFields.Password)
                    )
                )
            )
            return
        }
        val passwordRegex = Regex("^(?=.*[a-z])(?=.*[A-Z])[a-zA-Z0-9!@#\$%^&*()_+\\-={}\\[\\]:;\"'<>,.?/`~|\\\\]+$")
        if (!password.matches(passwordRegex)) {
            updateState(
                SignupState.Error(
                    AuthError.LocalizedInputError(
                        com.windscribe.vpn.R.string.password_requirement,
                        listOf(AuthInputFields.Password)
                    )
                )
            )
            return
        }
        if (email.isNotEmpty() && !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            updateState(
                SignupState.Error(
                    AuthError.LocalizedInputError(
                        com.windscribe.vpn.R.string.invalid_email_format,
                        listOf(AuthInputFields.Email)
                    )
                )
            )
            return
        }
        viewModelScope.launch {
            startSignupProcess()
        }
    }

    private suspend fun startSignupProcess() {
        logger.info("Trying to registering with provided credentials...")
        val authResult = apiCallManager.authTokenLogin(false).toResult()
        authResult.onSuccess {
            if (it.errorClass != null) {
                logger.info("Error login: ${it.errorClass!!.errorMessage}")
                _signupButtonEnabled.emit(true)
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

    fun onCaptchaSolutionReceived(solution: CaptchaSolution) {
        viewModelScope.launch(Dispatchers.IO) {
            updateState(SignupState.Registering("Signing up..."))
            _signupButtonEnabled.emit(false)
            loginWithCaptcha(solution)
        }
    }

    private suspend fun handleAuthToken(authToken: AuthToken) {
        val captcha = authToken.captcha
        val token = authToken.token
        if (captcha != null) {
            logger.info("Received captcha: ${captcha.top}")
            val request = CaptchaRequest(
                captcha.background!!,
                captcha.top!!,
                captcha.slider!!,
                authToken.token
            )
            updateState(SignupState.Captcha(request))
        } else {
            val result = apiCallManager.signUserIn(
                username.trim(),
                password.trim(),
                referralUsername.trim(),
                email.trim(),
                voucher.trim(),
                token,
                null,
                floatArrayOf(), floatArrayOf()
            ).toResult()
            result.onFailure {
                if (it is WSNetException) {
                    logger.error("SignupError: ${it.message}")
                    handleNetworkError(it.getType())
                }
            }
            result.onSuccess {
                if (it.dataClass != null) {
                    logger.info("User signup in successfully.")
                    handleSuccessfulSignup(it.dataClass!!.sessionAuthHash)
                }
                if (it.errorClass != null) {
                    _signupButtonEnabled.emit(true)
                    handleApiError(it.errorClass!!.errorCode, it.errorClass!!.errorMessage)
                }
            }
        }
    }

    private suspend fun loginWithCaptcha(captchaSolution: CaptchaSolution) {
        val trailX = captchaSolution.trail["x"]?.toFloatArray() ?: floatArrayOf()
        val trailY = captchaSolution.trail["y"]?.toFloatArray() ?: floatArrayOf()
        val result = apiCallManager.signUserIn(
            username.trim(),
            password.trim(),
            referralUsername.trim(),
            email.trim(),
            voucher.trim(),
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
                handleSuccessfulSignup(it.dataClass!!.sessionAuthHash)
            }
            if (it.errorClass != null) {
                _signupButtonEnabled.emit(true)
                handleApiError(it.errorClass!!.errorCode, it.errorClass!!.errorMessage)
            }
        }
    }


    private fun handleSuccessfulSignup(sessionAuthHash: String) {
        preferenceHelper.sessionHash = sessionAuthHash
        firebaseManager.getFirebaseToken { firebaseToken ->
            viewModelScope.launch(Dispatchers.IO) {
                userRepository.prepareDashboard(firebaseToken).collect {
                    when (it) {
                        is UserDataState.Error -> {
                            updateState(
                                SignupState.Error(
                                    AuthError.InputError(it.error)
                                )
                            )
                        }

                        is UserDataState.Loading -> {
                            updateState(SignupState.Registering(it.status))
                        }

                        is UserDataState.Success -> {
                            updateState(SignupState.Success)
                        }
                    }
                }
            }
        }
    }

    private fun handleNetworkError(failure: ApiFailure) {
        when (failure) {
            ApiFailure.AllFallbackFailed -> {
                updateState(SignupState.Idle)
                viewModelScope.launch {
                    _showAllBackupFailedDialog.emit(true)
                }
            }

            ApiFailure.IncorrectJsonError -> {
                updateState(SignupState.Error(AuthError.InputError("Incorrect JSON response received from server.")))
            }

            ApiFailure.Network -> {
                updateState(SignupState.Error(AuthError.InputError("Network error, unable to connect to server.")))
            }

            ApiFailure.NoNetwork -> {
                updateState(SignupState.Error(AuthError.LocalizedInputError(com.windscribe.vpn.R.string.no_internet)))
            }
        }
    }

    private fun handleApiError(errorCode: Int, error: String) {
        logger.error("SignupError: $errorCode-$error")
        val errorMessage = SessionErrorHandler.instance.getErrorMessage(errorCode, error)

        when (errorCode) {
            NetworkErrorCodes.ERROR_USER_NAME_ALREADY_TAKEN, NetworkErrorCodes.ERROR_USER_NAME_ALREADY_IN_USE -> {
                updateState(
                    SignupState.Error(
                        AuthError.InputError(
                            errorMessage,
                            listOf(AuthInputFields.Username)
                        )
                    )
                )
            }

            else -> updateState(
                SignupState.Error(
                    AuthError.InputError(
                        errorMessage,
                        listOf(AuthInputFields.Username, AuthInputFields.Password)
                    )
                )
            )
        }
    }

    fun dismissCaptcha() {
        updateState(SignupState.Idle)
    }

    private fun updateState(state: SignupState) {
        viewModelScope.launch {
            _signupState.emit(state)
        }
    }
}

