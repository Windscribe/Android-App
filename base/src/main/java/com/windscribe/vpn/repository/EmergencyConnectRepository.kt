package com.windscribe.vpn.repository

import com.windscribe.vpn.BuildConfig
import com.windscribe.vpn.model.OpenVPNConnectionInfo
import com.wsnet.lib.WSNet
import com.windscribe.vpn.wsnet.WSNetWrapper
import dagger.Lazy
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

interface EmergencyConnectRepository {
    suspend fun getConnectionInfo(): Result<List<OpenVPNConnectionInfo>>
}

/**
 * Implementation of emergency connect repository.
 */
class EmergencyConnectRepositoryImpl(private val wsNetWrapper: WSNetWrapper) : EmergencyConnectRepository {

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
                wsNetWrapper.withWSNet { wsNet ->
                    try {
                        val callback = wsNet.emergencyConnect().getIpEndpoints { ips ->
                            try {
                                val configs = ips.map { ipEndpoint ->
                                    val proto = if (ipEndpoint.protocol() == 0) "udp" else "tcp"
                                    OpenVPNConnectionInfo(
                                        wsNet.emergencyConnect().ovpnConfig(),
                                            ipEndpoint.ip(),
                                            ipEndpoint.port().toString(),
                                            proto,
                                        wsNet.emergencyConnect().username(),
                                        wsNet.emergencyConnect().password()
                                    )
                                }.shuffled()
                                cont.resume(configs)
                            } catch (e: Exception) {
                                cont.resume(emptyList())
                            }
                        }
                        cont.invokeOnCancellation {
                            callback.cancel()
                        }
                    } catch (e: Exception) {
                        cont.resume(emptyList())
                    }
                } ?: cont.resume(emptyList())
            }
        }
    }
}