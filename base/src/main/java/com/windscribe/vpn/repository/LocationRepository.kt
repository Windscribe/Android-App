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
import io.reactivex.Single
import io.reactivex.SingleSource
import io.reactivex.functions.Function
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.rx2.await
import org.slf4j.LoggerFactory
import java.time.ZonedDateTime
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
    private val logger = LoggerFactory.getLogger("selected_location_updater")
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

    private val alternativeLocation: Single<Int>
        get() {
            logger.debug("Location is not valid anymore looking for alternatives")
            WindUtilities.deleteProfile(appContext)
            preferencesHelper.setConnectingToConfiguredLocation(false)
            preferencesHelper.setConnectingToStaticIP(false)
            return sisterLocation.onErrorResumeNext(
                lowestPingLocation.onErrorResumeNext(
                    randomLocation
                )
            )
        }
    val bestLocation: Single<CityAndRegion>
        get() = lowestPingLocation
            .onErrorResumeNext(randomLocation)
            .flatMap { localDbInterface.getCityAndRegionByID(it) }

    private fun update(): Single<Int> {
        logger.debug("updating last selected location: ${selectedCity.value}")
        val userStatus = userRepository.get().user.value?.userStatusInt ?: 0
        return isLocationValid(selectedCity.value, userStatus)
            .flatMap { if (it) Single.fromCallable { selectedCity.value } else alternativeLocation }
    }

    suspend fun updateLocation(): Int {
        return try {
            update().await()
        } catch (e: WindScribeException) {
            -1
        }
    }

    suspend fun isNodeAvailable(): Boolean {
        if (WindUtilities.getSourceTypeBlocking() == SelectedLocationType.CityLocation) {
            return localDbInterface.getCityByID(preferencesHelper.selectedCity).map {
                it.getNodes()
            }.flatMap {
                return@flatMap Single.just(ipAvailable(preferencesHelper.selectedIp, it))
            }.onErrorReturnItem(false).await()
        } else {
            return true
        }
    }

    private val sisterLocation: Single<Int>
        get() {
            val selectedCity = preferencesHelper.selectedCity
            logger.debug("Getting sister city.")
            return localDbInterface.getRegionIdFromCity(selectedCity)
                .flatMap { region: Int ->
                    localDbInterface.getAllCities(region)
                        .flatMap { cities ->
                            if (userRepository.get().user.value?.isPro == true) {
                                logger.debug("User is pro getting random location.")
                                Single.just(cities.random())
                            } else {
                                logger.debug("User is not pro getting free location.")
                                val freeLocation = cities.shuffled().firstOrNull { it.pro == 0 }
                                if (freeLocation == null) {
                                    throw Exception("No free city found in RegionId: $region")
                                } else {
                                    Single.just(freeLocation)
                                }
                            }
                        }
                        .flatMap(
                            Function<City, SingleSource<Int>> { city: City ->
                                if (city.nodesAvailable()) {
                                    return@Function Single.fromCallable { city.getId() }
                                } else {
                                    throw Exception()
                                }
                            }
                        )
                        .doOnError { logger.debug("No sister location found.") }
                        .doOnSuccess { city: Int -> logger.debug("Found sister city$city") }
                }
        }
    private val lowestPingLocation: Single<Int>
        get() = localDbInterface.lowestPingId
            .flatMap { localDbInterface.getCityByID(it) }
            .flatMap { city: City -> Single.fromCallable { city.getId() } }
            .doOnError { logger.debug("Still waiting for latency to complete.") }
            .doOnSuccess { city: Int -> logger.debug("Found lowest ping city: $city") }

    private val randomLocation: Single<Int>
        get() {
            val isUserPro = userRepository.get().user.value?.isPro ?: false
            return localDbInterface.cities.map { cities ->
                val filteredLocations = cities.filter { city ->
                    val isLocationPro = city.pro == 1
                    (!isLocationPro || isUserPro) && city.nodesAvailable()
                }
                return@map pickBestCityId(filteredLocations)
            }.onErrorReturnItem(-1)
        }

    private fun pickBestCityId(cities: List<City>): Int {
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
            val coordinates = city.coordinates.split(",")
            val cityLatitude = coordinates[0].toDouble()
            val cityLongitude = coordinates[1].toDouble()
            val timeDifference = abs(userOffsetMinutes - cityOffsetMinutes)
            val distance = haversine(cityLatitude, cityLongitude, 43.7, -79.42)
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
        nodes.firstOrNull {
            ip == it.hostname || (ip == it.ip) or (ip == it.ip2) || (ip == it.ip3)
        }?.let {
            return true
        } ?: kotlin.run {
            return false
        }
    }

    private fun isCityAvailable(id: Int, userPro: Int): Single<Boolean> {
        return localDbInterface.getCityAndRegionByID(id)
            .flatMap { cityAndRegion: CityAndRegion ->
                Single.fromCallable {
                    val isLocationPro = cityAndRegion.city.pro == 1
                    val isUserPro = userPro == 1
                    if (!isUserPro && isLocationPro) {
                        logger.debug("Location is premium user has no access to it.")
                        return@fromCallable false
                    } else if (!cityAndRegion.city.nodesAvailable()) {
                        return@fromCallable false
                    } else if (cityAndRegion.region.status
                        == NetworkKeyConstants.SERVER_STATUS_TEMPORARILY_UNAVAILABLE
                    ) {
                        logger.debug("City location : server status is temporary unavailable.")
                        return@fromCallable false
                    }
                    //Avoid reconnect if ip changes in the nodes Ip may changed because of altered server list.
                    /* else if (!ipAvailable(
                            preferencesHelper.selectedIp,
                            cityAndRegion.city.getNodes()
                        )
                    ) {
                        preferencesHelper.selectedCity = -1
                        return@fromCallable true
                    } */ else {
                        return@fromCallable true
                    }
                }
            }.onErrorReturnItem(false)
    }

    private fun isConfigProfileAvailable(id: Int): Single<Boolean> {
        return localDbInterface.getConfigFile(id)
            .flatMap { Single.fromCallable { true } }
            .onErrorReturnItem(false)
    }

    private fun isLocationValid(id: Int, userPro: Int): Single<Boolean> {
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

    private fun isStaticIpAvailable(id: Int): Single<Boolean> {
        return localDbInterface.getStaticRegionByID(id)
            .flatMap { Single.fromCallable { true } }
            .onErrorReturnItem(false)
    }
}
