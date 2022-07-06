/*
 * Copyright (c) 2021 Windscribe Limited.
 */
package com.windscribe.mobile.networksecurity.networkdetails

import com.windscribe.vpn.localdatabase.tables.NetworkInfo

interface NetworkDetailView {
    var networkInfo: NetworkInfo?
    fun hideError()
    fun onNetworkDeleted()
    fun onNetworkDetailAvailable(networkInfo: NetworkInfo)
    fun setAutoSecureToggle(autoSecure: Boolean)
    fun setNetworkDetailError(error: String)
    fun setPreferredProtocolToggle(preferredProtocol: Boolean)
    fun setupPortMapAdapter(port: String, portMap: List<String>)
    fun setupProtocolAdapter(protocol: String, mProtocols: Array<String>)
    fun showToast(message: String)
}