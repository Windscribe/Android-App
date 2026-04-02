package com.windscribe.vpn.backend.wireguard

import android.os.ParcelFileDescriptor
import android.system.Os
import android.system.OsConstants
import android.system.StructPollfd
import com.windscribe.vpn.apppreference.PreferencesHelper
import com.windscribe.vpn.services.ping.EchoPacketBuilder
import com.windscribe.vpn.state.DeviceStateManager
import com.windscribe.vpn.workers.WindScribeWorkManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.slf4j.LoggerFactory
import java.io.FileDescriptor
import java.net.InetAddress
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Monitors WireGuard tunnel health and triggers recovery when tunnel is broken.
 *
 * A tunnel is considered broken when:
 * 1. Handshakes have been failing for > 3 minutes
 * 2. Device has internet connectivity
 * 3. WireGuard server is reachable outside the tunnel
 *
 * Recovery strategy: Disconnect + Update Session to get fresh server list
 */
class TunnelHealthCheck(
    private val scope: CoroutineScope,
    private val wgLogger: WgLogger,
    private val deviceStateManager: DeviceStateManager,
    private val workManager: WindScribeWorkManager,
    private val getService: () -> WireGuardWrapperService?,
    private val disconnect: suspend () -> Unit
) {
    private val logger = LoggerFactory.getLogger("tunnel-health-check")

    // Handshake tracking
    private var lastHandshakeTimestamp: Long? = null
    private var handshakeReceivedJob: Job? = null
    private var handshakeFailureJob: Job? = null

    // Single execution guard
    private val isHealthCheckRunning = AtomicBoolean(false)

    // Progressive backoff for external connectivity checks
    private var lastExternalCheckTime: Long? = null
    private var externalCheckFailureCount: Int = 0

    // Endpoint host extracted from WireGuard profile
    private var endpointHost: String? = null

    companion object {
        // Handshake must be failing for at least 3 minutes before triggering recovery
        private const val HANDSHAKE_TIMEOUT_MS = 180_000L // 3 minutes

        // Progressive backoff intervals: 5s, 10s, 20s, 30s, 60s (max)
        private val EXTERNAL_CHECK_BACKOFF_INTERVALS = longArrayOf(
            5_000L,   // 5 seconds
            10_000L,  // 10 seconds
            20_000L,  // 20 seconds
            30_000L,  // 30 seconds
            60_000L   // 1 minute (max)
        )

        // External connectivity check timeout
        private const val CONNECTIVITY_CHECK_TIMEOUT_MS = 2000 // 2 seconds
    }

    /**
     * Starts monitoring handshake events.
     * Should be called after WireGuard connection is established.
     *
     * @param profileContent The WireGuard profile content to extract endpoint info
     */
    fun start(profileContent: String) {
        // Extract endpoint host from profile (port not needed for ICMP)
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

        // Track successful handshakes
        handshakeReceivedJob = scope.launch {
            wgLogger.handshakeReceivedEvent.collect {
                lastHandshakeTimestamp = System.currentTimeMillis()
                // Reset backoff on successful handshake (tunnel is healthy)
                externalCheckFailureCount = 0
            }
        }

        // Monitor handshake failures
        handshakeFailureJob = scope.launch {
            wgLogger.handshakeFailureEvent.collect {
                handleHandshakeFailure()
            }
        }
    }

    /**
     * Stops monitoring handshake events.
     * Should be called when WireGuard backend is deactivated.
     */
    fun stop() {
        logger.info("Stopping tunnel health monitoring")
        handshakeReceivedJob?.cancel()
        handshakeFailureJob?.cancel()
        handshakeReceivedJob = null
        handshakeFailureJob = null
        lastHandshakeTimestamp = null
        lastExternalCheckTime = null
        externalCheckFailureCount = 0
        isHealthCheckRunning.set(false)
    }

    /**
     * Handles handshake failure by performing 3-stage health check.
     *
     * Stage 1: Time check - Has it been > 3 minutes since last successful handshake?
     * Stage 2: Device check - Does device have internet connectivity?
     * Stage 3: External check - Can we reach WG server outside the tunnel?
     *
     * If all checks pass → Tunnel is broken → Trigger recovery
     */
    private suspend fun handleHandshakeFailure() {
        // Guard against concurrent health checks
        if (!isHealthCheckRunning.compareAndSet(false, true)) {
            return
        }

        try {
            // STAGE 1: Check handshake timeout
            val lastHandshake = lastHandshakeTimestamp ?: return

            val timeSinceHandshake = System.currentTimeMillis() - lastHandshake
            if (timeSinceHandshake < HANDSHAKE_TIMEOUT_MS) {
                // Not enough time has passed - wait for more failures
                return
            }

            // STAGE 2: Check device connectivity first (before logging anything)
            // If device is offline, handshake failures are expected and normal
            if (!deviceStateManager.isOnline.value) {
                return
            }

            // Device is ONLINE but handshakes failing - this is the problem case
            logger.warn("Handshake timeout exceeded: ${timeSinceHandshake}ms since last successful handshake (device is online)")

            // STAGE 3: Check external connectivity to WG server (with progressive backoff)
            // Progressive backoff: 5s, 10s, 20s, 30s, 60s to avoid spamming ICMP pings
            val lastCheck = lastExternalCheckTime
            val now = System.currentTimeMillis()

            if (lastCheck != null) {
                // Calculate current backoff based on failure count
                val backoffIndex = (externalCheckFailureCount - 1).coerceIn(0, EXTERNAL_CHECK_BACKOFF_INTERVALS.size - 1)
                val currentBackoff = EXTERNAL_CHECK_BACKOFF_INTERVALS[backoffIndex]

                if ((now - lastCheck) < currentBackoff) {
                    // Recently checked - skip to avoid spamming pings
                    return
                }
            }

            lastExternalCheckTime = now
            val hasExternalConnectivity = checkExternalConnectivity()

            if (!hasExternalConnectivity) {
                externalCheckFailureCount++
                logger.debug("Cannot reach WG server externally (attempt $externalCheckFailureCount), likely network routing issue")
                return
            }

            // External connectivity succeeded - reset backoff and proceed with recovery
            externalCheckFailureCount = 0

            // ALL CHECKS PASSED - TUNNEL IS BROKEN
            logger.error(
                "TUNNEL HEALTH CHECK FAILED: " +
                        "handshake_timeout=${timeSinceHandshake}ms, " +
                        "device_online=true, " +
                        "external_connectivity=true. " +
                        "Tunnel is broken - initiating recovery."
            )

            recoverFromBrokenTunnel()

        } catch (e: Exception) {
            logger.error("Error during tunnel health check: ${e.message}", e)
        } finally {
            isHealthCheckRunning.set(false)
        }
    }

    /**
     * Checks if we can reach the WireGuard server outside the VPN tunnel.
     *
     * Uses ICMP ping with VPN protection (whitelist) to test connectivity
     * to the WireGuard server directly, bypassing the tunnel.
     *
     * @return true if we can reach the server, false otherwise
     */
    private suspend fun checkExternalConnectivity(): Boolean {
        return withContext(Dispatchers.IO) {
            val serverHost = endpointHost
            if (serverHost == null) {
                logger.warn("No endpoint host available for external connectivity check")
                return@withContext false
            }

            val service = getService()
            if (service == null) {
                logger.warn("WireGuard service not available for socket protection")
                return@withContext false
            }

            var fd: FileDescriptor? = null
            var pfd: ParcelFileDescriptor? = null

            try {
                // Resolve destination (always IPv4)
                val destination = InetAddress.getByName(serverHost)

                // Create IPv4 ICMP socket
                fd = Os.socket(OsConstants.AF_INET, OsConstants.SOCK_DGRAM, OsConstants.IPPROTO_ICMP)

                if (!fd.valid()) {
                    logger.warn("Failed to create valid ICMP socket")
                    return@withContext false
                }

                // CRITICAL: Protect socket so it bypasses VPN tunnel
                // Use ParcelFileDescriptor.dup() to get int fd cleanly
                pfd = ParcelFileDescriptor.dup(fd)
                val rawFd = pfd.fd

                if (!service.protect(rawFd)) {
                    logger.warn("Failed to protect socket from VPN")
                    pfd.close()
                    Os.close(fd)
                    return@withContext false
                }

                logger.debug("Sending ICMP ping to WireGuard server: $serverHost")

                // Build IPv4 ICMP echo request packet
                val echoBuilder = EchoPacketBuilder(EchoPacketBuilder.TYPE_ICMP_V4, "wg-health-check".toByteArray())
                val packet = echoBuilder.build()

                // Set up polling for response
                val pollFd = StructPollfd().apply {
                    this.fd = fd
                    this.events = OsConstants.POLLIN.toShort()
                }

                // Send ICMP echo request
                val bytesSent = Os.sendto(fd, packet, 0, destination, 0)
                if (bytesSent < 0) {
                    logger.debug("Failed to send ICMP packet")
                    return@withContext false
                }

                // Wait for response with timeout
                val pollResult = Os.poll(arrayOf(pollFd), CONNECTIVITY_CHECK_TIMEOUT_MS)
                if (pollResult <= 0) {
                    logger.debug("ICMP ping timeout (no response within ${CONNECTIVITY_CHECK_TIMEOUT_MS}ms)")
                    return@withContext false
                }

                // Check if we got data
                if (pollFd.revents.toInt() and OsConstants.POLLIN == 0) {
                    logger.debug("No ICMP response received")
                    return@withContext false
                }

                // Receive response
                val buffer = ByteArray(1024)
                val bytesReceived = Os.recvfrom(fd, buffer, 0, buffer.size, 0x40, null) // MSG_DONTWAIT = 0x40
                if (bytesReceived > 0) {
                    logger.info("External connectivity check PASSED: ICMP ping successful to $serverHost")
                    return@withContext true
                }

                logger.debug("Failed to receive ICMP response")
                false

            } catch (e: java.net.UnknownHostException) {
                logger.debug("External connectivity check failed: Unknown host $serverHost")
                false
            } catch (e: android.system.ErrnoException) {
                logger.debug("External connectivity check failed: ${e.message}")
                false
            } catch (e: Exception) {
                logger.debug("External connectivity check failed: ${e.javaClass.simpleName} - ${e.message}")
                false
            } finally {
                pfd?.close()
                fd?.let {
                    try {
                        if (it.valid()) Os.close(it)
                    } catch (e: Exception) {
                        // Ignore close errors
                    }
                }
            }
        }
    }

    /**
     * Recovers from broken tunnel by disconnecting and updating session.
     *
     * Steps:
     * 1. Disconnect from WireGuard (cleans up broken tunnel)
     * 2. Update session (fetches fresh server list from API)
     *
     * After recovery, auto-connection manager or user can reconnect,
     * potentially to a different server.
     */
    private suspend fun recoverFromBrokenTunnel() {
        try {
            logger.error("Initiating tunnel recovery: disconnect + session update")

            // Step 1: Disconnect from broken tunnel
            logger.info("Disconnecting from broken WireGuard tunnel")
            disconnect()

            // Step 2: Update session to get fresh server list
            logger.info("Updating session to fetch fresh server list")
            workManager.updateSession()

            logger.info("Tunnel recovery completed. User can reconnect or auto-connect will handle it.")

        } catch (e: Exception) {
            logger.error("Error during tunnel recovery: ${e.message}", e)
        }
    }
}
