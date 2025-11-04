/*
 * Copyright (c) 2021 Windscribe Limited.
 */

package com.windscribe.vpn.backend

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
import com.windscribe.vpn.state.NetworkInfoListener
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
) : NetworkInfoListener {

    val vpnLogger: Logger = LoggerFactory.getLogger("vpn")
    var connectionJob: Job? = null
    var reconnecting = false
    var protocolInformation: ProtocolInformation? = null
    var connectionId: UUID? = null
    var error: VPNState.Error? = null
    private var isHandlingNetworkChange = false

    private var connectivityTestJob: Job? = null

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

    override fun onNetworkInfoUpdate(networkInfo: NetworkInfo?, userReload: Boolean) {
        // Prevent duplicate processing of network changes
        if (isHandlingNetworkChange) {
            vpnLogger.debug("Already handling network change, ignoring duplicate event.")
            return
        }
        // Check if VPN is connected and network requires different protocol/port
        val vpnState = stateManager.state.value
        if (vpnState.status == VPNState.Status.Connected &&
            preferencesHelper.autoConnect &&
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
                appContext.vpnController.connectAsync()
            }
        }
    }

    fun startConnectionJob() {
        val preferredProtocolOn = networkInfoManager.networkInfo?.isPreferredOn ?: false
        if (preferredProtocolOn.not() && preferencesHelper.getResponseString(
                PreferencesKeyConstants.CONNECTION_MODE_KEY
            ) != PreferencesKeyConstants.CONNECTION_MODE_AUTO
        ) {
            vpnLogger.debug("Manual connection mode selected without preferred protocol.")
            return
        }
        connectionJob = mainScope.launch {
            delay(CONNECTING_WAIT)
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
        connectionJob?.cancel()
        connectivityTestJob?.cancel()
        connectivityTestJob = null
        vpnLogger.info("Starting connectivity test.")
        val startDelay = advanceParameterRepository.getTunnelStartDelay() ?: 2000L
        val retryDelay = advanceParameterRepository.getTunnelTestRetryDelay() ?: 500L
        val maxAttempts = advanceParameterRepository.getTunnelTestAttempts() ?: 3

        connectivityTestJob = mainScope.launch {
            try {
                // Check if we need to worry about IP pinning
                val selectedCity = preferencesHelper.selectedCity
                val selectedIp = preferencesHelper.selectedIp

                // First: Check if selected city is in favourites and has a pinned node IP
                val favourite = localDbInterface.getFavouritesAsync().firstOrNull {
                    it.id == selectedCity
                }
                val shouldCheckPinning = favourite != null && favourite.pinnedNodeIp != null

                // Second: If we care about pinning, check if the pinnedNodeIp matches selectedIp
                val hasPinnedNodeMismatch = shouldCheckPinning && favourite.pinnedNodeIp != selectedIp

                val ip = favourite?.pinnedIp
                var ipPinningFailed = false

                withTimeout(15_000) { // 15 seconds total timeout
                    // Initial delay before first attempt
                    delay(startDelay)

                    // Pin IP if available
                    if (ip != null) {
                        vpnLogger.info("Pinning IP: $ip for node: $selectedIp")
                        bridgeAPI.setIgnoreSslErrors(true)
                        bridgeAPI.setConnectedState(true)
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

                        val result = result<GetMyIpResponse> {
                            apiManager.checkConnectivityAndIpAddress()
                        }

                        when (result) {
                            is CallResult.Success -> {
                                val userIp = result.data.userIp
                                if (userIp != null && Util.validIpAddress(userIp)) {
                                    val ipAddress = Util.getModifiedIpAddress(userIp.trim())
                                    preferencesHelper.saveResponseStringData(
                                        PreferencesKeyConstants.USER_IP,
                                        ipAddress
                                    )
                                    connectivityTestPassed(userIp)
                                    success = true
                                    if (hasPinnedNodeMismatch || ipPinningFailed) {
                                        val title = resourceHelper.getString(com.windscribe.vpn.R.string.could_not_pin_ip)
                                        val description = resourceHelper.getString(com.windscribe.vpn.R.string.favourite_node_not_available)
                                        appContext.applicationInterface.showPinnedNodeErrorDialog(title, description)
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
            preferencesHelper.removeResponseData(PreferencesKeyConstants.USER_IP)
            updateState(VPNState(VPNState.Status.Connected))
            mainScope.launch {
                delay(500)
                reconnecting = false
            }
        }
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
