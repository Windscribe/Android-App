package com.windscribe.vpn.repository

import com.windscribe.vpn.BuildConfig
import com.windscribe.vpn.model.OpenVPNConnectionInfo
import com.wsnet.lib.WSNetEmergencyConnect
import kotlinx.coroutines.suspendCancellableCoroutine
import org.slf4j.LoggerFactory
import kotlin.coroutines.resume

interface EmergencyConnectRepository {
    suspend fun getConnectionInfo(): Result<List<OpenVPNConnectionInfo>>
}

/**
 * Implementation of emergency connect repository.
 */
class EmergencyConnectRepositoryImpl(private val emergencyConnect: WSNetEmergencyConnect) : EmergencyConnectRepository {

    /** Get emergency connect profiles.
     * @return
    List of [OpenVPNConnectionInfo]
     */
    override suspend fun getConnectionInfo(): Result<List<OpenVPNConnectionInfo>> {
        if (BuildConfig.DEV) {
            return Result.failure(Throwable("Emergency connect not available in staging version."))
        }
        return runCatching {
            suspendCancellableCoroutine { cont ->
                val callback = emergencyConnect.getIpEndpoints { ips ->
                    val configs = ips.map { ipEndpoint ->
                        val proto = if (ipEndpoint.protocol() == 0) "udp" else "tcp"
                        OpenVPNConnectionInfo(
                                emergencyConnect.ovpnConfig(),
                                ipEndpoint.ip(),
                                ipEndpoint.port().toString(),
                                proto,
                                emergencyConnect.username(),
                                emergencyConnect.password()
                        )
                    }.shuffled()
                    cont.resume(configs)
                }
                cont.invokeOnCancellation {
                    callback.cancel()
                }
            }
        }
    }
}