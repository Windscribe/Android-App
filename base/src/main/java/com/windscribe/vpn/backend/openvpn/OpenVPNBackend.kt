/*
 * Copyright (c) 2021 Windscribe Limited.
 */

package com.windscribe.vpn.backend.openvpn

import android.content.Intent
import com.windscribe.common.tunnel.TunnelManager
import com.windscribe.vpn.Windscribe.Companion.appContext
import com.windscribe.vpn.api.IApiCallManager
import com.windscribe.vpn.apppreference.PreferencesHelper
import com.windscribe.vpn.apppreference.PreferencesKeyConstants
import com.windscribe.vpn.autoconnection.ProtocolInformation
import com.windscribe.vpn.backend.ProxyDNSManager
import com.windscribe.vpn.backend.VPNState
import com.windscribe.vpn.backend.VPNState.Status.Connecting
import com.windscribe.vpn.backend.VPNState.Status.Disconnected
import com.windscribe.vpn.backend.VpnBackend
import com.windscribe.vpn.backend.tunnel.VpnServiceGate
import com.windscribe.vpn.commonutils.ResourceHelper
import com.windscribe.vpn.localdatabase.LocalDbInterface
import com.windscribe.vpn.repository.AdvanceParameterRepository
import com.windscribe.vpn.state.NetworkInfoManager
import com.windscribe.vpn.state.VPNConnectionStateManager
import com.windscribe.vpn.wsnet.WSNetWrapper
import com.wireguard.android.backend.GoBackend
import de.blinkt.openvpn.core.ConnectionStatus
import de.blinkt.openvpn.core.LogItem
import de.blinkt.openvpn.core.VpnStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.cancellable
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.slf4j.LoggerFactory
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class OpenVPNBackend
    @Inject
    constructor(
        var backend: GoBackend,
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
        ),
        VpnStatus.StateListener,
        VpnStatus.ByteCountListener {
        override var active = false
        private var stickyDisconnectEvent = false
        private val openVPNLogger = LoggerFactory.getLogger("openvpn")
        private var connectionStateJob: Job? = null
        private var noReplyJob: Job? = null

        /**
         * Whether the server has said anything at all on this attempt. Sticky rather than
         * cancelling [noReplyJob]: one backend serves udp, tcp, stunnel and wstunnel, and the
         * outgoing native process keeps reporting after the incoming one starts.
         */
        private var serverReplied = false

        @Volatile
        private var lastLoggedStatus: ConnectionStatus? = null

        private val openVpnTunnel = OpenVpnTunnel()
        private val logListener: VpnStatus.LogListener =
            VpnStatus.LogListener { item ->
                when (item.logLevel) {
                    // An engine already asked to exit reports on a config it no longer holds.
                    // Kept at debug rather than dropped, so the line is still in the file.
                    VpnStatus.LogLevel.ERROR ->
                        if (disconnecting) openVPNLogger.debug(item.text()) else openVPNLogger.error(item.text())
                    VpnStatus.LogLevel.WARNING ->
                        item.text().let { if (isConfigChatter(it)) openVPNLogger.debug(it) else openVPNLogger.warn(it) }
                    VpnStatus.LogLevel.INFO ->
                        item.text().let { if (!isCertificateSuccessChatter(it)) openVPNLogger.debug(it) }
                    else -> {}
                }
            }

        private fun isConfigChatter(text: String): Boolean =
            text.startsWith("DEPRECATED: --persist-key option ignored") ||
                text.startsWith("WARNING: this configuration may cache passwords in memory")

        /** The success path: six lines per connect. A failure prints VERIFY ERROR, matched by none. */
        private fun isCertificateSuccessChatter(text: String): Boolean =
            text.startsWith("VERIFY OK:") ||
                text.startsWith("VERIFY KU OK") ||
                text.startsWith("VERIFY EKU OK") ||
                text.startsWith("VERIFY X509NAME OK") ||
                text.startsWith("Validating certificate extended key usage") ||
                text.startsWith("++ Certificate has EKU")

        /**
         * Not [LogItem.toString], which passes a null context and yields "resid 2131951829". Falls
         * back to it when resource and arguments disagree, rather than throwing on a native thread.
         */
        private fun LogItem.text(): String = runCatching { getString(appContext) }.getOrElse { toString() }

        fun getTunnel() = openVpnTunnel

        /** OpenVPN's own connection status, reported directly by the engine. */
        fun onEngineStatus(status: ConnectionStatus) {
            // Only the log line is dropped: the engine re-reports a level it is already on.
            if (status != lastLoggedStatus) {
                lastLoggedStatus = status
                vpnLogger.debug("OpenVPN state changed: $status")
            }
            trackServerReply(status)
            openVpnTunnel.onConnectionStatusChange(status)?.let { tunnelState ->
                openVpnTunnel.onStateChange(tunnelState)
            }
        }

        /**
         * Fails the attempt early while the server is still silent, reading the raw status before
         * [OpenVpnTunnel] folds `WAIT`/`TCP_CONNECT` and `AUTH`/`GET_CONFIG` into one `CONNECTING`.
         *
         * Only before the tunnel is up: a mid-session reconnect would lose a working connection.
         */
        private fun trackServerReply(status: ConnectionStatus) {
            when (status) {
                ConnectionStatus.LEVEL_CONNECTING_NO_SERVER_REPLY_YET -> {
                    val established =
                        serverReplied ||
                            reconnecting ||
                            stateManager.state.value.status == VPNState.Status.Connected
                    if (!established && noReplyJob?.isActive != true) {
                        noReplyJob =
                            scope.launch {
                                delay(NO_SERVER_REPLY_WAIT)
                                // Cleared before the disconnect, so that disconnect()'s own
                                // cancelNoReplyDeadline() cannot land on the coroutine running it.
                                noReplyJob = null
                                vpnLogger.error(
                                    "No reply from the server in ${NO_SERVER_REPLY_WAIT / 1000}s.",
                                )
                                disconnect(
                                    VPNState.Error(
                                        VPNState.ErrorType.TimeoutError,
                                        "connection timeout",
                                    ),
                                )
                            }
                    }
                }

                ConnectionStatus.LEVEL_CONNECTING_SERVER_REPLIED,
                ConnectionStatus.LEVEL_CONNECTED,
                -> {
                    serverReplied = true
                    cancelNoReplyDeadline()
                }

                else -> Unit
            }
        }

        private fun cancelNoReplyDeadline() {
            noReplyJob?.cancel()
            noReplyJob = null
        }

        override fun activate() {
            stickyDisconnectEvent = true
            lastLoggedStatus = null
            vpnLogger.info("Activating OpenVPN backend.")

            connectionStateJob =
                scope.launch {
                    openVpnTunnel.stateFlow.cancellable().collectLatest { state ->
                        when (state) {
                            OpenVpnTunnel.State.DOWN -> {
                                if (!stickyDisconnectEvent && !reconnecting) {
                                    connectionJob?.cancel()
                                    updateState(VPNState(Disconnected))
                                }
                            }

                            OpenVpnTunnel.State.CONNECTING -> {
                                if (disconnecting || stateManager.state.value.status == VPNState.Status.Connected) {
                                    return@collectLatest
                                }
                                // Deliberately does NOT clear stickyDisconnectEvent — see CONNECTED.
                                updateState(VPNState(Connecting))
                            }

                            OpenVpnTunnel.State.CONNECTED -> {
                                if (disconnecting || stateManager.state.value.status == VPNState.Status.Connected) {
                                    return@collectLatest
                                }
                                // Cleared here and nowhere else. One OpenVPNBackend serves udp, tcp,
                                // stunnel and wstunnel, so the outgoing native process is still dying
                                // when the next one starts and its LEVEL_NOTCONNECTED lands on the
                                // live attempt. A DOWN before the tunnel is up is therefore ignored;
                                // a failed connect is the connectionJob timeout's job.
                                stickyDisconnectEvent = false
                                testConnectivity()
                            }

                            OpenVpnTunnel.State.DISCONNECTING -> {
                                updateState(VPNState(VPNState.Status.Disconnecting))
                            }
                        }
                    }
                }

            scope.launch {
                openVpnTunnel.errorFlow.cancellable().collectLatest { error ->
                    vpnLogger.error("OpenVPN error: ${error.errorType} - ${error.message}")
                    when (error.errorType) {
                        VPNState.ErrorType.AuthenticationError -> {
                            preferencesHelper.isReconnecting = false
                            disconnect(VPNState.Error(error.errorType, error.message))
                        }

                        else -> {
                            val status = error.vpnStatus ?: VPNState.Status.Disconnected
                            updateState(VPNState(status, VPNState.Error(error.errorType, error.message)))
                        }
                    }
                }
            }

            VpnStatus.addStateListener(this)
            VpnStatus.addLogListener(logListener)
            VpnStatus.addByteCountListener(this)

            active = true
            startNetworkInfoObserver()
            vpnLogger.info("OpenVPN backend activated.")
        }

        override fun deactivate() {
            cancelNoReplyDeadline()
            connectionStateJob?.cancel()
            VpnStatus.removeLogListener(logListener)
            VpnStatus.removeStateListener(this)
            VpnStatus.removeByteCountListener(this)
            stopNetworkInfoObserver()
            active = false
            vpnLogger.info("OpenVPN backend deactivated.")
        }

        override fun connect(
            protocolInformation: ProtocolInformation,
            connectionId: UUID,
        ) {
            this.protocolInformation = protocolInformation
            this.connectionId = connectionId
            vpnLogger.info("Starting Open VPN Service.")
            serverReplied = false
            cancelNoReplyDeadline()
            startConnectionJob()
            launchEngineStart {
                runConnect("OpenVPN", tunnelManager) {
                    // OpenVPN's interface config is server-pushed, so nothing is known yet. The
                    // session holds the live TUN if there is one, and the engine calls
                    // host.requestTun() from openTun() once the push arrives.
                    EnginePlan(
                        OpenVpnEngineProfile(),
                        provisionalTunConfig(
                            blackholeAddress = "10.255.255.1",
                            // Stealth and WStunnel run a local proxy whose upstream socket is created
                            // AND connected by WSTunnelLib (Go); ProxyTunnelManager can only protect
                            // it afterwards by polling socketFd(). Excluding our package for the
                            // negotiation window sidesteps that race. Plain UDP/TCP protect their own
                            // socket through the management channel before connecting, so they pay
                            // nothing. See §12.7.6.
                            excludeSelf =
                                protocolInformation?.protocol.let {
                                    it == PreferencesKeyConstants.PROTO_STEALTH ||
                                        it == PreferencesKeyConstants.PROTO_WS_TUNNEL
                                },
                        ),
                    )
                }
            }
        }

        override suspend fun disconnect(error: VPNState.Error?) {
            this.error = error
            if (proxyDNSManager.invalidConfig) {
                proxyDNSManager.stopControlD()
            }
            cancelNoReplyDeadline()
            connectionJob?.cancel()
            // Unconditional, as WireGuard and IKEv2 are: runDisconnect is the only thing that
            // resolves the hold, publishes the terminal state and ends the session.
            runDisconnect("OpenVPN", tunnelManager, error) {
                openVpnTunnel.onStateChange(OpenVpnTunnel.State.DOWN)
            }
        }

        override fun updateState(
            openVpnState: String?,
            logmessage: String?,
            localizedResId: Int,
            level: ConnectionStatus?,
            Intent: Intent?,
        ) {
        }

        override fun updateByteCount(
            download: Long,
            upload: Long,
            diffIn: Long,
            diffOut: Long,
        ) {}

        override fun setConnectedVPN(uuid: String?) {
        }

        fun protect(fd: Int): Boolean = serviceGate.current?.protect(fd) == true

        override fun handleNetworkChange() {
            if (active) {
                engineCore?.getmDeviceStateReceiver()?.networkStateChange(appContext)
            }
        }

        /**
         * The live engine core, for the one thing that still needs to reach into it: telling
         * OpenVPN's DeviceStateReceiver that the network moved.
         */
        var engineCore: WindscribeOpenVpnService? = null
    }
