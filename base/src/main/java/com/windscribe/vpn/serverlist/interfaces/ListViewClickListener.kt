/*
 * Copyright (c) 2021 Windscribe Limited.
 */
package com.windscribe.vpn.serverlist.interfaces

import com.windscribe.vpn.serverlist.entity.ConfigFile

interface ListViewClickListener {
    fun addToFavourite(cityId: Int)
    fun deleteConfigFile(configFile: ConfigFile)
    fun editConfigFile(file: ConfigFile)
    fun onCityClick(cityId: Int)
    fun onConfigFileClicked(configFile: ConfigFile)
    fun onStaticIpClick(staticIpId: Int)
    fun onUnavailableRegion()
    fun removeFromFavourite(cityId: Int)
    fun setScrollTo(scrollTo: Int)
}