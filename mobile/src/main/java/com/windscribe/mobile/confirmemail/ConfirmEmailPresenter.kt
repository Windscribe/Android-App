/*
 * Copyright (c) 2021 Windscribe Limited.
 */
package com.windscribe.mobile.confirmemail

interface ConfirmEmailPresenter {
    fun init(reasonToConfirmEmail: String?)
    fun onDestroy()
    fun resendVerificationEmail()
}