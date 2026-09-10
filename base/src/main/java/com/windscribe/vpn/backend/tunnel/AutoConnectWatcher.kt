/*
 * Copyright (c) 2026 Windscribe Limited.
 */

package com.windscribe.vpn.backend.tunnel

import com.windscribe.common.tunnel.TunnelManager
import com.windscribe.vpn.autoconnection.AutoConnectionManager
import com.windscribe.vpn.backend.utils.ConnectSource
import com.windscribe.vpn.backend.utils.SYSTEM_DISCONNECT
import com.windscribe.vpn.backend.utils.WindVpnController
import com.windscribe.vpn.localdatabase.tables.NetworkInfo
import com.windscribe.vpn.state.ConnectionPolicy
import com.windscribe.vpn.state.ConnectionPolicyInputs
import com.windscribe.vpn.state.DeviceStateManager
import com.windscribe.vpn.state.NetworkInfoManager
import com.windscribe.vpn.state.VPNConnectionStateManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch
import org.slf4j.LoggerFactory
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Watches the network and auto-connects on one that should be protected.
 *
 * Owned by [WindscribeVpnService], which stays alive while idle: a `VpnService` that never called
 * `establish()` is an ordinary `Service`, with no TUN and no key icon.
 */
@Singleton
class AutoConnectWatcher
    @Inject
    constructor(
        private val networkInfoManager: NetworkInfoManager,
        private val autoConnectionManager: AutoConnectionManager,
        private val vpnController: WindVpnController,
        private val policyInputs: ConnectionPolicyInputs,
        private val vpnConnectionStateManager: VPNConnectionStateManager,
        private val deviceStateManager: DeviceStateManager,
        private val tunnelManager: TunnelManager,
    ) {
        private val logger = LoggerFactory.getLogger("auto-connect-watcher")
        private var job: Job? = null
        private var onlineJob: Job? = null
        private var lastNetworkName: String? = null

        /** Idempotent: a second call while already watching is a no-op. */
        fun start(scope: CoroutineScope) {
            if (job?.isActive == true) {
                return
            }
            logger.debug("Watching for networks that need protecting.")
            job =
                scope.launch {
                    networkInfoManager.networkInfo.collectLatest { networkInfo ->
                        runCatching { onNetworkChanged(networkInfo) }
                            .onFailure { logger.error("Network evaluation failed", it) }
                    }
                }
            // Connectivity returning is not a network change: networkInfo is derived from the
            // network's identity, so a reconnect under the same name conflates to nothing.
            onlineJob =
                scope.launch {
                    // drop(1): the networkInfo collector's first emission already covers it, and
                    // two watchers agreeing produced two connects.
                    deviceStateManager.isOnline.drop(1).collect { online ->
                        if (!online) return@collect
                        logger.debug("Network is available again; re-evaluating.")
                        runCatching { onNetworkChanged(networkInfoManager.networkInfo.value) }
                            .onFailure { logger.error("Network evaluation failed", it) }
                    }
                }
        }

        fun stop() {
            job?.cancel()
            job = null
            onlineJob?.cancel()
            onlineJob = null
        }

        /** The rules live in [ConnectionPolicy]; this only reacts to them. */
        private fun onNetworkChanged(networkInfo: NetworkInfo?) {
            clearStaleModeOnNewNetwork(networkInfo?.networkName)
            val input = policyInputs.from(networkInfo)
            logger.debug(
                "Network: ${networkInfo?.networkName}, AutoSecure: ${input.isAutoSecureOn}, " +
                    "StoodDown: ${input.isNetworkStoodDown}, AutoConnect: ${input.autoConnect}, " +
                    "State: ${input.status}",
            )

            releaseHeldTunnelIfBlockingLapsed(input, networkInfo?.networkName)

            when {
                ConnectionPolicy.shouldAutoConnect(input) -> {
                    logger.debug("Connecting: ${networkInfo?.networkName} should be secured.")
                    autoConnectionManager.reset()
                    vpnController.connectAsync(ConnectSource.App)
                }

                ConnectionPolicy.shouldAutoDisconnect(input) -> {
                    logger.debug("Disconnecting: auto-secure is off for ${networkInfo?.networkName}.")
                    // Not the user's doing, so the network is not stood down for it.
                    vpnController.disconnectAsync(SYSTEM_DISCONNECT)
                }

                else -> logger.debug("Nothing to do for ${networkInfo?.networkName}.")
            }
        }

        /** A blocking hold is installed at session end; on a network nothing reconnects on, nothing else lifts it. */
        private fun releaseHeldTunnelIfBlockingLapsed(
            input: ConnectionPolicy.PolicyInput,
            networkName: String?,
        ) {
            if (ConnectionPolicy.blockingApplies(input)) {
                return
            }
            // A handover holds the TUN too, and that hold belongs to the attempt in flight.
            if (vpnConnectionStateManager.isTunnelUp()) {
                return
            }
            if (tunnelManager.releaseHeldTunnel()) {
                logger.info("Blocking no longer applies on $networkName; released the held TUN.")
            }
        }

        /**
         * A failed connect suppresses auto-connect so it does not retry in a loop, but only on the
         * network it failed on.
         */
        private fun clearStaleModeOnNewNetwork(networkName: String?) {
            if (networkName == null) {
                return
            }
            // A network arriving makes this untrue whether or not it is a new one, and nothing else
            // clears it when Auto-Connect is off and no connect follows.
            if (vpnConnectionStateManager.serviceMode.value is ConnectionPolicy.ServiceMode.NoNetwork) {
                logger.debug("Network $networkName is available; clearing the no-network state.")
                vpnConnectionStateManager.setServiceMode(null)
            }
            if (networkName == lastNetworkName) {
                return
            }
            lastNetworkName = networkName
            if (vpnConnectionStateManager.serviceMode.value is ConnectionPolicy.ServiceMode.Failed) {
                logger.debug("New network $networkName; clearing the previous network's failure.")
                vpnConnectionStateManager.setServiceMode(null)
            }
        }
    }

/** Starts [WindscribeVpnService] in watch-only mode: foreground, observing the network, no tunnel. */
fun android.content.Context.startVpnServiceWatching() {
    try {
        androidx.core.content.ContextCompat.startForegroundService(
            this,
            android.content
                .Intent(this, WindscribeVpnService::class.java)
                .setAction(WindscribeVpnService.ACTION_WATCH),
        )
    } catch (e: Exception) {
        LoggerFactory.getLogger("auto-connect-watcher").error("Could not start the watcher: ${e.message}")
    }
}

/** True when we can read the current network's name, which the auto-connect rules depend on. */
fun android.content.Context.canAccessNetworkName(): Boolean {
    val isBackgroundPermissionGranted =
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            checkSelfPermission(android.Manifest.permission.ACCESS_BACKGROUND_LOCATION) ==
                android.content.pm.PackageManager.PERMISSION_GRANTED
        } else {
            return true
        }
    val isForegroundPermissionGranted =
        checkCallingOrSelfPermission(android.Manifest.permission.ACCESS_FINE_LOCATION) ==
            android.content.pm.PackageManager.PERMISSION_GRANTED
    return isBackgroundPermissionGranted && isForegroundPermissionGranted
}
