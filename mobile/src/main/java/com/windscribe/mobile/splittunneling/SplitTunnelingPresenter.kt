/*
 * Copyright (c) 2021 Windscribe Limited.
 */
package com.windscribe.mobile.splittunneling

import android.content.Context

interface SplitTunnelingPresenter {
    fun onBackPressed()
    fun onDestroy()
    fun onFilter(query: String)
    fun onNewRoutingModeSelected(mode: String)
    fun onToggleButtonClicked()
    fun setTheme(context: Context)
    fun setupLayoutBasedOnPreviousSettings()
}