/*
 * Copyright (c) 2021 Windscribe Limited.
 */
package com.windscribe.mobile.confirmemail

interface ConfirmEmailView {
    fun finishActivity()
    fun setReasonToConfirmEmail(reasonForConfirmEmail: String)
    fun showEmailConfirmProgress(show: Boolean)
    fun showToast(toast: String)
}