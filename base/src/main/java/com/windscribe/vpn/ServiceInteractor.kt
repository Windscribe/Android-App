/*
 * Copyright (c) 2021 Windscribe Limited.
 */

package com.windscribe.vpn

import com.windscribe.vpn.api.IApiCallManager
import com.windscribe.vpn.api.response.GenericSuccess
import com.windscribe.vpn.api.response.UserSessionResponse
import com.windscribe.vpn.apppreference.PreferencesHelper
import com.windscribe.vpn.localdatabase.tables.NetworkInfo
import com.windscribe.vpn.localdatabase.tables.UserStatusTable
import com.windscribe.vpn.repository.CallResult
import com.windscribe.vpn.serverlist.entity.*
import io.reactivex.Completable
import io.reactivex.Single
import io.reactivex.disposables.CompositeDisposable

/**
 * This interface exposes base module functionality
 * like database, api, preferences etc. to
 * app level components like services, workers and broadcast receivers.
 */
interface ServiceInteractor {
    val apiManager: IApiCallManager
    val compositeDisposable: CompositeDisposable
    val preferenceHelper: PreferencesHelper
    fun getResourceString(resId: Int): String
    val savedLocale: String
    fun insertOrUpdateUserStatus(userStatusTable: UserStatusTable): Completable
    fun addPing(pingTime: PingTime): Completable
    fun getAllCities(): Single<List<City>>
    fun getAllRegion(): Single<List<RegionAndCities>>
    fun getAllFavourites(): Single<List<Favourite>>
    fun getCity(id: Int): Single<City>
    fun getAllStaticRegions(): Single<List<StaticRegion>>
    fun getLowestPingId(): Single<Int>
    fun getStaticIpCount(): Int
    fun addNetworkToKnown(networkName: String): Single<Long>
    fun getNetwork(networkName: String): Single<NetworkInfo>
    suspend fun getUserSession(): UserSessionResponse
    fun getCoordinates(countryCode: String): Single<Array<String>>
    fun getCityAndRegionByID(cityId: Int): CityAndRegion
    fun getConfigFile(id: Int): Single<ConfigFile>
    fun addConfigFile(configFile: ConfigFile): Completable
    fun getStaticRegionByID(staticId: Int): Single<StaticRegion>
    suspend fun sendLog(): CallResult<GenericSuccess>
    suspend fun clearData()
    fun saveNetwork(networkInfo: NetworkInfo): Single<Int>
    suspend fun getAllConfigs(): List<ConfigFile>
}
