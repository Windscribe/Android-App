package com.windscribe.vpn.repository

import com.windscribe.vpn.ServiceInteractor
import com.windscribe.vpn.backend.Util
import com.windscribe.vpn.backend.wireguard.WireGuardVpnProfile
import com.windscribe.vpn.commonutils.WindUtilities
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
    val vpnConnectionStateManager: dagger.Lazy<VPNConnectionStateManager>
) {
    enum class LatencyType {
        Servers, StaticIp, Config
    }

    private var _latencyEvent = MutableStateFlow(Pair(false, LatencyType.Servers))
    val latencyEvent: StateFlow<Pair<Boolean, LatencyType>> = _latencyEvent.asStateFlow()
    private val skipPing
        get() = vpnConnectionStateManager.get().isVPNActive() || WindUtilities.isOnline().not()

    suspend fun updateAllServerLatencies(): Boolean {
        val cityPings = runCatching {
            interactor.getPingableCities().await().map { city ->
                if (skipPing) {
                    throw Exception()
                }
                getLatency(city.getId(), city.regionID, city.pingIp, false, city.pro == 1)
            }.map { pingTime ->
                interactor.addPing(pingTime).await()
                pingTime
            }.run {
                return@run updateState(this, LatencyType.Servers)
            }
        }
        val staticPings = updateStaticIp()
        return cityPings.getOrElse { false } || staticPings
    }

    suspend fun updateFavourites(): Boolean {
        val cityPings = runCatching {
            interactor.getAllFavourites().await().map {
                interactor.getCity(it.id).await()
            }.map { city ->
                if (skipPing) {
                    throw Exception()
                }
                getLatency(city.getId(), city.regionID, city.pingIp, false, city.pro == 1)
            }.map { pingTime ->
                interactor.addPing(pingTime).await()
                pingTime
            }.run {
                return@run updateState(this, LatencyType.Servers)
            }
        }
        return cityPings.getOrElse { false }
    }

    suspend fun updateStreamingServers(): Boolean {
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
                    getLatency(city.getId(), city.regionID, city.pingIp, false, city.pro == 1)
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

    suspend fun updateStaticIp(): Boolean {
        val staticPings = runCatching {
            interactor.getAllStaticRegions().await().map { region ->
                if (skipPing) {
                    throw Exception()
                }
                if (region.staticIpNode != null) {
                    getLatency(
                        region.id,
                        region.ipId,
                        region.staticIpNode.ip.toString(),
                        isStatic = true,
                        isPro = true
                    )
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

    suspend fun updateConfigs(): Boolean {
        val configPings = runCatching {
            interactor.getAllConfigs().map { configFile ->
                if (skipPing) {
                    throw Exception()
                }
                val hostname: String? = if (WireGuardVpnProfile.validConfig(configFile.content)) {
                    WireGuardVpnProfile.getHostName(configFile.content)
                } else {
                    Util.getHostNameFromOpenVPNConfig(configFile.content)
                }
                getLatency(configFile.getPrimaryKey(), 0, hostname, isStatic = false, isPro = false)
            }.map { pingTime ->
                interactor.addPing(pingTime).await()
                pingTime
            }.run {
                return@run updateState(this, LatencyType.Config)
            }
        }
        return configPings.getOrElse { false }
    }

    private fun getLatency(
        id: Int, regionId: Int, ip: String?, isStatic: Boolean, isPro: Boolean
    ): PingTime {
        if (ip == null) {
            throw Exception()
        }
        val result = runCatching<PingTime> {
            val inetAddress = Inet4Address.getByName(ip)
            val ping = Ping()
            val pingTime = PingTime()
            val timeMs = ping.run(inetAddress, 500)
            pingTime.id = id
            pingTime.isPro = isPro
            pingTime.setRegionId(regionId)
            pingTime.setPingTime(timeMs.toFloat().roundToInt())
            pingTime.setStatic(isStatic)
            return@runCatching pingTime
        }
        return result.getOrNull() ?: run {
            return getLatencyFromSocketConnection(id, regionId, ip, isStatic, isPro)
        }
    }

    private fun getLatencyFromSocketConnection(
        id: Int, regionId: Int, ip: String?, isStatic: Boolean, isPro: Boolean
    ): PingTime {
        val result = runCatching {
            val pingTime = PingTime()
            val dnsResolved = System.currentTimeMillis()
            val address = InetSocketAddress(ip, 443)
            val socket = Socket()
            socket.connect(address, 500)
            socket.close()
            val probeFinish = System.currentTimeMillis()
            pingTime.id = id
            pingTime.isPro = isPro
            pingTime.setRegionId(regionId)
            val time = (probeFinish - dnsResolved).toInt()
            pingTime.setPingTime(time)
            pingTime.setStatic(isStatic)
            pingTime
        }
        return result.getOrNull() ?: run {
            val pingTime = PingTime()
            pingTime.id = id
            pingTime.isPro = isPro
            pingTime.setRegionId(regionId)
            pingTime.setPingTime(-1)
            pingTime.setStatic(isStatic)
            return pingTime
        }
    }
}