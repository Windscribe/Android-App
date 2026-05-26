/*
 * Copyright (c) 2021 Windscribe Limited.
 */
package com.windscribe.tv.welcome

import com.windscribe.tv.welcome.WelcomeView
import kotlinx.coroutines.CoroutineScope

interface WelcomePresenter {
    fun bind(view: WelcomeView, scope: CoroutineScope)
    fun exportLog()
    fun onBackPressed()
    fun onDestroy()
    fun onGenerateCodeClick()
    fun onActivityCreated()
    fun startAccountClaim(
        username: String,
        password: String,
        email: String?,
        ignoreEmptyEmail: Boolean
    )
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
    fun onAuthSignUpClick(username: String, password: String, email: String?)
}
