/*
 * Copyright (c) 2021 Windscribe Limited.
 */
package com.windscribe.tv.welcome

import android.text.TextUtils
import android.util.Patterns
import com.windscribe.vpn.api.IApiCallManager
import com.windscribe.vpn.api.response.ApiErrorResponse
import com.windscribe.vpn.api.response.AuthToken
import com.windscribe.vpn.api.response.ClaimAccountResponse
import com.windscribe.vpn.api.response.UserLoginResponse
import com.windscribe.vpn.api.response.UserRegistrationResponse
import com.windscribe.vpn.api.response.UserSessionResponse
import com.windscribe.vpn.api.response.XPressLoginCodeResponse
import com.windscribe.vpn.api.response.XPressLoginVerifyResponse
import com.windscribe.vpn.apppreference.PreferencesHelper
import com.windscribe.vpn.commonutils.CommonPasswordChecker
import com.windscribe.vpn.commonutils.Ext.result
import com.windscribe.vpn.repository.LogRepository
import com.windscribe.vpn.commonutils.ResourceHelper
import com.windscribe.vpn.constants.NetworkErrorCodes
import com.windscribe.vpn.constants.UserStatusConstants
import com.windscribe.vpn.errormodel.SessionErrorHandler
import com.windscribe.vpn.repository.CallResult
import com.windscribe.vpn.repository.ConnectionDataRepository
import com.windscribe.vpn.repository.ServerListRepository
import com.windscribe.vpn.repository.StaticIpRepository
import com.windscribe.vpn.repository.UserRepository
import com.windscribe.vpn.services.FirebaseManager
import com.windscribe.vpn.state.PreferenceChangeObserver
import com.windscribe.vpn.workers.WindScribeWorkManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import org.slf4j.LoggerFactory
import java.io.File
import java.util.Date
import java.util.UUID
import java.util.concurrent.TimeUnit
import java.util.regex.Pattern
import javax.inject.Inject

