/*
 * Copyright (c) 2021 Windscribe Limited.
 */
package com.windscribe.vpn.localdatabase

import android.database.sqlite.SQLiteException
import com.windscribe.vpn.Windscribe.Companion.appContext
import com.windscribe.vpn.localdatabase.tables.NetworkInfo
import com.windscribe.vpn.localdatabase.tables.PingTestResults
import com.windscribe.vpn.localdatabase.tables.PopupNotificationTable
import com.windscribe.vpn.localdatabase.tables.ServerStatusUpdateTable
import com.windscribe.vpn.localdatabase.tables.UserStatusTable
import com.windscribe.vpn.localdatabase.tables.WindNotification
import com.windscribe.vpn.serverlist.dao.CityAndRegionDao
import com.windscribe.vpn.serverlist.dao.CityDao
import com.windscribe.vpn.serverlist.dao.ConfigFileDao
import com.windscribe.vpn.serverlist.dao.FavouriteDao
import com.windscribe.vpn.serverlist.dao.PingTimeDao
import com.windscribe.vpn.serverlist.dao.RegionAndCitiesDao
import com.windscribe.vpn.serverlist.dao.RegionDao
import com.windscribe.vpn.serverlist.dao.StaticRegionDao
import com.windscribe.vpn.serverlist.entity.City
import com.windscribe.vpn.serverlist.entity.CityAndRegion
import com.windscribe.vpn.serverlist.entity.ConfigFile
import com.windscribe.vpn.serverlist.entity.Favourite
import com.windscribe.vpn.serverlist.entity.PingTime
import com.windscribe.vpn.serverlist.entity.Region
import com.windscribe.vpn.serverlist.entity.RegionAndCities
import com.windscribe.vpn.serverlist.entity.StaticRegion
import com.windscribe.vpn.state.PreferenceChangeObserver
import io.reactivex.Completable
import io.reactivex.Flowable
import io.reactivex.Single
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LocalDatabaseImpl @Inject constructor(
    private val pingTestDao: PingTestDao,
    private val userStatusDao: UserStatusDao,
    private val popupNotificationDao: PopupNotificationDao,
    private val regionDao: RegionDao,
    private val cityDao: CityDao,
    private val cityAndRegionDao: CityAndRegionDao,
    private val configFileDao: ConfigFileDao,
    private val staticRegionsDao: StaticRegionDao,
    private val pingTimeDao: PingTimeDao,
    private val favouriteDao: FavouriteDao,
    private val regionAndCitiesDao: RegionAndCitiesDao,
    private val networkInfoDao: NetworkInfoDao,
    private val serverStatusDao: ServerStatusDao,
    private val preferenceChangeObserver: PreferenceChangeObserver,
    private val windNotificationDao: WindNotificationDao
) : LocalDbInterface {
    override fun addConfig(configFile: ConfigFile): Completable {
        return configFileDao.addConfig(configFile)
    }

    override fun addNetwork(networkInfo: NetworkInfo): Single<Long> {
        return networkInfoDao.addNetwork(networkInfo)
    }

    override fun addPing(pingTime: PingTime): Completable {
        return pingTimeDao.addPingTime(pingTime)
    }

    override fun addStaticRegions(staticRegions: List<StaticRegion>): Completable {
        return staticRegionsDao.addStaticRegions(staticRegions)
    }

    override fun addToCities(cities: List<City>): Completable {
        return cityDao.addCities(cities)
    }

    override fun addToFavourites(favourite: Favourite): Single<Long> {
        return favouriteDao.addToFavourites(favourite)
    }

    override fun addToPopupNotification(popupNotificationTable: PopupNotificationTable) {
        popupNotificationDao.insertPopupNotification(popupNotificationTable)
    }

    override fun addToRegions(regions: List<Region>): Completable {
        return regionDao.addRegions(regions)
    }

    override fun clearAllTables() {
        userStatusDao.clean()
        popupNotificationDao.clean()
        regionDao.clean()
        cityDao.clean()
        configFileDao.clean()
        staticRegionsDao.clean()
        networkInfoDao.clean()
        serverStatusDao.clean()
        windNotificationDao.clean()
    }

    override fun delete(favourite: Favourite) {
        favouriteDao.delete(favourite)
    }

    override suspend fun deleteFavourite(id: Int) {
        favouriteDao.deleteFavourite(id)
    }

    override fun delete(id: Int): Completable {
        return configFileDao.delete(id)
    }

    override fun deleteAllCities(): Completable {
        return cityDao.deleteAll()
    }

    override fun deleteNetwork(networkName: String): Single<Int> {
        return networkInfoDao.delete(networkName)
    }

    override fun getAllCities(id: Int): Single<List<City>> {
        return cityDao.getAllCities(id)
    }

    override val allConfigs: Single<List<ConfigFile>>
        get() = configFileDao.allConfigs
    override val allNetworksWithUpdate: Flowable<List<NetworkInfo>>
        get() = networkInfoDao.allNetworksWithUpdate
    override val allPingTimes: Single<List<PingTime>>
        get() = pingTimeDao.allPings
    override val allPings: Single<List<PingTestResults>>
        get() = pingTestDao.pingList
    override val allRegion: Single<List<RegionAndCities>>
        get() = regionAndCitiesDao.getAllRegion()
    override val allStaticRegions: Single<List<StaticRegion>>
        get() = staticRegionsDao.allStaticRegions
    override val allStaticRegionsFlowAble: Flowable<List<StaticRegion>>
        get() = staticRegionsDao.allStaticRegionsFlowAble
    override val cities: Single<List<City>>
        get() = cityDao.cities
    override val pingableCities: Single<List<City>>
        get() = cityDao.pingableCities

    override fun getCitiesByRegion(regionID: Int, pro: Int): Single<Int> {
        return cityAndRegionDao.getCitiesByRegion(regionID, pro)
    }

    override val city: Single<CityAndRegion>
        get() = cityAndRegionDao.getCity()

    override fun getCityAndRegionByID(cityAndRegionID: Int): Single<CityAndRegion> {
        return cityAndRegionDao.getCityAndRegionByID(cityAndRegionID)
    }

    override fun getCityByID(cityID: Int): Single<City> {
        return cityDao.getCityByID(cityID)
    }

    override fun getCityByID(ids: IntArray?): Single<List<City>> {
        return cityDao.getCityByID(ids)
    }

    override fun getConfigFile(configFileID: Int): Single<ConfigFile> {
        return configFileDao.getConfigFile(configFileID)
    }

    override fun getCordsByRegionId(regionId: Int): Single<String> {
        return cityDao.getCordsByRegionId(regionId)
    }

    override val favourites: Single<List<Favourite>>
        get() = favouriteDao.favourites

    override fun getFreePingIdFromTime(pro: Boolean, pingTime: Int): Single<Int> {
        return pingTimeDao.getFreePingIdFromTime(pro, pingTime)
    }

    override val lowestPing: Single<Int>
        get() = pingTimeDao.lowestPing

    override fun getLowestPingForFreeUser(pro: Boolean): Single<Int> {
        return pingTimeDao.getLowestPingForFreeUser(pro)
    }

    override val lowestPingId: Single<Int>
        get() = pingTimeDao.lowestPingId
    override val maxPrimaryKey: Single<Int>
        get() = configFileDao.maxPrimaryKey

    override fun getNetwork(networkName: String): Single<NetworkInfo> {
        return networkInfoDao.getNetwork(networkName)
    }

    override fun getPingIdFromTime(pingTime: Int): Single<Int> {
        return pingTimeDao.getPingIdFromTime(pingTime)
    }

    override fun getPopupNotifications(userName: String): Flowable<List<PopupNotificationTable>> {
        return popupNotificationDao.getPopupNotification(userName)
    }

    override fun getPopupNotificationsAsFlow(userName: String): Flow<List<PopupNotificationTable>> {
        return popupNotificationDao.getPopupNotificationAsFlow(userName)
    }

    override fun getRegion(id: Int): Single<RegionAndCities> {
        return regionAndCitiesDao.getRegion(id)
    }

    override fun getRegionByCountryCode(countryCode: String): Single<Region> {
        return regionDao.getRegionByCountryCode(countryCode)
    }

    override fun getRegionIdFromCity(cityID: Int): Single<Int> {
        return cityAndRegionDao.getRegionIdFromCity(cityID)
    }

    override fun getServerStatus(username: String): Single<ServerStatusUpdateTable> {
        return serverStatusDao.getServerStatus(username)
    }

    override fun getStaticRegionByID(id: Int): Single<StaticRegion> {
        return staticRegionsDao.getStaticRegionByID(id)
    }

    override val staticRegionCount: Single<Int>
        get() = staticRegionsDao.staticRegionCount

    override fun getUserStatus(username: String): Flowable<UserStatusTable> {
        return userStatusDao.getUserStatusTable(username)
    }

    override val windNotifications: Single<List<WindNotification>>
        get() = windNotificationDao.getWindNotifications()

    override fun insertOrUpdateServerUpdateStatusTable(serverStatusUpdateTable: ServerStatusUpdateTable) {
        preferenceChangeObserver.postCityServerChange()
    }

    override fun insertOrUpdateStatus(serverStatusUpdateTable: ServerStatusUpdateTable): Completable {
        return serverStatusDao.insertOrUpdateStatus(serverStatusUpdateTable)
    }

    override fun insertWindNotifications(windNotifications: List<WindNotification>): Completable {
        return windNotificationDao.insert(windNotifications)
    }

    override fun updateNetwork(networkInfo: NetworkInfo): Single<Int> {
        return networkInfoDao.updateNetwork(networkInfo)
    }

    override fun updateUserStatus(userStatusTable: UserStatusTable?): Completable {
        return userStatusDao.delete()
            .andThen(Completable.fromAction {
                userStatusDao.insertOrUpdateUserStatus(
                    userStatusTable
                )
            })
    }

    override fun getCityAndRegion(cityId: Int): CityAndRegion {
        return cityAndRegionDao.getCityAndRegion(cityId)
    }

    override fun getCountryCode(cityId: Int): String {
        return runCatching {
            cityAndRegionDao.getCityAndRegion(cityId).region.countryCode
        }.getOrDefault("")
    }

    override fun getConfigs(): Flow<List<ConfigFile>> {
        return configFileDao.allConfigsAsFlow
    }

    override fun getFavourites(): Flow<List<Favourite>> {
        return favouriteDao.favouritesAsFlow
    }

    override suspend fun getCityByIDAsync(cityID: Int): City {
        return cityDao.getCityByIDAsync(cityID)
    }

    override fun getLatency(): Flow<List<PingTime>> {
        return pingTimeDao.allPingsAsStateFlow
    }

    override fun getMaxPrimaryKey(): Int {
        return configFileDao.maxPrimaryKeySync
    }

    override fun addConfigSync(configFile: ConfigFile) {
        configFileDao.addConfigSync(configFile)
    }

    override fun deleteCustomConfig(id: Int) {
        configFileDao.deleteCustomConfig(id)
    }

    override val allNetworks: Flow<List<NetworkInfo>>
        get() = networkInfoDao.allNetworks()

    override suspend fun updateNetworkSync(networkInfo: NetworkInfo): Int {
        return networkInfoDao.updateNetworkSync(networkInfo)
    }

    override suspend fun deleteNetworkSync(networkName: String): Int {
        return networkInfoDao.deleteNetworkSync(networkName)
    }
}