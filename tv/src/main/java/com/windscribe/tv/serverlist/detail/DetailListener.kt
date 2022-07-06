/*
 * Copyright (c) 2021 Windscribe Limited.
 */
package com.windscribe.tv.serverlist.detail

import com.windscribe.vpn.serverlist.entity.City
import com.windscribe.tv.serverlist.customviews.State.FavouriteState

interface DetailListener {
    fun onConnectClick(city: City)
    fun onDisabledClick()
    fun onFavouriteClick(city: City, state: FavouriteState)
}