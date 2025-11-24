/*
 * Copyright (c) 2021 Windscribe Limited.
 */
package com.windscribe.vpn.localdatabase

import com.windscribe.vpn.localdatabase.tables.NetworkInfo
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
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LocalDatabaseImpl @Inject constructor(
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

    // Suspend functions (Coroutines)
    override suspend fun addNetwork(networkInfo: NetworkInfo): Long {
        return networkInfoDao.addNetwork(networkInfo)
    }

    override suspend fun addPing(pingTime: PingTime) {
        pingTimeDao.addPing(pingTime)
    }

    override suspend fun addStaticRegions(staticRegions: List<StaticRegion>) {
        staticRegionsDao.addStaticRegions(staticRegions)
    }

    override suspend fun deleteFavourite(id: Int) {
        favouriteDao.deleteFavourite(id)
    }

    override suspend fun getAllRegionAsync(): List<RegionAndCities> {
        return regionAndCitiesDao.getAllRegionAsync()
    }

    override suspend fun getCityByID(ids: IntArray?): List<City> {
        return cityDao.getCityByID(ids)
    }

    override suspend fun getServerStatus(username: String): ServerStatusUpdateTable {
        return serverStatusDao.getServerStatus(username)
    }

    override suspend fun getStaticRegionByIDAsync(id: Int): StaticRegion? {
        return staticRegionsDao.getStaticRegionByIDAsync(id)
    }

    override suspend fun insertOrUpdateStatus(serverStatusUpdateTable: ServerStatusUpdateTable) {
        return serverStatusDao.insertOrUpdateStatus(serverStatusUpdateTable)
    }

    override suspend fun insertOrUpdateStatusAsync(serverStatusUpdateTable: ServerStatusUpdateTable) {
        serverStatusDao.insertOrUpdateStatusAsync(serverStatusUpdateTable)
    }

    override suspend fun updateUserStatus(userStatusTable: UserStatusTable?) {
        try {
            userStatusDao.delete()
        } catch (_: Exception) {
        }
        if (userStatusTable != null)
            userStatusDao.insertOrUpdateUserStatus(userStatusTable)
    }

    override suspend fun getCityByIDAsync(cityID: Int): City {
        return cityDao.getCityByIDAsync(cityID)
    }

    override suspend fun getAllCitiesAsync(regionId: Int): List<City> {
        return cityDao.getAllCitiesAsync(regionId)
    }

    override suspend fun getAllPingsAsync(): List<PingTime> {
        return pingTimeDao.getAllPingsAsync()
    }

    override suspend fun getFavouritesAsync(): List<Favourite> {
        return favouriteDao.getFavouritesAsync()
    }

    override suspend fun getRegionAsync(regionId: Int): RegionAndCities {
        return regionAndCitiesDao.getRegionAsync(regionId)
    }

    override suspend fun addToFavouritesAsync(favourite: Favourite): Long {
        return favouriteDao.addToFavouritesAsync(favourite)
    }

    override suspend fun updateNetworkSync(networkInfo: NetworkInfo): Int {
        return networkInfoDao.updateNetworkSync(networkInfo)
    }

    override suspend fun deleteNetworkSync(networkName: String): Int {
        return networkInfoDao.deleteNetworkSync(networkName)
    }

    override suspend fun getCitiesAsync(): List<City> {
        return cityDao.getCitiesAsync()
    }

    override suspend fun getRegionIdFromCityAsync(cityID: Int): Int {
        return cityAndRegionDao.getRegionIdFromCityAsync(cityID)
    }

    override suspend fun getLowestPingIdAsync(): Int {
        return pingTimeDao.getLowestPingIdAsync()
    }

    override suspend fun getConfigFileAsync(configFileID: Int): ConfigFile {
        return configFileDao.getConfigFileAsync(configFileID)
    }

    override fun getPopupNotificationsAsFlow(userName: String): Flow<List<PopupNotificationTable>> {
        return popupNotificationDao.getPopupNotificationAsFlow(userName)
    }

    override fun getConfigs(): Flow<List<ConfigFile>> {
        return configFileDao.allConfigsAsFlow
    }

    override fun getFavourites(): Flow<List<Favourite>> {
        return favouriteDao.favouritesAsFlow
    }

    override fun getLatency(): Flow<List<PingTime>> {
        return pingTimeDao.allPingsAsStateFlow
    }

    override val allNetworks: Flow<List<NetworkInfo>>
        get() = networkInfoDao.allNetworks()

    override fun addToPopupNotification(popupNotificationTable: PopupNotificationTable) {
        popupNotificationDao.insertPopupNotification(popupNotificationTable)
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

    override fun getNetwork(networkName: String): NetworkInfo? {
        return networkInfoDao.getNetwork(networkName)
    }

    override fun insertOrUpdateServerUpdateStatusTable(serverStatusUpdateTable: ServerStatusUpdateTable) {
        preferenceChangeObserver.postCityServerChange()
    }

    override fun getCityAndRegion(cityId: Int): CityAndRegion {
        return cityAndRegionDao.getCityAndRegion(cityId)
    }

    override fun getCountryCode(cityId: Int): String {
        return runCatching {
            cityAndRegionDao.getCityAndRegion(cityId).region.countryCode
        }.getOrDefault("")
    }

    override fun getMaxPrimaryKey(): Int {
        return configFileDao.maxPrimaryKeySync ?: 0
    }

    override fun addConfigSync(configFile: ConfigFile) {
        configFileDao.addConfigSync(configFile)
    }

    override fun deleteCustomConfig(id: Int) {
        configFileDao.deleteCustomConfig(id)
    }

    override suspend fun addToCities(cities: List<City>) {
        return cityDao.addCities(cities)
    }

    override suspend fun addToRegions(regions: List<Region>) {
        return regionDao.addRegions(regions)
    }

    override suspend fun getPingableCities(): List<City> {
        return cityDao.getPingableCities()
    }

    override suspend fun getWindNotifications(): List<WindNotification> {
        return windNotificationDao.getWindNotifications()
    }

    override suspend fun insertWindNotifications(windNotifications: List<WindNotification>) {
        windNotificationDao.insert(windNotifications)
    }

    override suspend fun clearWindNotifications() {
        windNotificationDao.cleanAsync()
    }

    override suspend fun getAllStaticRegions(): List<StaticRegion> {
        return staticRegionsDao.getAllStaticRegions()
    }

    override suspend fun getAllConfigs(): List<ConfigFile> {
        return configFileDao.getAllConfigs()
    }
}