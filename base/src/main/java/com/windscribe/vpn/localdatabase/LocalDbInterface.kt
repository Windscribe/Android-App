/*
 * Copyright (c) 2021 Windscribe Limited.
 */
package com.windscribe.vpn.localdatabase

import com.windscribe.vpn.localdatabase.tables.NetworkInfo
import com.windscribe.vpn.localdatabase.tables.PopupNotificationTable
import com.windscribe.vpn.localdatabase.tables.ServerStatusUpdateTable
import com.windscribe.vpn.localdatabase.tables.UserStatusTable
import com.windscribe.vpn.localdatabase.tables.WindNotification
import com.windscribe.vpn.serverlist.entity.City
import com.windscribe.vpn.serverlist.entity.CityAndRegion
import com.windscribe.vpn.serverlist.entity.ConfigFile
import com.windscribe.vpn.serverlist.entity.Favourite
import com.windscribe.vpn.serverlist.entity.PingTime
import com.windscribe.vpn.serverlist.entity.Region
import com.windscribe.vpn.serverlist.entity.RegionAndCities
import com.windscribe.vpn.serverlist.entity.StaticRegion
import kotlinx.coroutines.flow.Flow

interface LocalDbInterface {
    suspend fun addNetwork(networkInfo: NetworkInfo): Long
    suspend fun addPing(pingTime: PingTime)
    suspend fun addStaticRegions(staticRegions: List<StaticRegion>)
    suspend fun addToCities(cities: List<City>)
    fun addToPopupNotification(popupNotificationTable: PopupNotificationTable)
    suspend fun addToRegions(regions: List<Region>)
    fun clearAllTables()
    suspend fun deleteFavourite(id: Int)
    suspend fun getAllConfigs(): List<ConfigFile>
    val allNetworks: Flow<List<NetworkInfo>>
    suspend fun getAllRegionAsync(): List<RegionAndCities>
    suspend fun getPingableCities(): List<City>
    suspend fun getCityByID(ids: IntArray?): List<City>
    fun getNetwork(networkName: String): NetworkInfo?
    fun getPopupNotificationsAsFlow(userName: String): Flow<List<PopupNotificationTable>>
    suspend fun getServerStatus(username: String): ServerStatusUpdateTable
    suspend fun getStaticRegionByIDAsync(id: Int): StaticRegion?
    fun insertOrUpdateServerUpdateStatusTable(serverStatusUpdateTable: ServerStatusUpdateTable)
    suspend fun insertOrUpdateStatus(serverStatusUpdateTable: ServerStatusUpdateTable)
    suspend fun insertOrUpdateStatusAsync(serverStatusUpdateTable: ServerStatusUpdateTable)
    suspend fun insertWindNotifications(windNotifications: List<WindNotification>)
    suspend fun updateUserStatus(userStatusTable: UserStatusTable?)
    fun getCityAndRegion(cityId: Int): CityAndRegion
    fun getConfigs(): Flow<List<ConfigFile>>
    fun getFavourites(): Flow<List<Favourite>>
    suspend fun getCityByIDAsync(cityID: Int): City
    suspend fun getAllCitiesAsync(regionId: Int): List<City>
    suspend fun getAllPingsAsync(): List<PingTime>
    suspend fun getFavouritesAsync(): List<Favourite>
    suspend fun getRegionAsync(regionId: Int): RegionAndCities
    suspend fun addToFavouritesAsync(favourite: Favourite): Long
    fun getLatency(): Flow<List<PingTime>>
    fun getMaxPrimaryKey(): Int
    fun addConfigSync(configFile: ConfigFile)
    fun deleteCustomConfig(id: Int)
    suspend fun updateNetworkSync(networkInfo: NetworkInfo): Int
    suspend fun deleteNetworkSync(networkName: String): Int
    fun getCountryCode(cityId: Int): String
    suspend fun getCitiesAsync(): List<City>
    suspend fun getWindNotifications(): List<WindNotification>
    suspend fun getAllStaticRegions(): List<StaticRegion>
    suspend fun getRegionIdFromCityAsync(cityID: Int): Int
    suspend fun getLowestPingIdAsync(): Int
    suspend fun getConfigFileAsync(configFileID: Int): ConfigFile
}