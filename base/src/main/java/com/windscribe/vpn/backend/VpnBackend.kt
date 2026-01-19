/*
 * Copyright (c) 2021 Windscribe Limited.
 */

package com.windscribe.vpn.backend

import android.R.attr.name
import android.util.Log
import com.windscribe.vpn.Windscribe.Companion.appContext
import com.windscribe.vpn.api.IApiCallManager
import com.windscribe.vpn.api.response.GetMyIpResponse
import com.windscribe.vpn.commonutils.ResourceHelper
import com.windscribe.vpn.apppreference.PreferencesHelper
import com.windscribe.vpn.autoconnection.ProtocolInformation
import com.windscribe.vpn.commonutils.Ext.result
import com.windscribe.vpn.constants.PreferencesKeyConstants
import com.windscribe.vpn.localdatabase.LocalDbInterface
import com.windscribe.vpn.localdatabase.tables.NetworkInfo
import com.windscribe.vpn.repository.AdvanceParameterRepository
import com.windscribe.vpn.repository.CallResult
import com.windscribe.vpn.state.NetworkInfoManager
import com.windscribe.vpn.state.VPNConnectionStateManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.UUID
import javax.inject.Singleton
import com.wsnet.lib.WSNetBridgeAPI
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop

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
    private val bridgeAPI: WSNetBridgeAPI,
    private val resourceHelper: ResourceHelper
) {

    val vpnLogger: Logger = LoggerFactory.getLogger("vpn_backend")
    var connectionJob: Job? = null
    var reconnecting = false
    var protocolInformation: ProtocolInformation? = null
    var connectionId: UUID? = null
    var error: VPNState.Error? = null
    private var isHandlingNetworkChange = false

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
        networkInfoObserverJob = mainScope.launch {
            networkInfoManager.networkInfo
                .collectLatest { networkInfo ->
                    vpnLogger.debug("Network Info: $networkInfo")
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

        val vpnState = stateManager.state.value

        // IMPORTANT: Protocol/port switching on network change requires autoConnect to be enabled
        // This ensures we only auto-switch protocols when user has auto-connect features enabled
        if (vpnState.status == VPNState.Status.Connected &&
            preferencesHelper.autoConnect &&  // ← Protocol switching is an "auto" feature
            networkInfo?.isAutoSecureOn == true &&
            networkInfo.isPreferredOn &&
            networkInfo.protocol != null &&
            networkInfo.port != null
        ) {

            val currentProtocol = protocolInformation?.protocol
            val currentPort = protocolInformation?.port
            val networkProtocol = networkInfo.protocol
            val networkPort = networkInfo.port

            if (currentProtocol != networkProtocol || currentPort != networkPort) {
                isHandlingNetworkChange = true
                vpnLogger.debug("Network change detected while connected. Current: $currentProtocol:$currentPort, Required: $networkProtocol:$networkPort. Reconnecting with correct protocol/port...")
                // System disconnect to change protocol - don't whitelist
                appContext.vpnController.disconnectAsync(error = null)
            }
        }
        if (networkInfo?.isAutoSecureOn == false && vpnState.status == VPNState.Status.Connected) {
            isHandlingNetworkChange = true
            vpnLogger.debug("Auto-secure OFF for ${networkInfo.networkName} - system disconnecting")
            // System disconnect due to auto-secure OFF - don't whitelist
            appContext.vpnController.disconnectAsync(error = null)
        }
    }

    fun startConnectionJob() {
        connectionJob = mainScope.launch {
            if (protocolInformation?.protocol == "wg") {
                delay(WG_CONNECTING_WAIT)
            } else {
                delay(CONNECTING_WAIT)
            }
            connectionTimeout()
        }
    }

    private suspend fun connectionTimeout() {
        vpnLogger.error("Connection timeout.")
        disconnect(
            error = VPNState.Error(
                error = VPNState.ErrorType.TimeoutError,
                "connection timeout"
            )
        )
    }

    /**
    Tests network connectivity after a successful VPN connection.
    Tries 3 times after 500ms delay. This delay becomes more important
    In TCP and stealth protocol.
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

        connectivityTestJob = mainScope.launch {
            try {
                val pinnedLocation = getPinnedIpForSelectedCity()
                val selectedIp = preferencesHelper.selectedIp
                val shouldCheckPinning = pinnedLocation?.second != null
                val hasPinnedNodeMismatch =
                    shouldCheckPinning && pinnedLocation.second != selectedIp
                val ip = pinnedLocation?.first
                var ipPinningFailed = false
                withTimeout(15_000) { // 15 seconds total timeout
                    // Initial delay before first attempt
                    delay(startDelay)
                    if (protocolInformation?.protocol == "wg") {
                        bridgeAPI.setCurrentHost(selectedIp)
                    } else {
                        bridgeAPI.setCurrentHost("")
                    }
                    bridgeAPI.setIgnoreSslErrors(true)
                    bridgeAPI.setConnectedState(true)
                    // Pin IP if available
                    if (ip != null) {
                        vpnLogger.info("Pinning IP: $ip for node: $selectedIp")
                        val pinResult = result<Any> {
                            apiManager.pinIp(ip)
                        }
                        when (pinResult) {
                            is CallResult.Success -> {
                                vpnLogger.info("IP pinned successfully")
                            }

                            is CallResult.Error -> {
                                vpnLogger.error("Failed to pin IP: ${pinResult.errorMessage}")
                                ipPinningFailed = true
                            }
                        }
                    }

                    var lastError: String? = null
                    var attemptCount = 0
                    var success = false

                    // Try maxAttempts times
                    while (attemptCount < maxAttempts && !success) {
                        attemptCount++
                        vpnLogger.info("Connectivity test attempt: $attemptCount/$maxAttempts")

                        val result = result<String> {
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
                                            resourceHelper.getString(com.windscribe.vpn.R.string.could_not_pin_ip)
                                        val description =
                                            resourceHelper.getString(com.windscribe.vpn.R.string.favourite_node_not_available)
                                        appContext.applicationInterface.showPinnedNodeErrorDialog(
                                            title,
                                            description
                                        )
                                    }
                                } else {
                                    lastError = "Invalid IP address: $userIp"
                                    vpnLogger.info("Failed Attempt: $attemptCount - $lastError")
                                    if (attemptCount < maxAttempts) {
                                        delay(retryDelay)
                                    }
                                }
                            }

                            is CallResult.Error -> {
                                lastError = result.errorMessage
                                vpnLogger.info("Failed Attempt: $attemptCount - $lastError")
                                if (attemptCount < maxAttempts) {
                                    delay(retryDelay)
                                }
                            }
                        }
                    }

                    if (!success) {
                        vpnLogger.error("Connectivity test failed after $attemptCount attempts")
                        failedConnectivityTest()
                    }
                }
            } catch (e: TimeoutCancellationException) {
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
        updateState(VPNState(VPNState.Status.Connected, ip = ip))
        mainScope.launch {
            delay(500)
            reconnecting = false
        }
    }

    private fun failedConnectivityTest() {
        connectivityTestJob?.cancel()
        connectivityTestJob = null
        connectionJob?.cancel()
        // If app is in foreground, try other protocols.
        if (reconnecting.not()) {
            mainScope.launch {
                vpnLogger.info("Connectivity test failed.")
                disconnect(
                    error = VPNState.Error(
                        VPNState.ErrorType.ConnectivityTestFailed,
                        "Connectivity test failed."
                    )
                )
            }
        } else {
            vpnLogger.info("Connectivity test failed in background.")
            // Consider it connected and will fetch ip on app launch.
            preferencesHelper.userIP = null
            updateState(VPNState(VPNState.Status.Connected))
            mainScope.launch {
                delay(500)
                reconnecting = false
            }
        }
    }

    suspend fun getPinnedIpForSelectedCity(): Pair<String, String>? {
        val selectedCity = preferencesHelper.selectedCity
        val favourite = localDbInterface.getFavouritesAsync().firstOrNull {
            it.id == selectedCity
        }
        if (favourite == null) return null
        return Pair(favourite.pinnedIp, favourite.pinnedNodeIp)
    }

    abstract var active: Boolean
    abstract fun activate()
    abstract fun deactivate()
    abstract fun connect(protocolInformation: ProtocolInformation, connectionId: UUID)
    abstract suspend fun disconnect(error: VPNState.Error? = null)

    companion object {
        var DISCONNECT_DELAY = 1000L
        var CONNECTING_WAIT = 30 * 1000L
        var WG_CONNECTING_WAIT = 20 * 1000L
    }
}
