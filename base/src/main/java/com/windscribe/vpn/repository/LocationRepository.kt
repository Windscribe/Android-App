/*
 * Copyright (c) 2021 Windscribe Limited.
 */
package com.windscribe.vpn.repository

import com.windscribe.vpn.Windscribe.Companion.appContext
import com.windscribe.vpn.apppreference.PreferencesHelper
import com.windscribe.vpn.backend.utils.SelectedLocationType
import com.windscribe.vpn.commonutils.WindUtilities
import com.windscribe.vpn.constants.NetworkKeyConstants
import com.windscribe.vpn.exceptions.WindScribeException
import com.windscribe.vpn.localdatabase.LocalDbInterface
import com.windscribe.vpn.serverlist.entity.City
import com.windscribe.vpn.serverlist.entity.CityAndRegion
import com.windscribe.vpn.serverlist.entity.Node
import dagger.Lazy
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.slf4j.LoggerFactory
import java.util.TimeZone
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

@Singleton
class LocationRepository @Inject constructor(
    private val scope: CoroutineScope,
    private val preferencesHelper: PreferencesHelper,
    private val localDbInterface: LocalDbInterface,
    private val userRepository: Lazy<UserRepository>
) {
    private val logger = LoggerFactory.getLogger("data")
    private var _selectedCityEvents = MutableStateFlow(preferencesHelper.selectedCity)
    val selectedCity: StateFlow<Int> = _selectedCityEvents

    init {
        setSelectedCity()
    }

    fun setSelectedCity(locationId: Int? = null) {
        locationId?.let {
            preferencesHelper.selectedCity = locationId
        }
        scope.launch {
            _selectedCityEvents.emit(preferencesHelper.selectedCity)
        }
    }

    private suspend fun getAlternativeLocation(): Int {
        logger.debug("Location is not valid anymore looking for alternatives")
        WindUtilities.deleteProfile(appContext)
        preferencesHelper.setConnectingToConfiguredLocation(false)
        preferencesHelper.setConnectingToStaticIP(false)
        return runCatching { getSisterLocation() }
            .getOrElse {
                runCatching { getLowestPingLocation() }
                    .getOrElse { getRandomLocation() }
            }
    }

    suspend fun getBestLocationAsync(): CityAndRegion {
        val locationId = runCatching { getLowestPingLocation() }
            .getOrElse { getRandomLocation() }
        return localDbInterface.getCityAndRegion(locationId) ?: throw Exception("Best location not found")
    }

    suspend fun updateLocation(): Int {
        logger.debug("updating last selected location: ${selectedCity.value}")
        val userStatus = userRepository.get().user.value?.userStatusInt ?: 0
        return try {
            val isValid = isLocationValid(selectedCity.value, userStatus)
            if (isValid) selectedCity.value else getAlternativeLocation()
        } catch (e: WindScribeException) {
            -1
        }
    }

    suspend fun isNodeAvailable(): Boolean {
        if (WindUtilities.getSourceTypeBlocking() == SelectedLocationType.CityLocation) {
            return runCatching {
                val city = localDbInterface.getCityByIDAsync(preferencesHelper.selectedCity)
                val nodes = city.getNodes()
                ipAvailable(preferencesHelper.selectedIp, nodes)
            }.getOrDefault(false)
        } else {
            return true
        }
    }

    private suspend fun getSisterLocation(): Int {
        val selectedCity = preferencesHelper.selectedCity
        logger.debug("Getting sister city.")
        return runCatching {
            val region = localDbInterface.getRegionIdFromCityAsync(selectedCity)
            val cities = localDbInterface.getAllCitiesAsync(region)

            val city = if (userRepository.get().user.value?.isPro == true) {
                logger.debug("User is pro getting random location.")
                cities.random()
            } else {
                logger.debug("User is not pro getting free location.")
                val freeLocation = cities.shuffled().firstOrNull { it.pro == 0 }
                freeLocation ?: throw Exception("No free city found in RegionId: $region")
            }

            if (city.nodesAvailable()) {
                logger.debug("Found sister city${city.getId()}")
                city.getId()
            } else {
                throw Exception("No nodes available in sister city")
            }
        }.getOrElse {
            logger.debug("No sister location found.")
            throw it
        }
    }

    private suspend fun getLowestPingLocation(): Int {
        val pingId = localDbInterface.getLowestPingIdAsync()
        val city = localDbInterface.getCityByIDAsync(pingId)
        return city.getId()
    }

    private suspend fun getRandomLocation(): Int {
        val isUserPro = userRepository.get().user.value?.isPro ?: false
        return runCatching {
            val cities = localDbInterface.getCitiesAsync()
            val filteredLocations = cities.filter { city ->
                val isLocationPro = city.pro == 1
                (!isLocationPro || isUserPro) && city.nodesAvailable()
            }
            pickBestCityId(filteredLocations)
        }.getOrDefault(-1)
    }

    private fun pickBestCityId(cities: List<City>): Int {
        if (cities.isEmpty()) {
            return -1
        }
        fun haversine(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
            val earthRadius = 6371.0 // Earth's radius in kilometers
            val dLat = Math.toRadians(lat2 - lat1)
            val dLon = Math.toRadians(lon2 - lon1)
            val a = sin(dLat / 2).pow(2.0) +
                    cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * sin(dLon / 2).pow(2.0)
            val c = 2 * atan2(sqrt(a), sqrt(1 - a))
            return earthRadius * c
        }
        val userTimeZone = TimeZone.getDefault()
        val userOffsetMinutes = userTimeZone.getOffset(System.currentTimeMillis()) / (1000 * 60)
        var bestCity: City? = null
        var smallestTimeDifference = Int.MAX_VALUE
        var smallestDistance = Double.MAX_VALUE
        for (city in cities) {
            val cityTimeZone = TimeZone.getTimeZone(city.tz)
            val cityOffsetMinutes = cityTimeZone.getOffset(System.currentTimeMillis()) / (1000 * 60)
            val timeDifference = abs(userOffsetMinutes - cityOffsetMinutes)
            val coordinates = city.coordinates.split(",")
            val cityLatitude = coordinates[0].toDouble()
            val cityLongitude = coordinates[1].toDouble()
            val distance = haversine(cityLatitude, cityLongitude, cityLatitude, cityLongitude)
            if (timeDifference < smallestTimeDifference ||
                (timeDifference == smallestTimeDifference && distance < smallestDistance)) {
                bestCity = city
                smallestTimeDifference = timeDifference
                smallestDistance = distance
            }
        }
        return bestCity?.id ?: -1
    }

    private fun ipAvailable(ip: String?, nodes: List<Node>): Boolean {
        return nodes.any {
            ip == it.hostname || ip == it.ip || ip == it.ip2 || ip == it.ip3
        }
    }

    private suspend fun isCityAvailable(id: Int, userPro: Int): Boolean {
        return runCatching {
            val cityAndRegion = localDbInterface.getCityAndRegion(id) ?: return@runCatching false
            if (cityAndRegion.region == null) return@runCatching false
            val isLocationPro = cityAndRegion.city.pro == 1
            val isUserPro = userPro == 1

            when {
                !isUserPro && isLocationPro -> {
                    logger.debug("Location is premium user has no access to it.")
                    false
                }
                !cityAndRegion.city.nodesAvailable() -> {
                    false
                }
                cityAndRegion.region.status == NetworkKeyConstants.SERVER_STATUS_TEMPORARILY_UNAVAILABLE -> {
                    logger.debug("City location : server status is temporary unavailable.")
                    false
                }
                else -> true
            }
        }.getOrDefault(false)
    }

    private suspend fun isConfigProfileAvailable(id: Int): Boolean {
        return runCatching {
            localDbInterface.getConfigFileAsync(id)
            true
        }.getOrDefault(false)
    }

    private suspend fun isLocationValid(id: Int, userPro: Int): Boolean {
        val locationSourceType = WindUtilities.getSourceTypeBlocking()
        return when {
            locationSourceType === SelectedLocationType.StaticIp -> {
                isStaticIpAvailable(id)
            }
            locationSourceType === SelectedLocationType.CustomConfiguredProfile -> {
                isConfigProfileAvailable(id)
            }
            else -> {
                isCityAvailable(id, userPro)
            }
        }
    }

    private suspend fun isStaticIpAvailable(id: Int): Boolean {
        return runCatching {
            localDbInterface.getStaticRegionByIDAsync(id)
            true
        }.getOrDefault(false)
    }

    fun getSelectedCityAndRegion(): CityAndRegion? {
        val selectedCityId = preferencesHelper.selectedCity
        if (selectedCityId == -1 || WindUtilities.getSourceTypeBlocking() != SelectedLocationType.CityLocation) {
            return null
        }
        return runCatching {
            localDbInterface.getCityAndRegion(selectedCityId)
        }.getOrNull()
    }
}
