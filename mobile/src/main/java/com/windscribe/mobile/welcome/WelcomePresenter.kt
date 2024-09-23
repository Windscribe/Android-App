/*
 * Copyright (c) 2021 Windscribe Limited.
 */
package com.windscribe.mobile.welcome

import java.io.File

interface WelcomePresenter {
    fun exportLog()
    val isUserPro: Boolean
    fun onBackPressed()
    fun onDestroy()
    fun startAccountClaim(
        username: String,
        password: String,
        email: String,
        ignoreEmptyEmail: Boolean,
        voucherCode: String
    )

    fun startGhostAccountSetup()
    fun startLoginProcess(username: String, password: String, twoFa: String)
    fun startSignUpProcess(
        username: String,
        password: String,
        email: String,
        referralUsername: String,
        ignoreEmptyEmail: Boolean,
        voucherCode: String
    )

    fun getLogUri(): File?
}