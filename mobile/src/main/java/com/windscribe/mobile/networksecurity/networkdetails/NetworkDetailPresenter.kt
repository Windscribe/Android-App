/*
 * Copyright (c) 2021 Windscribe Limited.
 */
package com.windscribe.mobile.networksecurity.networkdetails

import android.content.Context

interface NetworkDetailPresenter {
    fun onDestroy()
    fun onPortSelected(port: String)
    fun onProtocolSelected(protocol: String)
    fun removeNetwork(name: String)
    fun setNetworkDetails(name: String)
    fun setProtocols()
    fun setTheme(context: Context)
    fun toggleAutoSecure()
    fun togglePreferredProtocol()
    fun init()
    fun reloadNetworkOptions()
}