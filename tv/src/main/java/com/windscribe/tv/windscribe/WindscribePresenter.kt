/*
 * Copyright (c) 2021 Windscribe Limited.
 */

package com.windscribe.tv.windscribe

import com.windscribe.vpn.api.response.ServerCredentialsResponse
import kotlinx.coroutines.CoroutineScope

interface WindscribePresenter {
    fun bind(
        view: WindscribeView,
        scope: CoroutineScope,
    )

    fun connectWithSelectedStaticIp(
        regionID: Int,
        serverCredentialsResponse: ServerCredentialsResponse,
    )

    fun connectWithSelectedLocation(cityID: Int)

    fun init()

    fun logout()

    fun onBackPressed()

    fun onConnectClicked()

    fun onDestroy()

    fun onDisconnectIntentReceived()

    fun onHotStart()

    fun onMenuButtonClicked()

    suspend fun observeVPNState()

    suspend fun observeUserState()

    suspend fun observeIpAddress()

    suspend fun observeConnectedProtocol()

    suspend fun observeServerList()

    suspend fun observeSelectedLocation()

    suspend fun observeDisconnectedProtocol()

    suspend fun observeNetworkEvents()
}
