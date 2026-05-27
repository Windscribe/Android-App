/*
 * Copyright (c) 2021 Windscribe Limited.
 */
package com.windscribe.tv.serverlist.detail

import com.windscribe.tv.serverlist.customviews.State.FavouriteState
import com.windscribe.vpn.serverlist.entity.Datacenter

interface DetailListener {
    fun onConnectClick(city: Datacenter)

    fun onDisabledClick()

    fun onFavouriteClick(
        city: Datacenter,
        state: FavouriteState,
    )
}
