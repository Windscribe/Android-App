/*
 * Copyright (c) 2021 Windscribe Limited.
 */
package com.windscribe.tv.serverlist.detail

import com.windscribe.vpn.serverlist.entity.Datacenter
import com.windscribe.tv.serverlist.customviews.State.FavouriteState

interface DetailListener {
    fun onConnectClick(city: Datacenter)
    fun onDisabledClick()
    fun onFavouriteClick(city: Datacenter, state: FavouriteState)
}