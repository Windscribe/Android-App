/*
 * Copyright (c) 2021 Windscribe Limited.
 */
package com.windscribe.mobile.networksecurity

import android.content.Context
import com.windscribe.vpn.localdatabase.tables.NetworkInfo

interface NetworkSecurityPresenter {
    val savedLocale: String
    fun onAdapterSet()
    fun onDestroy()
    fun onNetworkSecuritySelected(networkInfo: NetworkInfo)
    fun onCurrentNetworkClick()
    fun setTheme(context: Context)
    fun setupNetworkListAdapter()
    fun init()
    fun onAutoSecureToggleClick()
}