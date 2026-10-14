/*
 * Copyright (c) 2026 Windscribe Limited.
 */

package com.windscribe.vpn.backend.openvpn

import android.content.Context
import android.content.Intent
import android.os.ParcelFileDescriptor
import com.windscribe.common.tunnel.EngineProfile
import com.windscribe.common.tunnel.TunConfig
import com.windscribe.common.tunnel.VpnEngine
import com.windscribe.common.tunnel.VpnHost
import com.windscribe.vpn.backend.ProxyDNSManager
import com.windscribe.vpn.backend.Util
import com.windscribe.vpn.repository.AdvanceParameterRepository
import de.blinkt.openvpn.VpnProfile
import de.blinkt.openvpn.core.ConnectionStatus
import de.blinkt.openvpn.core.OpenVPNService
import de.blinkt.openvpn.core.VpnStatus
import org.slf4j.LoggerFactory

/** OpenVPN's opaque payload. The profile itself is read from disk by [OpenVPNService.getProfile]. */
class OpenVpnEngineProfile : EngineProfile {
    override val protocol: String = PROTOCOL

    companion object {
        const val PROTOCOL = "openvpn"
    }
}

/**
 * The concrete [OpenVPNService], now a plain object rather than a service.
 *
 * It supplies the four things the engine core cannot know on its own: the profile, whether packet
 * logging is on, the live ctrld port, and where to report state.
 */
class WindscribeOpenVpnService(
    base: Context,
    private val advanceParameterRepository: AdvanceParameterRepository,
    private val proxyDNSManager: ProxyDNSManager,
    private val onStatus: (ConnectionStatus) -> Unit,
) : OpenVPNService(base) {
    override fun getProfile(): VpnProfile? = Util.getProfile<VpnProfile>()

    /**
     * Always false. Process restore is the single service's job now — it is what the system
     * restarts, and it decides whether to reconnect. This engine is never resurrected on its own.
     */
    override fun onProcessRestore(): Boolean = false

    override fun shouldEnablePacketLogging(): Boolean = advanceParameterRepository.showCdLog()

    /** Live port. The value snapshotted into the saved profile predates ctrld binding. */
    override fun getControlDPort(): Int = proxyDNSManager.getListenPort()

    override fun updateState(
        state: String?,
        logmessage: String?,
        localizedResId: Int,
        level: ConnectionStatus?,
        intent: Intent?,
    ) {
        level?.let(onStatus)
    }

    override fun setConnectedVPN(uuid: String?) = Unit
}

/**
 * OpenVPN as a [VpnEngine].
 *
 * OpenVPN learns its addresses and routes from the server push, after the handshake, so this engine
 * cannot hand a [TunConfig] to the host up front.
 * [OpenVPNService.openTun] builds one mid-flight instead, which is why [VpnHost.requestTun] is
 * re-entrant.
 *
 * Native asks `PERSIST_TUN_ACTION` on every reconnect, and a pushed route or DNS change answers
 * `OPEN_BEFORE_CLOSE`, re-entering `openTun()`. The host handles that as a replace, and
 * `getTunReopenStatus()` consults the host's live TUN rather than this engine's snapshot.
 */
class OpenVpnEngine(
    private val serviceFactory: ((ConnectionStatus) -> Unit) -> WindscribeOpenVpnService,
    private val onStatus: (ConnectionStatus) -> Unit,
) : VpnEngine {
    private val logger = LoggerFactory.getLogger("vpn")

    @Volatile
    private var core: WindscribeOpenVpnService? = null

    @Volatile
    private var engineFd: ParcelFileDescriptor? = null

    override val id: String = OpenVpnEngineProfile.PROTOCOL

    override fun start(
        profile: EngineProfile,
        host: VpnHost,
        engineFd: ParcelFileDescriptor,
    ) {
        require(profile is OpenVpnEngineProfile) { "not an OpenVPN profile: ${profile.protocol}" }
        val instance = serviceFactory(onStatus)
        // The engine keeps this descriptor for the life of the connection and hands native a fresh
        // dup on each openTun(), because sendTunFD() closes its copy after the SCM_RIGHTS send.
        instance.attachHost(host, engineFd)
        core = instance
        this.engineFd = engineFd
        if (!instance.startConnection()) {
            instance.detachHost()
            core = null
            this.engineFd = null
            runCatching { engineFd.close() }
            throw IllegalStateException("OpenVPN failed to start: no profile")
        }
        logger.info("OpenVPN engine started on a mux descriptor")
    }

    override fun stop() {
        val instance = core ?: return
        core = null
        runCatching { instance.stopVPN(false) }
            .onFailure { logger.warn("OpenVPN stopVPN failed", it) }
        runCatching { instance.destroy() }
        instance.detachHost()
        VpnStatus.removeStateListener(instance)
        // Native received dups; this is the engine's own copy.
        engineFd?.let { fd -> runCatching { fd.close() } }
        engineFd = null
    }

    override fun onRevoked() {
        val instance = core
        core = null
        runCatching { instance?.revoked() }
        instance?.detachHost()
        engineFd?.let { fd -> runCatching { fd.close() } }
        engineFd = null
    }
}
