/*
 * Copyright (c) 2021 Windscribe Limited.
 */
package com.windscribe.tv.serverlist.listeners

import com.windscribe.vpn.serverlist.entity.City
import com.windscribe.tv.serverlist.customviews.State.FavouriteState
import com.windscribe.vpn.serverlist.entity.Region
import com.windscribe.vpn.serverlist.entity.StaticRegion

interface NodeClickListener {
    fun onBestLocationClick(cityAndId: Int)
    fun onDisabledClick()
    fun onFavouriteButtonClick(city: City, state: FavouriteState)
    fun onFavouriteNodeCLick(city: City)
    fun onGroupSelected(city: Region)
    fun onStaticIpClick(staticIp: StaticRegion)
}