/*
 * Copyright (c) 2021 Windscribe Limited.
 */
package com.windscribe.mobile.windscribe

import com.thoughtbot.expandablerecyclerview.models.ExpandableGroup
import com.windscribe.mobile.adapter.ConfigAdapter
import com.windscribe.mobile.adapter.FavouriteAdapter
import com.windscribe.mobile.adapter.RegionsAdapter
import com.windscribe.mobile.adapter.StaticRegionAdapter
import com.windscribe.mobile.adapter.StreamingNodeAdapter
import com.windscribe.mobile.connectionui.ConnectedAnimationState
import com.windscribe.mobile.connectionui.ConnectedState
import com.windscribe.mobile.connectionui.ConnectingAnimationState
import com.windscribe.mobile.connectionui.ConnectingState
import com.windscribe.mobile.connectionui.ConnectionUiState
import com.windscribe.mobile.connectionui.DisconnectedState
import com.windscribe.mobile.windscribe.WindscribeActivity.NetworkLayoutState
import com.windscribe.vpn.localdatabase.tables.NetworkInfo
import com.windscribe.vpn.serverlist.entity.ConfigFile
import com.windscribe.vpn.serverlist.entity.ServerListData
import com.windscribe.vpn.serverlist.interfaces.ListViewClickListener

interface WindscribeView {
    fun exitSearchLayout()
    val flagViewHeight: Int
    val flagViewWidth: Int
    val networkLayoutState: NetworkLayoutState?
    val uiConnectionState: ConnectionUiState?
    fun gotoLoginRegistrationActivity()
    fun handleRateView()
    fun hideProgressView()
    fun hideRecyclerViewProgressBar()
    val isBannedLayoutShown: Boolean
    val isConnectedToNetwork: Boolean
    fun openEditConfigFileDialog(configFile: ConfigFile)
    fun openFileChooser()
    fun openHelpUrl()
    fun openMenuActivity()
    fun openConnectionActivity()
    fun openNewsFeedActivity(showPopUp: Boolean, popUp: Int)
    fun openNodeStatusPage(url: String)
    fun openProvideUsernameAndPasswordDialog(configFile: ConfigFile)
    fun openStaticIPUrl(url: String)
    fun openUpgradeActivity()
    fun performButtonClickHapticFeedback()
    fun performConfirmConnectionHapticFeedback()
    fun scrollTo(scrollTo: Int)
    fun setAdapter(adapter: RegionsAdapter)
    fun setConfigLocListAdapter(configLocListAdapter: ConfigAdapter?)
    fun setConnectionStateText(connectionStateText: String)
    fun setCountryFlag(flagIconResource: Int)
    fun setFavouriteAdapter(favouriteAdapter: FavouriteAdapter?)
    fun setIpAddress(ipAddress: String)
    fun setIpBlur(blur: Boolean)
    fun setNetworkNameBlur(blur: Boolean)
    fun setLastConnectionState(state: ConnectionUiState)
    fun setMainConstraints(customBackground: Boolean)
    fun setNetworkLayout(
        info: NetworkInfo?,
        state: NetworkLayoutState?,
        resetAdapter: Boolean
    )

    fun setPortAndProtocol(protocol: String, port: String)
    fun setRefreshLayout(refreshing: Boolean)
    fun setStaticRegionAdapter(staticRegionAdapter: StaticRegionAdapter)
    fun setStreamingNodeAdapter(streamingNodeAdapter: StreamingNodeAdapter)
    fun setUpLayoutForNodeUnderMaintenance()
    fun setupAccountStatusBanned()
    fun setupAccountStatusExpired()
    fun setupLayoutConnected(state: ConnectedState)
    fun setupLayoutConnecting(state: ConnectingState)
    fun setupLayoutDisconnected(connectionState: DisconnectedState)
    fun setupLayoutDisconnecting(connectionState: String, connectionStateTextColor: Int)
    fun setupLayoutForCustomBackground(path: String)
    fun setupLayoutForFreeUser(dataLeft: String, upgradeLabel: String, color: Int)
    fun setupLayoutForProUser()
    fun setupLayoutForReconnect(connectionState: String, connectionStateTextColor: Int)
    fun setupLayoutUnsecuredNetwork(uiState: ConnectionUiState)
    fun setupPortMapAdapter(savedPort: String, ports: List<String>)
    fun setupProtocolAdapter(savedProtocol: String, protocols: Array<String>)
    fun setupSearchLayout(
        groups: List<ExpandableGroup<*>>,
        serverListData: ServerListData,
        listViewClickListener: ListViewClickListener
    )

    fun showConfigLocAdapterLoadError(errorText: String, configCount: Int)
    fun showDialog(message: String)
    fun showFavouriteAdapterLoadError(errorText: String)
    fun showListBarSelectTransition(resourceSelected: Int)
    fun showNotificationCount(count: Int)
    fun showRecyclerViewProgressBar()
    fun showReloadError(error: String)
    fun showStaticIpAdapterLoadError(errorText: String, buttonText: String, deviceName: String)
    fun showToast(toastMessage: String)
    fun startVpnConnectedAnimation(state: ConnectedAnimationState)
    fun startVpnConnectingAnimation(state: ConnectingAnimationState)
    fun updateLocationName(nodeName: String, nodeNickName: String)
    fun updateProgressView(text: String)
    fun updateSearchAdapter(serverListData: ServerListData)
    fun clearConnectingAnimation()
    fun setDecoyTrafficInfoVisibility(visibility: Int)
    fun showShareLinkDialog()
    fun setupAccountStatusOkay()
    fun setCensorShipIconVisibility(visible: Int)
    fun launchBatteryOptimizationActivity()
}
