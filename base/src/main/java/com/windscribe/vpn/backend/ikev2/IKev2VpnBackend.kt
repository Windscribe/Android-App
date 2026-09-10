/*
 * Copyright (c) 2021 Windscribe Limited.
 */

package com.windscribe.vpn.backend.ikev2

import com.windscribe.common.tunnel.TunnelManager
import com.windscribe.vpn.Windscribe.Companion.appContext
import com.windscribe.vpn.api.IApiCallManager
import com.windscribe.vpn.apppreference.PreferencesHelper
import com.windscribe.vpn.autoconnection.ProtocolInformation
import com.windscribe.vpn.backend.ProxyDNSManager
import com.windscribe.vpn.backend.VPNState
import com.windscribe.vpn.backend.VPNState.Status.Connecting
import com.windscribe.vpn.backend.VPNState.Status.Disconnected
import com.windscribe.vpn.backend.VpnBackend
import com.windscribe.vpn.backend.ikev2.StrongswanCertificateManager.init
import com.windscribe.vpn.backend.tunnel.VpnServiceGate
import com.windscribe.vpn.backend.tunnel.WindscribeVpnService
import com.windscribe.vpn.commonutils.ResourceHelper
import com.windscribe.vpn.localdatabase.LocalDbInterface
import com.windscribe.vpn.repository.AdvanceParameterRepository
import com.windscribe.vpn.state.NetworkInfoManager
import com.windscribe.vpn.state.VPNConnectionStateManager
import com.windscribe.vpn.wsnet.WSNetWrapper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.cancellable
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.strongswan.android.logic.CharonVpnService
import org.strongswan.android.logic.StrongSwanApplication
import java.io.File
import java.io.IOException
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class IKev2VpnBackend
    @Inject
    constructor(
        var scope: CoroutineScope,
        var networkInfoManager: NetworkInfoManager,
        vpnStateManager: VPNConnectionStateManager,
        var preferencesHelper: PreferencesHelper,
        advanceParameterRepository: AdvanceParameterRepository,
        val proxyDNSManager: ProxyDNSManager,
        apiManager: IApiCallManager,
        localDbInterface: LocalDbInterface,
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

        // StateFlow-based tunnel like WireGuard
        private val ikev2Tunnel = IKev2Tunnel()

        // Sticky flag to prevent initial DOWN state from triggering disconnect (like WireGuard)
        private var stickyDisconnectEvent = false

        @Volatile
        private var lastLoggedState: IKev2Tunnel.State? = null

        private var strongswanInitialized = false

        private companion object {
            /** Charon's status codes, resolved by value from native. */
            const val STATE_CHILD_SA_UP = 1
            const val STATE_CHILD_SA_DOWN = 2
            const val STATE_AUTH_ERROR = 3
            const val STATE_PEER_AUTH_ERROR = 4
            const val STATE_LOOKUP_ERROR = 5
            const val STATE_UNREACHABLE_ERROR = 6
            const val STATE_CERTIFICATE_UNAVAILABLE = 7
            const val STATE_GENERIC_ERROR = 8
        }

        /**
         * Initialises the strongSwan Java layer once per process.
         *
         * `setService` gets the single VPN service, which is what the system starts for Always-on.
         * The engine itself is a plain object driven by [TunnelManager].
         */
        private fun initStrongswan() {
            if (strongswanInitialized) {
                return
            }
            vpnLogger.info("Initializing StrongSwan for the first time.")
            StrongSwanApplication.setContext(appContext)
            StrongSwanApplication.setService(WindscribeVpnService::class.java)
            init(appContext)
            strongswanInitialized = true
        }

        /** Charon's raw status codes, reported by the engine core. */
        fun onCharonStatus(status: Int) {
            when (status) {
                STATE_CHILD_SA_DOWN -> ikev2Tunnel.onStateChange(IKev2Tunnel.State.CONNECTING)
                STATE_CHILD_SA_UP -> ikev2Tunnel.onStateChange(IKev2Tunnel.State.CONNECTED)
                STATE_AUTH_ERROR ->
                    failWith(VPNState.ErrorType.AuthenticationError, "Authentication failed.")
                STATE_PEER_AUTH_ERROR ->
                    failWith(VPNState.ErrorType.AuthenticationError, "Peer authentication failed.")
                STATE_LOOKUP_ERROR ->
                    failWith(VPNState.ErrorType.GenericError, "Failed to resolve server hostname.")
                STATE_UNREACHABLE_ERROR ->
                    failWith(VPNState.ErrorType.TimeoutError, "Server unreachable.")
                STATE_CERTIFICATE_UNAVAILABLE ->
                    failWith(VPNState.ErrorType.AuthenticationError, "Certificate unavailable.")
                STATE_GENERIC_ERROR ->
                    failWith(VPNState.ErrorType.GenericError, "Connection error.")
                else -> vpnLogger.warn("Unknown IKEv2 status: $status")
            }
        }

        private fun failWith(
            type: VPNState.ErrorType,
            message: String,
        ) {
            vpnLogger.error("IKEv2: $message")
            scope.launch { disconnect(VPNState.Error(error = type, message = message)) }
        }

        fun onTunnelDown() {
            ikev2Tunnel.onStateChange(IKev2Tunnel.State.DOWN)
        }

        /**
         * Returns the tunnel object for state reporting from CharonVpnService
         */
        fun getTunnel() = ikev2Tunnel

        override fun activate() {
            stickyDisconnectEvent = true // Prevent initial DOWN from triggering disconnect
            lastLoggedState = null
            vpnLogger.info("Activating IKEv2 backend.")
            // Start observing tunnel state changes (like WireGuard does)
            connectionStateJob =
                scope.launch {
                    ikev2Tunnel.stateFlow.cancellable().collectLatest { state ->
                        // The repeat still runs the handling below; only the line is dropped.
                        if (state != lastLoggedState) {
                            lastLoggedState = state
                            vpnLogger.info("IKEv2 tunnel state changed to ${state.name}")
                        }
                        when (state) {
                            IKev2Tunnel.State.DOWN -> {
                                // Only disconnect if not the initial activation event and not reconnecting
                                if (!stickyDisconnectEvent && !reconnecting) {
                                    connectionJob?.cancel()
                                    updateState(VPNState(Disconnected))
                                }
                            }

                            IKev2Tunnel.State.CONNECTING -> {
                                // Charon reports CHILD_SA_DOWN as CONNECTING while it tears down, so
                                // without this a disconnect in flight gets overwritten by the very
                                // teardown it asked for.
                                if (!disconnecting) {
                                    updateState(VPNState(Connecting))
                                }
                            }

                            IKev2Tunnel.State.CONNECTED -> {
                                if (!disconnecting) {
                                    testConnectivity()
                                }
                            }

                            IKev2Tunnel.State.DISCONNECTING -> {
                                updateState(VPNState(VPNState.Status.Disconnecting))
                            }
                        }
                        stickyDisconnectEvent = false // Reset after first event
                    }
                }
            active = true
            startNetworkInfoObserver()
            vpnLogger.debug("IKEv2 backend activated.")
        }

        override fun deactivate() {
            connectionStateJob?.cancel()
            stopNetworkInfoObserver()
            active = false
            vpnLogger.debug("IKEv2 backend deactivated.")
        }

        override fun connect(
            protocolInformation: ProtocolInformation,
            connectionId: UUID,
        ) {
            this.protocolInformation = protocolInformation
            this.connectionId = connectionId
            vpnLogger.info("Connecting to IKEv2 Service.")
            startConnectionJob()
            launchEngineStart {
                initStrongswan()
                runConnect("IKEv2", tunnelManager) {
                    // Like OpenVPN, IKEv2's interface is negotiated: charon fills the BuilderCache
                    // and calls requestTun() from establish().
                    EnginePlan(
                        IKev2EngineProfile(),
                        // Self excluded: charon fetches CRLs over plain HTTP from a socket nothing
                        // protects, and a fresh install has no cached CRL. See §12.7.3.
                        provisionalTunConfig(blackholeAddress = "172.16.252.1", excludeSelf = true),
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
            runDisconnect("IKEv2", tunnelManager, error) {
                ikev2Tunnel.onStateChange(IKev2Tunnel.State.DOWN)
            }
        }

        private fun checkLogFileSize() {
            val logFile =
                File(appContext.filesDir.absolutePath + File.separator + CharonVpnService.LOG_FILE)
            if (logFile.exists()) {
                try {
                    val sizeInMb = logFile.length() / (1024 * 1024)
                    if (sizeInMb > 1) {
                        logFile.delete()
                        logFile.createNewFile()
                    }
                } catch (e: IOException) {
                    vpnLogger.debug("Unable to create log file: $e")
                }
            }
        }
    }
