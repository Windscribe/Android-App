/*
 * Copyright (c) 2021 Windscribe Limited.
 */
package com.windscribe.mobile.email

interface AddEmailPresenter {
    fun onAddEmailClicked(emailAddress: String)
    fun onDestroy()
    fun setUpLayout()
}