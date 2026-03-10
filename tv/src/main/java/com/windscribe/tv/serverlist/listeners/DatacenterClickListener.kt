/*
 * Copyright (c) 2021 Windscribe Limited.
 */
package com.windscribe.tv.serverlist.listeners

import com.windscribe.vpn.serverlist.entity.Datacenter
import com.windscribe.tv.serverlist.customviews.State.FavouriteState
import com.windscribe.vpn.serverlist.entity.Location
import com.windscribe.vpn.serverlist.entity.StaticRegion

interface DatacenterClickListener {
    fun onBestLocationClick(cityAndId: Int)
    fun onDisabledClick()
    fun onFavouriteButtonClick(city: Datacenter, state: FavouriteState)
    fun onFavouriteDatacenterClick(city: Datacenter)
    fun onGroupSelected(city: Location)
    fun onStaticIpClick(staticIp: StaticRegion)
}