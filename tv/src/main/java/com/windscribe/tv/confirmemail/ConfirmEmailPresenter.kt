/*
 * Copyright (c) 2021 Windscribe Limited.
 */
package com.windscribe.tv.confirmemail

import kotlinx.coroutines.CoroutineScope

interface ConfirmEmailPresenter {
    fun bind(
        view: ConfirmEmailView,
        scope: CoroutineScope,
    )

    fun init()

    val isUserPro: Boolean

    fun onDestroy()

    fun resendVerificationEmail()
}
