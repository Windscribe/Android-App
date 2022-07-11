/*
 * Copyright (c) 2021 Windscribe Limited.
 */

package com.windscribe.vpn.backend

import com.windscribe.vpn.Windscribe.Companion.appContext
import com.windscribe.vpn.apppreference.AppPreferenceHelper
import com.windscribe.vpn.backend.ikev2.IKev2VpnBackend
import com.windscribe.vpn.backend.openvpn.OpenVPNBackend
import com.windscribe.vpn.backend.wireguard.WireGuardVpnProfile
import com.windscribe.vpn.backend.wireguard.WireguardBackend
import com.windscribe.vpn.constants.PreferencesKeyConstants.PROTO_IKev2
import com.windscribe.vpn.constants.PreferencesKeyConstants.PROTO_STEALTH
import com.windscribe.vpn.constants.PreferencesKeyConstants.PROTO_TCP
import com.windscribe.vpn.constants.PreferencesKeyConstants.PROTO_UDP
import com.windscribe.vpn.constants.PreferencesKeyConstants.PROTO_WIRE_GUARD
import de.blinkt.openvpn.VpnProfile
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.slf4j.LoggerFactory

@Singleton
class VpnBackendHolder(
        val scope: CoroutineScope,
        private val preferenceHelper: AppPreferenceHelper,
        private val iKev2VpnBackend: IKev2VpnBackend,
        private val wireguardBackend: WireguardBackend,
        private val openVPNBackend: OpenVPNBackend
) {

    var activeBackend: VpnBackend? = null
    private val vpnLogger = LoggerFactory.getLogger("vpn_backend")

    /**
    @return VpnBackend Based on selected protocol and existing vpn profile.
     */
    private fun getBackend(): VpnBackend? {
        return when (preferenceHelper.selectedProtocol) {
            PROTO_UDP, PROTO_TCP, PROTO_STEALTH -> {
                if (Util.getProfile<VpnProfile>() != null) {
                    return openVPNBackend
                }
                return null
            }
            PROTO_IKev2 -> {
                if (Util.getProfile<org.strongswan.android.data.VpnProfile>() != null) {
                    return iKev2VpnBackend
                }
                return null
            }
            PROTO_WIRE_GUARD -> {
                if (Util.getProfile<WireGuardVpnProfile>() != null) {
                    return wireguardBackend
                }
                return null
            }
            else -> {
                null
            }
        }
    }

    fun connect() {
        scope.launch {
            val active: Boolean = activeBackend?.active == true
            if (active) {
                vpnLogger.debug("Active VPN Backend found.")
                delay(100)
            }
            activeBackend = getBackend()
            activeBackend?.let {
                it.activate()
                it.connect()
            } ?: kotlin.run {
                // Unexpected state.
                delay(500)
                vpnLogger.debug("Could not find vpn backend matching the current vpn profile Now disconnecting.")
                appContext.vpnController.disconnect()
            }
        }
    }

    suspend fun disconnect() {
        activeBackend?.disconnect() ?: kotlin.run {
            vpnLogger.debug("VPN backend not found.")
        }
    }
}
