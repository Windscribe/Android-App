/*
 * Copyright (c) 2021 Windscribe Limited.
 */
package com.windscribe.vpn.repository

import android.content.Context
import androidx.annotation.Keep
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.windscribe.vpn.Windscribe.Companion.appContext
import com.windscribe.vpn.api.IApiCallManager
import com.windscribe.vpn.apppreference.PreferencesHelper
import com.windscribe.vpn.constants.AdvanceParamsValues.IGNORE
import com.windscribe.vpn.localdatabase.LocalDbInterface
import com.windscribe.vpn.model.User
import com.windscribe.vpn.serverlist.entity.City
import com.windscribe.vpn.serverlist.entity.Region
import com.windscribe.vpn.serverlist.entity.RegionAndCities
import com.windscribe.vpn.state.AppLifeCycleObserver
import com.windscribe.vpn.state.PreferenceChangeObserver
import dagger.Lazy
import io.reactivex.Completable
import io.reactivex.Single
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.rx2.await
import org.json.JSONObject
import org.slf4j.LoggerFactory
import java.io.FileInputStream
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.IOException
import java.security.MessageDigest
import javax.inject.Inject
import javax.inject.Singleton

@Keep
data class CustomCity(
    val id: Int,
    val name: String,
    val nick: String
)

@Keep
data class CustomRegion(val id: Int, val country: String, val cities: List<CustomCity>)

@Keep
data class CustomLocationsData(val locations: List<CustomRegion>)

