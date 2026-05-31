/*
 * Copyright (c) 2021 Windscribe Limited.
 */
package com.windscribe.vpn.repository

import com.windscribe.vpn.Windscribe.Companion.appContext
import com.windscribe.vpn.apppreference.PreferencesHelper
import com.windscribe.vpn.backend.utils.SelectedLocationType
import com.windscribe.vpn.commonutils.WindUtilities
import com.windscribe.vpn.exceptions.WindScribeException
import com.windscribe.vpn.localdatabase.LocalDbInterface
import com.windscribe.vpn.serverlist.entity.Datacenter
import com.windscribe.vpn.serverlist.entity.DatacenterAndLocation
import com.windscribe.vpn.serverlist.entity.Server
import com.windscribe.vpn.services.ping.Pinger
import dagger.Lazy
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.slf4j.LoggerFactory
import java.util.TimeZone
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.abs

@Singleton
class LocationRepository
    @Inject
    constructor(
        private val scope: CoroutineScope,
        private val preferencesHelper: PreferencesHelper,
        private val localDbInterface: LocalDbInterface,
        private val userRepository: Lazy<UserRepository>,
        private val pinger: Pinger,
    ) {
        private val logger = LoggerFactory.getLogger("data")
        private var _selectedCityEvents = MutableStateFlow(preferencesHelper.selectedCity)
        val selectedCity: StateFlow<Int> = _selectedCityEvents

        // Cache for best location to avoid duplicate ping operations
        private var cachedTimezoneBasedLocationId: Int? = null

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
            preferencesHelper.isConnectingToConfigured = false
            preferencesHelper.isConnectingToStaticIp = false
            return runCatching { getSisterLocation() }
                .getOrElse {
                    runCatching { getLowestPingLocation() }
                        .getOrElse { getRandomLocation() }
                }
        }

        suspend fun getBestLocationAsync(): DatacenterAndLocation {
            val locationId =
                runCatching { getLowestPingLocation() }
                    .getOrElse { getRandomLocation() }
            return localDbInterface.getDatacenterAndLocation(locationId) ?: throw Exception("Best location not found")
        }

        suspend fun updateLocation(): Int {
            logger.debug("updating last selected location: ${selectedCity.value}")
            val userStatus =
                userRepository
                    .get()
                    .user.value
                    ?.userStatusInt ?: 0
            return try {
                val isValid = isLocationValid(selectedCity.value, userStatus)
                if (isValid) selectedCity.value else getAlternativeLocation()
            } catch (e: WindScribeException) {
                -1
            }
        }

        private fun currentSourceType(): SelectedLocationType =
            when {
                preferencesHelper.isConnectingToConfigured -> SelectedLocationType.CustomConfiguredProfile
                preferencesHelper.isConnectingToStaticIp -> SelectedLocationType.StaticIp
                else -> SelectedLocationType.CityLocation
            }

        suspend fun isNodeAvailable(): Boolean =
            if (currentSourceType() == SelectedLocationType.CityLocation) {
                runCatching {
                    val nodes = localDbInterface.getServersByDatacenter(preferencesHelper.selectedCity)
                    ipAvailable(preferencesHelper.selectedIp, nodes)
                }.getOrDefault(false)
            } else {
                true
            }

        private suspend fun getSisterLocation(): Int {
            val selectedCity = preferencesHelper.selectedCity
            logger.debug("Getting sister city.")
            return runCatching {
                val region = localDbInterface.getLocationIdFromDatacenterAsync(selectedCity)
                val cities = localDbInterface.getAllDatacentersAsync(region)
                val city = cities.random()
                if (localDbInterface.getServersByDatacenter(city.id).isNotEmpty()) {
                    logger.debug("Found sister city${city.id}")
                    city.id
                } else {
                    throw Exception("No servers available in sister datacenter")
                }
            }.getOrElse {
                logger.debug("No sister location found.")
                throw it
            }
        }

        private suspend fun getLowestPingLocation(): Int {
            val pingId = localDbInterface.getLowestPingIdAsync()
            val city = localDbInterface.getDatacenterByIDAsync(pingId)
            return city.id
        }

        private suspend fun getRandomLocation(): Int {
            val isUserPro =
                userRepository
                    .get()
                    .user.value
                    ?.isPro ?: false
            return runCatching {
                val cities = localDbInterface.getDatacentersAsync()
                val filteredLocations =
                    cities.filter { city ->
                        val servers = localDbInterface.getServersByDatacenter(city.id)
                        servers.isNotEmpty()
                    }
                pickBestCityId(filteredLocations)
            }.getOrDefault(-1)
        }

        private suspend fun pickBestCityId(cities: List<Datacenter>): Int {
            // Return cached result if available
            cachedTimezoneBasedLocationId?.let {
                return it
            }

            if (cities.isEmpty()) {
                return -1
            }
            val userTimeZone = TimeZone.getDefault()
            val userOffsetMinutes = userTimeZone.getOffset(System.currentTimeMillis()) / (1000 * 60)

            data class CityWithTimeDiff(
                val city: Datacenter,
                val timeDiffMinutes: Int,
                val tzScore: Int,
            )

            val citiesWithTimezone =
                cities.map { city ->
                    val cityTimeZone = TimeZone.getTimeZone(city.tz)
                    val cityOffsetMinutes = cityTimeZone.getOffset(System.currentTimeMillis()) / (1000 * 60)
                    val timeDifference = abs(userOffsetMinutes - cityOffsetMinutes)
                    val tzScore =
                        when {
                            city.tz == userTimeZone.id -> 3
                            city.tz?.contains("/") == true -> 2
                            else -> 1
                        }
                    CityWithTimeDiff(city, timeDifference, tzScore)
                }
            val sortedByTimezone =
                citiesWithTimezone.sortedWith(
                    compareBy<CityWithTimeDiff> { it.timeDiffMinutes }
                        .thenByDescending { it.tzScore },
                )
            val topCitiesToPing = sortedByTimezone.take(10)
            val context = currentCoroutineContext()
            val pingJobs =
                topCitiesToPing.map { cityWithDiff ->
                    CoroutineScope(context).async {
                        val city = cityWithDiff.city
                        val pingResult = pingCity(city)
                        Pair(city, pingResult)
                    }
                }
            val pingResults = pingJobs.awaitAll()
            val successfulPings = pingResults.filter { it.second >= 0 }
            val bestCityId =
                if (successfulPings.isEmpty()) {
                    topCitiesToPing.firstOrNull()?.city?.id ?: -1
                } else {
                    val bestCity = successfulPings.minByOrNull { it.second }?.first
                    bestCity?.id ?: -1
                }

            // Cache the result
            if (bestCityId != -1) {
                cachedTimezoneBasedLocationId = bestCityId
            }

            return bestCityId
        }

        private suspend fun pingCity(city: Datacenter): Int {
            val pingIpAndHost = localDbInterface.getPingIpAndHost(city.id) ?: return -1
            return pinger.ping(pingIpAndHost.first, 500)
        }

        private fun ipAvailable(
            ip: String?,
            nodes: List<Server>,
        ): Boolean =
            nodes.any {
                ip == it.hostname || ip == it.ip || ip == it.ip2 || ip == it.ip3
            }

        private suspend fun isCityAvailable(
            id: Int,
            userPro: Int,
        ): Boolean {
            return runCatching {
                val cityAndRegion = localDbInterface.getDatacenterAndLocation(id) ?: return@runCatching false
                if (cityAndRegion.location == null) return@runCatching false
                val nodes = localDbInterface.getServersByDatacenter(cityAndRegion.datacenter.id)
                when {
                    nodes.isEmpty() -> {
                        false
                    }

                    else -> {
                        true
                    }
                }
            }.getOrDefault(false)
        }

        private suspend fun isConfigProfileAvailable(id: Int): Boolean =
            runCatching {
                localDbInterface.getConfigFileAsync(id)
                true
            }.getOrDefault(false)

        private suspend fun isLocationValid(
            id: Int,
            userPro: Int,
        ): Boolean {
            val locationSourceType = currentSourceType()
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

        private suspend fun isStaticIpAvailable(id: Int): Boolean =
            runCatching {
                localDbInterface.getStaticRegionByIDAsync(id)
                true
            }.getOrDefault(false)

        fun getSelectedCityAndRegion(): DatacenterAndLocation? {
            val selectedCityId = preferencesHelper.selectedCity
            if (selectedCityId == -1 || currentSourceType() != SelectedLocationType.CityLocation) {
                return null
            }
            return runCatching {
                localDbInterface.getDatacenterAndLocation(selectedCityId)
            }.getOrNull()
        }
    }
