/*
 * Copyright (c) 2026 Windscribe Limited.
 */

package com.windscribe.vpn.backend.ikev2

import android.os.ParcelFileDescriptor
import com.windscribe.common.tunnel.EngineProfile
import com.windscribe.common.tunnel.VpnEngine
import com.windscribe.common.tunnel.VpnHost
import org.slf4j.LoggerFactory

/** IKEv2's opaque payload; the profile itself is read from disk by the engine core. */
class IKev2EngineProfile : EngineProfile {
    override val protocol: String = PROTOCOL

    companion object {
        const val PROTOCOL = "ikev2"
    }
}

/**
 * IKEv2 as a [VpnEngine].
 *
 * `libandroidbridge.so` resolves classes and methods by name, so the engine core cannot be freely
 * restructured — see the constraints on [org.strongswan.android.logic.CharonVpnService]. Notably
 * charon calls `protect(int)` on the instance, which is hand-written here and delegates to the host.
 *
 * Charon replaces the TUN more than any other engine — reauth, CHILD_SA rekey failure, roam — all
 * through [VpnHost.requestTun], which skips the establish when the live interface already fits.
 *
 * One charon per process: `initializeCharon` installs process-global handlers and a singleton
 * `charonservice`, so the engine core is created once and reused.
 */
class IKev2Engine(
    private val core: WindscribeCharonEngineCore,
) : VpnEngine {
    private val logger = LoggerFactory.getLogger("vpn")

    @Volatile
    private var engineFd: ParcelFileDescriptor? = null

    @Volatile
    private var created = false

    override val id: String = IKev2EngineProfile.PROTOCOL

    override fun start(
        profile: EngineProfile,
        host: VpnHost,
        engineFd: ParcelFileDescriptor,
    ) {
        require(profile is IKev2EngineProfile) { "not an IKEv2 profile: ${profile.protocol}" }

        // Exactly one charon per process: initialise the core once and reuse it.
        if (!created) {
            core.create()
            created = true
        }

        // Before charon can call protect(). C2 has no compile-time signal; this is the only check.
        core.attachHost(host, engineFd)
        this.engineFd = engineFd

        if (!core.connect()) {
            core.detachHost()
            this.engineFd = null
            runCatching { engineFd.close() }
            throw IllegalStateException("IKEv2 failed to start: no profile")
        }
        logger.info("IKEv2 engine started on a mux descriptor")
    }

    override fun stop() {
        runCatching { core.stopTunnel() }
            .onFailure { logger.warn("IKEv2 stopTunnel failed", it) }
        core.detachHost()
        engineFd?.let { fd -> runCatching { fd.close() } }
        engineFd = null
    }

    override fun onRevoked() {
        runCatching { core.revoked() }
        core.detachHost()
        engineFd?.let { fd -> runCatching { fd.close() } }
        engineFd = null
    }
}
