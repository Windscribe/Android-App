/*
 * Copyright (c) 2021 Windscribe Limited.
 */

package com.windscribe.vpn.robots

import androidx.test.espresso.ViewInteraction
import com.windscribe.mobile.R
import com.windscribe.test.BaseRobot
import com.windscribe.test.click
import com.windscribe.test.isViewVisible
import com.windscribe.test.scrollTo
import com.windscribe.test.typeText
import com.windscribe.test.view

class SignUpRobot : BaseRobot() {
    fun pressGetStartedButton() = getStartedButton.click()
    fun enterUserName(username: String) = userNameText.typeText(username)
    fun enterPassword(password: String) = passwordText.typeText(password)
    fun enterEmail(email: String) = emailText.typeText(email)
    fun pressContinueButton() = loginButton.click()
    fun progressViewVisible() = progress.isViewVisible()
    fun scrollToEmail() = emailText.scrollTo()
    fun signUpViewVisible() = userNameText.isViewVisible()
    fun homeViewVisible() = homeView.isViewVisible()

    companion object {
        val userNameText: ViewInteraction = view(R.id.username)
        val passwordText: ViewInteraction = view(R.id.password)
        val loginButton: ViewInteraction = view(R.id.loginButton)
        val progress: ViewInteraction = view(R.id.progress_container)
        val getStartedButton: ViewInteraction = view(R.id.get_started_button)
        val emailText: ViewInteraction = view(R.id.email)
        val homeView: ViewInteraction = view(R.id.cl_windscribe_main)
    }
}