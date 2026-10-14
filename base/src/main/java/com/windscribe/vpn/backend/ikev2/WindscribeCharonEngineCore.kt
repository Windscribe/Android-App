/*
 * Copyright (c) 2021 Windscribe Limited.
 */

package com.windscribe.vpn.backend.ikev2

import com.windscribe.common.tunnel.PacketMux
import com.windscribe.vpn.backend.ProxyDNSManager
import com.windscribe.vpn.backend.Util
import com.windscribe.vpn.backend.tunnel.WindscribeVpnService
import com.windscribe.vpn.repository.AdvanceParameterRepository
import org.slf4j.LoggerFactory
import org.strongswan.android.data.VpnProfile
import org.strongswan.android.logic.CharonVpnService

/**
 * The concrete [CharonVpnService], as a plain object rather than a service.
 *
 * Charon resolves this object's class hierarchy by name from `libandroidbridge.so`, so the shape is
 * constrained — see [CharonVpnService]. The TUN belongs to
 * [WindscribeVpnService]; this engine runs on a socketpair behind
 * [PacketMux].
 */
class WindscribeCharonEngineCore(
    base: android.content.Context,
    private val advanceParameterRepository: AdvanceParameterRepository,
    private val proxyDNSManager: ProxyDNSManager,
    private val onStatus: (Int) -> Unit,
    private val onTunnelDown: () -> Unit,
) : CharonVpnService(base) {
    companion object {
        private const val STATE_CHILD_SA_UP = 1
        private const val STATE_CHILD_SA_DOWN = 2
        private const val STATE_AUTH_ERROR = 3
        private const val STATE_PEER_AUTH_ERROR = 4
        private const val STATE_LOOKUP_ERROR = 5
        private const val STATE_UNREACHABLE_ERROR = 6
        private const val STATE_CERTIFICATE_UNAVAILABLE = 7
        private const val STATE_GENERIC_ERROR = 8
    }

    private var logger = LoggerFactory.getLogger("vpn")

    /** Starts a connection with the saved profile. Called by the engine, not by an intent. */
    fun connect(): Boolean {
        val profile = Util.getProfile<VpnProfile>()
        if (profile == null) {
            logger.error("Failed to get IKEv2 profile for connection")
            onTunnelDown()
            return false
        }
        logger.info("Setting next profile: ${profile.name}")
        setNextProfile(profile)
        return true
    }

    /** Stops the tunnel without ending the session; the TUN is not ours to close. */
    fun stopTunnel() {
        setNextProfile(null)
    }

    /**
     * Charon's status codes, reported straight through. Which error type, and whether to
     * disconnect, is [IKev2VpnBackend]'s to decide.
     */
    override fun updateStatus(status: Int) {
        super.updateStatus(status)
        onStatus(status)
    }

    override fun shouldEnablePacketLogging(): Boolean = advanceParameterRepository.showCdLog()

    /** Live port. The value snapshotted into a saved profile predates ctrld binding. */
    override fun getControlDPort(): Int = proxyDNSManager.getListenPort()
}
