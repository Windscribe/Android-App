/*
 * Copyright (c) 2021 Windscribe Limited.
 */
package com.windscribe.mobile.windscribe

import android.content.Context
import android.content.Intent
import android.os.Bundle
import com.windscribe.vpn.serverlist.entity.ConfigFile

interface WindscribePresenter {
    fun contactSupport()
    val lastSelectedTabIndex: Int
    val selectedPort: String
    val selectedProtocol: String
    fun handlePushNotification(extras: Bundle?)
    fun init()
    val isConnectedOrConnecting: Boolean
    val isHapticFeedbackEnabled: Boolean
    fun loadConfigLocations()
    fun logoutFromCurrentSession()
    suspend fun observeNextProtocolToConnect()
    suspend fun observeVPNState()
    fun onAddConfigLocation()
    fun onAddStaticIPClicked()
    fun onAutoSecureInfoClick()
    fun onAutoSecureToggleClick()
    fun onCheckNodeStatusClick()
    fun onConfigFileContentReceived(
            name: String,
            content: String,
            username: String,
            password: String
    )

    fun onConnectClicked()
    fun onConnectedAnimationCompleted()
    fun onConnectingAnimationCompleted()
    fun onDestroy()
    fun onDisconnectIntentReceived()
    fun onHotStart()
    fun onIpClicked()
    fun onLanguageChanged()
    fun onMenuButtonClicked()
    fun onNetworkLayoutCollapsed(checkForReconnect: Boolean)
    fun onNetworkStateChanged()
    fun onNewsFeedItemClick()
    fun onPortSelected(port: String)
    fun onPreferredProtocolInfoClick()
    fun onPreferredProtocolToggleClick()
    fun onProtocolSelected(protocol: String)
    fun onRefreshPingsForAllServers()
    fun onRefreshPingsForConfigServers()
    fun onRefreshPingsForFavouritesServers()
    fun onRefreshPingsForStaticServers()
    fun onRefreshPingsForStreamingServers()
    fun onReloadClick()
    fun onRenewPlanClicked()
    fun onSearchButtonClicked()
    fun onShowAllServerListClicked()
    fun onShowConfigLocListClicked()
    fun onShowFavoritesClicked()
    fun onShowFlixListClicked()
    fun onShowLocationHealthChanged()
    fun onShowStaticIpListClicked()
    fun onSkipNodeCheckingClicked()
    fun onSkipNowClicked()
    fun onStart()
    fun onUpgradeClicked()
    fun registerNetworkInfoListener()
    fun reloadNetworkInfo()
    fun saveLastSelectedTabIndex(index: Int)
    fun saveRateDialogPreference(type: Int)
    fun sendLog()
    fun setMainCustomConstraints()
    fun setProtocolAdapter(protocol: String)
    fun setProtocolPreferred()
    fun setTheme(context: Context)
    fun togglePreferredProtocolLayout()
    fun updateConfigFile(configFile: ConfigFile)
    fun updateConfigFileConnect(configFile: ConfigFile)
    fun updateLatency()
    fun userHasAccess(): Boolean
    fun observeUserData(windscribeActivity: WindscribeActivity)
    suspend fun observeStaticRegions()
    suspend fun observeAllLocations()
    suspend fun observerSelectedLocation()
    suspend fun observeDecoyTrafficState()
    fun setAdapters()
    fun toggleBlurNetworkName()
    fun loadConfigFile(data: Intent)
    fun onDeocoyTrafficClick()
    fun onProtocolChangeClick()
    suspend fun observeConnectedProtocol()
    suspend fun showShareLinkDialog()
}
