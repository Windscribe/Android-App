/*
 * Copyright (c) 2021 Windscribe Limited.
 */
package com.windscribe.tv.email

import com.windscribe.tv.email.AddEmailView
import kotlinx.coroutines.CoroutineScope

interface AddEmailPresenter {
    fun bind(view: AddEmailView, scope: CoroutineScope)
    fun onAddEmailClicked(emailAddress: String)
    fun onDestroy()
    fun onResendEmail(emailAddress: String)
    fun onSkipEmailClicked()
}