class WelcomePresenterImpl @Inject constructor(
    private val welcomeView: WelcomeView,
    private val activityScope: CoroutineScope,
    private val preferencesHelper: PreferencesHelper,
    private val apiCallManager: IApiCallManager,
    private val firebaseManager: FirebaseManager,
    private val userRepository: UserRepository,
    private val staticIpRepository: StaticIpRepository,
    private val connectionDataRepository: ConnectionDataRepository,
    private val serverListRepository: ServerListRepository,
    private val preferenceChangeObserver: PreferenceChangeObserver,
    private val workManager: WindScribeWorkManager,
    private val resourceHelper: ResourceHelper,
    private val logRepository: LogRepository,
) : WelcomePresenter {
    private var isRegistration = false
    private var xpressVerificationJob: Job? = null
    private val logger = LoggerFactory.getLogger("basic")

    override fun onDestroy() {
        xpressVerificationJob?.cancel()
    }

    override fun exportLog() {
        val src = File(logRepository.getDebugFilePath())
        welcomeView.launchShareIntent(src)
    }

    override fun onBackPressed() {
        xpressVerificationJob?.cancel()
        welcomeView.hideSoftKeyboard()
    }

    override fun onGenerateCodeClick() {
        logger.debug("user clicked on generate code button.")
        welcomeView.prepareUiForApiCallStart()
        activityScope.launch(Dispatchers.IO) {
            val result = result<XPressLoginCodeResponse> { apiCallManager.generateXPressLoginCode() }
            withContext(Dispatchers.Main) {
                when (result) {
                    is CallResult.Success -> {
                        welcomeView.prepareUiForApiCallFinished()
                        logger.debug("Successfully generated XPress login code.")
                        welcomeView.setSecretCode(result.data.xPressLoginCode)
                        startXPressLoginCodeVerifier(result.data)
                    }
                    is CallResult.Error -> {
                        if (NetworkErrorCodes.ERROR_UNABLE_TO_REACH_API == result.code) {
                            welcomeView.showError("Unable to generate Login code. Check you network connection.")
                        } else {
                            logger.error("Generate login code: {}", result.errorMessage)
                            welcomeView.showError(result.errorMessage)
                        }
                    }
                }
            }
        }
    }

    override fun startAccountClaim(
        username: String,
        password: String,
        email: String?,
        ignoreEmptyEmail: Boolean
    ) {
        isRegistration = false
        welcomeView.hideSoftKeyboard()
        if (validateLoginInputs(username, password, email, false)) {
            if (!ignoreEmptyEmail && email?.isEmpty() == true) {
                welcomeView.showNoEmailAttentionFragment()
                return
            }
            logger.info("Trying to claim account with provided credentials...")
            welcomeView.prepareUiForApiCallStart()
            activityScope.launch(Dispatchers.IO) {
                withContext(Dispatchers.Main) {
                    welcomeView.updateCurrentProcess("Signing up")
                }
                val result = result<ClaimAccountResponse> {
                    apiCallManager.claimAccount(username, password, email ?: "", "")
                }
                withContext(Dispatchers.Main) {
                    when (result) {
                        is CallResult.Success -> {
                            logger.info("Account claimed successfully...")
                            welcomeView.updateCurrentProcess("SignUp successful...")
                            onAccountClaimSuccess()
                        }
                        is CallResult.Error -> {
                            if (NetworkErrorCodes.ERROR_UNABLE_TO_REACH_API == result.code) {
                                onLoginFailed()
                            } else {
                                logger.error("Claim account: {}", result.errorMessage)
                                val apiErrorResponse = ApiErrorResponse()
                                apiErrorResponse.errorCode = result.code
                                apiErrorResponse.errorMessage = result.errorMessage
                                onLoginResponseError(apiErrorResponse, username, password)
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onAuthLoginClick(username: String, password: String) {
        isRegistration = false
        welcomeView.hideSoftKeyboard()
        if (validateLoginInputs(username, password, "", true)) {
            logger.info("Requesting auth login token.")
            welcomeView.prepareUiForApiCallStart()
            activityScope.launch(Dispatchers.IO) {
                withContext(Dispatchers.Main) {
                    welcomeView.updateCurrentProcess("Signing in...")
                }
                val result = result<AuthToken> {
                    apiCallManager.authTokenLogin(true)
                }
                withContext(Dispatchers.Main) {
                    when (result) {
                        is CallResult.Success -> {
                            if (result.data.captcha != null) {
                                val captchaArt = result.data.captcha!!.asciiArt!!
                                logger.info("Captcha received: $captchaArt")
                                welcomeView.prepareUiForApiCallFinished()
                                welcomeView.captchaReceived(
                                    username,
                                    password,
                                    result.data.token,
                                    captchaArt,
                                    null,
                                    false
                                )
                            } else {
                                logger.info("Starting login")
                                startLoginProcess(username, password, null, result.data.token, null)
                            }
                        }
                        is CallResult.Error -> {
                            if (NetworkErrorCodes.ERROR_UNABLE_TO_REACH_API == result.code) {
                                onLoginFailed()
                            } else {
                                logger.error("Auth token: {}", result.errorMessage)
                                val apiErrorResponse = ApiErrorResponse()
                                apiErrorResponse.errorCode = result.code
                                apiErrorResponse.errorMessage = result.errorMessage
                                onLoginResponseError(apiErrorResponse, username, password)
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onAuthSignUpClick(username: String, password: String, email: String?) {
        isRegistration = true
        welcomeView.hideSoftKeyboard()
        if (validateLoginInputs(username, password, email ?: "", false)) {
            logger.info("Requesting auth signup token.")
            welcomeView.prepareUiForApiCallStart()
            activityScope.launch(Dispatchers.IO) {
                withContext(Dispatchers.Main) {
                    welcomeView.updateCurrentProcess("Preparing signup...")
                }
                val result = result<AuthToken> {
                    apiCallManager.authTokenSignup(true)
                }
                withContext(Dispatchers.Main) {
                    when (result) {
                        is CallResult.Success -> {
                            if (result.data.captcha != null) {
                                val captchaArt = result.data.captcha!!.asciiArt!!
                                logger.info("Signup captcha received: $captchaArt")
                                welcomeView.prepareUiForApiCallFinished()
                                welcomeView.captchaReceived(
                                    username,
                                    password,
                                    result.data.token,
                                    captchaArt,
                                    email,
                                    true
                                )
                            } else {
                                logger.info("Starting signup without captcha")
                                startSignUpProcess(
                                    username,
                                    password,
                                    email,
                                    true,
                                    result.data.token,
                                    null
                                )
                            }
                        }
                        is CallResult.Error -> {
                            if (NetworkErrorCodes.ERROR_UNABLE_TO_REACH_API == result.code) {
                                onLoginFailed()
                            } else {
                                logger.error("Auth signup token: {}", result.errorMessage)
                                val apiErrorResponse = ApiErrorResponse()
                                apiErrorResponse.errorCode = result.code
                                apiErrorResponse.errorMessage = result.errorMessage
                                onLoginResponseError(apiErrorResponse, username, password)
                            }
                        }
                    }
                }
            }
        }
    }

    override fun startLoginProcess(
        username: String,
        password: String,
        twoFa: String?,
        secureToken: String?,
        captcha: String?
    ) {
        isRegistration = false
        welcomeView.hideSoftKeyboard()
        logger.info("Trying to login with provided credentials...")
        welcomeView.prepareUiForApiCallStart()
        activityScope.launch(Dispatchers.IO) {
            withContext(Dispatchers.Main) {
                welcomeView.updateCurrentProcess("Signing in...")
            }
            val result = result<UserLoginResponse> {
                apiCallManager.logUserIn(
                    username,
                    password,
                    twoFa,
                    secureToken,
                    captcha,
                    floatArrayOf(),
                    floatArrayOf()
                )
            }
            withContext(Dispatchers.Main) {
                when (result) {
                    is CallResult.Success -> {
                        logger.info("Logged user in successfully...")
                        welcomeView.updateCurrentProcess("Login successful...")
                        preferencesHelper.sessionHash = result.data.sessionAuthHash
                        firebaseManager.getFirebaseToken { token ->
                            prepareLoginRegistrationDashboard(token)
                        }
                    }
                    is CallResult.Error -> {
                        if (NetworkErrorCodes.ERROR_UNABLE_TO_REACH_API == result.code) {
                            onLoginFailed()
                        } else {
                            logger.error("Login: {}", result.errorMessage)
                            val apiErrorResponse = ApiErrorResponse()
                            apiErrorResponse.errorCode = result.code
                            apiErrorResponse.errorMessage = result.errorMessage
                            onLoginResponseError(apiErrorResponse, username, password)
                        }
                    }
                }
            }
        }
    }

    override fun startSignUpProcess(
        username: String,
        password: String,
        email: String?,
        ignoreEmptyEmail: Boolean,
        secureToken: String?,
        captcha: String?
    ) {
        isRegistration = true
        welcomeView.hideSoftKeyboard()
        if (validateLoginInputs(username, password, email, false)) {
            if (!ignoreEmptyEmail && email?.isEmpty() == true) {
                welcomeView.showNoEmailAttentionFragment()
                return
            }
            logger.info("Trying to sign up with provided credentials...")
            welcomeView.prepareUiForApiCallStart()
            activityScope.launch(Dispatchers.IO) {
                withContext(Dispatchers.Main) {
                    welcomeView.updateCurrentProcess("Signing up")
                }
                val result = result<UserRegistrationResponse> {
                    apiCallManager.signUserIn(
                        username,
                        password,
                        null,
                        email,
                        "",
                        secureToken,
                        captcha,
                        floatArrayOf(),
                        floatArrayOf()
                    )
                }
                withContext(Dispatchers.Main) {
                    when (result) {
                        is CallResult.Success -> {
                            logger.info("Sign up user successfully...")
                            welcomeView.updateCurrentProcess("SignUp successful...")
                            preferencesHelper.sessionHash = result.data.sessionAuthHash
                            firebaseManager.getFirebaseToken { token ->
                                prepareLoginRegistrationDashboard(token)
                            }
                        }
                        is CallResult.Error -> {
                            if (NetworkErrorCodes.ERROR_UNABLE_TO_REACH_API == result.code) {
                                onLoginFailed()
                            } else {
                                logger.error("Signup: {}", result.errorMessage)
                                val apiErrorResponse = ApiErrorResponse()
                                apiErrorResponse.errorCode = result.code
                                apiErrorResponse.errorMessage = result.errorMessage
                                onLoginResponseError(apiErrorResponse, username, password)
                            }
                        }
                    }
                }
            }
        }
    }

    fun startXPressLoginCodeVerifier(xPressLoginCodeResponse: XPressLoginCodeResponse) {
        val startTime = System.currentTimeMillis()
        xpressVerificationJob = activityScope.launch(Dispatchers.IO) {
            while (isActive) {
                val secondsPassed = TimeUnit.SECONDS.convert(
                    System.currentTimeMillis() - startTime,
                    TimeUnit.MILLISECONDS
                )
                if (secondsPassed > xPressLoginCodeResponse.ttl) {
                    logger.error("Failed to verify XPress login code in ttl. Giving up")
                    withContext(Dispatchers.Main) {
                        welcomeView.setSecretCode("")
                    }
                    break
                }
                val result = withTimeoutOrNull(20000) {
                    result<XPressLoginVerifyResponse> {
                        apiCallManager.verifyXPressLoginCode(
                            xPressLoginCodeResponse.xPressLoginCode,
                            xPressLoginCodeResponse.signature
                        )
                    }
                }

                withContext(Dispatchers.Main) {
                    when (result) {
                        is CallResult.Success -> {
                            logger.debug("Successfully verified XPress login code.")
                            preferencesHelper.sessionHash = result.data.sessionAuth
                            xpressVerificationJob?.cancel()
                            firebaseManager.getFirebaseToken { token ->
                                prepareLoginRegistrationDashboard(token)
                            }
                            return@withContext
                        }
                        is CallResult.Error -> {
                            logger.debug("XPress login code not verified yet: {}", result.errorMessage)
                            // Continue polling
                        }
                        null -> {
                            logger.debug("XPress login code verification timeout, retrying...")
                            // Continue polling
                        }
                    }
                }

                // Wait 3 seconds before next poll
                delay(3000)
            }
        }
    }

    private fun evaluatePassword(password: String): Boolean {
        val pattern = Regex("(?=.*[a-z])(?=.*[A-Z])(?=\\S+$).{8,}")
        return password.matches(pattern)
    }


    private fun onAccountClaimSuccess() {
        welcomeView.updateCurrentProcess("Getting session")
        activityScope.launch(Dispatchers.IO) {
            val result = result<UserSessionResponse> {
                apiCallManager.getSessionGeneric(null)
            }
            withContext(Dispatchers.Main) {
                when (result) {
                    is CallResult.Success -> {
                        userRepository.reload(result.data)
                        userRepository.user.value?.let { user ->
                            user.email?.let {
                                welcomeView.gotoHomeActivity()
                            } ?: kotlin.run {
                                welcomeView.gotoAddEmailActivity(user.isPro)
                            }
                        }
                    }
                    is CallResult.Error -> {
                        welcomeView.prepareUiForApiCallFinished()
                        welcomeView.showError("Unable to auto login. Log in using new credentials.")
                        logger.error("Account claim: {}", result.errorMessage)
                    }
                }
            }
        }
    }

    private fun onLoginFailed() {
        welcomeView.prepareUiForApiCallFinished()
        welcomeView.showFailedAlert()
    }

    private fun onLoginResponseError(
        apiErrorResponse: ApiErrorResponse,
        username: String,
        password: String
    ) {
        logger.debug(apiErrorResponse.toString())
        welcomeView.prepareUiForApiCallFinished()
        val errorMessage = SessionErrorHandler.instance.getErrorMessage(apiErrorResponse)
        when (apiErrorResponse.errorCode) {
            NetworkErrorCodes.ERROR_2FA_REQUIRED -> {
                welcomeView.setTwoFaRequired(username, password)
            }

            NetworkErrorCodes.ERROR_INVALID_2FA -> {
                welcomeView.setTwoFaError(errorMessage)
            }

            NetworkErrorCodes.ERROR_USER_NAME_ALREADY_TAKEN, NetworkErrorCodes.ERROR_USER_NAME_ALREADY_IN_USE -> {
                welcomeView.setUsernameError(errorMessage)
            }

            else -> {
                welcomeView.setLoginRegistrationError(errorMessage)
            }
        }
    }

    private fun prepareLoginRegistrationDashboard(firebaseToken: String?) {
        preferencesHelper.loginTime = Date()
        welcomeView.updateCurrentProcess(resourceHelper.getString(com.windscribe.vpn.R.string.getting_session))
        activityScope.launch(Dispatchers.IO) {
            try {
                // Get session
                val sessionResult = result<UserSessionResponse> {
                    apiCallManager.getSessionGeneric(firebaseToken)
                }
                when (sessionResult) {
                    is CallResult.Success -> {
                        if (preferencesHelper.deviceUuid == null) {
                            logger.debug("No device id is found for the current user, generating and saving UUID")
                            preferencesHelper.deviceUuid = UUID.randomUUID().toString()
                        }
                        userRepository.reload(sessionResult.data)
                    }
                    is CallResult.Error -> {
                        throw Exception("Failed to get session: ${sessionResult.errorMessage}")
                    }
                }

                // Update connection data
                withContext(Dispatchers.Main) {
                    welcomeView.updateCurrentProcess(
                        resourceHelper.getString(com.windscribe.vpn.R.string.getting_server_credentials)
                    )
                }
                connectionDataRepository.update()

                // Update server list
                withContext(Dispatchers.Main) {
                    welcomeView.updateCurrentProcess(
                        resourceHelper.getString(com.windscribe.vpn.R.string.getting_server_list)
                    )
                }
                serverListRepository.update()

                // Update static IPs
                updateStaticIps()

                // Post city server change and update user data
                preferenceChangeObserver.postCityServerChange()
                userRepository.reload()

                // Success - navigate to appropriate screen
                withContext(Dispatchers.Main) {
                    workManager.onAppStart()
                    workManager.onAppMovedToForeground()
                    workManager.updateNodeLatencies()
                    if (isRegistration) {
                        welcomeView.gotoAddEmailActivity(
                            preferencesHelper.userStatus == UserStatusConstants.USER_STATUS_PREMIUM
                        )
                    } else {
                        welcomeView.gotoHomeActivity()
                    }
                }
            } catch (throwable: Throwable) {
                logger.error("Prepare data: {}", throwable.message)
                try {
                    // Fallback: try to complete with basic setup
                    preferenceChangeObserver.postCityServerChange()
                    userRepository.reload()

                    withContext(Dispatchers.Main) {
                        workManager.onAppStart()
                        workManager.onAppMovedToForeground()
                        workManager.updateNodeLatencies()
                        if (isRegistration) {
                            welcomeView.gotoAddEmailActivity(
                                preferencesHelper.userStatus == UserStatusConstants.USER_STATUS_PREMIUM
                            )
                        } else {
                            welcomeView.gotoHomeActivity()
                        }
                    }
                } catch (e: Throwable) {
                    withContext(Dispatchers.Main) {
                        welcomeView.prepareUiForApiCallFinished()
                        logger.error("Prepare data fallback failed: {}", e.message)
                    }
                }
            }
        }
    }

    private suspend fun updateStaticIps() {
        val sipCount = userRepository.user.value?.sipCount ?: 0
        if (sipCount > 0) {
            staticIpRepository.updateFromApi()
        }
    }

    private fun validateLoginInputs(
        username: String,
        password: String,
        email: String?,
        isLogin: Boolean
    ): Boolean {
        logger.info("Validating login credentials for $username")
        welcomeView.clearInputErrors()

        // Empty username
        if (TextUtils.isEmpty(username)) {
            logger.info("[username] is empty, displaying toast to the user...")
            welcomeView.setUsernameError(resourceHelper.getString(com.windscribe.vpn.R.string.username_empty))
            welcomeView.showToast(resourceHelper.getString(com.windscribe.vpn.R.string.enter_username))
            return false
        }

        // Invalid username
        if (!isLogin && !validateUsernameCharacters(username)) {
            logger.info("[username] has invalid characters in , displaying toast to the user...")
            welcomeView.setUsernameError(resourceHelper.getString(com.windscribe.vpn.R.string.login_with_username))
            welcomeView.showToast(resourceHelper.getString(com.windscribe.vpn.R.string.login_with_username))
            return false
        }

        // Empty Password
        if (TextUtils.isEmpty(password)) {
            logger.info("[password] is empty, displaying toast to the user...")
            welcomeView.setPasswordError(resourceHelper.getString(com.windscribe.vpn.R.string.password_empty))
            welcomeView.showToast(resourceHelper.getString(com.windscribe.vpn.R.string.enter_password))
            return false
        }
        if (email?.isNotEmpty() == true && !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            logger.info("[Email] is invalid, displaying toast to the user...")
            welcomeView.showToast(resourceHelper.getString(com.windscribe.vpn.R.string.invalid_email_format))
            return false
        }
        if (email?.isNotEmpty() == true && !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            logger.info("[Email] is invalid, displaying toast to the user...")
            welcomeView.showToast(resourceHelper.getString(com.windscribe.vpn.R.string.invalid_email_format))
            return false
        }
        if (!isLogin && password.length < 8) {
            logger.info("[Password] is small, displaying toast to the user...")
            welcomeView.showToast(resourceHelper.getString(com.windscribe.vpn.R.string.small_password))
            return false
        }
        // Sign up and claim account password minimum strength enforce.
        if (!isLogin && !evaluatePassword(password)) {
            logger.info("[Password] is weak, displaying toast to the user...")
            welcomeView.showToast(resourceHelper.getString(com.windscribe.vpn.R.string.weak_password))
            return false
        }
        if (!isLogin && CommonPasswordChecker.isAMatch(password)) {
            logger.info("[Password] matches worst password list, displaying toast to the user...")
            welcomeView.setPasswordError(resourceHelper.getString(com.windscribe.vpn.R.string.common_password))
            welcomeView.showToast(resourceHelper.getString(com.windscribe.vpn.R.string.common_password))
            return false
        }
        return true
    }

    private fun validateUsernameCharacters(username: String): Boolean {
        return Pattern.matches("[a-zA-Z0-9_-]*", username)
    }
}
