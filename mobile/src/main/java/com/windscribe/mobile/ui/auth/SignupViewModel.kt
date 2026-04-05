package com.windscribe.mobile.ui.auth

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Patterns
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.windscribe.vpn.api.IApiCallManager
import com.windscribe.vpn.api.response.AuthToken
import com.windscribe.vpn.api.response.UserRegistrationResponse
import com.windscribe.vpn.apppreference.PreferencesHelper
import com.windscribe.vpn.commonutils.Ext.result
import com.windscribe.vpn.commonutils.HashUtils
import com.windscribe.vpn.commonutils.WindUtilities
import com.windscribe.vpn.constants.NetworkErrorCodes
import com.windscribe.vpn.errormodel.SessionErrorHandler
import com.windscribe.vpn.exceptions.ApiFailure
import com.windscribe.vpn.repository.CallResult
import com.windscribe.vpn.repository.UserDataState
import com.windscribe.vpn.repository.UserRepository
import com.windscribe.vpn.repository.getNetworkError
import com.windscribe.vpn.services.FirebaseManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.slf4j.LoggerFactory
import java.io.File
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

    private val _selectedAuthType = MutableStateFlow(AuthType.STANDARD)
    val selectedAuthType: StateFlow<AuthType> = _selectedAuthType.asStateFlow()

    private val _accountHash = MutableStateFlow("")
    val accountHash: StateFlow<String> = _accountHash.asStateFlow()

    private val _isBackupConfirmed = MutableStateFlow(false)
    val isBackupConfirmed: StateFlow<Boolean> = _isBackupConfirmed.asStateFlow()

    private val _generatedUsername = MutableStateFlow("")
    val generatedUsername: StateFlow<String> = _generatedUsername.asStateFlow()

    private val _generatedPassword = MutableStateFlow("")
    val generatedPassword: StateFlow<String> = _generatedPassword.asStateFlow()

    private val _hashFileBytes = MutableStateFlow<ByteArray?>(null)
    private val _triggerFilePicker = MutableSharedFlow<Boolean>(replay = 0)
    val triggerFilePicker: SharedFlow<Boolean> = _triggerFilePicker

    private val _toastMessage = MutableSharedFlow<String>(replay = 0)
    val toastMessage: SharedFlow<String> = _toastMessage

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

    fun onAuthTypeChanged(authType: AuthType) {
        viewModelScope.launch {
            _selectedAuthType.emit(authType)
            updateState(SignupState.Idle)
            _signupButtonEnabled.emit(false)
            if (authType == AuthType.HASHED) {
                generateAccountHash()
            } else {
                validateInput()
            }
        }
    }

    fun onBackupConfirmedChanged(confirmed: Boolean) {
        viewModelScope.launch {
            _isBackupConfirmed.emit(confirmed)
            validateHashedSignup()
        }
    }

    fun generateAccountHash() {
        viewModelScope.launch(Dispatchers.IO) {
            // Generate random bytes for the hash file
            val random = java.security.SecureRandom()
            val bytes = ByteArray(1024) // 1KB random file
            random.nextBytes(bytes)

            // Generate SHA256 hash from the bytes
            val hash = HashUtils.sha256FromInputStream(bytes.inputStream())

            // Store both the file bytes and hash
            _hashFileBytes.emit(bytes)
            _accountHash.emit(hash)
            validateHashedSignup()

            logger.info("Generated hash file and hash: ${hash.take(20)}...")
        }
    }

    fun onUploadHashClick() {
        viewModelScope.launch {
            _triggerFilePicker.emit(true)
        }
    }

    fun onFileSelected(context: Context, uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val inputStream = context.contentResolver.openInputStream(uri)
                if (inputStream != null) {
                    val bytes = inputStream.readBytes()
                    val hash = HashUtils.sha256FromInputStream(bytes.inputStream())

                    _hashFileBytes.emit(bytes)
                    _accountHash.emit(hash)

                    withContext(Dispatchers.Main) {
                        validateHashedSignup()
                    }
                    logger.info("Generated hash from uploaded file: ${hash.take(20)}...")
                } else {
                    logger.error("Failed to open input stream for file")
                }
            } catch (e: Exception) {
                logger.error("Error hashing file: ${e.message}")
            }
        }
    }

    fun onDownloadHashClick(context: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val bytes = _hashFileBytes.value
                if (bytes == null) {
                    logger.error("No hash file bytes available to download")
                    _toastMessage.emit("Error: No hash file to download")
                    return@launch
                }

                val fileName = "windscribe_account_${System.currentTimeMillis()}.key"

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    // Use MediaStore for Android 10+
                    val contentValues = ContentValues().apply {
                        put(MediaStore.Downloads.DISPLAY_NAME, fileName)
                        put(MediaStore.Downloads.MIME_TYPE, "application/octet-stream")
                        put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                    }

                    val uri = context.contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
                    if (uri != null) {
                        context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                            outputStream.write(bytes)
                        }
                        logger.info("Hash file saved: $fileName")
                        _toastMessage.emit("Account key saved to Downloads/$fileName")
                    } else {
                        logger.error("Failed to create file in Downloads")
                        _toastMessage.emit("Error: Failed to save file")
                    }
                } else {
                    // Use legacy file path for older Android
                    val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                    val file = File(downloadsDir, fileName)
                    file.writeBytes(bytes)
                    logger.info("Hash file saved: ${file.absolutePath}")
                    _toastMessage.emit("Account key saved to Downloads/$fileName")
                }
            } catch (e: Exception) {
                logger.error("Error saving hash file: ${e.message}")
                _toastMessage.emit("Error: ${e.message}")
            }
        }
    }

    fun generateUsername() {
        viewModelScope.launch {
            val result = result<com.windscribe.vpn.api.response.GenerateUsernameResponse> {
                apiCallManager.generateUsername()
            }
            when (result) {
                is CallResult.Success -> {
                    username = result.data.username
                    _generatedUsername.emit(result.data.username)
                    validateInput()
                    logger.info("Generated username: $username")
                }
                is CallResult.Error -> {
                    logger.error("Failed to generate username: ${result.errorMessage}")
                }
            }
        }
    }

    fun generatePassword() {
        viewModelScope.launch {
            val result = result<com.windscribe.vpn.api.response.GeneratePasswordResponse> {
                apiCallManager.generatePassword()
            }
            when (result) {
                is CallResult.Success -> {
                    password = result.data.password
                    _generatedPassword.emit(result.data.password)
                    validateInput()
                    logger.info("Generated password")
                }
                is CallResult.Error -> {
                    logger.error("Failed to generate password: ${result.errorMessage}")
                }
            }
        }
    }

    private fun validateHashedSignup() {
        viewModelScope.launch {
            _signupButtonEnabled.emit(_accountHash.value.isNotEmpty() && _isBackupConfirmed.value)
        }
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
        // For hashed signup, use the hash as both username and password
        if (_selectedAuthType.value == AuthType.HASHED) {
            username = _accountHash.value
            password = _accountHash.value
        }

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

        // Skip validation for hashed signup (hash is auto-generated and valid)
        if (_selectedAuthType.value == AuthType.HASHED) {
            viewModelScope.launch {
                startSignupProcess()
            }
            return
        }

        // Standard signup validation
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
        val passwordRegex =
            Regex("^(?=.*[a-z])(?=.*[A-Z])[a-zA-Z0-9!@#\$%^&*()_+\\-={}\\[\\]:;\"'<>,.?/`~|\\\\]+$")
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
        // Standard signup - proceed with validation passed
        viewModelScope.launch {
            startSignupProcess()
        }
    }

    private suspend fun startSignupProcess() {
        logger.info("Trying to registering with provided credentials...")
        val authResult = result<AuthToken> { apiCallManager.authTokenSignup(username, false) }
        when (authResult) {
            is CallResult.Error -> {
                val networkError = getNetworkError(authResult.code)
                if (networkError != null) {
                    handleNetworkError(networkError)
                } else {
                    logger.info("Error login: ${authResult.errorMessage}")
                    _signupButtonEnabled.emit(true)
                    handleApiError(authResult.code, authResult.errorMessage)
                }
            }

            is CallResult.Success -> {
                logger.info("Received auth token: ${authResult.data.token}")
                handleAuthToken(authResult.data)
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
            val result = result<UserRegistrationResponse> {
                apiCallManager.signUserIn(
                    username.trim(),
                    password.trim(),
                    referralUsername.trim(),
                    email.trim(),
                    voucher.trim(),
                    token,
                    null,
                    floatArrayOf(), floatArrayOf()
                )
            }

            when (result) {
                is CallResult.Error -> {
                    val networkError = getNetworkError(result.code)
                    if (networkError != null) {
                        logger.error("SignupError: ${result.errorMessage}")
                        handleNetworkError(networkError)
                    } else {
                        logger.info("Error signup: ${result.errorMessage}")
                        _signupButtonEnabled.emit(true)
                        handleApiError(result.code, result.errorMessage)
                    }
                }

                is CallResult.Success -> {
                    logger.info("User signup in successfully.")
                    handleSuccessfulSignup(result.data.sessionAuthHash)
                }
            }
        }
    }

    private suspend fun loginWithCaptcha(captchaSolution: CaptchaSolution) {
        val trailX = captchaSolution.trail["x"]?.toFloatArray() ?: floatArrayOf()
        val trailY = captchaSolution.trail["y"]?.toFloatArray() ?: floatArrayOf()
        val result = result<UserRegistrationResponse> {
            apiCallManager.signUserIn(
                username.trim(),
                password.trim(),
                referralUsername.trim(),
                email.trim(),
                voucher.trim(),
                captchaSolution.token,
                "${captchaSolution.leftOffset}",
                trailX,
                trailY
            )
        }
        when (result) {
            is CallResult.Error -> {
                val networkError = getNetworkError(result.code)
                if (networkError != null) {
                    handleNetworkError(networkError)
                } else {
                    _signupButtonEnabled.emit(true)
                    handleApiError(result.code, result.errorMessage)
                }
            }

            is CallResult.Success -> {
                logger.info("User signup in successfully.")
                handleSuccessfulSignup(result.data.sessionAuthHash)
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

    fun clearDialog() {
        viewModelScope.launch {
            _showAllBackupFailedDialog.emit(false)
        }
    }
}

