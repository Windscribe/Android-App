/*
 * Copyright (c) 2021 Windscribe Limited.
 */

package com.windscribe.vpn.backend.utils

import android.app.ActivityManager
import android.content.Context.ACTIVITY_SERVICE
import android.content.Intent
import android.net.VpnService
import com.windscribe.common.tunnel.TunnelManager
import com.windscribe.vpn.R
import com.windscribe.vpn.Windscribe.Companion.appContext
import com.windscribe.vpn.alert.showRetryDialog
import com.windscribe.vpn.apppreference.PreferencesHelper
import com.windscribe.vpn.apppreference.PreferencesKeyConstants
import com.windscribe.vpn.apppreference.PreferencesKeyConstants.PROTO_IKev2
import com.windscribe.vpn.apppreference.PreferencesKeyConstants.PROTO_WIRE_GUARD
import com.windscribe.vpn.autoconnection.AutoConnectionManager
import com.windscribe.vpn.autoconnection.ProtocolConnectionStatus
import com.windscribe.vpn.autoconnection.ProtocolInformation
import com.windscribe.vpn.backend.Util
import com.windscribe.vpn.backend.VPNState
import com.windscribe.vpn.backend.VPNState.Status.Connected
import com.windscribe.vpn.backend.VPNState.Status.Connecting
import com.windscribe.vpn.backend.VPNState.Status.Disconnected
import com.windscribe.vpn.backend.VpnBackendHolder
import com.windscribe.vpn.backend.openvpn.WindStunnelUtility
import com.windscribe.vpn.backend.tunnel.VpnServiceGate
import com.windscribe.vpn.backend.tunnel.startVpnServiceWatching
import com.windscribe.vpn.backend.utils.SelectedLocationType.CityLocation
import com.windscribe.vpn.backend.utils.SelectedLocationType.StaticIp
import com.windscribe.vpn.commonutils.ConnectionLogContext
import com.windscribe.vpn.commonutils.WindUtilities
import com.windscribe.vpn.constants.NetworkErrorCodes.ERROR_INVALID_DNS_ADDRESS
import com.windscribe.vpn.constants.NetworkErrorCodes.ERROR_LICENCE_SUSPENDED
import com.windscribe.vpn.constants.NetworkErrorCodes.ERROR_UNABLE_TO_REACH_API
import com.windscribe.vpn.constants.NetworkErrorCodes.ERROR_UNABLE_TO_SELECT_WIRE_GUARD_IP
import com.windscribe.vpn.constants.NetworkErrorCodes.ERROR_UNEXPECTED_API_DATA
import com.windscribe.vpn.constants.NetworkErrorCodes.ERROR_VALID_CONFIG_NOT_FOUND
import com.windscribe.vpn.constants.NetworkErrorCodes.ERROR_WG_INVALID_PUBLIC_KEY
import com.windscribe.vpn.constants.NetworkErrorCodes.ERROR_WG_KEY_LIMIT_EXCEEDED
import com.windscribe.vpn.constants.NetworkErrorCodes.ERROR_WG_UNABLE_TO_GENERATE_PSK
import com.windscribe.vpn.constants.NetworkErrorCodes.EXPIRED_OR_BANNED_ACCOUNT
import com.windscribe.vpn.exceptions.InvalidVPNConfigException
import com.windscribe.vpn.exceptions.WindScribeException
import com.windscribe.vpn.localdatabase.LocalDbInterface
import com.windscribe.vpn.repository.AdvanceParameterRepository
import com.windscribe.vpn.repository.CallResult
import com.windscribe.vpn.repository.EmergencyConnectRepository
import com.windscribe.vpn.repository.LocationRepository
import com.windscribe.vpn.repository.WgConfigRepository
import com.windscribe.vpn.serverlist.entity.Server
import com.windscribe.vpn.state.ConnectionPolicy
import com.windscribe.vpn.state.ConnectionPolicy.Effect
import com.windscribe.vpn.state.ConnectionPolicyInputs
import com.windscribe.vpn.state.DeviceStateManager
import com.windscribe.vpn.state.VPNConnectionStateManager
import dagger.Lazy
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.takeWhile
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import org.slf4j.LoggerFactory
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Ceiling for building a profile: DB reads plus `createVPNProfile`'s API round trips. Generous
 * because wsnet retries internally, so the attempt fails predictably rather than at their mercy.
 */
private const val PROFILE_TIMEOUT_MS = 30_000L

