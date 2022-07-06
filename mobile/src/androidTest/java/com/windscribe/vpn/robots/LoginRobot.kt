/*
 * Copyright (c) 2021 Windscribe Limited.
 */

package com.windscribe.vpn.robots

import androidx.test.espresso.ViewInteraction
import com.windscribe.mobile.R
import com.windscribe.test.BaseRobot
import com.windscribe.test.clearInput
import com.windscribe.test.click
import com.windscribe.test.hidden
import com.windscribe.test.isDisabled
import com.windscribe.test.isEnabled
import com.windscribe.test.isViewVisible
import com.windscribe.test.showing
import com.windscribe.test.typeText
import com.windscribe.test.view

class LoginRobot : BaseRobot() {
    fun pressLoginButton() = loginButton.click()
    fun enterUserName(username: String) = userNameText.typeText(username)
    fun enterPassword(password: String) = passwordText.typeText(password)
    fun pressContinueButton() = loginButton.click()
    fun pressPasswordVisibilityToggle() = passwordVisibilityToggle.click()
    fun passwordVisible() = passwordText.showing()
    fun passwordHidden() = passwordText.hidden()
    fun progressViewVisible() = progress.isViewVisible()
    fun userNameErrorVisible() = userNameError.isViewVisible()
    fun passwordErrorVisible() = passwordError.isViewVisible()
    fun loginButtonActive() = loginButton.isEnabled()
    fun loginButtonInactive() = loginButton.isDisabled()
    fun pressForgotPassword() = forgotPassword.click()
    fun loginViewVisible() = loginButton.isViewVisible()
    fun welcomeViewVisible() = getStartedButton.isViewVisible()
    fun twoFaInputBoxVisible() = twoFaInputBox.isViewVisible()
    fun homeViewVisible() = homeView.isViewVisible()

    fun clearInputs() {
        userNameText.clearInput()
        passwordText.clearInput()
    }

    companion object {
        val userNameText: ViewInteraction = view(R.id.username)
        val passwordText: ViewInteraction = view(R.id.password)
        val loginButton: ViewInteraction = view(R.id.loginButton)
        val progress: ViewInteraction = view(R.id.progress_container)
        val userNameError: ViewInteraction = view(R.id.username_error)
        val passwordError: ViewInteraction = view(R.id.password_error)
        val passwordVisibilityToggle: ViewInteraction = view(R.id.password_visibility_toggle)
        val forgotPassword: ViewInteraction = view(R.id.forgot_password)
        val getStartedButton: ViewInteraction = view(R.id.get_started_button)
        val twoFaInputBox: ViewInteraction = view(R.id.two_fa)
        val homeView: ViewInteraction = view(R.id.cl_windscribe_main)
    }
}