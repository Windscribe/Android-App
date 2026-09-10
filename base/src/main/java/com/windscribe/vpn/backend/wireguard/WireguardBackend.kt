/*
 * Copyright (c) 2021 Windscribe Limited.
 */

package com.windscribe.vpn.backend.wireguard

import com.windscribe.common.tunnel.TunnelManager
import com.windscribe.vpn.R
import com.windscribe.vpn.Windscribe.Companion.appContext
import com.windscribe.vpn.api.IApiCallManager
import com.windscribe.vpn.apppreference.PreferencesHelper
import com.windscribe.vpn.autoconnection.ProtocolInformation
import com.windscribe.vpn.backend.ProxyDNSManager
import com.windscribe.vpn.backend.Util
import com.windscribe.vpn.backend.VPNState
import com.windscribe.vpn.backend.VPNState.Status.Connecting
import com.windscribe.vpn.backend.VPNState.Status.Disconnected
import com.windscribe.vpn.backend.VpnBackend
import com.windscribe.vpn.backend.tunnel.VpnServiceGate
import com.windscribe.vpn.backend.tunnel.WindscribeVpnService
import com.windscribe.vpn.commonutils.ResourceHelper
import com.windscribe.vpn.localdatabase.LocalDbInterface
import com.windscribe.vpn.repository.AdvanceParameterRepository
import com.windscribe.vpn.repository.UserRepository
import com.windscribe.vpn.repository.WgConfigRepository
import com.windscribe.vpn.state.DeviceStateManager
import com.windscribe.vpn.state.NetworkInfoManager
import com.windscribe.vpn.state.VPNConnectionStateManager
import com.windscribe.vpn.wsnet.WSNetWrapper
import com.wireguard.android.backend.GoBackend
import com.wireguard.android.backend.Tunnel
import com.wireguard.android.backend.Tunnel.State.DOWN
import com.wireguard.android.backend.Tunnel.State.TOGGLE
import com.wireguard.android.backend.Tunnel.State.UP
import com.wireguard.android.engine.WireGuardEngineProfile
import com.wireguard.android.engine.WireGuardTunConfigMapper
import dagger.Lazy
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Orchestrates WireGuard on top of the single VPN service.
 *
 * Holds no service and no TUN. It asks [TunnelManager] to run the WireGuard engine; the TUN is
 * established once per session by [WindscribeVpnService] and reused across every handover.
 */
