/*
 * Copyright (c) 2021 Windscribe Limited.
 */

package com.windscribe.vpn.backend.wireguard

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
import com.windscribe.vpn.commonutils.ResourceHelper
import com.windscribe.vpn.localdatabase.LocalDbInterface
import com.windscribe.vpn.repository.AdvanceParameterRepository
import com.windscribe.vpn.repository.UserRepository
import com.windscribe.vpn.state.DeviceStateManager
import com.windscribe.vpn.state.NetworkInfoManager
import com.windscribe.vpn.state.VPNConnectionStateManager
import com.windscribe.vpn.wsnet.WSNetWrapper
import com.wireguard.android.backend.GoBackend
import com.wireguard.android.backend.Tunnel.State.DOWN
import com.wireguard.android.backend.Tunnel.State.TOGGLE
import com.wireguard.android.backend.Tunnel.State.UP
import dagger.Lazy
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.cancellable
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WireguardBackend
    @Inject
    constructor(
        private val backendLazy: Lazy<GoBackend>,
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
        val wgConfigRepository: com.windscribe.vpn.repository.WgConfigRepository,
        private val apiManager: IApiCallManager,
        wsNetWrapper: WSNetWrapper,
        resourceHelper: ResourceHelper,
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
        ) {
        var service: WireGuardWrapperService? = null
        private var connectionStateJob: Job? = null
        override var active = false
        private var wgErrorJob: Job? = null
        private var captureLogsJob: Job? = null
        private val backend: GoBackend by lazy { backendLazy.get() }

        private val testTunnel =
            WireGuardTunnel(
                name = appContext.getString(R.string.app_name),
                config = null,
                state = DOWN,
            )

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
                workManager = appContext.workManager,
                getService = { service },
                disconnect = { error -> disconnect(error = error) },
            )

        fun serviceCreated(vpnService: WireGuardWrapperService) {
            vpnLogger.info("WireGuard service created.")
            service = vpnService
        }

        fun serviceDestroyed() {
            vpnLogger.info("WireGuard service destroyed.")
            service = null
        }

        private var stickyDisconnectEvent = false

        override fun activate() {
            stickyDisconnectEvent = true
            vpnLogger.info("Activating wireGuard backend.")
            connectionStateJob =
                scope.launch {
                    testTunnel.stateFlow.cancellable().collectLatest {
                        vpnLogger.info("WireGuard tunnel state changed to ${it.name}")
                        when (it) {
                            DOWN -> {
                                if (!stickyDisconnectEvent && !reconnecting) {
                                    updateState(VPNState(Disconnected))
                                }
                            }

                            TOGGLE -> {
                                updateState(VPNState(Connecting))
                            }

                            UP -> {
                                testConnectivity()
                            }
                        }
                        stickyDisconnectEvent = false
                    }
                }
            vpnLogger.info("WireGuard backend activated.")
            active = true
            captureLogsJob?.cancel()
            captureLogsJob =
                scope.launch {
                    wgLogger.captureLogs(appContext)
                }
            pinIpRecovery.start()
            startNetworkInfoObserver()
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
            scope.launch {
                proxyDNSManager.startControlDIfRequired()
                vpnLogger.info("Getting WireGuard profile.")
                Util.getProfile<WireGuardVpnProfile>()?.let { profile ->
                    withContext(Dispatchers.IO) {
                        val content = WireGuardVpnProfile.createConfigFromString(profile.content)
                        vpnLogger.info(profile.content)
                        try {
                            backend.setState(testTunnel, UP, content)
                            // Start tunnel health monitoring after connection is established
                            if (!preferencesHelper.isConnectingToConfigured){
                                tunnelHealthCheck.start(profile.content)
                            }
                        } catch (e: Exception) {
                            vpnLogger.error("Exception while setting WireGuard state UP.", e)
                            updateState(VPNState(Disconnected))
                        }
                    }
                } ?: kotlin.run {
                    vpnLogger.info("Failed to get WireGuard profile.")
                    updateState(VPNState(Disconnected))
                }
            }
        }

        override suspend fun disconnect(error: VPNState.Error?) {
            this.error = error
            // Save disconnection error to persistent preferences for tunnel recovery
            preferencesHelper.lastDisconnectionError = error?.error?.name
            if (proxyDNSManager.invalidConfig) {
                proxyDNSManager.stopControlD()
            }
            connectionJob?.cancel()
            vpnLogger.info("Stopping WireGuard service.")
            service?.close()
            delay(20)
            vpnLogger.info("Setting WireGuard tunnel state down.")
            backend.setState(testTunnel, DOWN, null)
            delay(DISCONNECT_DELAY)
            vpnLogger.info("Deactivating WireGuard backend.")
            deactivate()
        }
    }
