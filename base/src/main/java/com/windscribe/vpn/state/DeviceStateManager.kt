/*
 * Copyright (c) 2021 Windscribe Limited.
 */

package com.windscribe.vpn.state

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.Intent.ACTION_SCREEN_OFF
import android.content.Intent.ACTION_SCREEN_ON
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.net.wifi.WifiManager
import android.os.Build
import android.telephony.TelephonyManager
import androidx.core.content.ContextCompat
import androidx.work.impl.utils.getActiveNetworkCompat
import com.windscribe.vpn.services.DeviceStateService.Companion.enqueueWork
import com.wsnet.lib.WSNet
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.slf4j.LoggerFactory
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages device state changes including network connectivity and screen state.
 * Uses modern Android APIs (NetworkCallback) for network monitoring.
 *
 * Provides reactive state flows for monitoring:
 * - [isOnline] - Simple boolean indicating if device has working internet
 * - [networkDetail] - Network name and type (null if name unavailable, e.g., no location permission)
 * - [isDeviceInteractive] - Screen on/off state
 */
@Singleton
class DeviceStateManager @Inject constructor(
    private val scope: CoroutineScope
) {

    private val logger = LoggerFactory.getLogger("device-state-manager")

    // Screen state
    private val _deviceInteractiveEvents = MutableStateFlow(false)
    val isDeviceInteractive: StateFlow<Boolean> = _deviceInteractiveEvents.asStateFlow()

    // Simple boolean for network connectivity
    private val _isOnline = MutableStateFlow(false)
    val isOnline: StateFlow<Boolean> = _isOnline.asStateFlow()

    // Network details - Only set when we have a proper network name
    private val _networkDetail = MutableStateFlow<NetworkDetail?>(null)
    val networkDetail: StateFlow<NetworkDetail?> = _networkDetail.asStateFlow()

    // Whitelisted network state - indicates if current network is whitelisted
    private val _isCurrentNetworkWhitelisted = MutableStateFlow(false)
    val isCurrentNetworkWhitelisted: StateFlow<Boolean> = _isCurrentNetworkWhitelisted.asStateFlow()

    // Store the whitelisted network name for comparison
    private var whitelistedNetworkName: String? = null

    private var context: Context? = null
    private var connectivityManager: ConnectivityManager? = null
    private var networkCallback: ConnectivityManager.NetworkCallback? = null
    private var screenStateReceiver: ScreenStateBroadcastReceiver? = null

    // SharedFlow for network change events (for debouncing)
    private val _networkChangeEvents = MutableSharedFlow<Unit>(extraBufferCapacity = 1)

    // Job for WSNet status update collector (to prevent multiple collectors)
    private var networkStatusUpdateJob: Job? = null

    /**
     * Initializes the device state manager and starts monitoring network and screen state.
     * Must be called before any state monitoring occurs.
     * @param context Application context
     */
    @OptIn(FlowPreview::class)
    fun init(context: Context) {
        this.context = context.applicationContext
        this.connectivityManager =
            context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

        // Set up debounced network change processing
        scope.launch {
            _networkChangeEvents
                .debounce(400) // Wait 400ms after last event
                .collect {
                    processNetworkChange()
                }
        }

        registerNetworkCallback()
        registerScreenStateReceiver()

        // Trigger initial network state check through debounced flow
        handleNetworkChange()
    }

    private fun registerNetworkCallback() {
        val connectivityManager = this.connectivityManager ?: return

        val networkRequest =
            NetworkRequest.Builder().addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .build()

        networkCallback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                handleNetworkChange()
            }

            override fun onLost(network: Network) {
                handleNetworkChange()
            }

            override fun onCapabilitiesChanged(
                network: Network, networkCapabilities: NetworkCapabilities
            ) {
                handleNetworkChange()
            }

            override fun onUnavailable() {
                handleNetworkChange()
            }
        }

        try {
            connectivityManager.registerNetworkCallback(networkRequest, networkCallback!!)
        } catch (e: Exception) {
            logger.error("Failed to register network callback", e)
        }
    }

    /**
     * Handles network change events from NetworkCallback.
     * Emits to a flow that is debounced by 400ms to prevent processing rapid successive events.
     */
    private fun handleNetworkChange() {
        _networkChangeEvents.tryEmit(Unit)
    }

    /**
     * Processes network changes after debouncing.
     * Called 400ms after the last network change event.
     * @param force If true, emits even if values haven't changed (used by refreshNetworkDetail)
     */
    private suspend fun processNetworkChange(force: Boolean = false) {
        // Get current values
        val currentOnline = _isOnline.value
        val currentDetail = _networkDetail.value

        // Calculate new values
        val online = hasUnderlyingConnectivity()
        val detail = getCurrentNetworkDetail()

        // Only emit if values have changed or force is true
        if (force || currentOnline != online) {
            _isOnline.emit(online)
        }

        if (force || currentDetail != detail) {
            _networkDetail.emit(detail)

            // Clear whitelist when network changes (so user can auto-connect when returning to network)
            if (currentDetail?.name != detail?.name && whitelistedNetworkName != null) {
                logger.info("Network changed from ${currentDetail?.name} to ${detail?.name} - clearing whitelist")
                whitelistedNetworkName = null
            }
        }

        // Update whitelisted network state
        updateWhitelistedNetworkState(detail)

        // Log only if something changed
        if (force || currentOnline != online || currentDetail != detail) {
            logger.info("Network: online={}, {}", online, detail ?: "no detail")
        }
    }

    /**
     * Updates the whitelisted network state by comparing current network with stored whitelist.
     */
    private suspend fun updateWhitelistedNetworkState(currentNetworkDetail: NetworkDetail?) {
        val isWhitelisted = currentNetworkDetail?.name != null &&
                           currentNetworkDetail.name == whitelistedNetworkName

        if (_isCurrentNetworkWhitelisted.value != isWhitelisted) {
            _isCurrentNetworkWhitelisted.emit(isWhitelisted)
            logger.debug("Whitelisted network state changed: $isWhitelisted (current: ${currentNetworkDetail?.name}, whitelisted: $whitelistedNetworkName)")
        }
    }

    /**
     * Sets the whitelisted network name and updates the state.
     * Pass null to clear the whitelist.
     * @param networkName The network name to whitelist, or null to clear
     */
    fun setWhitelistedNetwork(networkName: String?) {
        whitelistedNetworkName = networkName
        logger.info("Whitelisted network set to: $networkName")
        scope.launch {
            updateWhitelistedNetworkState(_networkDetail.value)
        }
    }

    /**
     * Convenience method: marks the current network as whitelisted.
     * @return The network name that was whitelisted, or null if no network available
     */
    fun getCurrentNetworkName(): String? {
        return _networkDetail.value?.name
    }

    /**
     * Checks if there's underlying connectivity (WiFi or Cellular with validated internet).
     * Returns true only if there's a network with underlying transport and working internet.
     */
    private fun hasUnderlyingConnectivity(): Boolean {
        val activeNetwork = connectivityManager?.getActiveNetworkCompat() ?: return false
        val caps = connectivityManager?.getNetworkCapabilities(activeNetwork) ?: return false

        val hasInternet = caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        val isValidated = caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
        val hasWifi = caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
        val hasCellular = caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)
        val hasEthernet = caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)
        val result = hasInternet && isValidated && (hasWifi || hasCellular || hasEthernet)
        return result
    }

    /**
     * Gets the current network type (WiFi, Cellular, or None).
     * Returns the underlying transport type, even when VPN is active.
     */
    private fun getCurrentNetworkType(): NetworkType {
        // Get the active network
        val activeNetwork = connectivityManager?.getActiveNetworkCompat() ?: return NetworkType.NONE
        val caps =
            connectivityManager?.getNetworkCapabilities(activeNetwork) ?: return NetworkType.NONE

        // Check if it has internet capability
        if (!caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)) {
            return NetworkType.NONE
        }

        // Return type based on transport (prioritize WiFi > Ethernet > Cellular)
        return when {
            caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> NetworkType.WIFI
            caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> NetworkType.ETHERNET
            caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> NetworkType.CELLULAR
            caps.hasTransport(NetworkCapabilities.TRANSPORT_VPN) -> NetworkType.OTHER
            else -> NetworkType.NONE
        }
    }

    /**
     * Gets the current network details including name and type.
     * Returns null if network name is not available.
     * Returns the underlying network information, even when VPN is active.
     */
    private fun getCurrentNetworkDetail(): NetworkDetail? {
        val type = getCurrentNetworkType()
        if (type == NetworkType.NONE) {
            return null
        }
        return getNetworkDetailForType(type)
    }

    /**
     * Gets the network detail for the specified type.
     * Returns null if network name cannot be determined.
     * Adapted from WindUtilities.getNetworkName() using modern APIs.
     */
    private fun getNetworkDetailForType(type: NetworkType): NetworkDetail? {
        val context = this.context ?: return null

        return when (type) {
            NetworkType.WIFI -> {
                if (!hasLocationPermission(context)) return null

                val wifiManager = context.applicationContext
                    .getSystemService(Context.WIFI_SERVICE) as? WifiManager ?: return null

                val ssid = wifiManager.connectionInfo?.ssid?.replace("\"", "")

                when {
                    ssid == "<unknown ssid>" || ssid.isNullOrBlank() -> null
                    else -> NetworkDetail(name = ssid, type = NetworkType.WIFI)
                }
            }

            NetworkType.CELLULAR -> {
                try {
                    val telephonyManager =
                        context.getSystemService(Context.TELEPHONY_SERVICE) as? TelephonyManager
                    val operatorName = telephonyManager?.networkOperatorName

                    when {
                        operatorName.isNullOrBlank() || operatorName == "Unknown" -> null
                        else -> NetworkDetail(
                            name = operatorName.uppercase(),
                            type = NetworkType.CELLULAR
                        )
                    }
                } catch (_: Exception) {
                    null
                }
            }

            NetworkType.ETHERNET -> NetworkDetail(name = "Ethernet", type = NetworkType.ETHERNET)
            NetworkType.NONE, NetworkType.OTHER -> null
        }
    }

    /**
     * Checks if location permission is available.
     * Required for WiFi SSID access on Android O MR1+.
     */
    private fun hasLocationPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }


    /**
     * Registers BroadcastReceiver for screen on/off events.
     */
    private fun registerScreenStateReceiver() {
        val context = this.context ?: return

        screenStateReceiver = ScreenStateBroadcastReceiver()
        val intentFilter = IntentFilter().apply {
            addAction(ACTION_SCREEN_OFF)
            addAction(ACTION_SCREEN_ON)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.registerReceiver(
                screenStateReceiver, intentFilter, Context.RECEIVER_NOT_EXPORTED
            )
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.registerReceiver(
                screenStateReceiver, intentFilter, Context.RECEIVER_NOT_EXPORTED
            )
        } else {
            context.registerReceiver(screenStateReceiver, intentFilter)
        }
    }

    /**
     * Inner class for handling screen state broadcasts.
     * Separated for better organization and lifecycle management.
     */
    private inner class ScreenStateBroadcastReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (!isInitialStickyBroadcast) {
                when (intent.action) {
                    ACTION_SCREEN_OFF -> scope.launch { _deviceInteractiveEvents.emit(false) }
                    ACTION_SCREEN_ON -> scope.launch { _deviceInteractiveEvents.emit(true) }
                }
            }
        }
    }

    /**
     * Cleans up all registered callbacks and receivers.
     * Should be called when the manager is no longer needed (e.g., app shutdown).
     */
    fun destroy() {
        // Unregister network callback
        networkCallback?.let { callback ->
            try {
                connectivityManager?.unregisterNetworkCallback(callback)
                logger.info("Network callback unregistered")
            } catch (e: Exception) {
                logger.error("Error unregistering network callback", e)
            }
        }
        networkCallback = null

        // Unregister screen state receiver
        screenStateReceiver?.let { receiver ->
            try {
                context?.unregisterReceiver(receiver)
                logger.info("Screen state receiver unregistered")
            } catch (e: Exception) {
                logger.error("Error unregistering screen state receiver", e)
            }
        }
        screenStateReceiver = null

        // Clear references
        context = null
        connectivityManager = null

        logger.info("DeviceStateManager destroyed")
    }

    /**
     * Refreshes the network detail state.
     * Useful when permissions change (e.g., location permission granted)
     * or when you need to force a re-check of the current network.
     */
    fun refreshNetworkDetail() {
        scope.launch {
            processNetworkChange(force = true)
        }
    }

    fun updateNetworkStatus() {
        if (networkStatusUpdateJob?.isActive == true) return

        networkStatusUpdateJob = scope.launch {
            isOnline.collect { online ->
                withContext(Dispatchers.Main) {
                    try {
                        if (WSNet.isValid()) {
                            WSNet.instance().setConnectivityState(online)
                        }
                    } catch (e: Exception) {
                        logger.error("WSNet setConnectivityState failed: ${e.message}")
                    }
                }
                if (online) {
                    context?.let { enqueueWork(it) }
                }
            }
        }
    }

    /**
     * Represents the type of underlying network connection.
     * VPN connections are ignored - this represents the actual transport type.
     */
    enum class NetworkType {
        NONE,      // No network connection
        WIFI,      // Connected via WiFi
        CELLULAR,  // Connected via Cellular
        ETHERNET,  // Connected via Ethernet
        OTHER      // Other transport type
    }

    /**
     * Network details including the network name and type.
     * Only created when we have a proper network name available.
     * Useful for UI display and per-network configuration.
     */
    data class NetworkDetail(
        val name: String,      // Network name (SSID for WiFi, operator for cellular, etc.)
        val type: NetworkType  // Network type (WiFi, Cellular, Ethernet, etc.)
    ) {
        override fun toString(): String {
            return "NetworkDetail(name=$name, type=$type)"
        }
    }
}
