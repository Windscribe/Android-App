/*
 * Copyright (c) 2021 Windscribe Limited.
 */

package com.windscribe.tv.windscribe

import android.net.NetworkInfo
import com.windscribe.tv.serverlist.adapters.ServerAdapter

interface WindscribeView {
    interface ConnectionStateAnimationListener {

        fun onConnectedAnimationCompleted()
        fun onConnectingAnimationCompleted()
    }
    val networkInfo: NetworkInfo?
    fun gotoLoginRegistrationActivity()
    fun handleRateView()
    fun onLogout()
    fun openMenuActivity()
    fun openNewsFeedActivity(showPopUp: Boolean, popUp: Int)
    fun openUpgradeActivity()
    fun quitApplication()
    fun setProtocolAndPortInfo(protocol: String, port: String, disconnected: Boolean)
    fun setCountryFlag(flagIconResource: Int)
    fun setGlowVisibility(visibility: Int)
    fun setIpAddress(ipAddress: String)
    fun setPartialAdapter(serverAdapter: ServerAdapter)
    fun setState(state: Int)
    fun setVpnButtonState()
    fun setupAccountStatusBanned()
    fun setupAccountStatusDowngraded()
    fun setupAccountStatusExpired()
    fun setupLayoutConnecting()
    fun setupLayoutDisconnected()
    fun setupLayoutDisconnecting()
    fun setupLayoutForFreeUser(dataLeft: String, color: Int)
    fun setupLayoutForProUser()
    fun showErrorDialog(error: String)
    fun showPartialViewProgress(inProgress: Boolean)
    fun showSplitViewIcon(show: Boolean)
    fun showToast(toastMessage: String)
    fun startSessionServiceScheduler()
    fun startVpnConnectedAnimation(
        connectionStateString: String,
        backgroundColorStart: Int,
        backgroundColorFinal: Int,
        textColorStart: Int,
        textColorFinal: Int,
        listenerState: ConnectionStateAnimationListener
    )
    fun startVpnConnectingAnimation(
        connectionStateString: String,
        flagIcon: Int,
        backgroundColorStart: Int,
        backgroundColorFinal: Int,
        textColorStart: Int,
        textColorFinal: Int,
        listenerState: ConnectionStateAnimationListener
    )
    fun updateLocationName(nodeName: String, nodeNickName: String)
}
