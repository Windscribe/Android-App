package com.windscribe.vpn.backend.wireguard

import android.os.ParcelFileDescriptor
import android.system.Os
import android.system.OsConstants
import android.system.StructPollfd
import com.windscribe.vpn.backend.VPNState
import com.windscribe.vpn.services.ping.EchoPacketBuilder
import com.windscribe.vpn.state.DeviceStateManager
import com.windscribe.vpn.workers.WindScribeWorkManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.slf4j.LoggerFactory
import java.io.FileDescriptor
import java.net.InetAddress
import java.util.concurrent.atomic.AtomicBoolean

class TunnelHealthCheck(
    private val scope: CoroutineScope,
    private val wgLogger: WgLogger,
    private val deviceStateManager: DeviceStateManager,
    private val workManager: WindScribeWorkManager,
    private val getService: () -> WireGuardWrapperService?,
    private val disconnect: suspend (VPNState.Error?) -> Unit,
) {
    private val logger = LoggerFactory.getLogger("tunnel-health-check")
    private var lastHandshakeTimestamp: Long? = null
    private var handshakeReceivedJob: Job? = null
    private var handshakeFailureJob: Job? = null
    private val isHealthCheckRunning = AtomicBoolean(false)
    private var lastExternalCheckTime: Long? = null
    private var externalCheckFailureCount: Int = 0
    private var tunnelCheckFailureCount: Int = 0
    private var endpointHost: String? = null

    companion object {
        private const val HANDSHAKE_TIMEOUT_MS = 180_000L
        private const val HANDSHAKE_GRACE_PERIOD_MS = 10_000L
        private val EXTERNAL_CHECK_BACKOFF_INTERVALS = longArrayOf(5_000L, 10_000L, 20_000L, 30_000L, 60_000L)
        private const val CONNECTIVITY_CHECK_TIMEOUT_MS = 5000
        private const val TUNNEL_CHECK_RETRIES = 3
        private const val TUNNEL_CHECK_RETRY_DELAY_MS = 2000L
        private const val REQUIRED_CONSECUTIVE_FAILURES = 3
    }

    fun start(profileContent: String) {
        try {
            val config = WireGuardVpnProfile.createConfigFromString(profileContent)
            val endpoint = config.peers[0].endpoint.orElse(null)
            if (endpoint != null) {
                endpointHost = endpoint.host
                logger.info("Starting tunnel health monitoring for endpoint: $endpointHost")
            } else {
                logger.warn("No endpoint found in WireGuard profile")
            }
        } catch (e: Exception) {
            logger.error("Failed to parse WireGuard endpoint from profile: ${e.message}")
        }

        handshakeReceivedJob =
            scope.launch {
                wgLogger.handshakeReceivedEvent.collect {
                    lastHandshakeTimestamp = System.currentTimeMillis()
                    externalCheckFailureCount = 0
                    tunnelCheckFailureCount = 0
                }
            }

        handshakeFailureJob =
            scope.launch {
                wgLogger.handshakeFailureEvent.collect {
                    handleHandshakeFailure()
                }
            }
    }

    fun stop() {
        logger.info("Stopping tunnel health monitoring")
        handshakeReceivedJob?.cancel()
        handshakeFailureJob?.cancel()
        handshakeReceivedJob = null
        handshakeFailureJob = null
        lastHandshakeTimestamp = null
        lastExternalCheckTime = null
        externalCheckFailureCount = 0
        tunnelCheckFailureCount = 0
        isHealthCheckRunning.set(false)
    }

    private suspend fun handleHandshakeFailure() {
        if (!isHealthCheckRunning.compareAndSet(false, true)) {
            return
        }

        try {
            val lastHandshake = lastHandshakeTimestamp ?: return
            val timeSinceHandshake = System.currentTimeMillis() - lastHandshake

            if (timeSinceHandshake < HANDSHAKE_TIMEOUT_MS + HANDSHAKE_GRACE_PERIOD_MS) {
                return
            }

            if (!deviceStateManager.isOnline.value) {
                return
            }

            logger.warn("Handshake timeout exceeded: ${timeSinceHandshake}ms since last successful handshake (device is online)")

            val hasTunnelConnectivity = checkConnectivityWithRetry(bypassTunnel = false)
            if (hasTunnelConnectivity) {
                tunnelCheckFailureCount = 0
                logger.info("Tunnel connectivity check PASSED: Traffic flowing despite handshake timeout. Tunnel is healthy.")
                return
            }

            tunnelCheckFailureCount++
            logger.warn(
                "Tunnel connectivity check FAILED ($tunnelCheckFailureCount/$REQUIRED_CONSECUTIVE_FAILURES): No traffic passing through tunnel",
            )

            if (tunnelCheckFailureCount < REQUIRED_CONSECUTIVE_FAILURES) {
                logger.info(
                    "Not triggering recovery yet - waiting for ${REQUIRED_CONSECUTIVE_FAILURES - tunnelCheckFailureCount} more consecutive failure(s)",
                )
                return
            }

            val lastCheck = lastExternalCheckTime
            val now = System.currentTimeMillis()

            if (lastCheck != null) {
                val backoffIndex = (externalCheckFailureCount - 1).coerceIn(0, EXTERNAL_CHECK_BACKOFF_INTERVALS.size - 1)
                val currentBackoff = EXTERNAL_CHECK_BACKOFF_INTERVALS[backoffIndex]
                if ((now - lastCheck) < currentBackoff) {
                    return
                }
            }

            lastExternalCheckTime = now
            val hasExternalConnectivity = checkConnectivity(bypassTunnel = true)

            if (!hasExternalConnectivity) {
                externalCheckFailureCount++
                logger.debug("Cannot reach WG server externally (attempt $externalCheckFailureCount), likely network routing issue")
                return
            }

            externalCheckFailureCount = 0

            logger.error(
                "TUNNEL HEALTH CHECK FAILED: " +
                    "handshake_timeout=${timeSinceHandshake}ms, " +
                    "device_online=true, " +
                    "tunnel_connectivity=false, " +
                    "external_connectivity=true. " +
                    "Tunnel is broken - initiating recovery.",
            )
            recoverFromBrokenTunnel()
        } catch (e: Exception) {
            logger.error("Error during tunnel health check: ${e.message}", e)
        } finally {
            isHealthCheckRunning.set(false)
        }
    }

    private suspend fun checkConnectivityWithRetry(
        bypassTunnel: Boolean,
        retries: Int = TUNNEL_CHECK_RETRIES,
    ): Boolean {
        repeat(retries) { attempt ->
            if (attempt > 0) {
                logger.debug("Retrying connectivity check (attempt ${attempt + 1}/$retries) after ${TUNNEL_CHECK_RETRY_DELAY_MS}ms delay")
                delay(TUNNEL_CHECK_RETRY_DELAY_MS)
            }
            if (checkConnectivity(bypassTunnel)) {
                if (attempt > 0) {
                    logger.info("Connectivity check PASSED on retry attempt ${attempt + 1}")
                }
                return true
            }
        }
        logger.warn("Connectivity check FAILED after $retries attempts")
        return false
    }

    private suspend fun checkConnectivity(bypassTunnel: Boolean): Boolean {
        return withContext(Dispatchers.IO) {
            val serverHost = endpointHost ?: return@withContext false
            val service = if (bypassTunnel) getService() ?: return@withContext false else null

            var fd: FileDescriptor? = null
            var pfd: ParcelFileDescriptor? = null

            try {
                val destination = InetAddress.getByName(serverHost)
                fd = Os.socket(OsConstants.AF_INET, OsConstants.SOCK_DGRAM, OsConstants.IPPROTO_ICMP)

                if (!fd.valid()) {
                    return@withContext false
                }

                if (bypassTunnel && service != null) {
                    pfd = ParcelFileDescriptor.dup(fd)
                    if (!service.protect(pfd.fd)) {
                        pfd.close()
                        Os.close(fd)
                        return@withContext false
                    }
                    logger.debug("Sending ICMP ping to WireGuard server (bypassing tunnel): $serverHost")
                } else {
                    logger.debug("Sending ICMP ping to WireGuard server (through tunnel): $serverHost")
                }

                val echoBuilder = EchoPacketBuilder(EchoPacketBuilder.TYPE_ICMP_V4, "wg-health-check".toByteArray())
                val packet = echoBuilder.build()

                val pollFd =
                    StructPollfd().apply {
                        this.fd = fd
                        this.events = OsConstants.POLLIN.toShort()
                    }

                val bytesSent = Os.sendto(fd, packet, 0, destination, 0)
                if (bytesSent < 0) {
                    return@withContext false
                }

                val pollResult = Os.poll(arrayOf(pollFd), CONNECTIVITY_CHECK_TIMEOUT_MS)
                if (pollResult <= 0) {
                    return@withContext false
                }

                if (pollFd.revents.toInt() and OsConstants.POLLIN == 0) {
                    return@withContext false
                }

                val buffer = ByteArray(1024)
                val bytesReceived = Os.recvfrom(fd, buffer, 0, buffer.size, 0x40, null)
                if (bytesReceived > 0) {
                    val checkType = if (bypassTunnel) "external (bypassing tunnel)" else "internal (through tunnel)"
                    logger.info("Connectivity check PASSED ($checkType): ICMP ping successful to $serverHost")
                    return@withContext true
                }

                false
            } catch (e: Exception) {
                logger.debug("Connectivity check failed: ${e.message}")
                false
            } finally {
                pfd?.close()
                fd?.let {
                    try {
                        if (it.valid()) Os.close(it)
                    } catch (_: Exception) {
                    }
                }
            }
        }
    }

    private suspend fun recoverFromBrokenTunnel() {
        try {
            logger.error("Initiating tunnel recovery: disconnect + session update")
            disconnect(VPNState.Error(error = VPNState.ErrorType.BrokenTunnel, message = "Tunnel health check failed"))
            workManager.updateSession()
            logger.info("Tunnel recovery completed")
        } catch (e: Exception) {
            logger.error("Error during tunnel recovery: ${e.message}", e)
        }
    }
}
