/*
 * Copyright (c) 2021 Windscribe Limited.
 */
package com.windscribe.mobile.welcome

import java.io.File

interface WelcomeView {
    fun clearInputErrors()
    fun goToSignUp()
    fun gotoHomeActivity(clearTop: Boolean)
    fun hideSoftKeyboard()
    fun launchShareIntent(file: File)
    fun prepareUiForApiCallFinished()
    fun prepareUiForApiCallStart()
    fun setEmailError(errorMessage: String)
    fun setFaFieldsVisibility(visible: Int)
    fun setLoginRegistrationError(error: String)
    fun setPasswordError(error: String)
    fun setTwoFaError(errorMessage: String)
    fun setUsernameError(error: String)
    fun showError(error: String)
    fun showFailedAlert(error: String)
    fun showNoEmailAttentionFragment(
        username: String,
        password: String,
        accountClaim: Boolean,
        pro: Boolean
    )

    fun showToast(message: String)
    fun updateCurrentProcess(mCurrentCall: String)
}