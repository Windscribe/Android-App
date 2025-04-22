/*
 * Copyright (c) 2021 Windscribe Limited.
 */
package com.windscribe.vpn.localdatabase

import com.windscribe.vpn.localdatabase.tables.NetworkInfo
import com.windscribe.vpn.localdatabase.tables.PingTestResults
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
import io.reactivex.Completable
import io.reactivex.Flowable
import io.reactivex.Single
import kotlinx.coroutines.flow.Flow

interface LocalDbInterface {
    fun addConfig(configFile: ConfigFile): Completable
    fun addNetwork(networkInfo: NetworkInfo): Single<Long>
    fun addPing(pingTime: PingTime): Completable
    fun addStaticRegions(staticRegions: List<StaticRegion>): Completable
    fun addToCities(cities: List<City>): Completable
    fun addToFavourites(favourite: Favourite): Single<Long>
    fun addToPopupNotification(popupNotificationTable: PopupNotificationTable)
    fun addToRegions(regions: List<Region>): Completable
    fun clearAllTables()
    fun delete(favourite: Favourite)
    suspend fun deleteFavourite(id: Int)
    fun delete(id: Int): Completable
    fun deleteAllCities(): Completable
    fun deleteNetwork(networkName: String): Single<Int>
    fun getAllCities(id: Int): Single<List<City>>
    val allConfigs: Single<List<ConfigFile>>
    val allNetworksWithUpdate: Flowable<List<NetworkInfo>>
    val allPingTimes: Single<List<PingTime>>
    val allPings: Single<List<PingTestResults>>
    val allRegion: Single<List<RegionAndCities>>
    val allStaticRegions: Single<List<StaticRegion>>
    val allStaticRegionsFlowAble: Flowable<List<StaticRegion>>
    val cities: Single<List<City>>
    val pingableCities: Single<List<City>>
    fun getCitiesByRegion(regionID: Int, pro: Int): Single<Int>
    val city: Single<CityAndRegion>
    fun getCityAndRegionByID(cityAndRegionID: Int): Single<CityAndRegion>
    fun getCityByID(cityID: Int): Single<City>
    fun getCityByID(ids: IntArray?): Single<List<City>>
    fun getConfigFile(configFileID: Int): Single<ConfigFile>
    fun getCordsByRegionId(regionId: Int): Single<String>
    val favourites: Single<List<Favourite>>
    fun getFreePingIdFromTime(pro: Boolean, pingTime: Int): Single<Int>
    val lowestPing: Single<Int>
    fun getLowestPingForFreeUser(pro: Boolean): Single<Int>
    val lowestPingId: Single<Int>
    val maxPrimaryKey: Single<Int>
    fun getNetwork(networkName: String): Single<NetworkInfo>
    fun getPingIdFromTime(pingTime: Int): Single<Int>
    fun getPopupNotifications(userName: String): Flowable<List<PopupNotificationTable>>
    fun getRegion(id: Int): Single<RegionAndCities>
    fun getRegionByCountryCode(countryCode: String): Single<Region>
    fun getRegionIdFromCity(cityID: Int): Single<Int>
    fun getServerStatus(username: String): Single<ServerStatusUpdateTable>
    fun getStaticRegionByID(id: Int): Single<StaticRegion>
    val staticRegionCount: Single<Int>
    fun getUserStatus(username: String): Flowable<UserStatusTable>
    val windNotifications: Single<List<WindNotification>>
    fun insertOrUpdateServerUpdateStatusTable(serverStatusUpdateTable: ServerStatusUpdateTable)
    fun insertOrUpdateStatus(serverStatusUpdateTable: ServerStatusUpdateTable): Completable
    fun insertWindNotifications(windNotifications: List<WindNotification>): Completable
    fun updateNetwork(networkInfo: NetworkInfo): Single<Int>
    fun updateUserStatus(userStatusTable: UserStatusTable?): Completable
    fun getCityAndRegion(cityId: Int): CityAndRegion
    fun getConfigs(): Flow<List<ConfigFile>>
    fun getFavourites(): Flow<List<Favourite>>
    suspend fun getCityByIDAsync(cityID: Int): City
    fun getLatency(): Flow<List<PingTime>>
    fun getMaxPrimaryKey(): Int
    fun addConfigSync(configFile: ConfigFile)
    fun getPopupNotificationsAsFlow(userName: String): Flow<List<PopupNotificationTable>>
    fun deleteCustomConfig(id: Int)
}