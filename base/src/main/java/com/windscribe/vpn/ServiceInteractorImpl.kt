/*
 * Copyright (c) 2021 Windscribe Limited.
 */

package com.windscribe.vpn

import android.os.Build
import com.google.gson.Gson
import com.windscribe.vpn.api.IApiCallManager
import com.windscribe.vpn.api.response.GenericSuccess
import com.windscribe.vpn.api.response.UserSessionResponse
import com.windscribe.vpn.apppreference.PreferencesHelper
import com.windscribe.vpn.commonutils.WindUtilities
import com.windscribe.vpn.constants.NetworkKeyConstants
import com.windscribe.vpn.constants.PreferencesKeyConstants
import com.windscribe.vpn.constants.UserStatusConstants
import com.windscribe.vpn.encoding.encoders.Base64
import com.windscribe.vpn.localdatabase.LocalDbInterface
import com.windscribe.vpn.localdatabase.tables.NetworkInfo
import com.windscribe.vpn.localdatabase.tables.UserStatusTable
import com.windscribe.vpn.repository.CallResult
import com.windscribe.vpn.serverlist.entity.*
import io.reactivex.Completable
import io.reactivex.Single
import io.reactivex.disposables.CompositeDisposable
import kotlinx.coroutines.rx2.await
import java.io.BufferedReader
import java.io.File
import java.io.FileReader
import java.nio.charset.Charset
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

    override fun getPingableCities(): Single<List<City>> {
        return localDbInterface.pingableCities
    }

    override fun getAllFavourites(): Single<List<Favourite>> {
        return localDbInterface.favourites
    }

    override fun getAllRegion(): Single<List<RegionAndCities>> {
        return localDbInterface.allRegion
    }

    override fun getCity(id: Int): Single<City> {
        return localDbInterface.getCityByID(id)
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
        return this.localDbInterface.addNetwork(preferenceHelper.getDefaultNetworkInfo(networkName))
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

    override suspend fun getAllConfigs(): List<ConfigFile> {
        return this.localDbInterface.allConfigs.await()
    }

    override fun addConfigFile(configFile: ConfigFile): Completable {
        return this.localDbInterface.addConfig(configFile)
    }

    override fun getStaticRegionByID(staticId: Int): Single<StaticRegion> {
        return this.localDbInterface.getStaticRegionByID(staticId)
    }

    override suspend fun sendLog(): CallResult<GenericSuccess> {
        return try {
            apiManager.postDebugLog(preferenceHelper.userName, getEncodedLog()).await().callResult()
        } catch (e: Exception) {
            CallResult.Error(errorMessage = "Unable to load debug logs from disk.")
        }
    }

    override fun saveWhiteListedNetwork(reset: Boolean){
        if (reset){
            preferenceHelper.whiteListedNetwork = null
        } else {
            try {
                preferenceHelper.whiteListedNetwork =  WindUtilities.getNetworkName()
            } catch (e: Exception){
                preferenceHelper.whiteListedNetwork = null
            }
        }
    }

    @Throws(Exception::class)
    fun getEncodedLog(): String {
        var logLine: String?
        val debugFilePath = getDebugFilePath()
        val logFile = Windscribe.appContext.resources.getString(
            R.string.log_file_header,
            Build.VERSION.SDK_INT, Build.BRAND, Build.DEVICE, Build.MODEL, Build.MANUFACTURER,
            Build.VERSION.RELEASE, WindUtilities.getVersionCode()
        )
        val builder = StringBuilder()
        builder.append(logFile)
        val file = File(debugFilePath)
        val bufferedReader = BufferedReader(FileReader(file))
        while (bufferedReader.readLine().also { logLine = it } != null) {
            builder.append(logLine)
            builder.append("\n")
        }
        bufferedReader.close()
        return String(Base64.encode(builder.toString().toByteArray(Charset.defaultCharset())))
    }

    private fun getDebugFilePath(): String {
        return Windscribe.appContext.cacheDir.path + PreferencesKeyConstants.DEBUG_LOG_FILE_NAME
    }

    override fun saveNetwork(networkInfo: NetworkInfo): Single<Int> {
        return localDbInterface.updateNetwork(networkInfo)
    }
}