/**
 * Who asked for a connection. Connecting records two per-network facts — auto-connect is no longer
 * held back, and the network is opted in — which app-initiated reconnects must not write.
 */
enum class ConnectSource {
    /** Someone pressed something: the home button, the notification, the tile, a location tap. */
    User,

    /** The app decided: auto-connect, tunnel recovery, protocol switch, boot, a worker. */
    App,

    /** Like [App], but not deduplicated against a live session: relocating one is the point. */
    SessionRefresh,
}

/** The user pressed disconnect. Stands the network down and ends failover. */
val USER_DISCONNECT = VPNState.Error(error = VPNState.ErrorType.UserDisconnect)

/** The app or system ended it: nothing to report, no stand-down, and failover carries on. */
val SYSTEM_DISCONNECT: VPNState.Error? = null

/** Signing out. Terminal: no account is left to lift a blocking hold. */
val SIGNED_OUT = VPNState.Error(error = VPNState.ErrorType.UserTurnOff)

@Singleton
open class WindVpnController
    @Inject
    constructor(
        val scope: CoroutineScope,
        private val preferencesHelper: PreferencesHelper,
        private val vpnProfileCreator: VPNProfileCreator,
        private val vpnConnectionStateManager: VPNConnectionStateManager,
        val vpnBackendHolder: VpnBackendHolder,
        private val locationRepository: LocationRepository,
        private val wgConfigRepository: WgConfigRepository,
        private val advanceParameterRepository: Lazy<AdvanceParameterRepository>,
        private val autoConnectionManager: AutoConnectionManager,
        private val emergencyConnectRepository: EmergencyConnectRepository,
        private val localDbInterface: LocalDbInterface,
        private val deviceStateManager: DeviceStateManager,
        private val excludedIpHolder: ExcludedIpHolder,
        private val tunnelManager: Lazy<TunnelManager>,
        private val serviceGate: VpnServiceGate,
        private val policyInputs: ConnectionPolicyInputs,
    ) {
        private val logger = LoggerFactory.getLogger("vpn")

        private suspend fun createVPNProfile(
            protocolInformation: ProtocolInformation,
            attempt: Int = 0,
            hostname: String? = null,
        ): String =
            when (WindUtilities.getSourceTypeBlocking()) {
                CityLocation -> {
                    createVpnProfileFromCity(
                        preferencesHelper.selectedCity,
                        protocolInformation,
                        attempt,
                        hostname,
                    )
                }

                StaticIp -> {
                    createVpnProfileFromStaticIp(
                        preferencesHelper.selectedCity,
                        protocolInformation,
                    )
                }

                else -> {
                    createProfileFromCustomConfig(preferencesHelper.selectedCity)
                }
            }

        /**
         * Starts the backend for [connectionId] unless something newer took over. The permission
         * dialog makes that gap arbitrarily long — it can be granted minutes later.
         */
        suspend fun connectWithGrantedPermission(
            protocolInformation: ProtocolInformation,
            connectionId: UUID,
        ) {
            if (connectionId != activeConnectionId) {
                logger.info("VPN permission granted for an attempt that is no longer current.")
                return
            }
            vpnBackendHolder.connect(protocolInformation, connectionId)
        }

        open suspend fun launchVPNService(
            protocolInformation: ProtocolInformation,
            connectionId: UUID,
        ) {
            try {
                if (VpnService.prepare(appContext) == null) {
                    logger.debug("VPN Permission available.")
                    vpnBackendHolder.connect(protocolInformation, connectionId)
                } else {
                    logger.debug("Requesting VPN Permission")
                    val startIntent = Intent(appContext, VPNPermissionActivity::class.java)
                    startIntent.putExtra("protocolInformation", protocolInformation)
                    startIntent.putExtra("connectionId", connectionId)
                    startIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    try {
                        appContext.startActivity(startIntent)
                    } catch (e: SecurityException) {
                        logger.error(
                            "[WINDSCRIBE_BG_ACTIVITY] Cannot start VPN permission activity from background: ${e.message}",
                            e,
                        )
                    }
                }
            } catch (e: Exception) {
                logger.error("Unexpected Error while checking permission for VPN", e)
                disconnectAsync(SYSTEM_DISCONNECT)
            }
        }

        private suspend fun createProfileFromCustomConfig(selectedCity: Int): String {
            val configFile = localDbInterface.getConfigFileAsync(selectedCity)
            logger.info("Creating profile from custom config $selectedCity $configFile")
            val profile = vpnProfileCreator.createVpnProfileFromConfig(configFile)
            if (!configFile.isRemember) {
                configFile.username = null
                configFile.password = null
            }
            localDbInterface.addConfigSync(configFile)
            autoConnectionManager.setSelectedProtocol(profile.second)
            return profile.first
        }

        private var lastUsedRandomIndex = 0

        private fun getForcedNodeIndex(nodes: List<Server>): Int {
            val forceNode = advanceParameterRepository.get().getForceNode()
            return if (forceNode != null && nodes.isNotEmpty()) {
                nodes.indexOfFirst { WindUtilities.hostnamesMatch(it.hostname, forceNode) }
            } else {
                -1
            }
        }

        private suspend fun createVpnProfileFromCity(
            selectedCity: Int,
            config: ProtocolInformation,
            attempt: Int = 0,
            hostname: String?,
        ): String {
            val cityAndRegion =
                localDbInterface.getDatacenterAndLocation(selectedCity)
                    ?: throw Exception("City not found in database: $selectedCity")
            val city = cityAndRegion.datacenter
            val nodes = localDbInterface.getServersByDatacenter(city.id).filter { it.isConnectable }
            if (nodes.isEmpty()) {
                throw Exception("No eligible node in datacenter: ${city.id}")
            }
            val pinnedIp =
                localDbInterface
                    .getFavouritesAsync()
                    .firstOrNull { it.id == selectedCity && it.pinnedNodeIp != null }
                    ?.pinnedNodeIp
            val pinnedNode = nodes.firstOrNull { WindUtilities.hostnamesMatch(it.hostname, pinnedIp) }
            // Random node
            var randomIndex = Util.getRandomNode(lastUsedRandomIndex, attempt, nodes)
            // Node if hostname is provided to retry same hostname
            nodes.forEachIndexed { index, node ->
                if (WindUtilities.hostnamesMatch(node.hostname, hostname)) {
                    randomIndex = index
                }
            }
            // Node with forced hostname from advance parameter
            val forcedNodeIndex = getForcedNodeIndex(nodes)
            if (forcedNodeIndex != -1) {
                logger.debug("Forcing node to {}", nodes[forcedNodeIndex])
                randomIndex = forcedNodeIndex
            }
            val selectedNode: Server =
                if (pinnedNode != null && attempt == 0) pinnedNode else nodes[randomIndex]
            logger.debug("{}", selectedNode)
            lastUsedRandomIndex = randomIndex
            val coordinatesArray = (city.coordinates ?: "").split(",".toRegex()).toTypedArray()
            val ikev2Ip = selectedNode.ip
            val udpIp = selectedNode.ip2
            val tcpIp = selectedNode.ip2
            val stealthIp = selectedNode.ip3
            val hostname = selectedNode.hostname
            val publicKey = city.pubKey
            val location =
                LastSelectedLocation(
                    city.id,
                    city.nodeName ?: "",
                    city.nickName ?: "",
                    cityAndRegion.location?.countryCode,
                    coordinatesArray[0],
                    coordinatesArray[1],
                )
            val vpnParameters =
                VPNParameters(
                    ikev2Ip,
                    udpIp,
                    tcpIp,
                    stealthIp,
                    hostname,
                    publicKey ?: "",
                    city.ovpnX509 ?: "",
                    selectedNode.ipv6 == 1,
                )
            when (config.protocol) {
                PROTO_IKev2 -> {
                    return vpnProfileCreator.createIkEV2Profile(
                        location,
                        vpnParameters,
                        config,
                    )
                }

                PROTO_WIRE_GUARD -> {
                    return vpnProfileCreator.createVpnProfileFromWireGuardProfile(
                        location,
                        vpnParameters,
                        config,
                    )
                }

                else -> {
                    return vpnProfileCreator.createOpenVpnProfile(
                        location,
                        vpnParameters,
                        config,
                    )
                }
            }
        }

        private suspend fun createVpnProfileFromStaticIp(
            staticId: Int,
            protocolInformation: ProtocolInformation,
        ): String {
            val staticRegion =
                localDbInterface.getStaticRegionByIDAsync(staticId)
                    ?: throw Exception("Static IP location not found.")
            val node = staticRegion.getStaticIpNode()
            appContext.preference.staticIpCredentials = staticRegion.credentials
            val coordinatesArray = staticRegion.coordinates?.split(",".toRegex())?.toTypedArray()
            val location =
                LastSelectedLocation(
                    staticRegion.id ?: 0,
                    staticRegion.cityName ?: "",
                    staticRegion.staticIp ?: "",
                    staticRegion.countryCode,
                    coordinatesArray?.get(0),
                    coordinatesArray?.get(1),
                )
            val vpnParameters =
                VPNParameters(
                    node?.ip ?: "",
                    node?.ip2 ?: "",
                    node?.ip2 ?: "",
                    node?.ip3 ?: "",
                    node?.hostname ?: "",
                    staticRegion.wgPubKey ?: "",
                    staticRegion.ovpnX509 ?: "",
                )
            when (protocolInformation.protocol) {
                PROTO_IKev2 -> {
                    return vpnProfileCreator.createIkEV2Profile(
                        location,
                        vpnParameters,
                        protocolInformation,
                    )
                }

                PROTO_WIRE_GUARD -> {
                    return vpnProfileCreator.createVpnProfileFromWireGuardProfile(
                        location,
                        vpnParameters,
                        protocolInformation,
                    )
                }

                else -> {
                    return vpnProfileCreator.createOpenVpnProfile(
                        location,
                        vpnParameters,
                        protocolInformation,
                    )
                }
            }
        }

        /** The connect running in the application scope; everything before a backend exists is unowned. */
        private var connectJob: Job? = null

        /** The attempt a connect is currently building, or null between attempts. */
        @Volatile
        private var activeConnectionId: UUID? = null

        fun connectAsync(source: ConnectSource) {
            connectJob?.cancel()
            connectJob =
                scope.launch {
                    connect(source)
                }
        }

        /**
         * Drops the tunnel but keeps the service, engine first so nothing goes upstream through a
         * half-dead tunnel. Does not stand the network down: a pause is about now, not the network.
         */
        suspend fun pause() {
            logger.info("Pausing: stopping the engine, then closing the TUN. Traffic will flow in the clear.")
            disconnect(error = VPNState.Error(error = VPNState.ErrorType.UserPause))
        }

        /**
         * Stops everything, watching included: the only way to make the notification go away, and the
         * only way out of a blocked device. Settles the service itself rather than relying on the
         * teardown to reach one — a TUN held with no engine has no backend to disconnect, so nothing
         * in [disconnect] runs, and the block used to stand.
         */
        suspend fun turnOff() {
            logger.info("Turning off: no tunnel, no watcher, no notification.")
            preferencesHelper.globalUserConnectionPreference = false
            runCatching { disconnect(error = VPNState.Error(error = VPNState.ErrorType.UserTurnOff)) }
                .onFailure { logger.error("Teardown failed on the way to turning off.", it) }
            endSession(ConnectionPolicy.SessionEnd.UserTurnOff)
        }

        /** Applies a change to Auto-Connect, which nothing observes. Call after writing it. */
        fun onAutoConnectChanged() {
            scope.launch {
                if (preferencesHelper.autoConnect) {
                    // Turning it on outranks a stand-down recorded on this network before it.
                    deviceStateManager.setWhitelistedNetwork(null)
                }
                reconcile("Auto-Connect is now ${preferencesHelper.autoConnect}")
            }
        }

        /**
         * [ConnectionPolicy.ServiceMode.AccountBlocked] is the only mode nothing else clears.
         *
         * Expired and banned both land there, and both took away a session the user had asked for,
         * so recovering gives it back regardless of Auto-Connect — that switch governs connecting on
         * its own, not restoring what we interrupted.
         */
        fun onAccountRecovered() {
            scope.launch {
                if (vpnConnectionStateManager.serviceMode.value !is ConnectionPolicy.ServiceMode.AccountBlocked) {
                    return@launch
                }
                logger.info("The account can connect again; clearing the block.")
                vpnConnectionStateManager.setServiceMode(null)
                if (preferencesHelper.globalUserConnectionPreference) {
                    logger.info("Restoring the session the account block interrupted.")
                    // Through failover, like every other reconnect the session refresh drives.
                    autoConnectionManager.connectInForeground(ConnectSource.SessionRefresh)
                    return@launch
                }
                reconcile("the account can connect again")
            }
        }

        /** Acts on [ConnectionPolicy.reconcile]. @param reason what changed, for the log. */
        private suspend fun reconcile(reason: String) {
            when (ConnectionPolicy.reconcile(policyInputs.current())) {
                Effect.Connect -> {
                    logger.info("Connecting: $reason, and this network should be secured.")
                    autoConnectionManager.reset()
                    connectAsync(ConnectSource.App)
                }

                Effect.StartWatching -> {
                    logger.info("Starting the watcher for the next network: $reason.")
                    appContext.startVpnServiceWatching()
                }

                // Releases a held TUN as well.
                Effect.StopService -> {
                    logger.info("Nothing left for the service to watch for: $reason.")
                    endSession(ConnectionPolicy.SessionEnd.UserTurnOff)
                }

                Effect.None -> logger.debug("Nothing to do: $reason.")
            }
        }

        /**
         * Ends a session the orchestrator stopped driving: a held TUN must be resolved once no
         * reconnect is coming, and the service told, or it lingers on the last published state.
         */
        fun endSession(end: ConnectionPolicy.SessionEnd) {
            scope.launch {
                // A failover hold is bounded and released when the orchestrator gives up — but
                // with blocking on, giving up is exactly when traffic must not be let out, so the
                // TUN is kept and its watchdog cancelled. Inputs read once, so the publish holds.
                val input = policyInputs.current()
                if (ConnectionPolicy.shouldBlockOutsideVpn(input, end)) {
                    if (tunnelManager.get().holdForBlocking()) {
                        logger.info("No reconnect is coming; holding the TUN to block traffic.")
                    }
                } else if (tunnelManager.get().releaseHeldTunnel()) {
                    logger.info("Released a held TUN: no reconnect is coming.")
                }
                serviceGate.current?.endSession(end, input)
            }
        }

        /**
         * Ends the tunnel. **[error] is required, and states who ended it and why.**
         *
         * A `UserDisconnect` stands the current network down, changes the notification copy, and
         * tells `AutoConnectionManager` to abandon failover. Pass [SYSTEM_DISCONNECT] when the app or
         * system decided, [USER_DISCONNECT] only when the user asked.
         */
        fun disconnectAsync(
            error: VPNState.Error?,
            waitForNextProtocol: Boolean = false,
            reconnecting: Boolean = false,
        ) {
            scope.launch {
                disconnect(waitForNextProtocol, reconnecting, error)
            }
        }

        /**
         * Connects or reconnect to the VPN
         */
        open suspend fun connect(
            source: ConnectSource,
            connectionId: UUID = UUID.randomUUID(),
            protocolInformation: ProtocolInformation? = null,
            attempt: Int = 0,
            hostname: String? = null,
        ) {
            if (isLicenceSuspended) {
                logger.debug("Licence is suspended. Not connecting.")
                appContext.applicationInterface.showLicenceSuspendedDialog()
                return
            }
            when {
                // Disconnect from VPN and connect to next selected location.
                vpnBackendHolder.activeBackend?.active == true -> {
                    preferencesHelper.isReconnecting = true
                    disconnect(
                        reconnecting = true,
                        error = VPNState.Error(error = VPNState.ErrorType.UserReconnect),
                    )
                    createProfileAndLaunchService(
                        source,
                        connectionId,
                        protocolInformation,
                        attempt,
                        hostname,
                    )
                }

                else -> {
                    // Make a fresh connection
                    createProfileAndLaunchService(
                        source,
                        connectionId,
                        protocolInformation,
                        attempt,
                        hostname,
                    )
                }
            }
        }

        private suspend fun createProfileAndLaunchService(
            source: ConnectSource,
            connectionId: UUID = UUID.randomUUID(),
            selectedProtocol: ProtocolInformation? = null,
            attempt: Int = 0,
            hostname: String? = null,
        ) {
            try {
                ConnectionLogContext.set(connectionId)
                activeConnectionId = connectionId
                logger.debug("Connecting to VPN.")
                vpnConnectionStateManager.setState(VPNState(Connecting, connectionId = connectionId))
                // Before anything is established: requestTun() applies these to *every* interface
                // and the blocking one comes first, so loading them later leaves it capturing the
                // addresses the user excluded. A cached DB read, so it costs nothing here.
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU && preferencesHelper.splitTunnelToggle) {
                    excludedIpHolder.loadCachedIps()
                } else {
                    excludedIpHolder.clear()
                }
                blockBeforeBuildingProfile()
                // After the block: an API round trip runs in the clear until the block lands, and
                // nothing above needs a location.
                setLocationToConnect()
                // Only a user connect settles the per-network facts: auto-connect is no longer held
                // back here, and the opt-in stops Auto-Secure-off tearing this session down. Both
                // describe what the user wants, so an app-initiated reconnect leaves them alone
                // rather than undoing a decision the user made or inventing one they did not.
                if (source == ConnectSource.User) {
                    deviceStateManager.setWhitelistedNetwork(null)
                    deviceStateManager.setOptedInNetwork(deviceStateManager.getCurrentNetworkName())
                }
                val protocolInformation =
                    selectedProtocol?.let {
                        autoConnectionManager.setSelectedProtocol(it)
                        return@let it
                    } ?: getProtocolInformationToConnect()
                // Bounded here because startConnectionJob()'s budget only starts inside
                // backend.connect(), leaving the API round trips that build the profile untimed.
                val profileToConnect =
                    withTimeout(PROFILE_TIMEOUT_MS) {
                        createVPNProfile(protocolInformation, attempt, hostname)
                    }
                logger.info("Location: $profileToConnect")
                launchVPNService(protocolInformation, connectionId)
            } catch (e: TimeoutCancellationException) {
                // Scoped to the withTimeout above, so catching a CancellationException here does not
                // swallow a real cancellation of the enclosing job.
                logger.error("Timed out preparing the connection after ${PROFILE_TIMEOUT_MS}ms.")
                disconnect(
                    error =
                        VPNState.Error(
                            VPNState.ErrorType.TimeoutError,
                            "Timed out preparing the connection.",
                        ),
                )
            } catch (e: CancellationException) {
                // Disconnecting again would re-enter the teardown waiting on this coroutine.
                logger.debug("Connect preparation was cancelled.")
                throw e
            } catch (e: Exception) {
                if (e is InvalidVPNConfigException) {
                    handleVPNError(e.error, connectionId, selectedProtocol)
                } else {
                    logger.debug(e.message)
                    disconnect()
                }
            }
        }

        /**
         * Blocks before the profile build, starting the service if that is what it takes.
         *
         * What follows is up to [PROFILE_TIMEOUT_MS] of API round trips, which would otherwise run
         * uncovered on a cold start where no service exists yet.
         *
         * Only starts the service when it buys something: blocking must be in force and VPN
         * permission already granted, since without it `establish()` returns null.
         */
        private suspend fun blockBeforeBuildingProfile() {
            serviceGate.current?.let {
                it.beginBlockingIfRequired()
                return
            }
            if (!ConnectionPolicy.blockingApplies(policyInputs.current())) {
                return
            }
            if (VpnService.prepare(appContext) != null) {
                logger.debug("No VPN permission yet; the profile build cannot be blocked.")
                return
            }
            serviceGate.ensureRunning()?.beginBlockingIfRequired()
        }

        private fun getProtocolInformationToConnect(): ProtocolInformation {
            // use default protocol if list protocol is not ready yet.
            if (autoConnectionManager.listOfProtocols.isEmpty()) {
                return ProtocolInformation(
                    PROTO_WIRE_GUARD,
                    PreferencesKeyConstants.DEFAULT_WIRE_GUARD_PORT,
                    "WireGuard is a modern, high-performance VPN protocol.",
                    ProtocolConnectionStatus.Disconnected,
                )
            }
            val config: ProtocolInformation =
                autoConnectionManager.listOfProtocols.firstOrNull { it.type == ProtocolConnectionStatus.NextUp }
                    ?: autoConnectionManager.listOfProtocols.first()
            // Decoy traffic only works in Wireguard
            if (preferencesHelper.isDecoyTrafficOn) {
                Util.buildProtocolInformation(
                    autoConnectionManager.listOfProtocols,
                    PROTO_WIRE_GUARD,
                    preferencesHelper.wireGuardPort,
                )
            }
            autoConnectionManager.setSelectedProtocol(config)
            return config
        }

        private suspend fun setLocationToConnect() {
            val city = locationRepository.updateLocation()
            locationRepository.setSelectedCity(city)
        }

        /**
         * @param waitForNextProtocol Disconnects VPN.
         * @param reconnecting only disconnecting to change location/protocol config.
         * */
        private suspend fun disconnect(
            waitForNextProtocol: Boolean = false,
            reconnecting: Boolean = false,
            error: VPNState.Error? = null,
        ) {
            logger.debug(
                "Disconnecting from VPN: Waiting for next protocol: $waitForNextProtocol Reconnecting: $reconnecting Error: ${error?.error}",
            )
            // Never joins itself: the reconnect path calls this from inside that connect.
            connectJob?.takeIf { it !== currentCoroutineContext()[Job] }?.let { preparing ->
                connectJob = null
                runCatching { preparing.cancelAndJoin() }
            }
            if (WindStunnelUtility.isStunnelRunning) {
                WindStunnelUtility.stopLocalTunFromAppContext(appContext)
            }
            // Before the teardown, which asks the policy what the service becomes and reads this.
            // Only a user disconnect stands the network down.
            val isUserDisconnect = error?.error == VPNState.ErrorType.UserDisconnect
            if (!reconnecting && isUserDisconnect) {
                // Here with the stand-down, not at each call site: one surface missing it left the
                // same act reconnecting on the next boot.
                preferencesHelper.globalUserConnectionPreference = false
                deviceStateManager.standDownCurrentNetwork()
            } else {
                logger.debug("Not standing the network down (reconnecting=$reconnecting, error=${error?.error})")
            }

            // A connect that failed before reaching a backend activated nothing, so runDisconnect
            // never publishes the terminal state and the UI sits on Connecting.
            val hadBackend = vpnBackendHolder.activeBackend != null
            vpnBackendHolder.disconnect(error)
            if (!hadBackend && vpnConnectionStateManager.state.value.status != Disconnected) {
                logger.debug("No backend was active; publishing the terminal state directly.")
                vpnConnectionStateManager.setState(VPNState(Disconnected, error = error), true)
                // The user's reason, not ConnectFailed: a deliberate stop reported as a failure
                // leaves the policy watching and blocking.
                val end =
                    when (error?.error) {
                        VPNState.ErrorType.UserPause -> ConnectionPolicy.SessionEnd.UserPause
                        VPNState.ErrorType.UserTurnOff,
                        VPNState.ErrorType.Revoked,
                        -> ConnectionPolicy.SessionEnd.UserTurnOff
                        VPNState.ErrorType.UserDisconnect -> ConnectionPolicy.SessionEnd.UserDisconnect
                        else -> ConnectionPolicy.SessionEnd.ConnectFailed
                    }
                serviceGate.current?.endSession(end, policyInputs.current())
            }

            // endSession() publishes what the service settles into; this covers only the reconnect
            // path, which never reaches it.
            if (reconnecting && vpnConnectionStateManager.state.value.status != Disconnected) {
                vpnConnectionStateManager.setState(VPNState(Disconnected, error = error), true)
                delay(100)
            }
            // Not off the Disconnected state, which most call sites emit without a connectionId and
            // on another dispatcher, landing after the next attempt stamped its own.
            if (!reconnecting) {
                activeConnectionId = null
                ConnectionLogContext.clear()
            }
        }

        /** Always false for plain Windscribe builds; their API never returns this code. */
        private val isLicenceSuspended: Boolean
            get() = preferencesHelper.lastSessionErrorCode == ERROR_LICENCE_SUSPENDED

        /**
         * @param serviceClass Service class name
         * @return return true if service is running
         */
        @Suppress("DEPRECATION")
        private fun isServiceRunning(serviceClass: Class<*>): Boolean {
            val manager: ActivityManager? =
                appContext.getSystemService(ACTIVITY_SERVICE) as ActivityManager?
            if (manager != null) {
                for (service in manager.getRunningServices(Int.MAX_VALUE)) {
                    if (serviceClass.name == service.service.className) {
                        return true
                    }
                }
            }
            return false
        }

        private suspend fun handleVPNError(
            error: CallResult.Error,
            connectionId: UUID,
            protocolInformation: ProtocolInformation?,
        ) {
            logger.debug("code: ${error.code} error: ${error.errorMessage}")
            when (error.code) {
                ERROR_UNABLE_TO_REACH_API, ERROR_UNEXPECTED_API_DATA -> {
                    val message = "Unable to reach the server. Please check your internet connection."
                    val errorType = VPNState.ErrorType.WireguardApiError
                    disconnect(
                        error =
                            VPNState.Error(
                                error = errorType,
                                message = message,
                                showError = true,
                            ),
                    )
                }

                ERROR_WG_KEY_LIMIT_EXCEEDED -> {
                    // With no screen the dialog is never posted, so neither callback ever runs.
                    if (appContext.activeActivity == null) {
                        logger.info("WireGuard key limit reached with no screen to ask on; ending the attempt.")
                        disconnect(
                            error =
                                VPNState.Error(
                                    error = VPNState.ErrorType.GenericError,
                                    message = error.errorMessage,
                                ),
                        )
                        return
                    }
                    showRetryDialog(error.errorMessage, {
                        // Not the connect's scope: it ends when this dialog is posted, and a
                        // launch on a finished job never runs.
                        scope.launch {
                            logger.info("Replacing the oldest WireGuard key and retrying.")
                            vpnProfileCreator.wgForceInit.set(true)
                            val vpnState =
                                VPNState(
                                    Disconnected,
                                    error =
                                        VPNState.Error(
                                            error = VPNState.ErrorType.WireguardAuthenticationError,
                                            "Wireguard wg key limit exceeded.",
                                        ),
                                )
                            vpnState.protocolInformation = protocolInformation
                            vpnState.connectionId = connectionId
                            vpnConnectionStateManager.setState(vpnState)
                            // Failover collects that state; an auto-connect has nothing that does.
                            if (!autoConnectionManager.isFailoverActive) {
                                connect(ConnectSource.User, protocolInformation = protocolInformation)
                            }
                        }
                    }, {
                        logger.info("Cancel clicked")
                        preferencesHelper.globalUserConnectionPreference = false
                        // The user dismissed the wg key-limit dialog, so this one really is theirs.
                        disconnectAsync(USER_DISCONNECT)
                    }, title = appContext.getString(R.string.note))
                }

                ERROR_UNABLE_TO_SELECT_WIRE_GUARD_IP, ERROR_WG_UNABLE_TO_GENERATE_PSK -> {
                    disconnect(
                        error =
                            VPNState.Error(
                                error = VPNState.ErrorType.WireguardApiError,
                                error.errorMessage,
                                showError = true,
                            ),
                    )
                }

                ERROR_WG_INVALID_PUBLIC_KEY -> {
                    wgConfigRepository.deleteKeys()
                    disconnect(
                        error =
                            VPNState.Error(
                                error = VPNState.ErrorType.WireguardApiError,
                                error.errorMessage,
                                showError = true,
                            ),
                    )
                }

                ERROR_INVALID_DNS_ADDRESS -> {
                    disconnect(
                        error =
                            VPNState.Error(
                                error = VPNState.ErrorType.WireguardApiError,
                                error.errorMessage,
                                showError = true,
                            ),
                    )
                }

                EXPIRED_OR_BANNED_ACCOUNT, ERROR_VALID_CONFIG_NOT_FOUND -> {
                    logger.debug("Forcing session update.")
                    preferencesHelper.migrationRequired = true
                    appContext.workManager.updateSession()
                    disconnect(
                        error =
                            VPNState.Error(
                                error = VPNState.ErrorType.WireguardApiError,
                                error.errorMessage,
                                showError = true,
                            ),
                    )
                }

                // Any code here means the profile could not be built, recognised or not; without
                // this the state stays Connecting forever.
                else -> {
                    logger.debug("Unhandled profile error ${error.code}; ending the attempt.")
                    disconnect(
                        error =
                            VPNState.Error(
                                error = VPNState.ErrorType.WireguardApiError,
                                error.errorMessage,
                                showError = true,
                            ),
                    )
                }
            }
        }

        suspend fun connectUsingEmergencyProfile(callback: (String) -> Unit): Result<Unit> {
            return emergencyConnectRepository.getConnectionInfo().mapCatching { connectionInfo ->
                var connectionAttempt = 0
                vpnConnectionStateManager.state
                    .takeWhile {
                        it.status != Connected
                    }.collect { state ->
                        logger.debug("$connectionAttempt ${state.status}")
                        if (connectionAttempt > 0 && state.error?.error == VPNState.ErrorType.UserDisconnect) {
                            return@collect
                        }
                        if (state.status == Disconnected && connectionAttempt < connectionInfo.size) {
                            val text = appContext.resources.getString(R.string.connecting)
                            callback(text)
                            vpnConnectionStateManager.setState(VPNState(status = Connecting))
                            val connectionUUID = UUID.randomUUID()
                            val openVPNInfo = connectionInfo[connectionAttempt]
                            vpnProfileCreator.createOpenVPNProfile(openVPNInfo)
                            val lastSelectedLocation =
                                LastSelectedLocation(
                                    -1,
                                    nodeName = "Emergency",
                                    nickName = "Windscribe location",
                                )
                            Util.saveSelectedLocation(lastSelectedLocation)
                            val protocolInformation = openVPNInfo.getProtocolInformation()
                            autoConnectionManager.setSelectedProtocol(protocolInformation)
                            launchVPNService(protocolInformation, connectionUUID)
                            connectionAttempt++
                        } else if (state.status != VPNState.Status.Connecting) {
                            if (connectionAttempt >= connectionInfo.size) {
                                throw WindScribeException("No more profiles left to connect.")
                            }
                        }
                    }
            }
        }
    }
