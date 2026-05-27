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
import com.windscribe.vpn.api.response.LocationResponse
import com.windscribe.vpn.api.response.ServerInventory
import com.windscribe.vpn.api.response.ServerResponse
import com.windscribe.vpn.api.response.UserSessionResponse
import com.windscribe.vpn.apppreference.PreferencesHelper
import com.windscribe.vpn.commonutils.Ext.result
import com.windscribe.vpn.localdatabase.LocalDbInterface
import com.windscribe.vpn.serverlist.entity.Datacenter
import com.windscribe.vpn.serverlist.entity.Location
import com.windscribe.vpn.serverlist.entity.LocationAndDatacenters
import com.windscribe.vpn.serverlist.entity.Server
import com.windscribe.vpn.serverlist.entity.ServerMapState
import dagger.Lazy
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch
import org.slf4j.LoggerFactory
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.collections.mapNotNull

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
    private val userRepository: Lazy<UserRepository>,
    private val preferenceHelper: PreferencesHelper,
    private val favouriteRepository: FavouriteRepository
) {
    private val logger = LoggerFactory.getLogger("server_list_repository")
    private var _events = MutableSharedFlow<List<LocationAndDatacenters>>(replay = 1)
    val locationAndDatacenters: SharedFlow<List<LocationAndDatacenters>> = _events
    private var _locationJsonToExport = MutableStateFlow("")
    val locationJsonToExport: StateFlow<String> = _locationJsonToExport
    private val _customCities = MutableStateFlow<List<CustomCity>>(listOf())
    val customCities: StateFlow<List<CustomCity>> = _customCities
    private val _customRegions = MutableStateFlow<List<CustomRegion>>(listOf())
    val customRegions: StateFlow<List<CustomRegion>> = _customRegions
    private val _serversState = MutableStateFlow<ServerMapState>(ServerMapState.Loading)
    val serversState: StateFlow<ServerMapState> = _serversState

    init {
        load()
        observeAllServers()
    }

    fun load() {
        scope.launch {
            _events.emit(localDbInterface.getAllLocationsAsync())
            observeServerLocations()
        }
        loadCustomLocationsJson()
    }

    suspend fun update() {
        val isFirstLoad = preferenceHelper.serverRevision == 0L
        val migrationRequired = preferenceHelper.migrationRequired
        if (isFirstLoad || migrationRequired) {
            if (isFirstLoad) {
                logger.debug("V2: First load - fetching locations and full server list")
            } else {
                logger.debug("Stale server list - fetching locations and full server list")
            }
            val locationsResult = loadLocations()
            if (locationsResult is CallResult.Error) {
                throw Exception("Failed to load locations list: ${locationsResult.errorMessage}")
            }
            val serversResult = loadFullServerList(backup = false)
            if (serversResult is CallResult.Error) {
                throw Exception("Failed to load full server list ${serversResult.errorMessage}")
            }
            if (migrationRequired) {
                preferenceHelper.migrationRequired = false
            }
            logger.debug("Full update completed successfully")
        } else {
            val backup = preferenceHelper.getBackupParameter()
            val sessionResult = result<UserSessionResponse> {
                apiCallManager.getSessionGeneric(null, backup)
            }
            when (sessionResult) {
                is CallResult.Success -> {
                    val userSession = sessionResult.data
                    userRepository.get().reload(userSession)
                    userSession.serverInventory?.let { serverInventory ->
                        updateServersDelta(serverInventory)
                    } ?: run {
                        logger.debug("V2: No server inventory delta - servers are up to date")
                    }
                }
                is CallResult.Error -> {
                    logger.error("V2: Error getting session: ${sessionResult.errorMessage}")
                    throw Exception("Failed to get session")
                }
            }
        }
    }

    private fun observeServerLocations() {
        scope.launch {
            locationAndDatacenters.collect { locationsAndDatacenters ->
                buildLocationsJson(locationsAndDatacenters)
            }
        }
    }

    private fun buildLocationsJson(regions: List<LocationAndDatacenters>) {
        // Run JSON serialization on IO dispatcher to avoid blocking
        // and catch OOM errors gracefully
        scope.launch(Dispatchers.IO) {
            try {
                val customLocationData = CustomLocationsData(regions.mapNotNull { region ->
                    val location = region.location ?: return@mapNotNull null
                    val cities = region.datacenters.map { CustomCity(it.id, it.nodeName ?: "", it.nickName ?: "") }
                    return@mapNotNull CustomRegion(location.id, location.name ?: "", cities)
                })
                val json = Gson().toJson(customLocationData)
                _locationJsonToExport.value = json
            } catch (e: OutOfMemoryError) {
                logger.error("OutOfMemoryError while building locations JSON - data too large", e)
                _locationJsonToExport.value = ""
            } catch (e: Exception) {
                logger.error("Error building locations JSON", e)
                _locationJsonToExport.value = ""
            }
        }
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
                _events.emit(localDbInterface.getAllLocationsAsync())
                favouriteRepository.load()
            }
        } catch (_: Exception) {
            _customCities.value = listOf()
            _customRegions.value = listOf()
            scope.launch {
                _events.emit(localDbInterface.getAllLocationsAsync())
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

    /**
     * V2 Federated Server List: Load static locations and datacenters
     * This should be called first in the V2 flow
     */
    private suspend fun loadLocations(): CallResult<Unit> {
        val locationsResult = result<LocationResponse> {
            apiCallManager.getLocations()
        }

        return when (locationsResult) {
            is CallResult.Success -> {
                try {
                    val locationResponse = locationsResult.data
                    val locations = mutableListOf<Location>()
                    val datacenters = mutableListOf<Datacenter>()
                    for (location in locationResponse.locations) {
                        val loc = Location(
                            location.id,
                            location.name,
                            location.countryCode,
                            location.shortName,
                            location.sortOrder,
                            location.continent
                        )
                        locations.add(loc)
                        for (datacenter in location.datacenters) {
                            val dc = Datacenter(
                                location.id,
                                datacenter.id,
                                datacenter.city,
                                datacenter.nick ?: "",
                                datacenter.gps,
                                datacenter.tz,
                                datacenter.iata,
                                datacenter.status,
                                datacenter.p2p,
                                datacenter.premium,
                                datacenter.wgPubkey,
                                datacenter.wgEndpoint,
                                datacenter.ovpnX509,
                                datacenter.linkSpeed
                            )
                            datacenters.add(dc)
                        }
                    }
                    // Save to database
                    localDbInterface.addToLocations(locations)
                    localDbInterface.addToDatacenters(datacenters)
                    CallResult.Success(Unit)
                } catch (e: Exception) {
                    logger.error("V2: Error parsing locations response", e)
                    CallResult.Error(-1, e.message ?: "Failed to parse locations")
                }
            }
            is CallResult.Error -> {
                logger.error("V2: Error loading locations: ${locationsResult.errorMessage}")
                locationsResult
            }
        }
    }

    /**
     * V2 Federated Server List: Load full server list with revision
     * This should be called after loadLocations()
     */
    private suspend fun loadFullServerList(backup: Boolean = false): CallResult<Unit> {
        val backupParam = if (backup) 1 else preferenceHelper.getBackupParameter()
        val serversResult = result<ServerResponse> {
            apiCallManager.getServers(backupParam)
        }

        return when (serversResult) {
            is CallResult.Success -> {
                try {
                    val serverResponse = serversResult.data
                    val servers = serverResponse.servers.map { serverData ->
                        Server(
                            id = serverData.id,
                            hostname = serverData.hostname,
                            ip = serverData.ip,
                            ip2 = serverData.ip2,
                            ip3 = serverData.ip3,
                            datacenterId = serverData.datacenterId,
                            weight = serverData.weight,
                            health = serverData.health,
                            ipv6 = serverData.ipv6
                        )
                    }
                    localDbInterface.deleteAllServers()
                    localDbInterface.addServers(servers)
                    preferenceHelper.serverRevision = serverResponse.revision
                    load()
                    CallResult.Success(Unit)
                } catch (e: Exception) {
                    logger.error("V2: Error parsing servers response", e)
                    CallResult.Error(-1, e.message ?: "Failed to parse servers")
                }
            }
            is CallResult.Error -> {
                logger.error("V2: Error loading servers: ${serversResult.errorMessage}")
                serversResult
            }
        }
    }

    /**
     * V2 Federated Server List: Apply delta update from session
     * This should be called when ServerInventory is present in session response
     */
    private suspend fun updateServersDelta(serverInventory: ServerInventory) {
        when (serverInventory.action) {
            "hold" -> {
                logger.debug("V2: Hold action received - not updating this session")
                return
            }
            "delta" -> {
                val enabledCount = serverInventory.enabled?.size ?: 0
                val disabledCount = serverInventory.disabled?.size ?: 0
                if (enabledCount == 0 && disabledCount == 0) {
                    return
                }
                logger.debug("V2: Delta update - enabling $enabledCount servers, disabling $disabledCount servers")
                serverInventory.enabled?.let { enabledServers ->
                    val servers = enabledServers.map { serverData ->
                        Server(
                            id = serverData.id,
                            hostname = serverData.hostname,
                            ip = serverData.ip,
                            ip2 = serverData.ip2,
                            ip3 = serverData.ip3,
                            datacenterId = serverData.datacenterId,
                            weight = serverData.weight,
                            health = serverData.health
                        )
                    }
                    localDbInterface.addServers(servers)
                }
                // Remove disabled servers
                serverInventory.disabled?.let { disabledServers ->
                    val serverIds = disabledServers.map { it.id }
                    localDbInterface.deleteServers(serverIds)
                }
                // Update revision
                preferenceHelper.serverRevision = serverInventory.revision
                load()
            }
            else -> {
                logger.warn("V2: Unknown action: ${serverInventory.action}")
            }
        }
    }

    /**
     * Observes all servers from the database and maintains a map grouped by datacenter ID.
     * This raw state can be used by ViewModels to calculate health and availability.
     */
    private fun observeAllServers() {
        scope.launch(Dispatchers.IO) {
            try {
                localDbInterface.observeAllServers()
                    .flowOn(Dispatchers.IO)
                    .collect { servers ->
                        // Skip empty server list emissions (happens during database updates)
                        if (servers.isEmpty()) {
                            return@collect
                        }
                        val serversMap = servers.groupBy { it.datacenterId }
                        _serversState.value = ServerMapState.Success(serversMap)
                    }
            } catch (e: Exception) {
                logger.error("Error observing servers", e)
                _serversState.value = ServerMapState.Error("Failed to load servers: ${e.message}")
            }
        }
    }
}
