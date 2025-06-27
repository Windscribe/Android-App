/*
 * Copyright (c) 2021 Windscribe Limited.
 */
package com.windscribe.tv.welcome

interface WelcomePresenter {
    fun exportLog()
    fun onBackPressed()
    fun onDestroy()
    fun onGenerateCodeClick()
    fun startAccountClaim(
        username: String,
        password: String,
        email: String?,
        ignoreEmptyEmail: Boolean
    )

    fun startGhostAccountSetup()
    fun startLoginProcess(username: String, password: String, twoFa: String?, secureToken: String?, captcha: String?)
    fun startSignUpProcess(
        username: String,
        password: String,
        email: String?,
        ignoreEmptyEmail: Boolean,
        secureToken: String?,
        captcha: String?
    )

    fun onAuthLoginClick(username: String, password: String)
}
