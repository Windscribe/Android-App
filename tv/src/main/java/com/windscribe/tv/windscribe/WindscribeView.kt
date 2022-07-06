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

    fun flashProtocolBadge(flash: Boolean)
    val networkInfo: NetworkInfo?
    fun gotoLoginRegistrationActivity()
    fun handleRateView()
    fun onLogout()
    fun openMenuActivity()
    fun openNewsFeedActivity(showPopUp: Boolean, popUp: Int)
    fun openUpgradeActivity()
    fun quitApplication()
    fun setBadgeIcon(badge: Int, disconnected: Boolean)
    fun setConnectionStateColor(connectionStateColor: Int)
    fun setConnectionStateText(connectionStateText: String)
    fun setCountryFlag(flagIconResource: Int)
    fun setGlowVisibility(visibility: Int)
    fun setIpAddress(ipAddress: String)
    fun setPartialAdapter(serverAdapter: ServerAdapter)
    fun setState(state: Int)
    fun setVpnButtonState()
    fun setupAccountStatusBanned()
    fun setupAccountStatusDowngraded()
    fun setupAccountStatusExpired()
    fun setupLayoutConnected(
        finalColor: Int,
        connectionStateTextColor: Int,
        showSplitTunnelAView: Boolean
    )

    fun setupLayoutConnecting(connectionState: String)
    fun setupLayoutDisconnected(connectionStateTextColor: Int)
    fun setupLayoutDisconnecting(connectionState: String, connectionStateTextColor: Int)
    fun setupLayoutForFreeUser(dataLeft: String, color: Int)
    fun setupLayoutForProUser()
    fun showErrorDialog(error: String)
    fun showNoNetworkDetected(connectionStatus: String, connectionStatusColor: Int)
    fun showPartialViewProgress(inProgress: Boolean)
    fun showSplitViewIcon()
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
