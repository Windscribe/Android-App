/*
 * Copyright (c) 2021 Windscribe Limited.
 */
package com.windscribe.tv.welcome

import android.text.TextUtils
import android.util.Patterns
import com.windscribe.tv.R
import com.windscribe.vpn.ActivityInteractor
import com.windscribe.vpn.api.response.*
import com.windscribe.vpn.commonutils.CommonPasswordChecker
import com.windscribe.vpn.constants.NetworkErrorCodes
import com.windscribe.vpn.constants.NetworkKeyConstants
import com.windscribe.vpn.constants.UserStatusConstants
import com.windscribe.vpn.errormodel.SessionErrorHandler
import com.windscribe.vpn.errormodel.WindError
import io.reactivex.Completable
import io.reactivex.Flowable
import io.reactivex.Single
import io.reactivex.SingleSource
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.functions.Function
import io.reactivex.observers.DisposableCompletableObserver
import io.reactivex.observers.DisposableSingleObserver
import io.reactivex.schedulers.Schedulers
import io.reactivex.subscribers.DisposableSubscriber
import org.slf4j.LoggerFactory
import java.io.File
import java.io.IOException
import java.util.*
import java.util.concurrent.TimeUnit
import java.util.regex.Pattern
import javax.inject.Inject

class WelcomePresenterImpl @Inject constructor(
    private val welcomeView: WelcomeView,
    private val interactor: ActivityInteractor
) : WelcomePresenter {
    private var isRegistration = false
    private val compositeDisposable = CompositeDisposable()
    private val logger = LoggerFactory.getLogger("basic")
    override fun onDestroy() {
        compositeDisposable.clear()
        interactor.getCompositeDisposable().clear()
    }

    override fun exportLog() {
        val src = File(interactor.getDebugFilePath())
        welcomeView.launchShareIntent(src)
    }

    override fun onBackPressed() {
        interactor.getCompositeDisposable().clear()
        welcomeView.hideSoftKeyboard()
    }

    override fun onGenerateCodeClick() {
        logger.debug("user clicked on generate code button.")
        welcomeView.prepareUiForApiCallStart()
        interactor.getCompositeDisposable()
            .add(
                interactor.getApiCallManager().generateXPressLoginCode()
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribeWith(
                        object :
                            DisposableSingleObserver<GenericResponseClass<XPressLoginCodeResponse?, ApiErrorResponse?>?>() {
                            override fun onError(e: Throwable) {
                                welcomeView.prepareUiForApiCallFinished()
                                logger.error("Generate login code: {}", e.message)
                                welcomeView.showError(
                                    "Unable to generate Login code. Check you network connection."
                                )
                            }

                            override fun onSuccess(
                                response: GenericResponseClass<XPressLoginCodeResponse?, ApiErrorResponse?>
                            ) {
                                welcomeView.prepareUiForApiCallFinished()
                                response.dataClass?.let {
                                    logger.debug("Successfully generated XPress login code.")
                                    welcomeView.setSecretCode(it.xPressLoginCode)
                                    startXPressLoginCodeVerifier(it)
                                } ?: response.errorClass?.let {
                                    logger.error("Generate login code: {}", it)
                                    welcomeView.showError(it.errorMessage)
                                } ?: kotlin.run {
                                    welcomeView.showError("Unable to generate Login code. Check you network connection.")
                                }
                            }
                        })
            )
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
            interactor.getCompositeDisposable().add(
                interactor.getApiCallManager()
                    .claimAccount(username, password, email ?: "", "")
                    .doOnSubscribe { welcomeView.updateCurrentProcess("Signing up") }
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribeWith(
                        object :
                            DisposableSingleObserver<GenericResponseClass<ClaimAccountResponse?, ApiErrorResponse?>?>() {
                            override fun onError(e: Throwable) {
                                logger.error("Claim account: {}", e.message)
                                onLoginFailed()
                            }

                            override fun onSuccess(
                                response: GenericResponseClass<ClaimAccountResponse?, ApiErrorResponse?>
                            ) {
                                response.dataClass?.let {
                                    logger.info("Account claimed successfully...")
                                    welcomeView.updateCurrentProcess("SignUp successful...")
                                    onAccountClaimSuccess()
                                } ?: response.errorClass?.let {
                                    logger.error("Claim account: {}", it)
                                    onLoginResponseError(it, username, password)
                                }
                            }
                        })
            )
        }
    }

    override fun onAuthLoginClick(username: String, password: String) {
        isRegistration = false
        welcomeView.hideSoftKeyboard()
        if (validateLoginInputs(username, password, "", true)) {
            logger.info("Requesting auth login token.")
            welcomeView.prepareUiForApiCallStart()
            interactor.getCompositeDisposable().add(
                interactor.getApiCallManager()
                    .authTokenLogin(true)
                    .doOnSubscribe { welcomeView.updateCurrentProcess("Signing in...") }
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribeWith(
                        object :
                            DisposableSingleObserver<GenericResponseClass<AuthToken?, ApiErrorResponse?>?>() {
                            override fun onError(e: Throwable) {
                                logger.error("Auth token: {}", e.message)
                                onLoginFailed()
                            }

                            override fun onSuccess(
                                response: GenericResponseClass<AuthToken?, ApiErrorResponse?>
                            ) {
                                response.dataClass?.let {
                                    if (it.captcha != null) {
                                        val captchaArt = it.captcha!!.asciiArt!!
                                        logger.info("Captcha received: $captchaArt")
                                        welcomeView.prepareUiForApiCallFinished()
                                        welcomeView.captchaReceived(
                                            username,
                                            password,
                                            it.token,
                                            captchaArt
                                        )
                                    } else {
                                        logger.info("Starting login")
                                        startLoginProcess(username, password, null, it.token, null)
                                    }
                                } ?: response.errorClass?.let {
                                    logger.error("Login: {}", it)
                                    onLoginResponseError(it, username, password)
                                }
                            }
                        })
            )
        }
    }

    override fun onAuthSignUpClick(username: String, password: String, email: String?) {
        isRegistration = true
        welcomeView.hideSoftKeyboard()
        if (validateLoginInputs(username, password, email ?: "", false)) {
            logger.info("Requesting auth signup token.")
            welcomeView.prepareUiForApiCallStart()
            interactor.getCompositeDisposable().add(
                interactor.getApiCallManager()
                    .authTokenSignup(true)
                    .doOnSubscribe { welcomeView.updateCurrentProcess("Preparing signup...") }
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribeWith(
                        object :
                            DisposableSingleObserver<GenericResponseClass<AuthToken?, ApiErrorResponse?>?>() {
                            override fun onError(e: Throwable) {
                                logger.error("Auth signup token: {}", e.message)
                                onLoginFailed()
                            }

                            override fun onSuccess(
                                response: GenericResponseClass<AuthToken?, ApiErrorResponse?>
                            ) {
                                response.dataClass?.let {
                                    if (it.captcha != null) {
                                        val captchaArt = it.captcha!!.asciiArt!!
                                        logger.info("Signup captcha received: $captchaArt")
                                        welcomeView.prepareUiForApiCallFinished()
                                        welcomeView.captchaReceived(
                                            username,
                                            password,
                                            it.token,
                                            captchaArt
                                        )
                                    } else {
                                        logger.info("Starting signup without captcha")
                                        startSignUpProcess(
                                            username,
                                            password,
                                            email,
                                            true,
                                            it.token,
                                            null
                                        )
                                    }
                                } ?: response.errorClass?.let {
                                    logger.error("Signup: {}", it)
                                    onLoginResponseError(it, username, password)
                                }
                            }
                        })
            )
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
        interactor.getCompositeDisposable().add(
            interactor.getApiCallManager()
                .logUserIn(
                    username,
                    password,
                    twoFa,
                    secureToken,
                    captcha,
                    floatArrayOf(),
                    floatArrayOf()
                )
                .doOnSubscribe { welcomeView.updateCurrentProcess("Signing in...") }
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeWith(
                    object :
                        DisposableSingleObserver<GenericResponseClass<UserLoginResponse?, ApiErrorResponse?>?>() {
                        override fun onError(e: Throwable) {
                            logger.error("Login: {}", e.message)
                            onLoginFailed()
                        }

                        override fun onSuccess(
                            response: GenericResponseClass<UserLoginResponse?, ApiErrorResponse?>
                        ) {
                            response.dataClass?.let {
                                logger.info("Logged user in successfully...")
                                welcomeView.updateCurrentProcess("Login successful...")
                                interactor.getAppPreferenceInterface().sessionHash =
                                    it.sessionAuthHash
                                interactor.getFireBaseManager().getFirebaseToken { token ->
                                    prepareLoginRegistrationDashboard(token)
                                }
                            } ?: response.errorClass?.let {
                                logger.error("Login: {}", it)
                                onLoginResponseError(it, username, password)
                            }
                        }
                    })
        )
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
            interactor.getCompositeDisposable().add(
                interactor.getApiCallManager()
                    .signUserIn(
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
                    .doOnSubscribe { welcomeView.updateCurrentProcess("Signing up") }
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribeWith(
                        object :
                            DisposableSingleObserver<GenericResponseClass<UserRegistrationResponse?, ApiErrorResponse?>?>() {
                            override fun onError(e: Throwable) {
                                logger.error("Signup: {}", e.message)
                                onLoginFailed()
                            }

                            override fun onSuccess(
                                response: GenericResponseClass<UserRegistrationResponse?, ApiErrorResponse?>
                            ) {
                                response.dataClass?.let {
                                    logger.info("Sign up user successfully...")
                                    welcomeView.updateCurrentProcess("SignUp successful...")
                                    interactor.getAppPreferenceInterface().sessionHash =
                                        it.sessionAuthHash
                                    interactor.getFireBaseManager().getFirebaseToken { token ->
                                        prepareLoginRegistrationDashboard(token)
                                    }
                                } ?: response.errorClass?.let {
                                    logger.error("Signup: {}", it)
                                    onLoginResponseError(it, username, password)
                                }
                            }
                        })
            )
        }
    }

    fun startXPressLoginCodeVerifier(xPressLoginCodeResponse: XPressLoginCodeResponse) {
        val startTime = System.currentTimeMillis()
        compositeDisposable.add(
            interactor.getApiCallManager().verifyXPressLoginCode(
                xPressLoginCodeResponse.xPressLoginCode,
                xPressLoginCodeResponse.signature
            )
                .timeout(20, TimeUnit.SECONDS).onErrorReturnItem(GenericResponseClass(null, null))
                .repeatWhen { completed: Flowable<Any?> -> completed.delay(3, TimeUnit.SECONDS) }
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeWith(
                    object :
                        DisposableSubscriber<GenericResponseClass<XPressLoginVerifyResponse?, ApiErrorResponse?>>() {
                        override fun onComplete() {}
                        override fun onError(t: Throwable) {
                            logger.error("Login code verify: {}", t.message)
                            invalidateLoginCode(startTime, xPressLoginCodeResponse)
                        }

                        override fun onNext(
                            response: GenericResponseClass<XPressLoginVerifyResponse?, ApiErrorResponse?>
                        ) {
                            response.dataClass?.let {
                                compositeDisposable.clear()
                                logger.debug("Successfully verified XPress login code.")
                                val sessionAuth = it.sessionAuth
                                interactor.getAppPreferenceInterface().sessionHash = sessionAuth
                                interactor.getFireBaseManager().getFirebaseToken { token ->
                                    prepareLoginRegistrationDashboard(token)
                                }
                            }
                            invalidateLoginCode(startTime, xPressLoginCodeResponse)
                        }
                    })
        )
    }

    private fun evaluatePassword(password: String): Boolean {
        val pattern = Regex("(?=.*[a-z])(?=.*[A-Z])(?=\\S+$).{8,}")
        return password.matches(pattern)
    }

    private fun invalidateLoginCode(
        startTime: Long, xPressLoginCodeResponse: XPressLoginCodeResponse
    ) {
        val secondsPassed =
            TimeUnit.SECONDS.convert(System.currentTimeMillis() - startTime, TimeUnit.MILLISECONDS)
        if (secondsPassed > xPressLoginCodeResponse.ttl) {
            compositeDisposable.clear()
            logger.error("Failed to verify XPress login code in ttl .Giving up")
            welcomeView.setSecretCode("")
        }
    }

    private fun onAccountClaimSuccess() {
        welcomeView.updateCurrentProcess("Getting session")
        interactor.getCompositeDisposable().add(
            interactor.getApiCallManager().getSessionGeneric(null)
                .flatMap { apiResponse: GenericResponseClass<UserSessionResponse?, ApiErrorResponse?> ->
                    Single.fromCallable {
                        apiResponse.dataClass?.let {
                            interactor.getUserRepository().reload(it)
                        } ?: kotlin.run {
                            throw Exception("Failed to update session.")
                        }
                    }
                }.subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({
                    interactor.getUserRepository().user.value?.let { user ->
                        user.email?.let {
                            welcomeView.gotoHomeActivity()
                        } ?: kotlin.run {
                            welcomeView.gotoAddEmailActivity(user.isPro)
                        }
                    }
                }, {
                    welcomeView.prepareUiForApiCallFinished()
                    welcomeView.showError("Unable to auto login. Log in using new credentials.")
                    logger.error("Account claim: {}", it.message)
                })
        )
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
        interactor.getAppPreferenceInterface().loginTime = Date()
        welcomeView.updateCurrentProcess(interactor.getResourceString(com.windscribe.vpn.R.string.getting_session))
        interactor.getCompositeDisposable()
            .add(
                interactor.getApiCallManager().getSessionGeneric(firebaseToken)
                    .flatMapCompletable { sessionResponse: GenericResponseClass<UserSessionResponse?, ApiErrorResponse?> ->
                        Completable.fromSingle(Single.fromCallable {
                            sessionResponse.dataClass?.let {
                                if (interactor.getAppPreferenceInterface()
                                        .getDeviceUUID() == null
                                ) {
                                    logger.debug("No device id is found for the current user, generating and saving UUID")
                                    interactor.getAppPreferenceInterface()
                                        .setDeviceUUID(UUID.randomUUID().toString())
                                }
                                interactor.getUserRepository().reload(sessionResponse.dataClass)
                            }
                            true
                        })
                    }
                    .doOnComplete {
                        welcomeView.updateCurrentProcess(
                            interactor.getResourceString(
                                com.windscribe.vpn.R.string.getting_server_credentials
                            )
                        )
                    }
                    .andThen(interactor.getConnectionDataUpdater().update())
                    .doOnComplete {
                        welcomeView.updateCurrentProcess(
                            interactor.getResourceString(
                                com.windscribe.vpn.R.string.getting_server_list
                            )
                        )
                    }
                    .andThen(interactor.getServerListUpdater().update())
                    .andThen(updateStaticIps())
                    .andThen(Completable.fromAction {
                        interactor.getPreferenceChangeObserver().postCityServerChange()
                    })
                    .andThen(interactor.updateUserData())
                    .onErrorResumeNext { throwable: Throwable ->
                        logger.error("Prepare data: {}", throwable.message)
                        Completable.fromAction {
                            interactor.getPreferenceChangeObserver().postCityServerChange()
                        }
                            .andThen(interactor.updateUserData())
                    }.subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribeWith(object : DisposableCompletableObserver() {
                        override fun onComplete() {
                            interactor.getWorkManager().onAppStart()
                            interactor.getWorkManager().onAppMovedToForeground()
                            interactor.getWorkManager().updateNodeLatencies()
                            if (isRegistration) {
                                welcomeView.gotoAddEmailActivity(
                                    interactor.getAppPreferenceInterface().userStatus
                                            == UserStatusConstants.USER_STATUS_PREMIUM
                                )
                            } else {
                                welcomeView.gotoHomeActivity()
                            }
                        }

                        override fun onError(e: Throwable) {
                            welcomeView.prepareUiForApiCallFinished()
                            logger.error("Prepare data: {}", e.message)
                        }
                    })
            )
    }

    private fun updateStaticIps(): Completable {
        val sipCount = interactor.getUserRepository().user.value?.sipCount ?: 0
        return if (sipCount > 0) {
            interactor.getStaticListUpdater().update()
        } else {
            Completable.fromAction {}
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
            welcomeView.setUsernameError(interactor.getResourceString(com.windscribe.vpn.R.string.username_empty))
            welcomeView.showToast(interactor.getResourceString(com.windscribe.vpn.R.string.enter_username))
            return false
        }

        // Invalid username
        if (!isLogin && !validateUsernameCharacters(username)) {
            logger.info("[username] has invalid characters in , displaying toast to the user...")
            welcomeView.setUsernameError(interactor.getResourceString(com.windscribe.vpn.R.string.login_with_username))
            welcomeView.showToast(interactor.getResourceString(com.windscribe.vpn.R.string.login_with_username))
            return false
        }

        // Empty Password
        if (TextUtils.isEmpty(password)) {
            logger.info("[password] is empty, displaying toast to the user...")
            welcomeView.setPasswordError(interactor.getResourceString(com.windscribe.vpn.R.string.password_empty))
            welcomeView.showToast(interactor.getResourceString(com.windscribe.vpn.R.string.enter_password))
            return false
        }
        if (email?.isNotEmpty() == true && !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            logger.info("[Email] is invalid, displaying toast to the user...")
            welcomeView.showToast(interactor.getResourceString(com.windscribe.vpn.R.string.invalid_email_format))
            return false
        }
        if (email?.isNotEmpty() == true && !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            logger.info("[Email] is invalid, displaying toast to the user...")
            welcomeView.showToast(interactor.getResourceString(com.windscribe.vpn.R.string.invalid_email_format))
            return false
        }
        if (!isLogin && password.length < 8) {
            logger.info("[Password] is small, displaying toast to the user...")
            welcomeView.showToast(interactor.getResourceString(com.windscribe.vpn.R.string.small_password))
            return false
        }
        // Sign up and claim account password minimum strength enforce.
        if (!isLogin && !evaluatePassword(password)) {
            logger.info("[Password] is weak, displaying toast to the user...")
            welcomeView.showToast(interactor.getResourceString(com.windscribe.vpn.R.string.weak_password))
            return false
        }
        if (!isLogin && CommonPasswordChecker.isAMatch(password)) {
            logger.info("[Password] matches worst password list, displaying toast to the user...")
            welcomeView.setPasswordError(interactor.getResourceString(com.windscribe.vpn.R.string.common_password))
            welcomeView.showToast(interactor.getResourceString(com.windscribe.vpn.R.string.common_password))
            return false
        }
        return true
    }

    private fun validateUsernameCharacters(username: String): Boolean {
        return Pattern.matches("[a-zA-Z0-9_-]*", username)
    }
}
