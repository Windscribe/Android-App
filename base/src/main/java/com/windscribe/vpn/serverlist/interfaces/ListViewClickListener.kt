/*
 * Copyright (c) 2021 Windscribe Limited.
 */
package com.windscribe.vpn.serverlist.interfaces

import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import com.windscribe.vpn.serverlist.entity.ConfigFile

interface ListViewClickListener {
    fun addToFavourite(cityId: Int, position: Int, adapter: RecyclerView.Adapter<ViewHolder>)
    fun deleteConfigFile(configFile: ConfigFile)
    fun editConfigFile(file: ConfigFile)
    fun onCityClick(cityId: Int)
    fun onConfigFileClicked(configFile: ConfigFile)
    fun onStaticIpClick(staticIpId: Int)
    fun onUnavailableRegion()
    fun removeFromFavourite(cityId: Int, position: Int, adapter: RecyclerView.Adapter<ViewHolder>)
    fun setScrollTo(scrollTo: Int)
}