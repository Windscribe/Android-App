/*
 * Copyright (c) 2026 Windscribe Limited.
 */

package com.windscribe.common.tunnel

import android.os.SystemClock
import com.windscribe.common.DnsType
import org.slf4j.LoggerFactory
import java.net.InetAddress
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit

/**
 * Owns the tunnel for a whole session: swap engines, never the TUN.
 *
 * A handover detaches one engine from [PacketMux] and attaches another; the interface keeps the
 * default route throughout, and is re-established only when [TunConfig.coversSameInterfaceAs] says
 * the live one cannot carry the incoming config. Between engines the mux sits in
 * [PacketMux.State.DROP], draining egress so apps see a black hole rather than a hang.
 *
 * See docs/architecture/VPN_BACKEND.md for the design.
 */
class TunnelManager(
    private val enginesByProtocol: Map<String, VpnEngine>,
) : PacketMux.Listener,
    ControlDDnsStage.FailureListener,
    TunReplacementListener {
    private companion object {
        val logger = LoggerFactory.getLogger("tunnel")

        /**
         * How long the TUN may sit held with no engine. Sized above a worst-case reconnect: a 30s
         * profile build, the connect budget, the failover countdown. Callers that gave up should
         * call [releaseHeldTunnel] rather than wait it out.
         */
        const val HOLD_TIMEOUT_MS = 90_000L

        /** Address for a TUN that carries nothing. Private range; nothing routes to it. */
        const val BLOCKING_ADDRESS = "172.16.252.2"
    }

    private var host: VpnHost? = null
    private var mux: PacketMux? = null
    private var activeEngine: VpnEngine? = null

    /** A flag, not a descriptor: the host owns every descriptor, and a second reference splits that. */
    private var tunnelUp = false

    /** Highest TUN generation the packet path has been moved onto. Guarded by this monitor. */
    private var appliedGeneration = 0L

    private val holdWatchdog =
        Executors.newSingleThreadScheduledExecutor { r ->
            Thread(r, "tun-hold-watchdog").apply { isDaemon = true }
        }
    private var holdExpiry: ScheduledFuture<*>? = null

    /** True once a session owns a TUN. The TUN outlives every engine within a session. */
    val hasTunnel: Boolean
        @Synchronized get() = tunnelUp

    val activeEngineId: String?
        @Synchronized get() = activeEngine?.id

    /** Per-session, not per-engine: the counters survive a handover, so callers work in deltas. */
    @Synchronized
    fun traffic(): PacketMux.Traffic? = mux?.traffic()

    @Synchronized
    fun attachHost(host: VpnHost) {
        this.host = host
        host.setTunReplacementListener(this)
        logger.info("host attached")
    }

    /** The service is going away. The only place a TUN is closed, and only at the end of a session. */
    @Synchronized
    fun detachHost() {
        cancelHoldWatchdog()
        stopEngineQuietly(StopReason.Requested)
        mux?.stop()
        mux = null
        closeTun()
        host?.setTunReplacementListener(null)
        host = null
        logger.info("host detached")
    }

    /**
     * Brings [profile] up, reusing the live TUN whenever it can carry [tunConfig].
     *
     * @throws IllegalStateException if no host is attached or the protocol has no engine.
     */
    @Synchronized
    @Throws(Exception::class)
    fun connect(
        profile: EngineProfile,
        tunConfig: TunConfig,
    ) {
        cancelHoldWatchdog()
        val currentHost = host ?: throw IllegalStateException("no VpnHost attached")
        val engine =
            enginesByProtocol[profile.protocol]
                ?: throw IllegalStateException("no engine registered for '${profile.protocol}'")

        val activeMux = ensureTunnel(currentHost, tunConfig)

        // Stop first so the engine releases its own descriptor, then detach. The TUN is untouched.
        stopEngineQuietly(StopReason.Requested)

        val engineFd = activeMux.attachEngine()
        activeEngine = engine
        try {
            engine.start(profile, currentHost, engineFd)
            logger.info("engine '${engine.id}' started on the existing TUN")
        } catch (e: Exception) {
            activeEngine = null
            // The engine never took ownership, so the mux side must be released here.
            activeMux.detachEngine()
            throw e
        }
    }

    /**
     * Stops the engine and holds the tunnel in DROP.
     *
     * @param hold whether the TUN stays established and blackholing, or is closed with the session.
     */
    @Synchronized
    fun disconnect(
        hold: TunHold,
        reason: StopReason = StopReason.Requested,
    ) {
        stopEngineQuietly(reason)
        mux?.detachEngine()
        when (hold) {
            TunHold.None -> {
                mux?.stop()
                mux = null
                closeTun()
            }

            TunHold.ForHandover -> {
                excludeSelfWhileHolding()
                armHoldWatchdog()
            }

            TunHold.ForBlocking -> {
                excludeSelfWhileHolding()
                // No watchdog: this hold must outlast a reconnect that may never come.
                cancelHoldWatchdog()
            }
        }
        logger.info("disconnected, hold=$hold")
    }

    /**
     * Lets our own process bypass the TUN while it is held with no engine attached, so the control
     * plane can build the next profile. Per-socket `protect` cannot do this: WSNet pools
     * connections and DNS fails before any HTTP socket exists.
     *
     * Costs one re-establish, and must not outlive the hold — a bypassed app makes the connectivity
     * test pass against a dead tunnel.
     */
    private fun excludeSelfWhileHolding() {
        val currentHost = host ?: return
        val live = currentHost.currentTunConfig() ?: return
        val self = currentHost.hostContext.packageName
        val excluded = live.withSelfExcluded(self)
        if (excluded == null) {
            // Leaks nothing, but our own traffic stays inside and a reconnect may not get out.
            logger.warn("we are the only tunnelled app, so we cannot be excluded from the hold")
            return
        }
        if (excluded == live) {
            return
        }
        try {
            currentHost.requestTun(excluded)
            logger.debug("holding the TUN with our own package excluded so the reconnect can use the network")
        } catch (e: Exception) {
            // Keep blackholing rather than drop it: an engine is about to attach.
            logger.error("could not exclude self while holding; the reconnect may fail", e)
        }
    }

    /** VPN consent was revoked. Terminal — the system has already torn the interface down. */
    @Synchronized
    fun onRevoked() {
        cancelHoldWatchdog()
        activeEngine?.let {
            runCatching { it.onRevoked() }
        }
        stopEngineQuietly(StopReason.Revoked)
        mux?.stop()
        mux = null
        closeTun()
    }

    /**
     * Drops a TUN that is being held with no engine attached. No-op otherwise.
     *
     * Callers must not use this to end a [TunHold.ForBlocking] hold: that hold exists precisely
     * because nothing should leave outside the VPN, and releasing it opens the leak it prevents.
     *
     * For an orchestrator that has given up: waiting out [HOLD_TIMEOUT_MS] when the answer is
     * already known is a needless outage.
     *
     * @return true if a held tunnel was released.
     */
    @Synchronized
    fun releaseHeldTunnel(): Boolean {
        if (!tunnelUp || activeEngine != null) {
            return false
        }
        cancelHoldWatchdog()
        mux?.stop()
        mux = null
        closeTun()
        logger.info("held tunnel released; no reconnect is coming")
        return true
    }

    /**
     * Establishes a TUN that carries nothing, covering a connection attempt from cold — blocking
     * otherwise works by holding an existing TUN, and a first attempt has none. No-op when a tunnel
     * already exists, so callers can ask on every attempt.
     *
     * @param lanBypass leaves private IPv4 ranges on the physical interface.
     * @param allowedApps split tunnel's inclusive list. Non-empty selects inclusive mode and
     * [disallowedApps] is ignored, since `VpnService.Builder` refuses both.
     * @param disallowedApps split tunnel's exclusive list: these bypass the interface.
     * @return true if a blocking tunnel was established.
     */
    @Synchronized
    fun beginBlocking(
        sessionName: String,
        lanBypass: Boolean,
        allowedApps: List<String> = emptyList(),
        disallowedApps: List<String> = emptyList(),
    ): Boolean {
        if (tunnelUp || mux != null) {
            return false
        }
        val currentHost = host ?: return false
        return try {
            val config =
                TunConfig.blackhole(
                    sessionName = sessionName,
                    addresses = listOf(IpNetwork(InetAddress.getByName(BLOCKING_ADDRESS), 32)),
                    lanBypass = lanBypass,
                    allowedApps = allowedApps,
                    disallowedApps = disallowedApps,
                    selfPackage = currentHost.hostContext.packageName,
                )
            val tun = currentHost.requestTun(config)
            tunnelUp = true
            val created = PacketMux(this, null, currentHost.packetLoggingEnabled())
            created.start(tun.fileDescriptor, config.mtu)
            mux = created
            logger.info("blocking tunnel established; nothing leaves until an engine attaches")
            true
        } catch (e: Exception) {
            // A TUN with no mux drains nothing, and hasTunnel would stop anything replacing it.
            closeTun()
            logger.error("could not establish the blocking tunnel", e)
            false
        }
    }

    /**
     * Turns a bounded [TunHold.ForHandover] into an unbounded blocking hold, for when failover gives
     * up with blocking on.
     *
     * @return true if a held tunnel was converted.
     */
    @Synchronized
    fun holdForBlocking(): Boolean {
        if (!tunnelUp || activeEngine != null) {
            return false
        }
        cancelHoldWatchdog()
        excludeSelfWhileHolding()
        logger.info("holding the TUN to block traffic; no reconnect is coming")
        return true
    }

    // ---------------------------------------------------------------- internals

    private fun armHoldWatchdog() {
        cancelHoldWatchdog()
        holdExpiry =
            holdWatchdog.schedule(
                { onHoldExpired() },
                HOLD_TIMEOUT_MS,
                TimeUnit.MILLISECONDS,
            )
    }

    private fun cancelHoldWatchdog() {
        holdExpiry?.cancel(false)
        holdExpiry = null
    }

    @Synchronized
    private fun onHoldExpired() {
        if (activeEngine != null || !tunnelUp) {
            return
        }
        if (host?.blockingApplies() == true) {
            // Closing here is the one path that leaks: the settling below has no TUN left to hold.
            cancelHoldWatchdog()
            logger.warn(
                "TUN held ${HOLD_TIMEOUT_MS}ms with no engine — the reconnect never came. " +
                    "Blocking applies, so the TUN stays and traffic stays held.",
            )
            host?.onEngineStopped("none", StopReason.EngineError, "hold expired; blocking")
            return
        }
        logger.error(
            "TUN held ${HOLD_TIMEOUT_MS}ms with no engine — the reconnect never came. " +
                "Dropping it rather than blackholing indefinitely.",
        )
        mux?.stop()
        mux = null
        closeTun()
        // Not TunnelBroken, a handover reason: it would send the orchestrator back into a hold for
        // a TUN just closed. EngineError is terminal and settles the service.
        host?.onEngineStopped("none", StopReason.EngineError, "hold expired")
    }

    /**
     * Guarantees a running mux over a TUN that can carry [tunConfig], establishing or replacing only
     * when unavoidable. Returns the live mux.
     */
    private fun ensureTunnel(
        currentHost: VpnHost,
        tunConfig: TunConfig,
    ): PacketMux {
        val existing = mux
        if (existing != null && tunnelUp) {
            if (currentHost.tunCovers(tunConfig)) {
                logger.debug("live TUN already covers the incoming config — no establish()")
                reconcileDnsStage(currentHost, existing)
                return existing
            }
            logger.warn(
                "incoming config is not covered by the live TUN; replacing it. " +
                    "This is the one path that opens a handover window.",
            )
            // requestTun notifies onTunReplaced, which moves the packet path; doing it here swaps twice.
            currentHost.requestTun(tunConfig)
            reconcileDnsStage(currentHost, existing)
            return existing
        }

        // First TUN of the session.
        val tun = currentHost.requestTun(tunConfig)
        tunnelUp = true
        val dnsStage = buildDnsStage(currentHost)
        dnsStageKey = dnsStageKeyFor(currentHost)
        val created = PacketMux(this, dnsStage, currentHost.packetLoggingEnabled())
        try {
            created.start(tun.fileDescriptor, tunConfig.mtu)
        } catch (e: Exception) {
            // A TUN with no mux drains nothing, and the hold this settles into would keep it.
            closeTun()
            dnsStageKey = null
            throw e
        }
        mux = created
        logger.info("tunnel established, dns=${if (dnsStage != null) "ctrld" else "passthrough"}")
        return created
    }

    /** What the live DNS stage was built from, or null when DNS is passing through. */
    private var dnsStageKey: Int? = null

    private fun dnsStageKeyFor(currentHost: VpnHost): Int? =
        if (currentHost.dnsDetails()?.type == DnsType.Proxy) currentHost.controlDPort() else null

    /**
     * Rebuilds the DNS stage when what it was built from changed, without dropping the TUN: mode and
     * port are per-connect while the mux is per-session. Only on a change; a rebuild restarts it.
     */
    private fun reconcileDnsStage(
        currentHost: VpnHost,
        activeMux: PacketMux,
    ) {
        val key = dnsStageKeyFor(currentHost)
        if (key == dnsStageKey) {
            return
        }
        dnsStageKey = key
        activeMux.setDnsStage(buildDnsStage(currentHost))
        logger.info("DNS interception changed to ${if (key == null) "passthrough" else "ctrld on port $key"}")
    }

    /** Only wired when the session uses the local resolver. The port is read live, not from the profile. */
    private fun buildDnsStage(currentHost: VpnHost): PacketMux.DnsStage? {
        val details = currentHost.dnsDetails() ?: return null
        if (details.type != DnsType.Proxy) return null
        return ControlDDnsStage(
            currentHost.controlDPort(),
            currentHost.packetLoggingEnabled(),
            this,
        )
    }

    private fun stopEngineQuietly(reason: StopReason) {
        val engine = activeEngine ?: return
        activeEngine = null
        val startedAt = SystemClock.elapsedRealtime()
        runCatching { engine.stop() }
            .onFailure { logger.warn("engine '${engine.id}' stop failed", it) }
        // On the main thread, via detachHost() and onRevoked(). An engine stuck in native code
        // unwinds to one frame, so an ANR report cannot name it; this can.
        val took = SystemClock.elapsedRealtime() - startedAt
        if (took > VpnEngine.STOP_BUDGET_MS) {
            logger.error("engine '${engine.id}' took ${took}ms to stop, over its ${VpnEngine.STOP_BUDGET_MS}ms budget")
        }
        host?.onEngineStopped(engine.id, reason)
    }

    /** Asks the host to close the live TUN. Only ever called when the session itself is ending. */
    private fun closeTun() {
        if (!tunnelUp) return
        tunnelUp = false
        runCatching { host?.releaseTun() }
    }

    // ---------------------------------------------------------------- callbacks

    override fun onMuxFailure(detail: String) {
        logger.error("mux failure: $detail")
        // The tunnel is up but not passing traffic. Surface it as a broken tunnel so the
        // orchestrator can reconnect; do not tear the TUN down from a pump thread.
        host?.onEngineStopped(activeEngineId ?: "none", StopReason.TunnelBroken, detail)
    }

    /**
     * The host replaced the TUN. Move the pumps onto it, then let the descriptor it superseded go.
     *
     * `@Synchronized` because the host notifies outside its own lock and two replacements can be in
     * flight. Order matters: [PacketMux.setTun] quiesces both pumps before returning, so releasing
     * afterwards cannot race a pump mid-read.
     */
    @Synchronized
    override fun onTunReplaced(
        source: VpnHost,
        tun: java.io.FileDescriptor,
        mtu: Int,
        generation: Long,
    ) {
        if (source !== host) {
            // Its interface is going away; its descriptors are its own to close.
            logger.warn("TUN generation $generation came from a detached host; not applying it.")
            source.releasePreviousTun(generation)
            return
        }
        if (generation <= appliedGeneration) {
            logger.warn("TUN generation $generation arrived after $appliedGeneration; not moving back onto it.")
            source.releasePreviousTun(generation)
            return
        }
        appliedGeneration = generation
        val activeMux = mux
        if (activeMux == null) {
            // First TUN of the session: connect() starts the mux on it directly.
            source.releasePreviousTun(generation)
            tunnelUp = true
            return
        }
        activeMux.setTun(tun, mtu)
        source.releasePreviousTun(generation)
        tunnelUp = true
    }

    override fun onDnsStageFailure(detail: String) {
        // Deliberately not fatal. The mux stays in pass-through, so traffic keeps flowing even
        // though ctrld is unreachable. A DNS failure must never stop traffic.
        logger.error("DNS stage failure (traffic unaffected): $detail")
    }
}
