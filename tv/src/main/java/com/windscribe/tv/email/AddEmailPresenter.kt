/*
 * Copyright (c) 2021 Windscribe Limited.
 */
package com.windscribe.tv.email

interface AddEmailPresenter {
    fun onAddEmailClicked(emailAddress: String)
    fun onDestroy()
    fun onResendEmail(emailAddress: String)
    fun onSkipEmailClicked()
}
