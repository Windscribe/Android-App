/*
 * Copyright (c) 2021 Windscribe Limited.
 */
package com.windscribe.mobile.account

import android.content.Context

interface AccountPresenter {
    fun onAddEmailClicked(tvEmailText: String)
    fun observeUserData(accountActivity: AccountActivity)
    fun onCodeEntered(code: String)
    fun onDestroy()
    fun onEditAccountClicked()
    fun onResendEmail()
    fun onUpgradeClicked(textViewText: String)
    fun onLazyLoginClicked()
    fun setLayoutFromApiSession()
    fun setTheme(context: Context)
}