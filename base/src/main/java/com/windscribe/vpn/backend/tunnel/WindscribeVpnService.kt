/*
 * Copyright (c) 2026 Windscribe Limited.
 */

package com.windscribe.vpn.backend.tunnel

import android.content.Context
import android.content.Intent
import android.net.VpnService
import android.os.Build
import android.os.ParcelFileDescriptor
import com.windscribe.common.DNSDetails
import com.windscribe.common.startSafeForeground
import com.windscribe.common.tunnel.PacketMux
import com.windscribe.common.tunnel.StopReason
import com.windscribe.common.tunnel.TunConfig
import com.windscribe.common.tunnel.TunEstablishException
import com.windscribe.common.tunnel.TunReplacementListener
import com.windscribe.common.tunnel.TunnelManager
import com.windscribe.common.tunnel.VpnHost
import com.windscribe.vpn.apppreference.PreferencesHelper
import com.windscribe.vpn.apppreference.PreferencesKeyConstants
import com.windscribe.vpn.backend.ProxyDNSManager
import com.windscribe.vpn.backend.VPNState
import com.windscribe.vpn.backend.VPNState.Status.Connecting
import com.windscribe.vpn.backend.utils.ConnectSource
import com.windscribe.vpn.backend.utils.ExcludedIpHolder
import com.windscribe.vpn.backend.utils.USER_DISCONNECT
import com.windscribe.vpn.backend.utils.WindNotificationBuilder
import com.windscribe.vpn.backend.utils.WindVpnController
import com.windscribe.vpn.backend.utils.startForegroundImmediately
import com.windscribe.vpn.backend.utils.startForegroundSafely
import com.windscribe.vpn.constants.NotificationConstants
import com.windscribe.vpn.repository.AdvanceParameterRepository
import com.windscribe.vpn.state.ConnectionPolicy
import com.windscribe.vpn.state.ConnectionPolicyInputs
import com.windscribe.vpn.state.ShortcutStateManager
import com.windscribe.vpn.state.VPNConnectionStateManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.slf4j.LoggerFactory
import java.util.UUID
import java.util.concurrent.atomic.AtomicLong
import javax.inject.Inject

/**
 * The only `android.net.VpnService` in the app.
 *
 * The TUN belongs to the session, not the protocol: established once and held until the session
 * ends. Handover happens behind it in [PacketMux] by swapping which engine holds a socketpair.
 *
 * [requestTun] is deliberately awkward to reach: [TunnelManager] asks [VpnHost.tunCovers] first.
 */
