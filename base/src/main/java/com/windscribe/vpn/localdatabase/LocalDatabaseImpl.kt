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
import com.windscribe.vpn.serverlist.dao.DatacenterAndLocationDao
import com.windscribe.vpn.serverlist.dao.DatacenterDao
import com.windscribe.vpn.serverlist.dao.ConfigFileDao
import com.windscribe.vpn.serverlist.dao.FavouriteDao
import com.windscribe.vpn.serverlist.dao.PingTimeDao
import com.windscribe.vpn.serverlist.dao.LocationAndDatacentersDao
import com.windscribe.vpn.serverlist.dao.LocationDao
import com.windscribe.vpn.serverlist.dao.ServerDao
import com.windscribe.vpn.serverlist.dao.StaticRegionDao
import com.windscribe.vpn.serverlist.entity.Datacenter
import com.windscribe.vpn.serverlist.entity.DatacenterAndLocation
import com.windscribe.vpn.serverlist.entity.ConfigFile
import com.windscribe.vpn.serverlist.entity.Favourite
import com.windscribe.vpn.serverlist.entity.PingTime
import com.windscribe.vpn.serverlist.entity.Location
import com.windscribe.vpn.serverlist.entity.LocationAndDatacenters
import com.windscribe.vpn.serverlist.entity.Server
import com.windscribe.vpn.serverlist.entity.StaticRegion
import com.windscribe.vpn.state.PreferenceChangeObserver
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LocalDatabaseImpl @Inject constructor(
    private val userStatusDao: UserStatusDao,
    private val popupNotificationDao: PopupNotificationDao,
    private val locationDao: LocationDao,
    private val serverDao: ServerDao,
    private val datacenterDao: DatacenterDao,
    private val datacenterAndLocationDao: DatacenterAndLocationDao,
    private val configFileDao: ConfigFileDao,
    private val staticRegionsDao: StaticRegionDao,
    private val pingTimeDao: PingTimeDao,
    private val favouriteDao: FavouriteDao,
    private val locationAndDatacentersDao: LocationAndDatacentersDao,
    private val networkInfoDao: NetworkInfoDao,
    private val serverStatusDao: ServerStatusDao,
    private val preferenceChangeObserver: PreferenceChangeObserver,
    private val windNotificationDao: WindNotificationDao,
    private val unblockWgDao: UnblockWgDao
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

    override suspend fun getAllLocationsAsync(): List<LocationAndDatacenters> {
        return locationAndDatacentersDao.getAllLocationsAsync()
    }

    override suspend fun getDatacenterByID(ids: IntArray?): List<Datacenter> {
        return datacenterDao.getCityByID(ids)
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

    override suspend fun getDatacenterByIDAsync(datacenterID: Int): Datacenter {
        return datacenterDao.getCityByIDAsync(datacenterID)
    }

    override suspend fun getAllDatacentersAsync(locationId: Int): List<Datacenter> {
        return datacenterDao.getAllCitiesAsync(locationId)
    }

    override suspend fun getAllPingsAsync(): List<PingTime> {
        return pingTimeDao.getAllPingsAsync()
    }

    override suspend fun getFavouritesAsync(): List<Favourite> {
        return favouriteDao.getFavouritesAsync()
    }

    override suspend fun getLocationAsync(locationId: Int): LocationAndDatacenters {
        return locationAndDatacentersDao.getLocationAsync(locationId)
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

    override suspend fun getDatacentersAsync(): List<Datacenter> {
        return datacenterDao.getCitiesAsync()
    }

    override suspend fun getLocationIdFromDatacenterAsync(datacenterID: Int): Int {
        return datacenterAndLocationDao.getLocationIdFromDatacenterAsync(datacenterID)
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

    override suspend fun popupNotificationExists(notificationId: Int): Boolean {
        return popupNotificationDao.getPopupNotificationId(notificationId) != null
    }

    override fun clearAllTables() {
        userStatusDao.clean()
        popupNotificationDao.clean()
        locationDao.clean()
        datacenterDao.clean()
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

    override fun getDatacenterAndLocation(datacenterId: Int): DatacenterAndLocation? {
        return datacenterAndLocationDao.getDatacenterAndLocation(datacenterId)
    }

    override fun getCountryCode(datacenterId: Int): String {
        return runCatching {
            datacenterAndLocationDao.getDatacenterAndLocation(datacenterId)?.location?.countryCode ?: ""
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

    override suspend fun addToDatacenters(datacenters: List<Datacenter>) {
        return datacenterDao.addCities(datacenters)
    }

    override suspend fun addToLocations(locations: List<Location>) {
        return locationDao.addRegions(locations)
    }

    override suspend fun getPingableDatacenters(): List<Datacenter> {
        return datacenterDao.getPingableCities()
    }

    override suspend fun getWindNotifications(): List<WindNotification> {
        return windNotificationDao.getWindNotifications()
    }

    override fun observeNotifications(): Flow<List<WindNotification>> {
        return windNotificationDao.observeNotifications()
    }

    override suspend fun isNotificationRead(notificationId: Int): Boolean {
        return windNotificationDao.isRead(notificationId) ?: false
    }

    override suspend fun markNotificationAsRead(notificationId: Int) {
        windNotificationDao.markAsRead(notificationId)
    }

    override fun markPopupAsShown(notificationId: Int) {
        popupNotificationDao.markPopupAsShown(notificationId)
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

    override suspend fun deleteUnblockWgParams() {
        unblockWgDao.deleteAll()
    }

    override suspend fun insertUnblockWgParams(unblockWgParams: List<UnBlockWgParam>) {
        unblockWgDao.insert(unblockWgParams)
    }

    override suspend fun getUnblockWgParams(): Flow<List<UnBlockWgParam>> {
        return unblockWgDao.getUnblockWgParams()
    }

    override suspend fun addServers(servers: List<Server>) {
        return serverDao.addAll(servers)
    }

    override suspend fun deleteServers(serverIds: List<Int>) {
        return serverDao.deleteByIds(serverIds)
    }

    override suspend fun deleteAllServers() {
        return serverDao.deleteAll()
    }

    override suspend fun getServersByDatacenter(datacenterId: Int): List<Server> {
        return serverDao.getServersByDatacenter(datacenterId)
    }

    override suspend fun getServerById(serverId: Int): Server? {
        return serverDao.getServerById(serverId)
    }

    override suspend fun getAllServers(): List<Server> {
        return serverDao.getAllServers()
    }

    override fun observeAllServers(): Flow<List<Server>> {
        return serverDao.observeAllServers()
    }

    override suspend fun getLocationById(locationId: Int): Location? {
        return runCatching {
            locationDao.getRegionById(locationId)
        }.getOrNull()
    }

    override suspend fun getPingIpAndHost(id: Int): Pair<String, String>? {
        val server = getServersByDatacenter(id)
            .randomOrNull() ?: return null
        val ip = server.ip.takeIf { it.isNotBlank() } ?: return null
        val host = server.hostname.takeIf { it.isNotBlank() } ?: return null
        return Pair(ip, "http://${host}:6464/latency")
    }
}