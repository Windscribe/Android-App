/*
 * Copyright (c) 2021 Windscribe Limited.
 */

package com.windscribe.vpn.backend

import com.windscribe.vpn.R
import com.windscribe.vpn.Windscribe.Companion.appContext
import com.windscribe.vpn.api.IApiCallManager
import com.windscribe.vpn.apppreference.PreferencesHelper
import com.windscribe.vpn.autoconnection.ProtocolInformation
import com.windscribe.vpn.commonutils.Ext.result
import com.windscribe.vpn.commonutils.ResourceHelper
import com.windscribe.vpn.commonutils.WindUtilities
import com.windscribe.vpn.localdatabase.LocalDbInterface
import com.windscribe.vpn.localdatabase.tables.NetworkInfo
import com.windscribe.vpn.repository.AdvanceParameterRepository
import com.windscribe.vpn.repository.CallResult
import com.windscribe.vpn.state.NetworkInfoManager
import com.windscribe.vpn.state.VPNConnectionStateManager
import com.windscribe.vpn.wsnet.WSNetWrapper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.withTimeoutOrNull
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.UUID
import javax.inject.Singleton
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

/**
 * Base class for Interfacing with VPN Modules.
 * */
@Singleton
abstract class VpnBackend(
    private val mainScope: CoroutineScope,
    val stateManager: VPNConnectionStateManager,
    private val preferencesHelper: PreferencesHelper,
    private val networkInfoManager: NetworkInfoManager,
    private val advanceParameterRepository: AdvanceParameterRepository,
    private val apiManager: IApiCallManager,
    protected val localDbInterface: LocalDbInterface,
    private val wsNetWrapper: WSNetWrapper,
    private val resourceHelper: ResourceHelper,
) {
    val vpnLogger: Logger = LoggerFactory.getLogger("vpn_backend")
    var connectionJob: Job? = null
    var reconnecting = false
    var protocolInformation: ProtocolInformation? = null
    var connectionId: UUID? = null
    var error: VPNState.Error? = null
    var isHandlingNetworkChange = false

    private var connectivityTestJob: Job? = null
    private var networkInfoObserverJob: Job? = null

    init {
        mainScope.launch {
            stateManager.state.collectLatest {
                if (it.status == VPNState.Status.Disconnected || it.status == VPNState.Status.Disconnecting) {
                    // Stop existing connectivity test if a disconnect is in process.
                    connectivityTestJob?.cancel()
                    connectivityTestJob = null
                    // Reset network change flag when disconnecting/disconnected
                    isHandlingNetworkChange = false
                }
            }
        }
    }

    protected fun startNetworkInfoObserver() {
        if (networkInfoObserverJob?.isActive == true) {
            vpnLogger.debug("Already handling running..")
            return
        }
        networkInfoObserverJob =
            mainScope.launch {
                networkInfoManager.networkInfo
                    .collectLatest { networkInfo ->
                        vpnLogger.debug("Network Info: {}", networkInfo)
                        handleNetworkInfoUpdate(networkInfo)
                    }
            }
    }

    protected fun stopNetworkInfoObserver() {
        networkInfoObserverJob?.cancel()
        networkInfoObserverJob = null
    }

    private fun handleNetworkInfoUpdate(networkInfo: NetworkInfo?) {
        // Prevent duplicate processing of network changes
        if (isHandlingNetworkChange) {
            vpnLogger.debug("Already handling network change, ignoring duplicate event.")
            return
        }
        val isConnected = stateManager.isVPNConnected()
        val autoConnectEnabled = preferencesHelper.autoConnect
        val appInForeground = appContext.activeActivity != null

        // Handle auto-secure OFF - always disconnect regardless of auto-connect setting
        if (networkInfo?.isAutoSecureOn == false && isConnected) {
            isHandlingNetworkChange = true
            vpnLogger.info("Auto-secure OFF for ${networkInfo.networkName} - system disconnecting")
            // System disconnect due to auto-secure OFF - don't whitelist
            appContext.vpnController.disconnectAsync(error = null)
            return
        }
        val isAutoSecureOn = networkInfo?.isAutoSecureOn == true
        val isPreferredOn = networkInfo?.isPreferredOn ?: false
        val hasProtocolConfig = networkInfo?.protocol != null && networkInfo.port != null
        val shouldHandleProtocolSwitch = autoConnectEnabled || appInForeground
        if (isConnected && shouldHandleProtocolSwitch && isAutoSecureOn && isPreferredOn && hasProtocolConfig) {
            val currentProtocol = protocolInformation?.protocol
            val currentPort = protocolInformation?.port
            val networkProtocol = networkInfo.protocol
            val networkPort = networkInfo.port

            if (currentProtocol != networkProtocol || currentPort != networkPort) {
                isHandlingNetworkChange = true
                vpnLogger.info("Protocol switch: $currentProtocol:$currentPort -> $networkProtocol:$networkPort")
                // Reconnect
                appContext.vpnController.connectAsync()
            } else {
                handleNetworkChange()
            }
        }
    }

    fun startConnectionJob() {
        connectionJob =
            mainScope.launch {
                if (protocolInformation?.protocol == "wg") {
                    delay(WG_CONNECTING_WAIT.milliseconds)
                } else {
                    delay(CONNECTING_WAIT.milliseconds)
                }
                connectionTimeout()
            }
    }

    private suspend fun connectionTimeout() {
        vpnLogger.error("Connection timeout.")
        disconnect(
            error =
                VPNState.Error(
                    error = VPNState.ErrorType.TimeoutError,
                    "connection timeout",
                ),
        )
    }

    /**
     Tests network connectivity after a successful VPN connection.
     Tries 3 times after delay. This delay becomes more important
     In TCP and stealth protocol. For WireGuard, initialWaitTime can be 0
     since handshake already confirmed tunnel is ready.
     @param initialWaitTime Optional override for initial delay (e.g., 0L for WireGuard). If null, uses advance parameters.
     @param pinnedLocation Optional pre-fetched pinned location to avoid DB lookup delay.
     */
    fun testConnectivity() {
        if (connectivityTestJob?.isActive == true) {
            vpnLogger.debug("Connectivity test already running, skipping new test.")
            return
        }
        connectionJob?.cancel()
        connectivityTestJob?.cancel()
        connectivityTestJob = null
        vpnLogger.info("Starting connectivity test.")
        val startDelay = advanceParameterRepository.getTunnelStartDelay() ?: 500L
        val retryDelay = advanceParameterRepository.getTunnelTestRetryDelay() ?: 500L
        val maxAttempts = advanceParameterRepository.getTunnelTestAttempts() ?: 3

        connectivityTestJob =
            mainScope.launch {
                try {
                    val selectedIp = preferencesHelper.selectedIp
                    val pinnedLocation = getPinnedIpForSelectedCity()
                    val shouldCheckPinning = pinnedLocation?.second != null
                    val hasPinnedNodeMismatch =
                        shouldCheckPinning &&
                            !WindUtilities.hostnamesMatch(
                                pinnedLocation.second,
                                selectedIp,
                            )
                    // 15 seconds total timeout
                    withTimeout(15.seconds) {
                        // Initial delay before first attempt
                        delay(startDelay.milliseconds)
                        // Wait for WSNet to be fully initialized before calling bridge API
                        ensureWSNetReady()
                        configureBridgeAPI(selectedIp)
                        // Pin IP if available (WireGuard only)
                        val ipPinningFailed = attemptIpPinning(pinnedLocation, selectedIp)

                        var lastError: String?
                        var attemptCount = 0
                        var success = false

                        // Try maxAttempts times
                        while (attemptCount < maxAttempts && !success) {
                            attemptCount++
                            vpnLogger.info("Connectivity test attempt: $attemptCount/$maxAttempts")

                            val result =
                                result<String> {
                                    apiManager.getIp()
                                }

                            when (result) {
                                is CallResult.Success -> {
                                    val userIp = result.data
                                    if (Util.validIpAddress(userIp)) {
                                        val ipAddress = Util.getModifiedIpAddress(userIp.trim())
                                        preferencesHelper.userIP = ipAddress
                                        connectivityTestPassed(userIp)
                                        success = true
                                        if (hasPinnedNodeMismatch || ipPinningFailed) {
                                            val title =
                                                resourceHelper.getString(R.string.could_not_pin_ip)
                                            val description =
                                                resourceHelper.getString(R.string.favourite_node_not_available)
                                            appContext.applicationInterface.showPinnedNodeErrorDialog(
                                                title,
                                                description,
                                            )
                                        }
                                    } else {
                                        lastError = "Invalid IP address: $userIp"
                                        vpnLogger.info("Failed Attempt: $attemptCount - $lastError")
                                        if (attemptCount < maxAttempts) {
                                            delay(retryDelay.milliseconds)
                                        }
                                    }
                                }

                                is CallResult.Error -> {
                                    lastError = result.errorMessage
                                    vpnLogger.info("Failed Attempt: $attemptCount - $lastError")
                                    if (attemptCount < maxAttempts) {
                                        delay(retryDelay.milliseconds)
                                    }
                                }
                            }
                        }

                        if (!success) {
                            vpnLogger.error("Connectivity test failed after $attemptCount attempts")
                            failedConnectivityTest()
                        }
                    }
                } catch (_: TimeoutCancellationException) {
                    vpnLogger.error("Connectivity test timeout: exceeded 15 seconds")
                    failedConnectivityTest()
                } catch (e: Exception) {
                    vpnLogger.error("Connectivity test error: ${e.message}")
                    failedConnectivityTest()
                }
            }
    }

    fun updateState(vpnState: VPNState) {
        mainScope.launch {
            vpnState.protocolInformation = protocolInformation
            vpnState.connectionId = connectionId
            error?.let {
                if (vpnState.status == VPNState.Status.Disconnected) {
                    vpnState.error = it
                    error = null
                }
            }
            if (connectionId != null) {
                stateManager.setState(vpnState)
            }
        }
    }

    open fun connectivityTestPassed(ip: String) {
        vpnLogger.info("Connectivity test successful: $ip")
        // Clear disconnection error on successful connection
        preferencesHelper.lastDisconnectionError = null
        updateState(VPNState(VPNState.Status.Connected, ip = ip))
        mainScope.launch {
            delay(500.milliseconds)
            reconnecting = false
        }
    }

    private fun failedConnectivityTest() {
        connectivityTestJob = null
        connectionJob?.cancel()

        // Check if this is a tunnel recovery reconnection
        val isTunnelRecovery =
            preferencesHelper.lastDisconnectionError == VPNState.ErrorType.BrokenTunnel.name

        // If app is in foreground or not a tunnel recovery, try other protocols.
        if (reconnecting.not() && !isTunnelRecovery) {
            mainScope.launch {
                vpnLogger.info("Connectivity test failed.")
                disconnect(
                    error =
                        VPNState.Error(
                            VPNState.ErrorType.ConnectivityTestFailed,
                            "Connectivity test failed.",
                        ),
                )
            }
        } else {
            vpnLogger.info("Connectivity test failed in background (tunnel recovery or reconnecting).")
            // Consider it connected and will fetch ip on app launch.
            preferencesHelper.userIP = null
            updateState(VPNState(VPNState.Status.Connected))
            mainScope.launch {
                delay(500.milliseconds)
                reconnecting = false
            }
        }
    }

    suspend fun getPinnedIpForSelectedCity(): Pair<String, String>? {
        val selectedCity = preferencesHelper.selectedCity
        val favourite =
            localDbInterface.getFavouritesAsync().firstOrNull {
                it.id == selectedCity
            }
        if (favourite == null) return null
        val pinnedIp = favourite.pinnedIp ?: return null
        val pinnedNodeIp = favourite.pinnedNodeIp ?: return null
        return Pair(pinnedIp, pinnedNodeIp)
    }

    /**
     * Waits for WSNet to be fully initialized before proceeding.
     * This prevents crashes when WSNet's native code isn't ready yet.
     */
    private suspend fun ensureWSNetReady() {
        if (!wsNetWrapper.isReady.value) {
            vpnLogger.debug("Waiting for WSNet to be fully ready...")
            withTimeoutOrNull(5.seconds) {
                wsNetWrapper.isReady.first { it }
            } ?: run {
                vpnLogger.warn("WSNet not ready after 5s, skipping bridge API call")
            }
        }
    }

    /**
     * Configures WSNet bridge API for connectivity test.
     * Sets current host for WireGuard connections and marks connection as active.
     */
    private fun configureBridgeAPI(selectedIp: String?) {
        wsNetWrapper.safeBridgeAPI()?.let { bridgeAPI ->
            val hostToSet =
                if (protocolInformation?.protocol == "wg" && !preferencesHelper.isConnectingToConfigured) {
                    selectedIp ?: ""
                } else {
                    ""
                }
            bridgeAPI.setCurrentHost(hostToSet)
            bridgeAPI.setIgnoreSslErrors(false)
            bridgeAPI.setConnectedState(true)
        }
    }

    /**
     * Attempts to pin IP for WireGuard connections.
     * @param pinnedLocation Pair of (pinnedIp, pinnedNodeIp)
     * @param selectedIp Currently selected IP
     * @return true if pinning failed, false if succeeded or not attempted
     */
    private suspend fun attemptIpPinning(
        pinnedLocation: Pair<String, String>?,
        selectedIp: String?,
    ): Boolean {
        val ip = pinnedLocation?.first ?: return false
        vpnLogger.info("Pinning IP: $ip for node: $selectedIp")
        val pinResult = result<Any> { apiManager.pinIp(ip) }
        return when (pinResult) {
            is CallResult.Success -> {
                vpnLogger.info("IP pinned successfully")
                false
            }

            is CallResult.Error -> {
                vpnLogger.error("Failed to pin IP: ${pinResult.errorMessage}")
                true
            }
        }
    }

    abstract var active: Boolean

    abstract fun activate()

    abstract fun deactivate()

    abstract fun connect(
        protocolInformation: ProtocolInformation,
        connectionId: UUID,
    )

    abstract suspend fun disconnect(error: VPNState.Error? = null)

    open fun handleNetworkChange() {}

    companion object {
        var DISCONNECT_DELAY = 1000L
        var CONNECTING_WAIT = 30 * 1000L
        var WG_CONNECTING_WAIT = 20 * 1000L
    }
}
