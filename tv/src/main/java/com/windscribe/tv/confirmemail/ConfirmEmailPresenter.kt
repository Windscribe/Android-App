/*
 * Copyright (c) 2021 Windscribe Limited.
 */
package com.windscribe.tv.confirmemail

interface ConfirmEmailPresenter {
    fun init()
    val isUserPro: Boolean
    fun onDestroy()
    fun resendVerificationEmail()
}
