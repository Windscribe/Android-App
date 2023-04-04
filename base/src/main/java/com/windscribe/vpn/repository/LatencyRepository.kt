package com.windscribe.vpn.repository

import com.windscribe.vpn.ServiceInteractor
import com.windscribe.vpn.api.IApiCallManager
import com.windscribe.vpn.backend.Util
import com.windscribe.vpn.backend.wireguard.WireGuardVpnProfile
import com.windscribe.vpn.commonutils.WindUtilities
import com.windscribe.vpn.localdatabase.LocalDbInterface
import com.windscribe.vpn.serverlist.entity.PingTime
import com.windscribe.vpn.services.ping.Ping
import com.windscribe.vpn.state.VPNConnectionStateManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.rx2.await
import java.net.Inet4Address
import java.net.InetSocketAddress
import java.net.Socket
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.roundToInt

@Singleton
class LatencyRepository @Inject constructor(
    val interactor: ServiceInteractor,
    val userRepository: dagger.Lazy<UserRepository>,
    val localDbInterface: LocalDbInterface,
    private val iApiCallManager: IApiCallManager,
    val vpnConnectionStateManager: dagger.Lazy<VPNConnectionStateManager>
) {
    enum class LatencyType {
        Servers, StaticIp, Config
    }

    private var _latencyEvent = MutableStateFlow(Pair(false, LatencyType.Servers))
    val latencyEvent: StateFlow<Pair<Boolean, LatencyType>> = _latencyEvent.asStateFlow()
    private val skipPing
        get() = vpnConnectionStateManager.get().isVPNActive() || WindUtilities.isOnline().not()

    suspend fun getPing(locationId: Int): PingTime {
        return localDbInterface.allPingTimes.await().firstOrNull { it.id == locationId }?.let {
            return it
        } ?: kotlin.run {
            kotlin.runCatching {
                val regionAndCity = localDbInterface.getCityAndRegion(locationId)
                val pingTime = getPingTime(
                    regionAndCity.city.getId(),
                    regionAndCity.region.id,
                    false,
                    regionAndCity.city.pro == 1
                )
                return@run getLatency(regionAndCity.city.pingHost, pingTime)
            }.getOrElse {
                val pingTime = PingTime()
                pingTime.id = locationId
                pingTime.pingTime = -1
                return pingTime
            }
        }
    }

    suspend fun updateAllServerLatencies(): Boolean {
        val cityPings = runCatching {
            interactor.getPingableCities().await().map { city ->
                if (skipPing) {
                    interactor.preferenceHelper.pingTestRequired = true
                    throw Exception()
                }
                val pingTime = getPingTime(city.getId(), city.regionID, false, city.pro == 1)
                getLatencyFromApi(city.pingHost, pingTime)
            }.map { pingTime ->
                interactor.addPing(pingTime).await()
                pingTime
            }.run {
                return@run updateState(this, LatencyType.Servers)
            }
        }
        val staticLatencyChanged = updateStaticIpLatency()
        val cityPingsChanged = cityPings.getOrElse { false }
        if (cityPingsChanged) {
            interactor.preferenceHelper.pingTestRequired = false
        }
        return cityPingsChanged || staticLatencyChanged
    }

    suspend fun updateFavouriteCityLatencies(): Boolean {
        val cityPings = runCatching {
            interactor.getAllFavourites().await().map {
                interactor.getCity(it.id).await()
            }.map { city ->
                if (skipPing) {
                    throw Exception()
                }
                val pingTime = getPingTime(city.getId(), city.regionID, false, city.pro == 1)
                getLatencyFromApi(city.pingHost, pingTime)
            }.map { pingTime ->
                interactor.addPing(pingTime).await()
                pingTime
            }.run {
                return@run updateState(this, LatencyType.Servers)
            }
        }
        return cityPings.getOrElse { false }
    }

    suspend fun updateStreamingServerLatencies(): Boolean {
        val cityPings = runCatching {
            interactor.getAllRegion().await().filter {
                it.region.locationType == "streaming"
            }.map {
                it.cities
            }.map { regionCities ->
                if (skipPing) {
                    throw Exception()
                }
                regionCities.map { city ->
                    val pingTime = getPingTime(city.getId(), city.regionID, false, city.pro == 1)
                    getLatencyFromApi(city.pingHost, pingTime)
                }
            }.map { regionPings ->
                regionPings.map { pingTime ->
                    interactor.addPing(pingTime).await()
                    pingTime
                }
            }.run {
                val list = mutableListOf<PingTime>()
                forEach {
                    it.forEach { pingTime ->
                        list.add(pingTime)
                    }
                }
                return@run updateState(list, LatencyType.Servers)
            }
        }
        return cityPings.getOrElse { false }
    }


    private suspend fun updateState(latencies: List<PingTime>, type: LatencyType): Boolean {
        if (type == LatencyType.Servers) {
            val lowestPingId = interactor.getLowestPingId().await()
            interactor.preferenceHelper.lowestPingId = lowestPingId
        }
        if (latencies.isNotEmpty()) {
            _latencyEvent.update {
                it.copy(it.first.not(), type)
            }
        }
        return latencies.isNotEmpty()
    }

    suspend fun updateStaticIpLatency(): Boolean {
        val staticPings = runCatching {
            interactor.getAllStaticRegions().await().map { region ->
                if (skipPing) {
                    throw Exception()
                }
                if (region.staticIpNode != null) {
                    val pingTime =
                        getPingTime(region.id, region.ipId, isStatic = true, isPro = true)
                    getLatency(region.staticIpNode.ip.toString(), pingTime)
                } else {
                    throw Exception("Static region has no ip")
                }
            }.map { pingTime ->
                interactor.addPing(pingTime).await()
                pingTime
            }.run {
                return@run updateState(this, LatencyType.StaticIp)
            }
        }
        return staticPings.getOrElse { false }
    }

    suspend fun updateConfigLatencies(): Boolean {
        return runCatching {
            interactor.getAllConfigs().map { configFile ->
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
                interactor.addPing(pingTime).await()
                pingTime
            }.run {
                return@run updateState(this, LatencyType.Config)
            }
        }.getOrElse { false }
    }

    private fun getPingTime(id: Int, regionId: Int, isStatic: Boolean, isPro: Boolean): PingTime {
        return PingTime().apply {
            ping_id = id
            pro = isPro
            setRegionId(regionId)
            setStatic(isStatic)
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
            val socket = Socket()
            socket.connect(address, 500)
            socket.close()
            val probeFinish = System.currentTimeMillis()
            val time = (probeFinish - dnsResolved).toInt()
            pingTime.setPingTime(time)
            return@runCatching pingTime
        }.getOrElse {
            pingTime.setPingTime(-1)
            return pingTime
        }
    }

    private suspend fun getLatencyFromApi(
        host: String,
        ping: PingTime,
    ): PingTime {
        return iApiCallManager.getLatency(host).mapCatching {
            return@mapCatching ping.apply {
                pingTime = it.dataClass?.rtt?.toInt()?.div(1000) ?: -1
            }
        }.recoverCatching {
            return@recoverCatching ping.apply {
                pingTime = -1
            }
        }.getOrThrow()
    }
}