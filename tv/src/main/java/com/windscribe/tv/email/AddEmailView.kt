/*
 * Copyright (c) 2021 Windscribe Limited.
 */
package com.windscribe.tv.email

interface AddEmailView {
    fun decideActivity()
    fun decideActivityForSkipButton()
    fun hideSoftKeyboard()
    fun prepareUiForApiCallFinished()
    fun prepareUiForApiCallStart()
    fun showInputError(errorText: String)
    fun showToast(toastString: String)
}
