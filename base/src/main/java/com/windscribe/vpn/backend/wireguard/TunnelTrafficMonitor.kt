/*
 * Copyright (c) 2026 Windscribe Limited.
 */

package com.windscribe.vpn.backend.wireguard

import android.os.SystemClock
import com.windscribe.common.tunnel.PacketMux
import com.windscribe.vpn.state.DeviceStateManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.slf4j.LoggerFactory
import java.util.concurrent.atomic.AtomicBoolean

/** wg-go's own view of the peer, read over the UAPI. */
data class PeerStats(
    val rxBytes: Long,
    val txBytes: Long,
    val handshakeEpochMillis: Long,
) {
    /** tx is ours — it keeps climbing on handshake retransmits alone, with the peer long gone. */
    fun answeredSince(p: PeerStats) = rxBytes != p.rxBytes || handshakeEpochMillis != p.handshakeEpochMillis
}

/**
 * Ends a WireGuard session whose tunnel has stopped carrying traffic, as `BrokenTunnel`.
 *
 * Watches [PacketMux]'s plaintext counters for a run with no ingress, and requires wg-go's own peer
 * counters to have stopped moving alongside it. Ingress alone cannot separate a dead tunnel from an
 * idle one: keepalives are wg's own packets and never reach the TUN, so a healthy tunnel nobody is
 * using reads the same zero a broken one does. [PeerStats] sees them.
 */
class TunnelTrafficMonitor(
    private val scope: CoroutineScope,
    private val deviceStateManager: DeviceStateManager,
    private val traffic: () -> PacketMux.Traffic?,
    private val peerStats: () -> PeerStats?,
    private val onBrokenTunnel: suspend () -> Unit,
) {
    private val logger = LoggerFactory.getLogger("tunnel-traffic")

    private var sampleJob: Job? = null
    private var previous: PacketMux.Traffic? = null
    private var previousPeer: PeerStats? = null
    private var everCarried = false

    private var silenceSince: Long? = null
    private var egressDuringSilence = 0L

    /** Monotonic, so a wall-clock jump in the handshake time cannot read as the peer going quiet. */
    private var peerMovedAt = 0L

    private var candidateSince: Long? = null

    /** The disconnect stops this monitor, but not before the next sample, which still sees silence. */
    private val acted = AtomicBoolean(false)
    private val sampleFailureLogged = AtomicBoolean(false)

    private enum class Window {
        /** No comparable previous reading. */
        Unknown,
        NoTunnel,

        /** The mux is draining rather than forwarding — a handover or a blocking hold. */
        Held,
        Offline,

        /** Nothing has come back yet this session. */
        Warmup,
        Healthy,

        /** Nothing came back. */
        Silent,
    }

    companion object {
        private const val SAMPLE_INTERVAL_MS = 5_000L

        /** Far beyond what detection needs: being wrong tears down a session someone is using. */
        private const val CANDIDATE_SILENCE_MS = 120_000L

        /**
         * How long the peer's counters may sit still. wg's `REJECT_AFTER_TIME` is 180 s, and the
         * profile's 25 s keepalive has to send inside that, so a peer that is answering cannot go
         * this long without a rekey moving one of them.
         */
        private const val PEER_SILENT_MS = 190_000L
    }

    fun start() {
        stop()
        sampleJob =
            scope.launch {
                previous = traffic()
                previousPeer = peerStats()
                peerMovedAt = SystemClock.elapsedRealtime()
                while (isActive) {
                    delay(SAMPLE_INTERVAL_MS)
                    runCatching { sample() }
                        .onFailure {
                            if (sampleFailureLogged.compareAndSet(false, true)) {
                                logger.warn("Traffic sample failed; not watching this session.", it)
                            }
                        }
                }
            }
    }

    fun stop() {
        sampleJob?.cancel()
        sampleJob = null
        previous = null
        previousPeer = null
        peerMovedAt = 0L
        everCarried = false
        silenceSince = null
        egressDuringSilence = 0L
        candidateSince = null
        acted.set(false)
        sampleFailureLogged.set(false)
    }

    private fun sample() {
        val now = SystemClock.elapsedRealtime()
        val current = traffic()
        val before = previous
        previous = current

        val peer = peerStats()
        val beforePeer = previousPeer
        previousPeer = peer
        if (peer == null || beforePeer == null || peer.answeredSince(beforePeer)) {
            peerMovedAt = now
        }

        val window =
            when {
                current == null -> Window.NoTunnel
                current.state != PacketMux.State.FORWARD -> Window.Held
                before == null || before.state != PacketMux.State.FORWARD -> Window.Unknown
                !deviceStateManager.isOnline.value -> Window.Offline
                else -> null
            }
        if (window != null) {
            settle(window, now, 0)
            return
        }

        val egress = current!!.egressPackets - before!!.egressPackets
        val ingress = current.ingressPackets - before.ingressPackets
        if (egress < 0 || ingress < 0) {
            // A new mux, so a new set of counters.
            settle(Window.Unknown, now, 0)
            return
        }
        if (ingress > 0) {
            everCarried = true
        }
        settle(
            when {
                ingress > 0 -> Window.Healthy
                !everCarried -> Window.Warmup
                else -> Window.Silent
            },
            now,
            egress,
        )
    }

    private fun settle(
        window: Window,
        now: Long,
        egress: Long,
    ) {
        if (window == Window.Silent) {
            if (silenceSince == null) {
                silenceSince = now
                egressDuringSilence = 0
            }
            egressDuringSilence += egress
        } else {
            silenceSince = null
            egressDuringSilence = 0
        }
        // Corroboration has to be earned inside the window it judges.
        if (window == Window.Offline || window == Window.Held || window == Window.NoTunnel) {
            peerMovedAt = now
        }

        val silentMs = silenceSince?.let { now - it } ?: 0L
        val peerSilentMs = now - peerMovedAt
        val holds =
            silenceSince != null &&
                silentMs >= CANDIDATE_SILENCE_MS &&
                previousPeer != null &&
                peerSilentMs >= PEER_SILENT_MS

        val was = candidateSince
        if (holds && was == null) {
            candidateSince = now
            logger.error(
                "BROKEN TUNNEL: no ingress for ${silentMs / 1000}s over $egressDuringSilence egress packets, " +
                    "nothing from the peer for ${peerSilentMs / 1000}s at ${previousPeer?.rxBytes}B received, " +
                    "device online. Ending the session so the refresh can say why.",
            )
            if (acted.compareAndSet(false, true)) {
                scope.launch {
                    runCatching { onBrokenTunnel() }
                        .onFailure { logger.error("Could not end the session.", it) }
                }
            }
        } else if (!holds && was != null) {
            candidateSince = null
            logger.error("BROKEN TUNNEL CLEARED after ${(now - was) / 1000}s as $window; it recovered on its own.")
        }
    }
}
