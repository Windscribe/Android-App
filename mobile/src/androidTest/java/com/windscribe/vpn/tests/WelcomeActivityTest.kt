/*
 * Copyright (c) 2021 Windscribe Limited.
 */

package com.windscribe.vpn.tests

import androidx.test.ext.junit.rules.activityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.windscribe.mobile.welcome.WelcomeActivity
import com.windscribe.test.Constants.Email
import com.windscribe.test.Constants.NewUserPassword
import com.windscribe.test.Constants.Password
import com.windscribe.test.Constants.Username
import com.windscribe.test.dispatcher.MockServerDispatcher
import com.windscribe.vpn.constants.NetworkErrorCodes
import de.codecentric.androidtestktx.espresso.extensions.waitFor
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class WelcomeActivityTest : BaseTest() {
    @get:Rule
    var activityScenarioRule = activityScenarioRule<WelcomeActivity>()

    @Test
    fun loginButtonState() {
        buildLoginRobot {
            pressLoginButton()
            waitFor(500)
            loginButtonInactive()
            enterUserName(Username)
            loginButtonInactive()
            enterPassword(Password)
            loginButtonActive()
            clearInputs()
            loginButtonInactive()
            waitFor(2000)
        }
    }

    @Test
    fun passwordVisibility() {
        buildLoginRobot {
            pressLoginButton()
            waitFor(500)
            enterPassword(Password)
            passwordHidden()
            pressPasswordVisibilityToggle()
            passwordVisible()
            pressPasswordVisibilityToggle()
            passwordHidden()
            waitFor(2000)
        }
    }

    @Test
    fun forgotPassword() {
        buildLoginRobot {
            welcomeViewVisible()
            pressLoginButton()
            loginViewVisible()
            pressForgotPassword()
            waitFor(1000)
            pageLoadedInBrowser()
            waitFor(2000)
        }
    }

    @Test
    fun bannedFromCreatingGhostAccount() {
        mockServer.dispatcher = MockServerDispatcher.ApiError("You are banned from creating Ghost account . Signup with username and password.")
        buildSignUpRobot {
            pressGetStartedButton()
            signUpViewVisible()
            waitFor(2000)
        }
    }

    @Test
    fun createGhostAccount() {
        buildSignUpRobot {
            pressGetStartedButton()
            progressViewVisible()
            waitFor(2000)
        }
    }

    @Test
    fun wrongUsernameOrPassword() {
        mockServer.dispatcher =
                MockServerDispatcher.ApiError("Something is wrong with your username and password.")
        buildLoginRobot {
            pressLoginButton()
            waitFor(500)
            enterUserName(Username)
            enterPassword(Password)
            pressContinueButton()
            progressViewVisible()
            userNameErrorVisible()
            passwordErrorVisible()
            waitFor(2000)
        }
    }

    @Test
    fun twoFaCodeMissingRequired() {
        mockServer.dispatcher = MockServerDispatcher.ApiError("Missing two fa code.", NetworkErrorCodes.ERROR_2FA_REQUIRED)
        buildLoginRobot {
            pressLoginButton()
            waitFor(1000)
            enterUserName(Username)
            enterPassword(Password)
            pressContinueButton()
            progressViewVisible()
            twoFaInputBoxVisible()
            waitFor(2000)
        }
    }

    @Test
    fun loginSuccess() {
        buildLoginRobot {
            pressLoginButton()
            waitFor(500)
            enterUserName(Username)
            enterPassword(Password)
            pressContinueButton()
            waitFor(1000)
            homeViewVisible()
            waitFor(2000)
        }
    }

    @Test
    fun signUpSuccess() {
        mockServer.dispatcher = MockServerDispatcher.ApiError()
        buildSignUpRobot {
            pressGetStartedButton()
            waitFor(500)
            mockServer.dispatcher = MockServerDispatcher.ResponseDispatcher()
            enterUserName(Username)
            enterPassword(NewUserPassword)
            scrollToEmail()
            enterEmail(Email)
            pressContinueButton()
            waitFor(1000)
            homeViewVisible()
            waitFor(2000)
        }
    }
}