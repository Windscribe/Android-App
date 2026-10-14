/*
 * Copyright (c) 2026 Windscribe Limited.
 */

package com.windscribe.common.tunnel

import android.content.Context
import android.os.ParcelFileDescriptor
import com.windscribe.common.DNSDetails

/**
 * What happens to the TUN when a session ends. A hold waiting for a reconnect is bounded; one that
 * exists to block traffic is not, or the guarantee lapses on a timer.
 */
enum class TunHold {
    /** Close it. The session is over and traffic flows in the clear. */
    None,

    /** Hold for an engine that is about to attach. Bounded by `TunnelManager.HOLD_TIMEOUT_MS`. */
    ForHandover,

    /** Hold deliberately and indefinitely: nothing may leave outside the VPN. */
    ForBlocking,
}

/** Why an engine stopped. Drives whether the orchestrator retries, fails over, or stands down. */
enum class StopReason {
    /** Orchestrator asked it to stop. Expected; no error surfaced. */
    Requested,

    /** The engine itself failed — handshake, auth, native error. Eligible for failover. */
    EngineError,

    /** The user or system revoked VPN consent. Terminal for the whole session. */
    Revoked,

    /** Engine is up but passing nothing. Eligible for reconnect. */
    TunnelBroken,
}

/**
 * Notified whenever the host swaps the TUN out from under the packet path. An engine can do that
 * without [TunnelManager]: OpenVPN calls [VpnHost.requestTun] from its own management thread.
 */
interface TunReplacementListener {
    /**
     * The live TUN is now [tun]. Move the packet path onto it, then call
     * [VpnHost.releasePreviousTun] with the same [generation].
     *
     * Notifications are unordered and [source] is not always the current host, so ignore a stale
     * [generation] but release it anyway, through [source]. Generations compare across hosts.
     */
    fun onTunReplaced(
        source: VpnHost,
        tun: java.io.FileDescriptor,
        mtu: Int,
        generation: Long,
    )
}

/**
 * What an engine needs from the one real `android.net.VpnService`. Engines are plain objects, so
 * `protect()`, `establish()`, `onRevoke()` and a `Context` all come through here.
 */
interface VpnHost {
    /** The service's own `Context`. Engines that need one (OpenVPN needs ~92 of these) use this. */
    val hostContext: Context

    /**
     * Marks a socket so its traffic bypasses the TUN. Safe from any thread, and from a component
     * that is not the establishing service.
     */
    fun protect(socketFd: Int): Boolean

    /**
     * Establishes the TUN, or **replaces** it if one is already up; never closes without replacing.
     * Re-entrant, since charon replaces twice per reauth and OpenVPN re-opens on a pushed config.
     *
     * Returns the *live* descriptor, which the caller must not close. Ask [tunCovers] first: if the
     * live TUN already covers the config, calling this opens a handover window for nothing.
     */
    @Throws(TunEstablishException::class)
    fun requestTun(config: TunConfig): ParcelFileDescriptor

    /** The config behind the live TUN, or null when no TUN is up. */
    fun currentTunConfig(): TunConfig?

    /** The one thing allowed to move the packet path. Every replacement notifies it, whoever triggered it. */
    fun setTunReplacementListener(listener: TunReplacementListener?)

    /**
     * Closes the descriptor that [generation] superseded; quiesce the pumps first. Keyed by
     * generation because with two replacements in flight, "the previous one" is ambiguous.
     */
    fun releasePreviousTun(generation: Long)

    /**
     * Closes the live TUN and forgets it, only at the end of a session. [currentTunConfig] must
     * return null afterwards, or [tunCovers] answers for an interface that is gone.
     */
    fun releaseTun()

    /** True when the live TUN can carry [config] as-is, so [requestTun] can be skipped entirely. */
    fun tunCovers(config: TunConfig): Boolean = currentTunConfig()?.coversSameInterfaceAs(config) == true

    /**
     * True when the system's "Block connections without VPN" is on, making an app-level DROP
     * redundant. Named apart from `VpnService.isLockdownEnabled()`, which is public *final*.
     */
    fun isSystemLockdownEnabled(): Boolean

    /** True when traffic stays held with no engine attached. Depends on settings above this module. */
    fun blockingApplies(): Boolean = false

    /** Engine terminated. The host forwards this to the orchestrator; engines never self-heal. */
    fun onEngineStopped(
        engineId: String,
        reason: StopReason,
        detail: String? = null,
    )

    /** Custom-DNS configuration for this session, or null. */
    fun dnsDetails(): DNSDetails?

    /**
     * The port ctrld is actually listening on. Read live — the value snapshotted into the saved
     * profile is chosen before ctrld binds and is not authoritative.
     */
    fun controlDPort(): Int

    /** User-settable advanced parameter, not a debug flag. Logging-gated bugs are production bugs. */
    fun packetLoggingEnabled(): Boolean
}

/** `establish()` returned null, or the config could not be applied. */
class TunEstablishException(
    message: String,
    cause: Throwable? = null,
) : Exception(message, cause)