@Singleton
class ServerListRepository @Inject constructor(
    private val scope: CoroutineScope,
    private val apiCallManager: IApiCallManager,
    private val localDbInterface: LocalDbInterface,
    private val preferenceChangeObserver: PreferenceChangeObserver,
    private val userRepository: Lazy<UserRepository>,
    private val appLifeCycleObserver: AppLifeCycleObserver,
    private val advanceParameterRepository: AdvanceParameterRepository,
    private val preferenceHelper: PreferencesHelper,
    private val favouriteRepository: FavouriteRepository
) {
    private val logger = LoggerFactory.getLogger("data")
    private var _events = MutableSharedFlow<List<RegionAndCities>>(replay = 1)
    val regions: SharedFlow<List<RegionAndCities>> = _events
    private var _locationJsonToExport = MutableStateFlow("")
    val locationJsonToExport: StateFlow<String> = _locationJsonToExport
    private val _customCities = MutableStateFlow<List<CustomCity>>(listOf())
    val customCities: StateFlow<List<CustomCity>> = _customCities
    private val _customRegions = MutableStateFlow<List<CustomRegion>>(listOf())
    val customRegions: StateFlow<List<CustomRegion>> = _customRegions

    var globalServerList = true

    init {
        load()
    }

    fun load() {
        scope.launch {
            _events.emit(localDbInterface.allRegion.await())
            observeServerLocations()
        }
        loadCustomLocationsJson()
    }

    private fun getCountryOverride(): String? {
        val countryCode = advanceParameterRepository.getCountryOverride()
        val isConnectedVPN = appContext.vpnConnectionStateManager.isVPNConnected()
        return if (countryCode != null) {
            if (countryCode == IGNORE) {
                "ZZ"
            } else {
                countryCode
            }
        } else if (appLifeCycleObserver.overriddenCountryCode == null && isConnectedVPN) {
            "ZZ"
        } else {
            logger.debug("Existing server override: ${appLifeCycleObserver.overriddenCountryCode ?: "Global"}")
            appLifeCycleObserver.overriddenCountryCode
        }
    }

    fun update(): Completable {
        logger.debug("Starting server list update")
        return apiCallManager.getSessionGeneric(null).flatMap {
            it.dataClass?.let { userSession ->
                userRepository.get().reload(userSession)
                val user = User(userSession)
                userRepository.get().reload(userSession)
                val alc = if (userSession.alcList.isNullOrEmpty()) {
                    arrayOf()
                } else {
                    userSession.alcList.toTypedArray()
                }

                val countryOverride = getCountryOverride()
                if (countryOverride != "ZZ") {
                    globalServerList = false
                }
                logger.debug("Country override: $countryOverride")
                apiCallManager.getServerList(
                    user.userStatusInt == 1,
                    user.locationHash,
                    alc,
                    countryOverride
                )
            } ?: it.errorClass?.let { error ->
                logger.debug("Error updating session $error")
                throw Exception()
            } ?: kotlin.run {
                logger.debug("Unknown error updating session")
                throw Exception()
            }
        }.flatMap { response ->
            Single.fromCallable {
                logger.debug("Parsing server list JSON")
                response.dataClass?.let {
                    val jsonObject = JSONObject(it)
                    val infoObject = jsonObject.getJSONObject("info")
                    logger.debug(infoObject.toString())
                    appLifeCycleObserver.overriddenCountryCode =
                        if (infoObject.has("country_override")) {
                            infoObject.getString("country_override")
                        } else {
                            null
                        }
                    preferenceHelper.locationHash = hash(it)
                    val dataArray = jsonObject.getJSONArray("data")
                    Gson().fromJson<List<Region>>(
                        dataArray.toString(),
                        object : TypeToken<ArrayList<Region?>?>() {}.type
                    )
                } ?: response.errorClass?.let {
                    throw Exception(it.errorMessage)
                }
            }
        }.flatMapCompletable { regions: List<Region> -> addToDatabase(regions) }
    }

    private fun hash(jsonString: String): String {
        val bytes = jsonString.toByteArray()
        val digest = MessageDigest.getInstance("SHA-256")
        val hashBytes = digest.digest(bytes)
        return hashBytes.joinToString("") { "%02x".format(it) }
    }

    private fun addToDatabase(regions: List<Region>): Completable {
        val cities: MutableList<City> = ArrayList()
        for (region in regions) {
            if (region.getCities() != null) {
                for (city in region.getCities()) {
                    city.regionID = region.id
                    cities.add(city)
                }
            }
        }
        return localDbInterface.addToRegions(regions)
            .andThen(localDbInterface.addToCities(cities))
            .andThen(Completable.fromAction {
                preferenceChangeObserver.postCityServerChange()
                load()
            })
    }

    private fun observeServerLocations() {
        scope.launch {
            regions.collect { regions ->
                buildLocationsJson(regions)
            }
        }
    }

    private fun buildLocationsJson(regions: List<RegionAndCities>) {
        val customLocationData = CustomLocationsData(regions.map { region ->
            val cities = region.cities.map { CustomCity(it.id, it.nodeName, it.nickName) }
            return@map CustomRegion(region.region.id, region.region.name, cities)
        })
        _locationJsonToExport.value = Gson().toJson(customLocationData)
    }

    fun saveLocationsJson(json: String) {
        try {
            val fileOutputStream: FileOutputStream =
                appContext.openFileOutput("locations.json", Context.MODE_PRIVATE)
            fileOutputStream.write(json.toByteArray())
            fileOutputStream.close()
            logger.info("JSON saved successfully to internal storage")
            loadCustomLocationsJson()
        } catch (e: IOException) {
            logger.error("Error saving JSON to internal storage", e)
        }
    }

    fun deleteLocationsJson() {
        try {
            appContext.deleteFile("locations.json")
            logger.info("JSON deleted successfully from internal storage")
            loadCustomLocationsJson()
        } catch (e: IOException) {
            logger.error("Error deleting JSON from internal storage", e)
        }
    }

    private fun loadCustomLocationsJson() {
        try {
            val fileInputStream: FileInputStream = appContext.openFileInput("locations.json")
            val jsonString = fileInputStream.bufferedReader().use { it.readText() }
            val type = object : TypeToken<CustomLocationsData>() {}.type
            val customLocationsData = Gson().fromJson<CustomLocationsData>(jsonString, type)
            val cities =
                customLocationsData.locations.map { it.cities }.reduce { t1, t2 -> t1 + t2 }
            _customCities.value = cities
            _customRegions.value = customLocationsData.locations
            scope.launch {
                _events.emit(localDbInterface.allRegion.await())
                favouriteRepository.load()
            }
        } catch (ignored: Exception) {
            _customCities.value = listOf()
            _customRegions.value = listOf()
            scope.launch {
                _events.emit(localDbInterface.allRegion.await())
                favouriteRepository.load()
            }
        }
    }

    fun getCustomCityName(id: Int): String? {
        val city = customCities.value.firstOrNull { it.id == id }
        return city?.name
    }

    fun getCustomCityNickName(id: Int): String? {
        val city = customCities.value.firstOrNull { it.id == id }
        return city?.nick
    }

    fun getCustomRegionName(id: Int): String? {
        val region = customRegions.value.firstOrNull { it.id == id }
        return region?.country
    }
}
