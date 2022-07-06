/*
 * Copyright (c) 2021 Windscribe Limited.
 */

package com.windscribe.vpn

import com.google.gson.Gson
import com.windscribe.vpn.api.IApiCallManager
import com.windscribe.vpn.api.response.UserSessionResponse
import com.windscribe.vpn.apppreference.PreferencesHelper
import com.windscribe.vpn.constants.PreferencesKeyConstants
import com.windscribe.vpn.localdatabase.LocalDbInterface
import com.windscribe.vpn.localdatabase.tables.NetworkInfo
import com.windscribe.vpn.localdatabase.tables.UserStatusTable
import com.windscribe.vpn.serverlist.entity.City
import com.windscribe.vpn.serverlist.entity.CityAndRegion
import com.windscribe.vpn.serverlist.entity.ConfigFile
import com.windscribe.vpn.serverlist.entity.PingTime
import com.windscribe.vpn.serverlist.entity.Region
import com.windscribe.vpn.serverlist.entity.StaticRegion
import io.reactivex.Completable
import io.reactivex.Single
import io.reactivex.disposables.CompositeDisposable
import javax.inject.Inject

/**
 * Implementation of ServiceInteractor
 * @see ServiceInteractor
 */
class ServiceInteractorImpl @Inject constructor(
    override val preferenceHelper: PreferencesHelper,
    override val apiManager: IApiCallManager,
    private val localDbInterface: LocalDbInterface
) : ServiceInteractor {

    override val compositeDisposable = CompositeDisposable()
    fun getNetworkInfo(name: String): Single<NetworkInfo> {
        return localDbInterface.getNetwork(name)
    }

    override fun getResourceString(resId: Int): String {
        return Windscribe.appContext.resources.getString(resId)
    }

    override val savedLocale: String
        get() {
            val selectedLanguage = preferenceHelper.savedLanguage
            return selectedLanguage.substring(selectedLanguage.indexOf("(") + 1, selectedLanguage.indexOf(")"))
        }

    override fun insertOrUpdateUserStatus(userStatusTable: UserStatusTable): Completable {
        return localDbInterface.updateUserStatus(userStatusTable)
    }

    override suspend fun clearData() {
        preferenceHelper.clearAllData()
        localDbInterface.clearAllTables()
    }

    override fun addPing(pingTime: PingTime): Completable {
        return localDbInterface.addPing(pingTime)
    }

    override fun getAllCities(): Single<List<City>> {
        return localDbInterface.cities
    }

    override fun getAllStaticRegions(): Single<List<StaticRegion>> {
        return localDbInterface.allStaticRegions
    }

    override fun getLowestPingId(): Single<Int> {
        return localDbInterface.lowestPingId
    }

    override fun getStaticIpCount(): Int {
        return localDbInterface.staticRegionCount
            .onErrorReturnItem(0)
            .blockingGet()
    }

    override fun addNetworkToKnown(networkName: String): Single<Long> {
        val networkInfo = NetworkInfo(networkName, true, false, "UDP", "443")
        return localDbInterface.addNetwork(networkInfo)
    }

    override fun getNetwork(networkName: String): Single<NetworkInfo> {
        return localDbInterface.getNetwork(networkName)
    }

    override suspend fun getUserSession(): UserSessionResponse {
        val session = preferenceHelper.getResponseString(PreferencesKeyConstants.GET_SESSION)
        return Gson().fromJson(session, UserSessionResponse::class.java)
    }

    override fun getCoordinates(countryCode: String): Single<Array<String>> {
        return localDbInterface.getRegionByCountryCode(countryCode).flatMap { region: Region ->
            localDbInterface.getCordsByRegionId(region.id)
        }.flatMap { Single.fromCallable { it.split(",".toRegex()).toTypedArray() } }
    }

    override fun getCityAndRegionByID(cityId: Int): CityAndRegion {
        return this.localDbInterface.getCityAndRegion(cityId)
    }

    override fun getConfigFile(id: Int): Single<ConfigFile> {
        return this.localDbInterface.getConfigFile(id)
    }

    override fun addConfigFile(configFile: ConfigFile): Completable {
        return this.localDbInterface.addConfig(configFile)
    }

    override fun getStaticRegionByID(staticId: Int): Single<StaticRegion> {
        return this.localDbInterface.getStaticRegionByID(staticId)
    }
}
