package com.windscribe.vpn.repository

import com.windscribe.vpn.BuildConfig
import com.windscribe.vpn.model.OpenVPNConnectionInfo
import com.wsnet.lib.WSNet
import dagger.Lazy
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

interface EmergencyConnectRepository {
    suspend fun getConnectionInfo(): Result<List<OpenVPNConnectionInfo>>
}

/**
 * Implementation of emergency connect repository.
 */
class EmergencyConnectRepositoryImpl(private val wsNet: Lazy<WSNet>) : EmergencyConnectRepository {

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
                val callback = wsNet.get().emergencyConnect().getIpEndpoints { ips ->
                    val configs = ips.map { ipEndpoint ->
                        val proto = if (ipEndpoint.protocol() == 0) "udp" else "tcp"
                        OpenVPNConnectionInfo(
                            wsNet.get().emergencyConnect().ovpnConfig(),
                                ipEndpoint.ip(),
                                ipEndpoint.port().toString(),
                                proto,
                            wsNet.get().emergencyConnect().username(),
                            wsNet.get().emergencyConnect().password()
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