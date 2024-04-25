/*
 * Copyright (c) 2021 Windscribe Limited.
 */
package com.windscribe.mobile.welcome

import android.text.TextUtils
import android.util.Patterns
import android.view.View
import com.windscribe.mobile.R
import com.windscribe.vpn.commonutils.CommonPasswordChecker
import com.windscribe.vpn.ActivityInteractor
import com.windscribe.vpn.api.CreateHashMap.createClaimAccountMap
import com.windscribe.vpn.api.CreateHashMap.createGhostModeMap
import com.windscribe.vpn.api.CreateHashMap.createLoginMap
import com.windscribe.vpn.api.CreateHashMap.createRegistrationMap
import com.windscribe.vpn.api.response.*
import com.windscribe.vpn.constants.NetworkErrorCodes
import com.windscribe.vpn.constants.NetworkKeyConstants
import com.windscribe.vpn.constants.UserStatusConstants.USER_STATUS_PREMIUM
import com.windscribe.vpn.errormodel.SessionErrorHandler
import com.windscribe.vpn.errormodel.WindError
import com.windscribe.vpn.repository.CallResult
import io.reactivex.Completable
import io.reactivex.Single
import io.reactivex.SingleSource
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.functions.Function
import io.reactivex.observers.DisposableCompletableObserver
import io.reactivex.observers.DisposableSingleObserver
import io.reactivex.schedulers.Schedulers
import org.slf4j.LoggerFactory
import java.io.File
import java.io.IOException
import java.util.*
import javax.inject.Inject