@AndroidEntryPoint
class WindscribeVpnService :
    VpnService(),
    VpnHost {
    @Inject
    lateinit var windNotificationBuilder: WindNotificationBuilder

    @Inject
    lateinit var tunnelManager: TunnelManager

    @Inject
    lateinit var serviceGate: VpnServiceGate

    @Inject
    lateinit var vpnController: WindVpnController

    @Inject
    lateinit var shortcutStateManager: ShortcutStateManager

    @Inject
    lateinit var proxyDNSManager: ProxyDNSManager

    @Inject
    lateinit var preferencesHelper: PreferencesHelper

    @Inject
    lateinit var excludedIpHolder: ExcludedIpHolder

    @Inject
    lateinit var advanceParameterRepository: AdvanceParameterRepository

    @Inject
    lateinit var scope: CoroutineScope

    @Inject
    lateinit var autoConnectWatcher: AutoConnectWatcher

    @Inject
    lateinit var policyInputs: ConnectionPolicyInputs

    @Inject
    lateinit var vpnConnectionStateManager: VPNConnectionStateManager

    private val logger = LoggerFactory.getLogger("vpn")

    /** The config behind the live TUN. Guarded by [tunLock] along with the descriptors. */
    private var liveTunConfig: TunConfig? = null
    private var liveTun: ParcelFileDescriptor? = null

    /**
     * Descriptors each generation superseded, held open while a pump may still be reading one. A map
     * because two replacements can be in flight. Closed by [releasePreviousTun] or [releaseTun].
     */
    private val supersededTuns = mutableMapOf<Long, ParcelFileDescriptor>()

    /** Process-wide: [TunnelManager] outlives us, and a per-instance counter would restart. */
    private val tunGeneration = TUN_GENERATIONS
    private val tunLock = Any()

    @Volatile
    private var tunReplacementListener: TunReplacementListener? = null

    override fun onCreate() {
        // Promote to foreground IMMEDIATELY, before DI, or a slow device throws
        // ForegroundServiceDidNotStartInTimeException.
        startForegroundImmediately(NotificationConstants.SERVICE_NOTIFICATION_ID)
        super.onCreate()
        startForegroundSafely(
            windNotificationBuilder,
            NotificationConstants.SERVICE_NOTIFICATION_ID,
            Connecting,
        )
        tunnelManager.attachHost(this)
        serviceGate.attach(this)
        // This service stays alive while idle, so it owns the network watching too.
        autoConnectWatcher.start(scope)
        logger.info("Windscribe VPN service created; it owns the TUN for this session.")
    }

    override fun onStartCommand(
        intent: Intent?,
        flags: Int,
        startId: Int,
    ): Int {
        // A start command cancels a stopSelf() that has not taken effect, and onCreate will not run
        // again, so this is the only thing that can clear the stopping flag.
        serviceGate.attach(this)
        // The system started us: Always-on, or a restart after being killed. The app may not be
        // ready, so hand off rather than assume a profile. Nobody pressed anything, so this records
        // no per-network user intent.
        if (intent == null || intent.action == SERVICE_INTERFACE) {
            logger.debug("Started by the system; deferring to ShortcutStateManager.")
            startForegroundSafely(
                windNotificationBuilder,
                NotificationConstants.SERVICE_NOTIFICATION_ID,
                Connecting,
            )
            shortcutStateManager.connect(ConnectSource.App)
            return START_NOT_STICKY
        }

        // Each action posts the notification for the state it is heading into.
        when (intent.action) {
            // Foreground and observing, but no TUN. Only when nothing is already connecting: at
            // boot the startup path and the watcher race.
            ACTION_WATCH ->
                if (vpnConnectionStateManager.isTunnelUp()) {
                    logger.debug("Already connecting; leaving the session alone rather than settling it.")
                } else {
                    logger.debug("Started to watch the network; no tunnel will be established.")
                    startForegroundSafely(
                        windNotificationBuilder,
                        NotificationConstants.SERVICE_NOTIFICATION_ID,
                        VPNState.Status.Disconnected,
                    )
                    endSession(ConnectionPolicy.SessionEnd.SystemDisconnect)
                }

            // A held TUN with no session means we are the reason the device has no internet, and
            // this notification is the only way to Turn off.
            ACTION_NOTIFICATION_DISMISSED ->
                if (tunnelManager.hasTunnel && !vpnConnectionStateManager.isTunnelUp()) {
                    logger.info("Notification dismissed while traffic is blocked; posting it again.")
                    startForegroundSafely(
                        windNotificationBuilder,
                        NotificationConstants.SERVICE_NOTIFICATION_ID,
                        VPNState.Status.Disconnected,
                    )
                }

            ACTION_DISCONNECT ->
                // Without a tunnel there is no active backend, so VpnBackendHolder.disconnect is a
                // no-op and the session end would never arrive. Decide it here instead.
                if (!tunnelManager.hasTunnel) {
                    endSession(ConnectionPolicy.SessionEnd.UserTurnOff)
                } else {
                    startForegroundSafely(
                        windNotificationBuilder,
                        NotificationConstants.SERVICE_NOTIFICATION_ID,
                        VPNState.Status.Disconnecting,
                    )
                    scope.launch { vpnController.disconnectAsync(USER_DISCONNECT) }
                }

            // Under lockdown, closing the TUN blackholes rather than releases, so Pause is
            // dishonest. The notification hides it too; this guards an intent that arrives anyway.
            ACTION_PAUSE ->
                if (isSystemLockdownEnabled()) {
                    logger.warn("Refusing to pause: the system killswitch would blackhole the device.")
                } else {
                    // Before teardown, or the backend's Disconnected briefly renders as
                    // "DISCONNECTED". A failed pause clears it via setState.
                    vpnConnectionStateManager.setServiceMode(ConnectionPolicy.ServiceMode.Paused)
                    scope.launch { vpnController.pause() }
                }

            // Resume from Paused and Connect from Watching are one act with two labels.
            // ShortcutStateManager covers a boot start where the session is not loaded yet.
            ACTION_RESUME -> {
                // Clear first, or the notification below renders the stale Paused mode.
                vpnConnectionStateManager.setServiceMode(null)
                startForegroundSafely(
                    windNotificationBuilder,
                    NotificationConstants.SERVICE_NOTIFICATION_ID,
                    Connecting,
                )
                shortcutStateManager.connect(ConnectSource.User)
            }

            ACTION_TURN_OFF -> {
                logger.info("Turned off by the user; the service is stopping.")
                scope.launch { vpnController.turnOff() }
            }

            else ->
                startForegroundSafely(
                    windNotificationBuilder,
                    NotificationConstants.SERVICE_NOTIFICATION_ID,
                    Connecting,
                )
        }

        // Sticky only while the user wants a tunnel; an idle service should stay dead.
        val idleAction =
            intent.action in setOf(ACTION_WATCH, ACTION_DISCONNECT, ACTION_PAUSE, ACTION_TURN_OFF, ACTION_NOTIFICATION_DISMISSED)
        return if (!idleAction && preferencesHelper.globalUserConnectionPreference) {
            START_STICKY
        } else {
            START_NOT_STICKY
        }
    }

    /**
     * Ends the session. This is the only path on which a TUN is closed — and it closes because the
     * session is over, never because a protocol changed.
     */
    override fun onDestroy() {
        logger.info("Windscribe VPN service destroyed.")
        // An idle mode describes a live service, so leaving one published shows Paused or Watching
        // with nothing behind it. Only while still registered: a replacement's mode is not ours.
        val stillRegistered = serviceGate.current === this
        if (stillRegistered) {
            vpnConnectionStateManager.serviceMode.value?.let {
                logger.info("Destroyed while $it; clearing the mode, since no service is left to be in one.")
                vpnConnectionStateManager.setServiceMode(null)
            }
        }
        serviceGate.detach(this)
        autoConnectWatcher.stop()
        tunnelManager.detachHost(this)
        // detachHost() already releases; this covers a kill the manager never saw.
        releaseTun()
        windNotificationBuilder.cancelNotification(NotificationConstants.SERVICE_NOTIFICATION_ID)
        super.onDestroy()
    }

    /**
     * The user or the system revoked VPN consent. Terminal: the interface is already gone, so there
     * is nothing to hand over to.
     */
    override fun onRevoke() {
        logger.warn("VPN consent revoked.")
        tunnelManager.onRevoked()
        scope.launch {
            vpnController.disconnectAsync(
                error = VPNState.Error(VPNState.ErrorType.Revoked, "VPN permission revoked"),
            )
        }
        super.onRevoke()
    }

    /**
     * A session ended. [ConnectionPolicy] decides what this service becomes; this publishes it.
     *
     * Synchronous, and takes [input] rather than reading it. Reading the policy inputs is a database
     * round trip, and anything between the disconnect and the publish is a window in which the
     * service can die with the session end unrecorded — the notification then disappears instead of
     * settling into Watching. Callers already hold these inputs; they decided the hold with them.
     *
     * Not unconditional: a user who never wanted this network secured would keep a notification.
     */
    fun endSession(
        end: ConnectionPolicy.SessionEnd,
        input: ConnectionPolicy.PolicyInput,
        endingConnectionId: UUID? = null,
    ) {
        val decided = ConnectionPolicy.serviceModeAfterSession(input, end)
        // The predicate that installs and releases a hold, so the outcome cannot depend on
        // whether a TUN happened to survive into this.
        if (ConnectionPolicy.shouldBlockOutsideVpn(input, end) && !tunnelManager.hasTunnel) {
            logger.warn("Nothing is left holding traffic back; establishing the blocking TUN.")
            beginBlocking()
        }
        // Policy said whether traffic should be held; only the interface says whether it is.
        val mode =
            if (decided is ConnectionPolicy.ServiceMode.Failed) {
                decided.copy(blocking = tunnelManager.hasTunnel)
            } else {
                decided
            }
        if (decided != mode) {
            logger.warn("Blocking was called for but no TUN is holding traffic; not reporting it as blocked.")
        }
        // A connect started while this session ends must not be settled over. It has to be a
        // *different* one: this session's terminal state is published asynchronously.
        val liveConnectionId = vpnConnectionStateManager.state.value.connectionId
        val anotherConnect =
            vpnConnectionStateManager.isTunnelUp() &&
                (endingConnectionId == null || liveConnectionId != endingConnectionId)
        if (anotherConnect) {
            logger.debug("A connect started while this session was ending; leaving it alone.")
            return
        }
        when (mode) {
            is ConnectionPolicy.ServiceMode.Off -> {
                logger.info("Nothing to watch; stopping the service.")
                // Before stopSelf and before the terminal state: nothing may post a notification
                // this service will not be around to cancel.
                serviceGate.markStopping()
                stopSelf()
            }

            is ConnectionPolicy.ServiceMode.Failed -> {
                // Not necessarily every protocol: a preferred-protocol switch tries only one.
                logger.info("Could not connect; staying up so the user can see it and retry.")
                publish(mode)
            }

            is ConnectionPolicy.ServiceMode.AwaitingSession -> {
                logger.info("Waiting for the session refresh to say why the tunnel dropped.")
                publish(mode)
            }

            is ConnectionPolicy.ServiceMode.AuthFailed -> {
                logger.info("The server in the user's config rejected their credentials.")
                publish(mode)
            }

            is ConnectionPolicy.ServiceMode.AccountBlocked -> {
                logger.info("Account is ${mode.status}; staying up to report it.")
                publish(mode)
            }

            is ConnectionPolicy.ServiceMode.NoNetwork -> {
                logger.info("No network to connect over; staying up to notice one arriving.")
                publish(mode)
            }

            is ConnectionPolicy.ServiceMode.Paused -> {
                logger.info("Paused by the user; no TUN, traffic flows in the clear, service stays up.")
                publish(mode)
            }

            is ConnectionPolicy.ServiceMode.Watching -> {
                logger.info("Watching for a network to secure (${mode.reason}).")
                publish(mode)
            }
        }
    }

    /** For callers with no inputs in hand. Prefer the overload above, which cannot lose the publish. */
    fun endSession(end: ConnectionPolicy.SessionEnd) {
        scope.launch { endSession(end, policyInputs.current()) }
    }

    /**
     * Blocks everything until an engine attaches. Idempotent, and suspends rather than launching:
     * the caller's next move is the profile build this covers.
     */
    suspend fun beginBlockingIfRequired() {
        if (tunnelManager.hasTunnel) {
            return
        }
        if (!ConnectionPolicy.blockingApplies(policyInputs.current())) {
            return
        }
        beginBlocking()
    }

    /** Establishes the blocking TUN, with the same exclusions a live tunnel would have. */
    private fun beginBlocking() {
        val splitTunnelApps =
            if (preferencesHelper.splitTunnelToggle) {
                preferencesHelper.installedApps
            } else {
                emptyList()
            }
        val inclusive =
            preferencesHelper.splitRoutingMode == PreferencesKeyConstants.INCLUSIVE_MODE
        val blocked =
            tunnelManager.beginBlocking(
                sessionName = getString(com.windscribe.vpn.R.string.app_name),
                lanBypass = preferencesHelper.lanByPass,
                allowedApps = if (inclusive) splitTunnelApps else emptyList(),
                disallowedApps = if (inclusive) emptyList() else splitTunnelApps,
            )
        if (blocked) {
            logger.info("Blocking traffic until the connection comes up.")
        }
    }

    /**
     * The status is always [VPNState.Status.Disconnected] here, with the mode alongside it.
     * `startForeground` is re-issued to keep foreground status while idle.
     */
    private fun publish(mode: ConnectionPolicy.ServiceMode) {
        // Mode before status, so a collector woken by the status change already sees which mode.
        vpnConnectionStateManager.setServiceMode(mode)
        vpnConnectionStateManager.setState(VPNState(VPNState.Status.Disconnected), force = true)
        startSafeForeground(
            NotificationConstants.SERVICE_NOTIFICATION_ID,
            windNotificationBuilder.buildNotification(VPNState.Status.Disconnected),
            android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE,
        )
    }

    // ---------------------------------------------------------------- VpnHost

    override val hostContext: Context
        get() = this

    override fun protect(socketFd: Int): Boolean = super<VpnService>.protect(socketFd)

    override fun currentTunConfig(): TunConfig? = synchronized(tunLock) { liveTunConfig }

    override fun setTunReplacementListener(listener: TunReplacementListener?) {
        tunReplacementListener = listener
    }

    override fun releasePreviousTun(generation: Long) {
        synchronized(tunLock) {
            val superseded = supersededTuns.remove(generation) ?: return
            runCatching { superseded.close() }
            logger.debug("Superseded TUN descriptor released.")
        }
    }

    override fun releaseTun() {
        synchronized(tunLock) {
            val tun = liveTun
            liveTun = null
            liveTunConfig = null
            if (tun != null) {
                runCatching { tun.close() }
                logger.info("TUN closed.")
            }
            // Anything whose notification never came back.
            supersededTuns.values.forEach { stale -> runCatching { stale.close() } }
            supersededTuns.clear()
        }
    }

    override fun isSystemLockdownEnabled(): Boolean =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            isLockdownEnabled
        } else {
            false
        }

    /** `from(null)`, not the suspending `current()`: answered under [TunnelManager]'s monitor. */
    override fun blockingApplies(): Boolean = ConnectionPolicy.blockingApplies(policyInputs.from(null))

    override fun dnsDetails(): DNSDetails? = proxyDNSManager.dnsDetails

    /** Read live. The port snapshotted into a saved profile is chosen before ctrld binds. */
    override fun controlDPort(): Int = proxyDNSManager.getListenPort()

    /** A user-settable advanced parameter, not a debug flag. */
    override fun packetLoggingEnabled(): Boolean = advanceParameterRepository.showCdLog()

    override fun onEngineStopped(
        engineId: String,
        reason: StopReason,
        detail: String?,
    ) {
        logger.info("Engine '$engineId' stopped: $reason${detail?.let { " ($it)" } ?: ""}")
        when (reason) {
            // The orchestrator asked for these and is already driving what comes next.
            StopReason.Requested -> Unit
            StopReason.Revoked -> Unit
            // Must carry a real error type. UserDisconnect here would stand the network down and
            // abort the failover meant to handle it.
            StopReason.EngineError ->
                scope.launch {
                    vpnController.disconnectAsync(
                        error = VPNState.Error(VPNState.ErrorType.GenericError, detail ?: "Engine failed"),
                    )
                }

            StopReason.TunnelBroken ->
                scope.launch {
                    vpnController.disconnectAsync(
                        error = VPNState.Error(VPNState.ErrorType.BrokenTunnel, detail ?: "Tunnel stopped passing traffic"),
                    )
                }
        }
    }

    /**
     * Establishes the TUN, or replaces it when the live one cannot carry [config].
     *
     * Establishing before releasing the old descriptor is what makes "never close, only replace"
     * true: the platform then takes its in-place path — same `NetworkAgent`, same netId, no `jniReset`.
     */
    @Throws(TunEstablishException::class)
    override fun requestTun(config: TunConfig): ParcelFileDescriptor {
        val result: ParcelFileDescriptor
        val generation: Long
        synchronized(tunLock) {
            val previous = liveTun
            val builder = Builder()
            builder.setSession(config.sessionName)

            config.addresses.forEach { builder.addAddress(it.address, it.prefixLength) }
            config.routes.forEach { builder.addRoute(it.address, it.prefixLength) }
            config.dnsServers.forEach { dns ->
                dns.hostAddress?.let { builder.addDnsServer(it) }
            }
            config.searchDomains.forEach { builder.addSearchDomain(it) }
            config.disallowedApps.forEach { runCatching { builder.addDisallowedApplication(it) } }
            config.allowedApps.forEach { runCatching { builder.addAllowedApplication(it) } }

            if (config.allowAllFamilies) {
                builder.allowFamily(android.system.OsConstants.AF_INET)
                builder.allowFamily(android.system.OsConstants.AF_INET6)
            }
            builder.setMtu(config.mtu)
            // PacketMux polls before each read; blocking would park a pump past quiesce's reach.
            builder.setBlocking(false)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                builder.setMetered(false)
            }
            setUnderlyingNetworks(null)

            // One implementation, one call site.
            excludedIpHolder.applyExcludedRoutes(builder)

            val established =
                builder.establish()
                    ?: throw TunEstablishException(
                        "establish() returned null — not prepared, or consent revoked",
                    )

            generation = tunGeneration.incrementAndGet()
            liveTun = established
            liveTunConfig = config
            // Retained, not closed: a pump may still be on it. Whoever moves the packet path off
            // calls releasePreviousTun with this generation.
            if (previous != null) {
                supersededTuns[generation] = previous
            }
            // App counts, or split tunnel and the control-plane exception are invisible in the log.
            val apps =
                when {
                    config.allowedApps.isNotEmpty() -> " onlyApps=${config.allowedApps.size}"
                    config.disallowedApps.isNotEmpty() -> " exceptApps=${config.disallowedApps.size}"
                    else -> ""
                }
            logger.info(
                "TUN ${if (previous == null) "established" else "REPLACED"}: " +
                    "mtu=${config.mtu} routes=${config.routes.size} addrs=${config.addresses.size}$apps",
            )
            result = established
        }

        // Outside tunLock: moving the packet path quiesces both pumps, and holding the lock across
        // that deadlocks. This is what makes OpenVPN's own management-thread requestTun() safe.
        val listener = tunReplacementListener
        if (listener == null) {
            // Nothing will move the packet path, so nothing else will retire the predecessor.
            releasePreviousTun(generation)
        } else {
            listener.onTunReplaced(this, result.fileDescriptor, config.mtu, generation)
        }
        return result
    }

    companion object {
        private val TUN_GENERATIONS = AtomicLong()

        const val ACTION_CONNECT = "com.windscribe.vpn.action.CONNECT"
        const val ACTION_DISCONNECT = "com.windscribe.vpn.action.DISCONNECT"

        /** Come up and watch the network without connecting. */
        const val ACTION_WATCH = "com.windscribe.vpn.action.WATCH"

        /** The user swiped the notification away. Ongoing ones are dismissible from Android 14. */
        const val ACTION_NOTIFICATION_DISMISSED = "com.windscribe.vpn.action.NOTIFICATION_DISMISSED"

        /** Drop the tunnel but stay alive, letting traffic flow in the clear until resumed. */
        const val ACTION_PAUSE = "com.windscribe.vpn.action.PAUSE"

        /** Connect to the last location. Serves both Resume (from Paused) and Connect (from Watching). */
        const val ACTION_RESUME = "com.windscribe.vpn.action.RESUME"

        /** Stop everything, watching included. */
        const val ACTION_TURN_OFF = "com.windscribe.vpn.action.TURN_OFF"
    }
}
