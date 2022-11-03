/*
 * Copyright (c) 2021 Windscribe Limited.
 */

package com.windscribe.vpn

import com.windscribe.vpn.ActivityInteractorImpl.PortMapLoadCallback
import com.windscribe.vpn.api.IApiCallManager
import com.windscribe.vpn.api.response.ServerNodeListOverLoaded
import com.windscribe.vpn.api.response.UserSessionResponse
import com.windscribe.vpn.apppreference.PreferencesHelper
import com.windscribe.vpn.backend.TrafficCounter
import com.windscribe.vpn.backend.utils.ProtocolManager
import com.windscribe.vpn.backend.utils.WindVpnController
import com.windscribe.vpn.decoytraffic.DecoyTrafficController
import com.windscribe.vpn.localdatabase.tables.*
import com.windscribe.vpn.repository.*
import com.windscribe.vpn.serverlist.entity.*
import com.windscribe.vpn.state.NetworkInfoManager
import com.windscribe.vpn.state.PreferenceChangeObserver
import com.windscribe.vpn.state.VPNConnectionStateManager
import com.windscribe.vpn.workers.WindScribeWorkManager
import io.reactivex.Completable
import io.reactivex.Flowable
import io.reactivex.Single
import io.reactivex.SingleSource
import io.reactivex.disposables.CompositeDisposable
import kotlinx.coroutines.CoroutineScope

/**
 * This interface exposes base module functionality
 * like database, api, preferences etc. to
 * ui modules.
 */
interface ActivityInteractor {
    fun addConfigFile(configFile: ConfigFile): Completable
    fun addNetwork(networkInfo: NetworkInfo): SingleSource<Long>
    fun addNetworkToKnown(networkName: String): Single<Long>
    fun addPing(pingTime: PingTime): Completable
    fun addToFavourites(favourite: Favourite): Single<Long>
    fun deleteConfigFile(id: Int): Completable
    fun deleteFavourite(favourite: Favourite)
    fun getAllCities(): Single<List<City>>
    fun getAllConfigs(): Single<List<ConfigFile>>
    fun getAllPings(): Single<List<PingTime>>
    fun getAllRegion(): Single<List<RegionAndCities>>
    fun getAllStaticRegions(): Single<List<StaticRegion>>
    fun getApiCallManager(): IApiCallManager
    fun getAppPreferenceInterface(): PreferencesHelper
    fun getCityAndRegionByID(cityId: Int): Single<CityAndRegion>
    fun getCityByID(favIds: IntArray): Single<List<City>>
    fun getColorResource(resourceId: Int): Int
    fun getCompositeDisposable(): CompositeDisposable
    fun getConfigFile(id: Int): Single<ConfigFile>
    fun getCurrentUserStatusTable(userName: String): Flowable<UserStatusTable>
    fun getDataLeftString(resourceId: Int, dataRemaining: Float): String
    @Throws(Exception::class)
    fun getEncodedLog(): String
    fun getFavourites(): Single<List<Favourite>>
    fun getLastTimeUpdated(): String
    fun getLowestPingId(): Single<Int>
    fun getMaxPrimaryKey(): Single<Int>
    fun getNetwork(networkName: String): Single<NetworkInfo>
    fun getNotifications(userName: String): Flowable<List<PopupNotificationTable>>
    fun getRateAppPreference(): Int
    fun getResourceString(resourceId: Int): String
    fun getStaticRegionByID(staticId: Int): Single<StaticRegion>
    fun getUserAccountStatus(): Int
    fun getUserSessionData(): Single<UserSessionResponse>
    fun insertOrUpdateUserStatus(userStatusTable: UserStatusTable): Completable
    fun isPremiumUser(): Boolean
    fun isUserEligibleForRatingApp(userSessionResponse: UserSessionResponse): Boolean
    fun loadPortMap(callback: PortMapLoadCallback)
    fun saveRateAppPreference(type: Int)
    fun setRateDialogUpdateTime()
    fun updateNetwork(networkInfo: NetworkInfo): Single<Int>
    fun getWindNotifications(): Single<List<WindNotification>>
    fun getUserSessionDataFromStorage(): Single<UserSessionResponse>
    fun saveConnectionMode(connectionMode: String)
    fun saveProtocol(protocol: String)
    fun saveSTEALTHPort(port: String)
    fun saveWSTunnelPort(port: String)
    fun saveTCPPort(port: String)
    fun saveUDPPort(port: String)
    fun getSavedConnectionMode(): String
    fun getSavedProtocol(): String
    fun getSavedSTEALTHPort(): String
    fun getSavedWSTunnelPort(): String
    fun getSavedTCPPort(): String
    fun getSavedUDPPort(): String
    fun getThemeColor(resourceId: Int): Int
    fun getWireGuardPort(): String
    fun getIKev2Port(): String
    fun getLanguageList(): Array<String>
    fun getSavedLanguage(): String
    fun getSavedSelection(): String
    fun saveSelectedLanguage(lang: String)
    fun saveSelection(selection: String)
    fun updateServerList(): Single<Boolean>
    fun getCurrentUserStatus(): Int
    fun saveNetwork(networkInfo: NetworkInfo): Single<Int>
    fun removeNetwork(networkName: String): Single<Int>
    fun getNetworkInfoUpdated(): Flowable<List<NetworkInfo>>
    fun getNotifications(): Single<List<WindNotification>>
    fun serverDataAvailable(): Single<Boolean>
    fun updateUserData(): Completable
    fun getDebugFilePath(): String
    fun updateServerData(): Completable
    fun getSortList(): Array<String>
    fun getServerStatus(): Single<ServerStatusUpdateTable>
    fun updateServerList(serverStatus: Int): Completable
    fun getPartialLog(): List<String>
    fun getAllCities(regionId: Int): Single<List<City>>
    fun getRegionAndCity(regionId: Int): Single<RegionAndCities>
    fun getAllStaticRegionsAsFlowAble(): Flowable<List<StaticRegion>>
    fun getFavouriteRegionAndCities(favourites: IntArray): Single<List<City>>
    fun getPingResults(): Single<List<PingTestResults>>
    fun getFavoriteServerList(): Single<List<ServerNodeListOverLoaded>>
    fun getUserRepository(): UserRepository
    fun getProtocolManager(): ProtocolManager
    fun getVpnConnectionStateManager(): VPNConnectionStateManager
    fun getNetworkInfoManager(): NetworkInfoManager
    fun getLocationProvider(): LocationRepository
    fun getVPNController(): WindVpnController
    fun getMainScope(): CoroutineScope
    fun getServerListUpdater(): ServerListRepository
    fun getConnectionDataUpdater(): ConnectionDataRepository
    fun getStaticListUpdater(): StaticIpRepository
    fun getPreferenceChangeObserver(): PreferenceChangeObserver
    fun getNotificationUpdater(): NotificationRepository
    fun getWorkManager(): WindScribeWorkManager
    fun getDecoyTrafficController(): DecoyTrafficController
    fun getTrafficCounter(): TrafficCounter
}
