/*
 * Copyright (c) 2021 Windscribe Limited.
 */
package com.windscribe.mobile.welcome.fragment

interface FragmentCallback {
    fun onAccountClaimButtonClick(
        username: String,
        password: String,
        email: String,
        ignoreEmptyEmail: Boolean,
        voucherCode: String
    )

    fun onBackButtonPressed()
    fun onContinueWithOutAccountClick()
    fun onForgotPasswordClick()
    fun onLoginButtonClick(username: String, password: String, twoFa: String)
    fun onLoginClick()
    fun onEmergencyClick()
    fun onSignUpButtonClick(
        username: String,
        password: String,
        email: String,
        referralUsername: String,
        ignoreEmptyEmail: Boolean,
        voucherCode: String
    )

    fun onSkipToHomeClick()
}