/*
 * Copyright (c) 2021 Windscribe Limited.
 */
package com.windscribe.tv.welcome

import java.io.File

interface WelcomeView {
    fun clearInputErrors()
    fun goToSignUp()
    fun gotoAddEmailActivity(proUser: Boolean)
    fun gotoHomeActivity()
    fun hideSoftKeyboard()
    fun launchShareIntent(file: File)
    fun prepareUiForApiCallFinished()
    fun prepareUiForApiCallStart()
    fun setLoginRegistrationError(error: String)
    fun setPasswordError(error: String)
    fun setSecretCode(secretCode: String)
    fun setTwoFaError(error: String)
    fun setTwoFaRequired(username: String, password: String)
    fun setUsernameError(error: String)
    fun showError(error: String)
    fun showFailedAlert()
    fun showNoEmailAttentionFragment()
    fun showToast(message: String)
    fun updateCurrentProcess(currentCall: String)
}