@Singleton
class WireguardBackend
    @Inject
    constructor(
        private val engineBackendLazy: Lazy<GoBackend>,
        var scope: CoroutineScope,
        var networkInfoManager: NetworkInfoManager,
        vpnStateManager: VPNConnectionStateManager,
        val userRepository: Lazy<UserRepository>,
        val deviceStateManager: DeviceStateManager,
        val preferencesHelper: PreferencesHelper,
        advanceParameterRepository: AdvanceParameterRepository,
        val proxyDNSManager: ProxyDNSManager,
        localDbInterface: LocalDbInterface,
        val wgLogger: WgLogger,
        val wgConfigRepository: WgConfigRepository,
        private val apiManager: IApiCallManager,
        wsNetWrapper: WSNetWrapper,
        resourceHelper: ResourceHelper,
        private val tunnelManager: TunnelManager,
        serviceGate: VpnServiceGate,
    ) : VpnBackend(
            scope,
            vpnStateManager,
            preferencesHelper,
            networkInfoManager,
            advanceParameterRepository,
            apiManager,
            localDbInterface,
            wsNetWrapper,
            resourceHelper,
            serviceGate,
            proxyDNSManager,
        ) {
        private var connectionStateJob: Job? = null
        override var active = false
        private var wgErrorJob: Job? = null
        private var captureLogsJob: Job? = null

        /** wg-go's own state, reported by the engine. It owns no `Tunnel`; it only reports. */
        private val engineState =
            MutableSharedFlow<Tunnel.State>(
                replay = 1,
                extraBufferCapacity = 5,
            ).apply { tryEmit(DOWN) }

        private val pinIpRecovery =
            PinIpRecovery(
                scope = scope,
                wgLogger = wgLogger,
                apiManager = apiManager,
                wsNetWrapper,
                preferencesHelper = preferencesHelper,
                deviceStateManager = deviceStateManager,
                getPinnedIpForSelectedCity = { getPinnedIpForSelectedCity() },
            )

        private val tunnelHealthCheck =
            TunnelHealthCheck(
                scope = scope,
                wgLogger = wgLogger,
                deviceStateManager = deviceStateManager,
                protect = { fd -> serviceGate.current?.protect(fd) == true },
                disconnect = { error -> disconnect(error = error) },
            )

        /**
         * Reports engine state up to connection state, swallowing the spurious initial DOWN a fresh
         * tunnel object emits.
         */
        private var stickyDisconnectEvent = false

        @Volatile
        private var lastLoggedEngineState: Tunnel.State? = null

        fun onEngineState(state: Tunnel.State) {
            engineState.tryEmit(state)
        }

        override fun activate() {
            stickyDisconnectEvent = true
            lastLoggedEngineState = null
            vpnLogger.info("Activating WireGuard backend.")
            connectionStateJob =
                scope.launch {
                    engineState.collectLatest {
                        // The repeat still runs the handling below; only the line is dropped. A
                        // teardown reports DOWN from both the engine stop and the deactivate.
                        if (it != lastLoggedEngineState) {
                            lastLoggedEngineState = it
                            vpnLogger.info("WireGuard engine state: ${it.name}")
                        }
                        when (it) {
                            DOWN -> {
                                if (!stickyDisconnectEvent && !reconnecting) {
                                    updateState(VPNState(Disconnected))
                                }
                            }

                            // Both guarded: an engine event that arrives while a disconnect is in
                            // flight must not overwrite it. wg has not been seen to do this, but the
                            // rule is the same for every engine and one of them did.
                            TOGGLE ->
                                if (!disconnecting) {
                                    updateState(VPNState(Connecting))
                                }

                            UP ->
                                if (!disconnecting) {
                                    vpnLogger.debug("Engine up, starting connectivity test.")
                                    testConnectivity()
                                }
                        }
                        stickyDisconnectEvent = false
                    }
                }
            active = true
            captureLogsJob?.cancel()
            captureLogsJob = scope.launch { wgLogger.captureLogs(appContext) }
            pinIpRecovery.start()
            startNetworkInfoObserver()
            vpnLogger.info("WireGuard backend activated.")
        }

        override fun deactivate() {
            wgErrorJob?.cancel()
            connectionStateJob?.cancel()
            pinIpRecovery.stop()
            tunnelHealthCheck.stop()
            wgLogger.stopCapture()
            captureLogsJob?.cancel()
            captureLogsJob = null
            stopNetworkInfoObserver()
            active = false
            vpnLogger.debug("WireGuard backend deactivated.")
        }

        override fun connect(
            protocolInformation: ProtocolInformation,
            connectionId: UUID,
        ) {
            this.protocolInformation = protocolInformation
            this.connectionId = connectionId
            startConnectionJob()
            launchEngineStart {
                runConnect("WireGuard", tunnelManager) {
                    val profile = Util.getProfile<WireGuardVpnProfile>() ?: return@runConnect null
                    val config = WireGuardVpnProfile.createConfigFromString(profile.content)
                    val sessionName = appContext.getString(R.string.app_name)
                    EnginePlan(
                        profile = WireGuardEngineProfile(sessionName, config),
                        tunConfig = WireGuardTunConfigMapper.toTunConfig(sessionName, config),
                        afterStart = {
                            if (!preferencesHelper.isConnectingToConfigured) {
                                tunnelHealthCheck.start(profile.content)
                            }
                        },
                    )
                }
            }
        }

        override suspend fun disconnect(error: VPNState.Error?) {
            this.error = error
            if (proxyDNSManager.invalidConfig) {
                proxyDNSManager.stopControlD()
            }
            connectionJob?.cancel()
            runDisconnect("WireGuard", tunnelManager, error) { engineState.tryEmit(DOWN) }
        }
    }