class WelcomePresenterImpl @Inject constructor(
    private val welcomeView: WelcomeView, private val interactor: ActivityInteractor
) : WelcomePresenter {

    private val logger = LoggerFactory.getLogger("login-p")

    override fun onDestroy() {
        interactor.getCompositeDisposable().clear()
    }

    override fun exportLog() {
        try {
            val file = File(interactor.getDebugFilePath())
            welcomeView.launchShareIntent(file)
        } catch (e: Exception) {
            welcomeView.showToast(WindError.instance.rxErrorToString(e))
        }
    }

    override val isUserPro: Boolean
        get() = interactor.getAppPreferenceInterface().userStatus == USER_STATUS_PREMIUM

    override fun onBackPressed() {
        interactor.getCompositeDisposable().clear()
        welcomeView.hideSoftKeyboard()
    }

    override fun startAccountClaim(
        username: String, password: String, email: String, ignoreEmptyEmail: Boolean
    ) {
        welcomeView.hideSoftKeyboard()
        if (validateLoginInputs(username, password, email, false)) {
            if (ignoreEmptyEmail.not() && email.isEmpty()) {
                val proUser =
                    (interactor.getAppPreferenceInterface().userStatus == USER_STATUS_PREMIUM)
                welcomeView.showNoEmailAttentionFragment(username, password, true, proUser)
                return
            }
            logger.info("Trying to claim account with provided credentials...")
            welcomeView.prepareUiForApiCallFinished()
            welcomeView.prepareUiForApiCallStart()
            interactor.getCompositeDisposable().add(interactor.getApiCallManager()
                .claimAccount(username, password, email)
                .doOnSubscribe { welcomeView.updateCurrentProcess("Signing up") }
                .subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread())
                .subscribeWith(object :
                    DisposableSingleObserver<GenericResponseClass<ClaimAccountResponse?, ApiErrorResponse?>>() {
                    override fun onError(e: Throwable) {
                        logger.debug("User SignUp error..." + e.message)
                        onSignUpFailedWithNoError()
                    }

                    override fun onSuccess(genericLoginResponse: GenericResponseClass<ClaimAccountResponse?, ApiErrorResponse?>) {
                        when (val result =
                            genericLoginResponse.callResult<ClaimAccountResponse>()) {
                            is CallResult.Error -> {
                                if (result.code == NetworkErrorCodes.ERROR_UNEXPECTED_API_DATA) {
                                    onSignUpFailedWithNoError()
                                } else {
                                    logger.info("Account claim Error ${result.errorMessage}")
                                    onLoginResponseError(result.code, result.errorMessage)
                                }
                            }
                            is CallResult.Success -> {
                                logger.info("Account claimed successfully...")
                                welcomeView.updateCurrentProcess("SignUp successful...")
                                onAccountClaimSuccess(username)
                            }
                        }
                    }
                })
            )
        }
    }

    override fun startGhostAccountSetup() {
        welcomeView.prepareUiForApiCallStart()
        welcomeView.updateCurrentProcess("Signing In")
        interactor.getCompositeDisposable().add(interactor.getApiCallManager().getReg()
            .flatMap(Function<GenericResponseClass<RegToken?, ApiErrorResponse?>, SingleSource<GenericResponseClass<UserRegistrationResponse?, ApiErrorResponse?>>> label@{ regToken: GenericResponseClass<RegToken?, ApiErrorResponse?> ->
                when (val result = regToken.callResult<RegToken>()) {
                    is CallResult.Error -> {
                        if (result.code == NetworkErrorCodes.ERROR_UNEXPECTED_API_DATA) {
                            throw Exception("Unknown Error")
                        } else {
                            throw Exception(result.errorMessage)
                        }
                    }
                    is CallResult.Success -> {
                        return@label interactor.getApiCallManager().signUpUsingToken(result.data.token)
                    }
                }
            }).subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread())
            .subscribeWith(object :
                DisposableSingleObserver<GenericResponseClass<UserRegistrationResponse?, ApiErrorResponse?>>() {
                override fun onError(e: Throwable) {
                    welcomeView.prepareUiForApiCallFinished()
                    if (e is IOException) {
                        welcomeView.showError("Unable to reach server. Check your network connection.")
                    } else {
                        logger.debug(e.message)
                        welcomeView.goToSignUp()
                    }
                }

                override fun onSuccess(
                    regResponse: GenericResponseClass<UserRegistrationResponse?, ApiErrorResponse?>
                ) {
                    when (val result = regResponse.callResult<UserRegistrationResponse>()) {
                        is CallResult.Error -> {
                            welcomeView.prepareUiForApiCallFinished()
                            if (result.code != NetworkErrorCodes.ERROR_UNEXPECTED_API_DATA) {
                                logger.debug(result.errorMessage)
                                welcomeView.goToSignUp()
                            }
                        }
                        is CallResult.Success -> {
                            interactor.getAppPreferenceInterface().sessionHash =
                                result.data.sessionAuthHash
                            interactor.getFireBaseManager().getFirebaseToken { session ->
                                prepareLoginRegistrationDashboard(session)
                            }
                        }
                    }
                }
            })
        )
    }

    override fun startLoginProcess(username: String, password: String, twoFa: String) {
        welcomeView.hideSoftKeyboard()
        if (validateLoginInputs(username, password, "", true)) {
            logger.info("Trying to login with provided credentials...")
            welcomeView.prepareUiForApiCallStart()
            interactor.getCompositeDisposable().add(
                interactor.getApiCallManager().logUserIn(username, password, twoFa)
                    .doOnSubscribe { welcomeView.updateCurrentProcess(interactor.getResourceString(R.string.signing_in)) }
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribeWith(
                        object :
                            DisposableSingleObserver<GenericResponseClass<UserLoginResponse?, ApiErrorResponse?>>() {
                            override fun onError(e: Throwable) {
                                if (e is Exception) {
                                    logger.debug(
                                        "Login Error: " + WindError.instance.rxErrorToString(
                                            e,
                                        )
                                    )
                                }
                                onLoginFailedWithNoError()
                            }

                            override fun onSuccess(
                                genericLoginResponse: GenericResponseClass<UserLoginResponse?, ApiErrorResponse?>
                            ) {
                                when (val result =
                                    genericLoginResponse.callResult<UserLoginResponse>()) {
                                    is CallResult.Error -> {
                                        if (result.code == NetworkErrorCodes.ERROR_UNEXPECTED_API_DATA) {
                                            onLoginFailedWithNoError()
                                        } else {
                                            logger.info("Login error..." + genericLoginResponse.errorClass)
                                            onLoginResponseError(result.code, result.errorMessage)
                                        }
                                    }
                                    is CallResult.Success -> {
                                        logger.info("Logged user in successfully...")
                                        welcomeView.updateCurrentProcess("Login successful...")
                                        interactor.getAppPreferenceInterface().sessionHash =
                                            result.data.sessionAuthHash
                                        interactor.getFireBaseManager().getFirebaseToken { session ->
                                            prepareLoginRegistrationDashboard(session)
                                        }
                                    }
                                }
                            }
                        })
            )
        }
    }

    override fun startSignUpProcess(
        username: String,
        password: String,
        email: String,
        referralUsername: String,
        ignoreEmptyEmail: Boolean
    ) {
        welcomeView.hideSoftKeyboard()
        if (validateLoginInputs(username, password, email, false)) {
            if (!ignoreEmptyEmail && email.isEmpty()) {
                welcomeView.showNoEmailAttentionFragment(
                    username, password, accountClaim = false, pro = false
                )
                return
            }
            logger.info("Trying to sign up with provided credentials...")
            welcomeView.prepareUiForApiCallStart()
            interactor.getCompositeDisposable().add(interactor.getApiCallManager()
                .signUserIn(username, password, referralUsername, email)
                .doOnSubscribe { welcomeView.updateCurrentProcess("Signing up") }
                .subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread())
                .subscribeWith(object :
                    DisposableSingleObserver<GenericResponseClass<UserRegistrationResponse?, ApiErrorResponse?>>() {
                    override fun onError(e: Throwable) {
                        logger.debug("User SignUp error..." + e.message)
                        onSignUpFailedWithNoError()
                    }

                    override fun onSuccess(
                        genericLoginResponse: GenericResponseClass<UserRegistrationResponse?, ApiErrorResponse?>
                    ) {
                        when (val result =
                            genericLoginResponse.callResult<UserRegistrationResponse>()) {
                            is CallResult.Error -> {
                                logger.info("SignUp...$result")
                                if (result.code == NetworkErrorCodes.ERROR_UNEXPECTED_API_DATA) {
                                    onSignUpFailedWithNoError()
                                } else {
                                    onLoginResponseError(result.code, result.errorMessage)
                                }
                            }
                            is CallResult.Success -> {
                                logger.info("Sign up user successfully...")
                                welcomeView.updateCurrentProcess("SignUp successful...")
                                interactor.getAppPreferenceInterface().sessionHash =
                                    result.data.sessionAuthHash
                                interactor.getFireBaseManager().getFirebaseToken { session ->
                                    prepareLoginRegistrationDashboard(session)
                                }
                            }
                        }
                    }
                })
            )
        }
    }

    private fun evaluatePassword(password: String): Boolean {
        val pattern = Regex("(?=.*[a-z])(?=.*[A-Z])(?=\\S+$).{8,}")
        return password.matches(pattern)
    }

    private fun onAccountClaimSuccess(username: String) {
        welcomeView.updateCurrentProcess(interactor.getResourceString(R.string.getting_session))
        interactor.getCompositeDisposable().add(
            interactor.getApiCallManager().getSessionGeneric(null)
                .flatMapCompletable { sessionResponse: GenericResponseClass<UserSessionResponse?, ApiErrorResponse?> ->
                    Completable.fromSingle(Single.fromCallable {
                        interactor.getUserRepository().reload(sessionResponse.dataClass, null)
                        true
                    })
                }.subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread())
                .subscribeWith(object : DisposableCompletableObserver() {
                    override fun onComplete() {
                        welcomeView.gotoHomeActivity(true)
                    }

                    override fun onError(e: Throwable) {
                        welcomeView.prepareUiForApiCallFinished()
                        welcomeView.showError("Unable to auto login. Log in using new credentials.")
                        logger.debug(
                            "Error getting session"
                                    + WindError.instance.convertThrowableToString(e)
                        )
                    }
                })
        )
    }

    private fun onLoginFailedWithNoError() {
        welcomeView.prepareUiForApiCallFinished()
        welcomeView.showFailedAlert(interactor.getResourceString(R.string.failed_network_alert))
    }

    private fun onLoginResponseError(errorCode: Int, error: String) {
        logger.debug("Error code $errorCode error $error")
        welcomeView.prepareUiForApiCallFinished()
        val errorMessage = SessionErrorHandler.instance.getErrorMessage(errorCode, error)
        when (errorCode) {
            NetworkErrorCodes.ERROR_2FA_REQUIRED, NetworkErrorCodes.ERROR_INVALID_2FA -> {
                welcomeView.setFaFieldsVisibility(View.VISIBLE)
                welcomeView.setTwoFaError(errorMessage)
            }
            NetworkErrorCodes.ERROR_USER_NAME_ALREADY_TAKEN, NetworkErrorCodes.ERROR_USER_NAME_ALREADY_IN_USE -> {
                welcomeView.setUsernameError(errorMessage)
            }
            NetworkErrorCodes.ERROR_EMAIL_ALREADY_EXISTS, NetworkErrorCodes.ERROR_DISPOSABLE_EMAIL -> {
                welcomeView.setEmailError(errorMessage)
            }
            else -> {
                welcomeView.setLoginRegistrationError(errorMessage)
            }
        }
    }

    private fun onSignUpFailedWithNoError() {
        welcomeView.prepareUiForApiCallFinished()
        welcomeView.showFailedAlert(interactor.getResourceString(R.string.sign_up_failed_network_alert))
    }

    private fun prepareLoginRegistrationDashboard(sessionMap: Map<String, String>) {
        welcomeView.updateCurrentProcess(interactor.getResourceString(R.string.getting_session))
        interactor.getCompositeDisposable()
            .add(interactor.getApiCallManager().getSessionGeneric(sessionMap)
                .flatMapCompletable { sessionResponse: GenericResponseClass<UserSessionResponse?, ApiErrorResponse?> ->
                    Completable.fromSingle(Single.fromCallable {
                        when (val result = sessionResponse.callResult<UserSessionResponse>()) {
                            is CallResult.Error -> {}
                            is CallResult.Success -> {
                                if (interactor.getAppPreferenceInterface()
                                        .getDeviceUUID(result.data.userName) == null) {
                                    logger.debug("No device id is found for the current user, generating and saving UUID")
                                    interactor.getAppPreferenceInterface().setDeviceUUID(
                                        result.data.userName, UUID.randomUUID().toString()
                                    )
                                }
                            }
                        }
                        interactor.getUserRepository().reload(sessionResponse.dataClass, null)
                        true
                    })
                }.andThen(updateStaticIps())
                .doOnComplete { welcomeView.updateCurrentProcess(interactor.getResourceString(R.string.getting_server_credentials)) }
                .andThen(interactor.getConnectionDataUpdater().update())
                .doOnComplete { welcomeView.updateCurrentProcess(interactor.getResourceString(R.string.getting_server_list)) }
                .andThen(interactor.getServerListUpdater().update())
                .andThen(Completable.fromAction {
                    interactor.getPreferenceChangeObserver().postCityServerChange()
                }).andThen(interactor.updateUserData()).onErrorResumeNext { throwable: Throwable ->
                    logger.info(
                        "*****Preparing dashboard failed: ${
                            WindError.instance.rxErrorToString(
                                throwable as Exception
                            )
                        } Use reload button in server list in home activity."
                    )
                    Completable.fromAction {
                        interactor.getPreferenceChangeObserver().postCityServerChange()
                    }.andThen(interactor.updateUserData())
                }.subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread())
                .subscribeWith(object : DisposableCompletableObserver() {
                    override fun onComplete() {
                        interactor.getWorkManager().onAppStart()
                        interactor.getWorkManager().onAppMovedToForeground()
                        interactor.getWorkManager().updateNodeLatencies()
                        welcomeView.gotoHomeActivity(true)
                    }

                    override fun onError(e: Throwable) {
                        welcomeView.prepareUiForApiCallFinished()
                        logger.debug(
                            "Error while updating server status to local db. StackTrace: " + WindError.instance.convertThrowableToString(
                                e
                            )
                        )
                    }
                })
            )
    }

    private fun updateStaticIps(): Completable {
        val user = interactor.getUserRepository().user.value
        return if (user != null && user.sipCount > 0) {
            interactor.getStaticListUpdater().update()
        } else {
            Completable.fromAction {}
        }
    }

    private fun validateLoginInputs(
        username: String, password: String, email: String,
        isLogin: Boolean
    ): Boolean {
        logger.info("Validating login credentials")
        welcomeView.clearInputErrors()

        //Empty username
        if (TextUtils.isEmpty(username)) {
            logger.info("[username] is empty, displaying toast to the user...")
            welcomeView.setUsernameError(interactor.getResourceString(R.string.username_empty))
            welcomeView.showToast(interactor.getResourceString(R.string.enter_username))
            return false
        }

        //Invalid username
        if (!validateUsernameCharacters(username)) {
            logger.info("[username] has invalid characters in , displaying toast to the user...")
            welcomeView.setUsernameError(interactor.getResourceString(R.string.login_with_username))
            welcomeView.showToast(interactor.getResourceString(R.string.login_with_username))
            return false
        }

        //Empty Password
        if (TextUtils.isEmpty(password)) {
            logger.info("[password] is empty, displaying toast to the user...")
            welcomeView.setPasswordError(interactor.getResourceString(R.string.password_empty))
            welcomeView.showToast(interactor.getResourceString(R.string.enter_password))
            return false
        }
        if (!TextUtils.isEmpty(email) && !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            logger.info("[Email] is invalid, displaying toast to the user...")
            welcomeView.setEmailError(interactor.getResourceString(R.string.invalid_email_format))
            welcomeView.showToast(interactor.getResourceString(R.string.invalid_email_format))
            return false
        }
        if (!isLogin && password.length < 8) {
            logger.info("[Password] is small, displaying toast to the user...")
            welcomeView.setPasswordError(interactor.getResourceString(R.string.small_password))
            welcomeView.showToast(interactor.getResourceString(R.string.small_password))
            return false
        }
        // Sign up and claim account password minimum strength enforce.
        if (!isLogin && !evaluatePassword(password)) {
            logger.info("[Password] is weak, displaying toast to the user...")
            welcomeView.setPasswordError(interactor.getResourceString(R.string.weak_password))
            welcomeView.showToast(interactor.getResourceString(R.string.weak_password))
            return false
        }
        if (!isLogin && CommonPasswordChecker.isAMatch(password)) {
            logger.info("[Password] matches worst password list, displaying toast to the user...")
            welcomeView.setPasswordError(interactor.getResourceString(R.string.common_password))
            welcomeView.showToast(interactor.getResourceString(R.string.common_password))
            return false
        }
        return true
    }

    private fun validateUsernameCharacters(username: String): Boolean {
        return username.matches(Regex("[a-zA-Z0-9_-]*"))
    }
}