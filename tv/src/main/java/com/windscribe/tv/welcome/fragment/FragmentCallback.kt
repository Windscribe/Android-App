/*
 * Copyright (c) 2021 Windscribe Limited.
 */
package com.windscribe.tv.welcome.fragment

interface FragmentCallback {
    fun onAccountClaimButtonClick(
        username: String,
        password: String,
        email: String?,
        ignoreEmptyEmail: Boolean
    )

    fun onBackButtonPressed()
    fun onContinueWithOutAccountClick()
    fun onForgotPasswordClick()
    fun onGenerateCodeClick()
    fun onLoginButtonClick(username: String, password: String, twoFa: String?, secureToken: String?, captcha: String?)
    fun onAuthLoginClick(username: String, password: String)
    fun onAuthSignUpClick()
    fun onLoginClick()
    fun onSignUpButtonClick(
        username: String,
        password: String,
        email: String?,
        ignoreEmptyEmail: Boolean,
        secureToken: String?,
        captcha: String?
    )
}
