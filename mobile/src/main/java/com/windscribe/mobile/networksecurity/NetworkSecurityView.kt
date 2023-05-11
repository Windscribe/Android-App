/*
 * Copyright (c) 2021 Windscribe Limited.
 */
package com.windscribe.mobile.networksecurity

import com.windscribe.vpn.localdatabase.tables.NetworkInfo

interface NetworkSecurityView {
    fun hideProgress()
    fun setupCurrentNetwork(networkInfo: NetworkInfo)
    fun onAdapterLoadFailed(showUpdate: String)
    fun openNetworkSecurityDetails(networkName: String)
    fun setAdapter(mNetworkList: List<NetworkInfo>?)
    fun showProgress(progressTitle: String)
    fun setAutoSecureToggle(resourceId: Int)
    fun hideCurrentNetwork()
}