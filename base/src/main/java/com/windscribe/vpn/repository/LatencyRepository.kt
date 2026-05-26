package com.windscribe.vpn.repository

import com.windscribe.vpn.apppreference.PreferencesHelper
import com.windscribe.vpn.backend.Util
import com.windscribe.vpn.backend.wireguard.WireGuardVpnProfile
import com.windscribe.vpn.localdatabase.LocalDbInterface
import com.windscribe.vpn.serverlist.entity.Datacenter
import com.windscribe.vpn.serverlist.entity.PingTime
import com.windscribe.vpn.serverlist.entity.StaticRegion
import com.windscribe.vpn.services.ping.Pinger
import com.windscribe.vpn.state.VPNConnectionStateManager
import dagger.Lazy
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import org.slf4j.LoggerFactory
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.DurationUnit
import kotlin.time.toDuration

@Singleton
class LatencyRepository @Inject constructor(
    private val preferencesHelper: PreferencesHelper,
    private val localDbInterface: LocalDbInterface,
    private val vpnConnectionStateManager: Lazy<VPNConnectionStateManager>,
    private val pinger: Pinger,
    private val isOnline: StateFlow<Boolean>
) {
    enum class LatencyType {
        Servers, StaticIp, Config
    }

    companion object {
        const val MINIMUM_PING_VALIDATION_MINUTES = 5
        private const val PING_TIMEOUT_MS = 500
        private const val BATCH_SIZE = 20
    }

    private val logger = LoggerFactory.getLogger("ping")
    private val _latencyEvent = MutableStateFlow(Pair(false, LatencyType.Servers))
    val latencyEvent: StateFlow<Pair<Boolean, LatencyType>> = _latencyEvent.asStateFlow()

    private val skipPing: Boolean
        get() = vpnConnectionStateManager.get().isVPNActive() || !isOnline.value

    private suspend fun pingJobAsync(city: Datacenter): Deferred<PingTime?> {
        val context = currentCoroutineContext()
        return CoroutineScope(context).async {
            val pingTime = newPingTime(city.getId(), city.regionID, isStatic = false, isPro = true)
            val pingIpAndHost = localDbInterface.getPingIpAndHost(city.id) ?: return@async null
            getLatency(pingIpAndHost.first, pingTime)
        }
    }

    private suspend fun pingJobAsync(region: StaticRegion): Deferred<PingTime> {
        val context = currentCoroutineContext()
        return CoroutineScope(context).async {
            val pingTime = newPingTime(region.id, region.ipId, isStatic = true, isPro = true)
            getLatency(region.staticIpNode.ip, pingTime)
        }
    }

    suspend fun updateAllServerLatencies(): Boolean {
        if (skipPing) return false
        val currentIp = preferencesHelper.userIP
        val validPings = localDbInterface.getAllPingsAsync().filter {
            val isSameIp = currentIp != null && currentIp == it.ip
            val isWithinTimeLimit = (System.currentTimeMillis() - it.updatedAt)
                .toDuration(DurationUnit.MILLISECONDS).inWholeMinutes <= MINIMUM_PING_VALIDATION_MINUTES
            val isPingValid = it.pingTime != -1
            isSameIp && isWithinTimeLimit && isPingValid
        }.map { it.id }
        val citiesToPing = localDbInterface.getPingableDatacenters()
            .filter { !validPings.contains(it.id) }
        logger.debug("Requesting latency for ${citiesToPing.count()} cities.")

        val allPingResults = mutableListOf<PingTime>()
        val cityPings = runCatching {
            citiesToPing.chunked(BATCH_SIZE).forEach { batch ->
                val batchResults = batch.map { pingJobAsync(it) }.awaitAll().filterNotNull()
                batchResults.forEach { pingTime ->
                    runCatching {
                        localDbInterface.addPing(pingTime)
                        allPingResults.add(pingTime)
                    }
                }
            }
            val successCount = allPingResults.count { it.pingTime > 0 && !it.isStatic }
            val failureCount = allPingResults.count { it.pingTime == -1 && !it.isStatic }
            val attemptedCount = allPingResults.count { !it.isStatic }
            logger.debug("Latency: $successCount successful, $failureCount failed out of $attemptedCount datacenters (${citiesToPing.count()} total).")
            updateLatencyEvent(allPingResults, LatencyType.Servers)
        }
        val staticLatencyChanged = updateStaticIpLatency()
        val cityPingsChanged = cityPings.getOrElse { false }
        if (cityPingsChanged) {
            preferencesHelper.pingTestRequired = false
        }
        return cityPingsChanged || staticLatencyChanged
    }

    suspend fun updateFavouriteCityLatencies(): Boolean {
        if (skipPing) return false
        val cities = localDbInterface.getFavouritesAsync().mapNotNull {
            runCatching { localDbInterface.getDatacenterByIDAsync(it.id) }.getOrNull()
        }
        val pingJobs = cities.map { pingJobAsync(it) }
        return runCatching {
            val results = pingJobs.awaitAll().filterNotNull().onEach { localDbInterface.addPing(it) }
            updateLatencyEvent(results, LatencyType.Servers)
        }.getOrElse { false }
    }

    suspend fun updateStaticIpLatency(): Boolean {
        if (skipPing) return false
        val regions = localDbInterface.getAllStaticRegions()
        val pingJobs = regions.map { pingJobAsync(it) }
        return runCatching {
            val results = pingJobs.awaitAll().onEach { localDbInterface.addPing(it) }
            updateLatencyEvent(results, LatencyType.StaticIp)
        }.getOrElse { false }
    }

    suspend fun updateConfigLatencies(): Boolean {
        if (skipPing) return false
        return runCatching {
            val results = localDbInterface.getAllConfigs().map { configFile ->
                val hostname: String? = if (WireGuardVpnProfile.validConfig(configFile.content)) {
                    WireGuardVpnProfile.getHostName(configFile.content)
                } else {
                    Util.getHostNameFromOpenVPNConfig(configFile.content)
                }
                val pingTime = newPingTime(configFile.getPrimaryKey(), 0, isStatic = false, isPro = false)
                getLatency(hostname, pingTime)
            }.onEach { localDbInterface.addPing(it) }
            updateLatencyEvent(results, LatencyType.Config)
        }.getOrElse { false }
    }

    private suspend fun updateLatencyEvent(latencies: List<PingTime>, type: LatencyType): Boolean {
        if (type == LatencyType.Servers) {
            preferencesHelper.lowestPingId = localDbInterface.getLowestPingIdAsync()
        }
        if (latencies.isNotEmpty()) {
            _latencyEvent.update { it.copy(it.first.not(), type) }
        }
        return latencies.isNotEmpty()
    }

    private fun newPingTime(id: Int, regionId: Int, isStatic: Boolean, isPro: Boolean): PingTime {
        return PingTime().apply {
            ping_id = id
            pro = isPro
            setRegionId(regionId)
            setStatic(isStatic)
            setUpdatedAt(System.currentTimeMillis())
            preferencesHelper.userIP?.let { setIp(it) }
        }
    }

    private suspend fun getLatency(host: String?, pingTime: PingTime): PingTime {
        if (host == null) {
            pingTime.setPingTime(-1)
            return pingTime
        }
        pingTime.setPingTime(pinger.ping(host, PING_TIMEOUT_MS))
        return pingTime
    }
}
