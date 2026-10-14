/*
 * Copyright (c) 2021 Windscribe Limited.
 */

package com.windscribe.vpn.backend

import com.windscribe.vpn.apppreference.PreferencesHelper
import com.windscribe.vpn.apppreference.PreferencesKeyConstants.PROTO_IKev2
import com.windscribe.vpn.apppreference.PreferencesKeyConstants.PROTO_STEALTH
import com.windscribe.vpn.apppreference.PreferencesKeyConstants.PROTO_TCP
import com.windscribe.vpn.apppreference.PreferencesKeyConstants.PROTO_UDP
import com.windscribe.vpn.apppreference.PreferencesKeyConstants.PROTO_WIRE_GUARD
import com.windscribe.vpn.apppreference.PreferencesKeyConstants.PROTO_WS_TUNNEL
import com.windscribe.vpn.autoconnection.ProtocolInformation
import com.windscribe.vpn.backend.ikev2.IKev2VpnBackend
import com.windscribe.vpn.backend.openvpn.OpenVPNBackend
import com.windscribe.vpn.backend.wireguard.WireguardBackend
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.slf4j.LoggerFactory
import java.util.UUID
import javax.inject.Singleton

@Singleton
class VpnBackendHolder(
    val scope: CoroutineScope,
    private val preferenceHelper: PreferencesHelper,
    private val iKev2VpnBackend: IKev2VpnBackend,
    private val wireguardBackend: WireguardBackend,
    private val openVPNBackend: OpenVPNBackend,
) {
    var activeBackend: VpnBackend? = null
    private val vpnLogger = LoggerFactory.getLogger("vpn")

    /**
     * Routes a protocol to the backend that serves it.
     *
     * The caller's [ProtocolInformation] decides. Whether a usable profile exists is the backend's
     * problem, reported through connection state rather than by returning null here.
     */
    private fun backendFor(protocol: String): VpnBackend? =
        when (protocol) {
            PROTO_UDP, PROTO_TCP, PROTO_STEALTH, PROTO_WS_TUNNEL -> openVPNBackend
            PROTO_IKev2 -> iKev2VpnBackend
            PROTO_WIRE_GUARD -> wireguardBackend
            else -> {
                vpnLogger.error("No backend serves protocol '$protocol'.")
                null
            }
        }

    fun connect(
        protocolInformation: ProtocolInformation,
        connectionId: UUID,
    ) {
        scope.launch {
            // No delay before activating: one service outlives every engine, so there is no
            // teardown for the next connect to race.
            activeBackend = backendFor(protocolInformation.protocol)
            activeBackend?.let {
                it.activate()
                it.connect(protocolInformation, connectionId)
            }
        }
    }

    suspend fun disconnect(error: VPNState.Error? = null) {
        val backend =
            activeBackend ?: run {
                vpnLogger.debug("VPN backend not found.")
                return
            }
        try {
            backend.disconnect(error)
        } finally {
            // Left set, callers read it as a backend still carrying traffic.
            activeBackend = null
        }
    }
}
