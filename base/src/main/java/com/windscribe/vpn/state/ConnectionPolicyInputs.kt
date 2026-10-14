/*
 * Copyright (c) 2026 Windscribe Limited.
 */

package com.windscribe.vpn.state

import android.content.Context
import com.windscribe.vpn.apppreference.PreferencesHelper
import com.windscribe.vpn.backend.tunnel.VpnServiceGate
import com.windscribe.vpn.backend.tunnel.canAccessNetworkName
import com.windscribe.vpn.localdatabase.LocalDbInterface
import com.windscribe.vpn.localdatabase.tables.NetworkInfo
import com.windscribe.vpn.repository.UserRepository
import com.windscribe.vpn.state.ConnectionPolicy.PolicyInput
import dagger.Lazy
import org.slf4j.LoggerFactory

/**
 * Gathers the live values [ConnectionPolicy] decides on, so the policy itself stays pure.
 */
class ConnectionPolicyInputs(
    private val context: Context,
    private val preferencesHelper: PreferencesHelper,
    private val deviceStateManager: DeviceStateManager,
    private val vpnConnectionStateManager: Lazy<VPNConnectionStateManager>,
    private val userRepository: Lazy<UserRepository>,
    private val localDbInterface: LocalDbInterface,
    private val serviceGate: VpnServiceGate,
) {
    private val logger = LoggerFactory.getLogger("connection-policy")

    /**
     * Preferred where the caller is already reacting to a network emission: no second DB read, and it
     * decides on the same snapshot the caller saw.
     *
     * @param activeProtocol what is connected right now, for the preferred-protocol comparison. Only
     * the backend knows it, so it is passed in rather than read here.
     */
    fun from(
        networkInfo: NetworkInfo?,
        activeProtocol: Pair<String, String>? = null,
    ): PolicyInput =
        PolicyInput(
            isAutoSecureOn = networkInfo?.isAutoSecureOn,
            isNetworkStoodDown = deviceStateManager.isCurrentNetworkWhitelisted.value,
            isNetworkOptedIn = deviceStateManager.isCurrentNetworkOptedIn.value,
            autoConnect = preferencesHelper.autoConnect,
            autoStartOnBoot = preferencesHelper.autoStartOnBoot,
            globalUserConnectionPreference = preferencesHelper.globalUserConnectionPreference,
            accountStatus =
                userRepository
                    .get()
                    .user.value
                    ?.accountStatus,
            status =
                vpnConnectionStateManager
                    .get()
                    .state.value.status,
            serviceMode = vpnConnectionStateManager.get().serviceMode.value,
            canReadNetworkName = context.canAccessNetworkName(),
            isOnline = deviceStateManager.isOnline.value,
            isPreferredProtocolOn = networkInfo?.isPreferredOn == true,
            preferredProtocol =
                networkInfo?.protocol?.let { protocol ->
                    networkInfo.port?.let { port -> protocol to port }
                },
            activeProtocol = activeProtocol,
            isSystemLockdownEnabled = serviceGate.current?.isSystemLockdownEnabled() == true,
            isConnectingToConfigured = preferencesHelper.isConnectingToConfigured,
        )

    /** For callers with no emission in hand. An unidentifiable network yields a null `isAutoSecureOn`. */
    suspend fun current(): PolicyInput {
        val networkInfo =
            try {
                deviceStateManager.getCurrentNetworkName()?.let { localDbInterface.getNetwork(it) }
            } catch (e: Exception) {
                logger.debug("Could not read the current network: ${e.message}")
                null
            }
        return from(networkInfo)
    }
}
