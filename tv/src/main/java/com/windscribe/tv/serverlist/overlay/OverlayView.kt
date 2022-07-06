/*
 * Copyright (c) 2021 Windscribe Limited.
 */
package com.windscribe.tv.serverlist.overlay

import com.windscribe.tv.serverlist.adapters.FavouriteAdapter
import com.windscribe.tv.serverlist.adapters.ServerAdapter
import com.windscribe.tv.serverlist.adapters.StaticIpAdapter

interface OverlayView {
    fun onDisabledNodeClick()
    fun onLocationSelected(regionId: Int)
    fun onNodeSelected(cityID: Int)
    fun onStaticSelected(regionID: Int, userNameEncoded: String, passwordEncoded: String)
    fun setAllAdapter(serverAdapter: ServerAdapter)
    fun setFavouriteAdapter(favouriteAdapter: FavouriteAdapter)
    fun setState(state: LoadState, stateDrawable: Int, stateText: Int, fragmentIndex: Int)
    fun setStaticAdapter(staticAdapter: StaticIpAdapter)
    fun setWindAdapter(serverAdapter: ServerAdapter)
    fun showToast(text: String)
}