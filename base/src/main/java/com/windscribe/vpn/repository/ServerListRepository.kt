/*
 * Copyright (c) 2021 Windscribe Limited.
 */
package com.windscribe.vpn.repository

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
import io.reactivex.Completable
import io.reactivex.Single
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.rx2.await
import org.json.JSONObject
import org.slf4j.LoggerFactory
import java.security.MessageDigest
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ServerListRepository @Inject constructor(
        private val scope: CoroutineScope,
        private val apiCallManager: IApiCallManager,
        private val localDbInterface: LocalDbInterface,
        private val preferenceChangeObserver: PreferenceChangeObserver,
        private val userRepository: UserRepository,
        private val appLifeCycleObserver: AppLifeCycleObserver,
        private val advanceParameterRepository: AdvanceParameterRepository,
        private val preferenceHelper: PreferencesHelper
) {
    private val logger = LoggerFactory.getLogger("data")
    private var _events = MutableSharedFlow<List<RegionAndCities>>(replay = 1)
    val regions: SharedFlow<List<RegionAndCities>> = _events
    private var _locationUIInvalidation = MutableSharedFlow<Boolean>(replay = 1)
    val locationUIInvalidation = _locationUIInvalidation
    var globalServerList = true

    init {
        load()
    }

    fun invalidateServerListUI() {
        scope.launch {
            _locationUIInvalidation.emit(true)
        }
    }

    fun load() {
        scope.launch {
            _events.emit(localDbInterface.allRegion.await())
        }
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
                userRepository.reload(userSession)
                val user = User(userSession)
                userRepository.reload(userSession)
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
                apiCallManager.getServerList(user.userStatusInt == 1, user.locationHash, alc, countryOverride)
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
                    appLifeCycleObserver.overriddenCountryCode = if (infoObject.has("country_override")) {
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
    fun hash(jsonString: String): String {
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


}
