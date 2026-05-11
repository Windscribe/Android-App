package com.windscribe.vpn.backend.wireguard

import com.windscribe.vpn.api.IApiCallManager
import com.windscribe.vpn.apppreference.PreferencesHelper
import com.windscribe.vpn.commonutils.Ext
import com.windscribe.vpn.repository.CallResult
import com.windscribe.vpn.state.DeviceStateManager
import com.windscribe.vpn.wsnet.WSNetWrapper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.withTimeoutOrNull
import org.slf4j.LoggerFactory

class PinIpRecovery(
    private val scope: CoroutineScope,
    private val wgLogger: WgLogger,
    private val apiManager: IApiCallManager,
    private val wsNetWrapper: WSNetWrapper,
    private val preferencesHelper: PreferencesHelper,
    private val deviceStateManager: DeviceStateManager,
    private val getPinnedIpForSelectedCity: suspend () -> Pair<String, String>?
) {

    private val vpnLogger = LoggerFactory.getLogger("pin-ip-recovery")
    private var handshakeRecoveryJob: Job? = null
    private var lastHandshakeTimestamp: Long? = null

    companion object {
        private const val HANDSHAKE_TIMEOUT_MS = 180_000L // 3 minutes
    }

    fun start() {
        vpnLogger.info("Starting IP pinning recovery monitor")
        startHandshakeRecoveryListener()
    }

    fun stop() {
        vpnLogger.info("Stopping IP pinning recovery monitor")
        handshakeRecoveryJob?.cancel()
        handshakeRecoveryJob = null
        lastHandshakeTimestamp = null
    }

    private fun startHandshakeRecoveryListener() {
        handshakeRecoveryJob = scope.launch {
            wgLogger.handshakeReceivedEvent.collect {
                // Only check if we have a pinned IP available for recovery
                // User might pin IP during an active connection
                if (getPinnedIpForSelectedCity.invoke() == null) {
                    lastHandshakeTimestamp = System.currentTimeMillis()
                    return@collect
                }

                val currentTime = System.currentTimeMillis()
                val previousTimestamp = lastHandshakeTimestamp

                if (previousTimestamp != null) {
                    val timeSinceLastHandshake = currentTime - previousTimestamp
                    if (timeSinceLastHandshake > HANDSHAKE_TIMEOUT_MS) {
                        vpnLogger.warn("Handshake delayed: ${timeSinceLastHandshake / 1000}s since last handshake (> 3 minutes)")
                        vpnLogger.info("Tunnel was unhealthy, attempting IP pinning recovery")
                        waitForNetworkAndPinIp()
                    }
                } else {
                    vpnLogger.debug("First handshake received")
                }

                lastHandshakeTimestamp = currentTime
            }
        }
    }

    private suspend fun waitForNetworkAndPinIp() {
        if (deviceStateManager.isOnline.value) {
            pinIpForRecovery()
            return
        }

        try {
            vpnLogger.debug("Waiting for network before IP pinning...")
            withTimeout(5_000) { // 5 seconds timeout
                deviceStateManager.isOnline.collect { isOnline ->
                    if (isOnline) {
                        pinIpForRecovery()
                        return@collect
                    }
                }
            }
        } catch (_: TimeoutCancellationException) {
            vpnLogger.warn("Network timeout (5s), attempting IP pinning anyway")
            pinIpForRecovery()
        } catch (e: Exception) {
            vpnLogger.error("Error waiting for network: ${e.message}", e)
            pinIpForRecovery()
        }
    }

    private suspend fun pinIpForRecovery() {
        try {
            val pinnedIp = getPinnedIpForSelectedCity()?.first
            val selectedIp = preferencesHelper.selectedIp

            if (pinnedIp == null || selectedIp == null) {
                vpnLogger.warn("Cannot pin IP: pinnedIp=$pinnedIp, selectedIp=$selectedIp")
                return
            }

            vpnLogger.info("Attempting IP pinning for recovery: pinnedIp=$pinnedIp, selectedIp=$selectedIp")

            // Wait for WSNet to be fully initialized before calling bridge API
            // This prevents crashes when WSNet's native code isn't ready yet
            if (!wsNetWrapper.isReady.value) {
                vpnLogger.debug("Waiting for WSNet to be fully ready...")
                withTimeoutOrNull(5000) {
                    wsNetWrapper.isReady.first { it }
                } ?: run {
                    vpnLogger.warn("WSNet not ready after 5s, skipping bridge API call")
                    return
                }
            }

            // Set bridge API state on main thread
            withContext(Dispatchers.Main) {
                wsNetWrapper.safeBridgeAPI()?.let { bridgeAPI ->
                    bridgeAPI.setConnectedState(false)
                    bridgeAPI.setCurrentHost(selectedIp ?: "")
                    bridgeAPI.setIgnoreSslErrors(false)
                    bridgeAPI.setConnectedState(true)
                }
            }

            // Call pin IP API
            val pinResult = Ext.result<Any> {
                apiManager.pinIp(pinnedIp)
            }

            when (pinResult) {
                is CallResult.Success -> {
                    vpnLogger.info("Successfully pinned IP $pinnedIp to server $selectedIp for recovery")
                }
                is CallResult.Error -> {
                    vpnLogger.error("Failed to pin IP for recovery: ${pinResult.errorMessage}")
                }
            }
        } catch (e: Exception) {
            vpnLogger.error("Error during IP pinning recovery: ${e.message}", e)
        }
    }
}
