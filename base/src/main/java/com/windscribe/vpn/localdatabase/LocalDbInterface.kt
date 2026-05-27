/*
 * Copyright (c) 2021 Windscribe Limited.
 */
package com.windscribe.vpn.localdatabase

import com.windscribe.vpn.localdatabase.tables.NetworkInfo
import com.windscribe.vpn.localdatabase.tables.PopupNotificationTable
import com.windscribe.vpn.localdatabase.tables.ServerStatusUpdateTable
import com.windscribe.vpn.localdatabase.tables.UserStatusTable
import com.windscribe.vpn.localdatabase.tables.UnBlockWgParam
import com.windscribe.vpn.localdatabase.tables.WindNotification
import com.windscribe.vpn.serverlist.entity.Datacenter
import com.windscribe.vpn.serverlist.entity.DatacenterAndLocation
import com.windscribe.vpn.serverlist.entity.ConfigFile
import com.windscribe.vpn.serverlist.entity.Favourite
import com.windscribe.vpn.serverlist.entity.PingTime
import com.windscribe.vpn.serverlist.entity.Location
import com.windscribe.vpn.serverlist.entity.LocationAndDatacenters
import com.windscribe.vpn.serverlist.entity.Server
import com.windscribe.vpn.serverlist.entity.StaticRegion
import kotlinx.coroutines.flow.Flow

interface LocalDbInterface {
    suspend fun addNetwork(networkInfo: NetworkInfo): Long
    suspend fun addPing(pingTime: PingTime)
    suspend fun addStaticRegions(staticRegions: List<StaticRegion>)
    suspend fun addToDatacenters(datacenters: List<Datacenter>)
    suspend fun addServers(servers: List<Server>)
    suspend fun deleteServers(serverIds: List<Int>)
    suspend fun deleteAllServers()
    suspend fun getServersByDatacenter(datacenterId: Int): List<Server>
    suspend fun getServerById(serverId: Int): Server?
    suspend fun getAllServers(): List<Server>
    fun observeAllServers(): Flow<List<Server>>
    fun addToPopupNotification(popupNotificationTable: PopupNotificationTable)
    suspend fun popupNotificationExists(notificationId: Int): Boolean
    suspend fun addToLocations(locations: List<Location>)
    fun clearAllTables()
    suspend fun deleteFavourite(id: Int)
    suspend fun getAllConfigs(): List<ConfigFile>
    val allNetworks: Flow<List<NetworkInfo>>
    suspend fun getAllLocationsAsync(): List<LocationAndDatacenters>
    suspend fun getPingableDatacenters(): List<Datacenter>
    suspend fun getDatacenterByID(ids: IntArray?): List<Datacenter>
    fun getNetwork(networkName: String): NetworkInfo?
    fun getPopupNotificationsAsFlow(userName: String): Flow<List<PopupNotificationTable>>
    suspend fun getServerStatus(username: String): ServerStatusUpdateTable
    suspend fun getStaticRegionByIDAsync(id: Int): StaticRegion?
    suspend fun insertOrUpdateStatus(serverStatusUpdateTable: ServerStatusUpdateTable)
    suspend fun insertOrUpdateStatusAsync(serverStatusUpdateTable: ServerStatusUpdateTable)
    suspend fun insertWindNotifications(windNotifications: List<WindNotification>)
    suspend fun clearWindNotifications()
    suspend fun updateUserStatus(userStatusTable: UserStatusTable?)
    fun getDatacenterAndLocation(datacenterId: Int): DatacenterAndLocation?
    fun getConfigs(): Flow<List<ConfigFile>>
    fun getFavourites(): Flow<List<Favourite>>
    suspend fun getDatacenterByIDAsync(datacenterID: Int): Datacenter
    suspend fun getAllDatacentersAsync(locationId: Int): List<Datacenter>
    suspend fun getAllPingsAsync(): List<PingTime>
    suspend fun getFavouritesAsync(): List<Favourite>
    suspend fun getLocationAsync(locationId: Int): LocationAndDatacenters
    suspend fun addToFavouritesAsync(favourite: Favourite): Long
    fun getLatency(): Flow<List<PingTime>>
    fun getMaxPrimaryKey(): Int
    fun addConfigSync(configFile: ConfigFile)
    fun deleteCustomConfig(id: Int)
    suspend fun updateNetworkSync(networkInfo: NetworkInfo): Int
    suspend fun deleteNetworkSync(networkName: String): Int
    fun getCountryCode(datacenterId: Int): String
    suspend fun getDatacentersAsync(): List<Datacenter>
    suspend fun getWindNotifications(): List<WindNotification>
    fun observeNotifications(): Flow<List<WindNotification>>
    suspend fun isNotificationRead(notificationId: Int): Boolean
    suspend fun markNotificationAsRead(notificationId: Int)
    fun markPopupAsShown(notificationId: Int)
    suspend fun getAllStaticRegions(): List<StaticRegion>
    suspend fun getLocationIdFromDatacenterAsync(datacenterID: Int): Int
    suspend fun getLowestPingIdAsync(): Int
    suspend fun getConfigFileAsync(configFileID: Int): ConfigFile
    suspend fun deleteUnblockWgParams()
    suspend fun insertUnblockWgParams(unblockWgParams: List<UnBlockWgParam>)
    suspend fun getUnblockWgParams(): Flow<List<UnBlockWgParam>>
    suspend fun getLocationById(locationId: Int): Location?
    suspend fun getPingIpAndHost(id: Int): Pair<String, String>?
}