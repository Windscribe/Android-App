/*
 * Copyright (c) 2026 Windscribe Limited.
 */

package com.windscribe.common.tunnel

import android.os.ParcelFileDescriptor

/**
 * Protocol-specific connection payload. `:common` cannot see `com.wireguard.config.Config`,
 * `de.blinkt.openvpn.VpnProfile` or `org.strongswan.android.data.VpnProfile`, so this is opaque
 * here and downcast inside the engine that owns it.
 */
interface EngineProfile {
    val protocol: String
}

/**
 * A protocol implementation, as a plain object rather than a `VpnService`.
 *
 * An engine never owns a TUN. It is handed one end of a socketpair by [PacketMux] and does plain
 * `read()`/`write()` on it.
 */
interface VpnEngine {
    /** Stable identity, used in logs and in [VpnHost.onEngineStopped]. */
    val id: String

    /**
     * Resolves ahead of [start] whatever [start] would otherwise resolve under the tunnel lock.
     *
     * Blocking is allowed here. [start] must still work without it.
     */
    @Throws(Exception::class)
    fun prepare(profile: EngineProfile) {}

    /**
     * Brings the protocol up on [engineFd].
     *
     * The engine owns [engineFd] and closes it on [stop]; the mux hands out a `dup` and never
     * touches that copy again.
     *
     * Must not block for the life of the connection: start threads and return.
     */
    @Throws(Exception::class)
    fun start(
        profile: EngineProfile,
        host: VpnHost,
        engineFd: ParcelFileDescriptor,
    )

    /**
     * Tears the protocol down. Idempotent, and must not touch the TUN, which outlives every engine.
     *
     * Off the main thread, but a reconnect waits behind it: an engine that cannot shut down within
     * [STOP_BUDGET_MS] must abandon the attempt rather than wait it out.
     */
    fun stop()

    /**
     * VPN consent was revoked. Terminal; the whole session is going away.
     *
     * Under the same budget as [stop].
     */
    fun onRevoked()

    companion object {
        /** How long [stop] and [onRevoked] may take before they are holding up the next connect. */
        const val STOP_BUDGET_MS = 1_000L
    }
}
