package com.windscribe.vpn.repository

import com.windscribe.vpn.Windscribe.Companion.appContext
import com.windscribe.vpn.api.IApiCallManager
import com.windscribe.vpn.apppreference.PreferencesHelper
import com.windscribe.vpn.backend.Util
import com.windscribe.vpn.backend.wireguard.WireGuardVpnProfile
import com.windscribe.vpn.commonutils.WindUtilities
import com.windscribe.vpn.constants.PreferencesKeyConstants
import com.windscribe.vpn.exceptions.WindScribeException
import com.windscribe.vpn.localdatabase.LocalDbInterface
import com.windscribe.vpn.serverlist.entity.City
import com.windscribe.vpn.serverlist.entity.PingTime
import com.windscribe.vpn.serverlist.entity.StaticRegion
import com.windscribe.vpn.services.ping.Ping
import com.windscribe.vpn.state.VPNConnectionStateManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.rx2.await
import kotlinx.coroutines.withTimeoutOrNull
import org.slf4j.LoggerFactory
import java.net.Inet4Address
import java.net.InetSocketAddress
import java.net.Socket
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.roundToInt
import kotlin.time.DurationUnit
import kotlin.time.toDuration

@Singleton
class LatencyRepository @Inject constructor(
        private val preferencesHelper: PreferencesHelper,
        private val localDbInterface: LocalDbInterface,
        private val iApiCallManager: IApiCallManager,
        private val vpnConnectionStateManager: dagger.Lazy<VPNConnectionStateManager>
) {
    enum class LatencyType {
        Servers, StaticIp, Config
    }

    companion object {
        const val MINIMUM_PING_VALIDATION_MINUTES = 5
    }
    private val logger = LoggerFactory.getLogger("latency")
    private var _latencyEvent = MutableStateFlow(Pair(false, LatencyType.Servers))
    val latencyEvent: StateFlow<Pair<Boolean, LatencyType>> = _latencyEvent.asStateFlow()
    private val skipPing
        get() = vpnConnectionStateManager.get().isVPNActive() || WindUtilities.isOnline().not()

    private suspend fun pingJobAsync(city: City): Deferred<PingTime> {
        val context = currentCoroutineContext()
        return CoroutineScope(context).async {
            val pingTime = getPingTime(city.getId(), city.regionID, false, city.pro == 1)
            if (appContext.isRegionRestricted) {
                return@async getLatency(city.pingIp, pingTime)
            } else {
                return@async getLatencyFromApi(city.pingHost, city.pingIp, pingTime)
            }
        }
    }

    private suspend fun pingJobAsync(region: StaticRegion): Deferred<PingTime> {
        val context = currentCoroutineContext()
        return CoroutineScope(context).async {
            val pingTime = getPingTime(region.id, region.ipId, isStatic = true, isPro = true)
            if (appContext.isRegionRestricted) {
                return@async getLatency(region.staticIpNode.ip, pingTime)
            } else {
                return@async getLatencyFromApi(region.pingHost, region.staticIpNode.ip, pingTime)
            }
        }
    }

    suspend fun updateAllServerLatencies(): Boolean {
        val currentIp = preferencesHelper.getResponseString(PreferencesKeyConstants.USER_IP)
        val validPings = localDbInterface.allPingTimes.await().filter {
            val isSameIp = currentIp == it.ip
            val isWithinTimeLimit = (System.currentTimeMillis() - it.updatedAt).toDuration(DurationUnit.MILLISECONDS).inWholeMinutes <= MINIMUM_PING_VALIDATION_MINUTES
            val isPingValid = it.pingTime != -1
            return@filter isSameIp && isWithinTimeLimit && isPingValid
        }.map { it.id }
        val pingJobs = localDbInterface.pingableCities.await()
                .filter { city ->
                    return@filter !validPings.contains(city.id)
                }.map {
                    pingJobAsync(it)
                }
        logger.debug("Requesting latency for ${pingJobs.count()} cities.")
        val cityPings = runCatching {
            pingJobs.awaitAll().map { pingTime ->
                localDbInterface.addPing(pingTime).await()
                pingTime
            }.run {
                return@run updateLatencyEvent(this, LatencyType.Servers)
            }
        }
        val staticLatencyChanged = updateStaticIpLatency()
        val cityPingsChanged = cityPings.getOrElse { false }
        if (cityPingsChanged) {
            preferencesHelper.pingTestRequired = false
        }
        return cityPingsChanged || staticLatencyChanged
    }

    suspend fun updateFavouriteCityLatencies(): Boolean {
        val cities =
                localDbInterface.favourites.await().map { localDbInterface.getCityByID(it.id).await() }
        val pingJobs = cities.map { pingJobAsync(it) }
        val cityPings = runCatching {
            pingJobs.awaitAll().map { pingTime ->
                localDbInterface.addPing(pingTime).await()
                pingTime
            }.run {
                return@run updateLatencyEvent(this, LatencyType.Servers)
            }
        }
        return cityPings.getOrElse { false }
    }

    suspend fun updateStreamingServerLatencies(): Boolean {
        val cities = localDbInterface.allRegion.await().filter {
            it.region.locationType == "streaming"
        }.map { it.cities }.reduce { l1, l2 -> l1.plus(l2) }
        val pingJobs = cities.map { pingJobAsync(it) }
        val cityPings = runCatching {
            pingJobs.awaitAll().map { pingTime ->
                localDbInterface.addPing(pingTime).await()
                pingTime
            }.run {
                return@run updateLatencyEvent(this, LatencyType.Servers)
            }
        }
        return cityPings.getOrElse { false }
    }


    private suspend fun updateLatencyEvent(latencies: List<PingTime>, type: LatencyType): Boolean {
        if (type == LatencyType.Servers) {
            val lowestPingId = localDbInterface.lowestPingId.await()
            preferencesHelper.lowestPingId = lowestPingId
        }
        if (latencies.isNotEmpty()) {
            _latencyEvent.update {
                it.copy(it.first.not(), type)
            }
        }
        return latencies.isNotEmpty()
    }

    suspend fun updateStaticIpLatency(): Boolean {
        val regions = localDbInterface.allStaticRegions.await()
        val pingJobs = regions.map { pingJobAsync(it) }
        val staticPings = runCatching {
            pingJobs.awaitAll().map { pingTime ->
                localDbInterface.addPing(pingTime).await()
                pingTime
            }.run {
                return@run updateLatencyEvent(this, LatencyType.StaticIp)
            }
        }
        return staticPings.getOrElse { false }
    }

    suspend fun updateConfigLatencies(): Boolean {
        return runCatching {
            localDbInterface.allConfigs.await().map { configFile ->
                if (skipPing) {
                    throw Exception()
                }
                val hostname: String? = if (WireGuardVpnProfile.validConfig(configFile.content)) {
                    WireGuardVpnProfile.getHostName(configFile.content)
                } else {
                    Util.getHostNameFromOpenVPNConfig(configFile.content)
                }
                val pingTime =
                        getPingTime(configFile.getPrimaryKey(), 0, isStatic = false, isPro = false)
                getLatency(hostname, pingTime)
            }.map { pingTime ->
                localDbInterface.addPing(pingTime).await()
                pingTime
            }.run {
                return@run updateLatencyEvent(this, LatencyType.Config)
            }
        }.getOrElse { false }
    }

    private fun getPingTime(id: Int, regionId: Int, isStatic: Boolean, isPro: Boolean): PingTime {
        return PingTime().apply {
            ping_id = id
            pro = isPro
            setRegionId(regionId)
            setStatic(isStatic)
            setUpdatedAt(System.currentTimeMillis())
            preferencesHelper.getResponseString(PreferencesKeyConstants.USER_IP)?.let {
                setIp(it)
            }
        }
    }

    private fun getLatency(ip: String?, pingTime: PingTime): PingTime {
        val result = runCatching<PingTime> {
            val inetAddress = Inet4Address.getByName(ip)
            val ping = Ping()
            val timeMs = ping.run(inetAddress, 500)
            pingTime.setPingTime(timeMs.toFloat().roundToInt())
            return@runCatching pingTime
        }
        return result.getOrNull() ?: run {
            return getLatencyFromSocketConnection(ip, pingTime)
        }
    }

    private fun getLatencyFromSocketConnection(ip: String?, pingTime: PingTime): PingTime {
        return runCatching {
            val dnsResolved = System.currentTimeMillis()
            val address = InetSocketAddress(ip, 443)
            Socket().apply {
                connect(address, 500)
                close()
            }
            val probeFinish = System.currentTimeMillis()
            val time = (probeFinish - dnsResolved).toInt()
            pingTime.setPingTime(time)
            return@runCatching pingTime
        }.getOrElse { pingTime.apply { setPingTime(-1) } }
    }

    private suspend fun getLatencyFromApi(
            host: String?, ip: String,
            ping: PingTime,
    ): PingTime {
        if (skipPing) {
            throw WindScribeException("Latency check not allowed once vpn is connected.")
        }
        if (host == null) {
            return ping.apply { pingTime = -1 }
        }
        val updatedPing = withTimeoutOrNull(3000) {
            iApiCallManager.getLatency(host, ip).mapCatching {
                return@mapCatching ping.apply {
                    pingTime = it.dataClass?.rtt?.toInt()?.div(1000) ?: -1
                }
            }.getOrElse { ping.apply { pingTime = -1 } }
        }
        return ping.apply {
            if (updatedPing == null) {
                pingTime = -1
            }
        }
    }
}