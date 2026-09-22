/*
 * Copyright (c) 2021 Windscribe Limited.
 */

package com.windscribe.vpn.backend

import com.windscribe.common.tunnel.EngineProfile
import com.windscribe.common.tunnel.IpNetwork
import com.windscribe.common.tunnel.StopReason
import com.windscribe.common.tunnel.TunConfig
import com.windscribe.common.tunnel.TunHold
import com.windscribe.common.tunnel.TunnelManager
import com.windscribe.vpn.R
import com.windscribe.vpn.Windscribe.Companion.appContext
import com.windscribe.vpn.api.IApiCallManager
import com.windscribe.vpn.apppreference.PreferencesHelper
import com.windscribe.vpn.autoconnection.ProtocolInformation
import com.windscribe.vpn.backend.tunnel.VpnServiceGate
import com.windscribe.vpn.backend.tunnel.WindscribeVpnService
import com.windscribe.vpn.backend.utils.ConnectSource
import com.windscribe.vpn.commonutils.Ext.result
import com.windscribe.vpn.commonutils.ResourceHelper
import com.windscribe.vpn.commonutils.WindUtilities
import com.windscribe.vpn.localdatabase.LocalDbInterface
import com.windscribe.vpn.localdatabase.tables.NetworkInfo
import com.windscribe.vpn.repository.AdvanceParameterRepository
import com.windscribe.vpn.repository.CallResult
import com.windscribe.vpn.state.ConnectionPolicy
import com.windscribe.vpn.state.NetworkInfoManager
import com.windscribe.vpn.state.VPNConnectionStateManager
import com.windscribe.vpn.wsnet.WSNetWrapper
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.withTimeoutOrNull
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.net.InetAddress
import java.util.UUID
import javax.inject.Singleton
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

/**
 * Base class for Interfacing with VPN Modules.
 * */
@Singleton
abstract class VpnBackend(
    private val mainScope: CoroutineScope,
    val stateManager: VPNConnectionStateManager,
    private val preferencesHelper: PreferencesHelper,
    private val networkInfoManager: NetworkInfoManager,
    private val advanceParameterRepository: AdvanceParameterRepository,
    private val apiManager: IApiCallManager,
    protected val localDbInterface: LocalDbInterface,
    private val wsNetWrapper: WSNetWrapper,
    private val resourceHelper: ResourceHelper,
    protected val serviceGate: VpnServiceGate,
    private val proxyDns: ProxyDNSManager,
) {
    val vpnLogger: Logger = LoggerFactory.getLogger("vpn_backend")
    var connectionJob: Job? = null

    /**
     * Guards [failedConnectivityTest] against running twice for one test: its two failure exits fire
     * together when the final attempt is what trips the timeout.
     */
    @Volatile
    private var connectivityFailureHandled = false

    /**
     * Whether this session ever came up. The same error types mean "connect failed" and "live
     * session dropped", which want opposite outcomes.
     */
    @Volatile
    private var sessionWasEstablished = false

    /**
     * True from the moment a disconnect starts until the next connect begins, so the engine cannot
     * overwrite it — charon reports `CHILD_SA_DOWN` during teardown, which maps to `CONNECTING`.
     */
    @Volatile
    protected var disconnecting = false
    var reconnecting = false
    var protocolInformation: ProtocolInformation? = null
    var connectionId: UUID? = null
    var error: VPNState.Error? = null
    var isHandlingNetworkChange = false

    /** The coroutine that starts the engine, retained so a disconnect can cancel it mid-suspend. */
    private var engineStartJob: Job? = null

    private var connectivityTestJob: Job? = null
    private var networkInfoObserverJob: Job? = null

    init {
        mainScope.launch {
            stateManager.state.collectLatest {
                if (it.status == VPNState.Status.Disconnected || it.status == VPNState.Status.Disconnecting) {
                    // Stop existing connectivity test if a disconnect is in process.
                    connectivityTestJob?.cancel()
                    connectivityTestJob = null
                    // Reset network change flag when disconnecting/disconnected
                    isHandlingNetworkChange = false
                }
            }
        }
    }

    protected fun startNetworkInfoObserver() {
        if (networkInfoObserverJob?.isActive == true) {
            vpnLogger.debug("Already handling running..")
            return
        }
        networkInfoObserverJob =
            mainScope.launch {
                networkInfoManager.networkInfo
                    .collectLatest { networkInfo ->
                        vpnLogger.debug("Network Info: {}", networkInfo)
                        handleNetworkInfoUpdate(networkInfo)
                    }
            }
    }

    protected fun stopNetworkInfoObserver() {
        networkInfoObserverJob?.cancel()
        networkInfoObserverJob = null
    }

    private fun handleNetworkInfoUpdate(networkInfo: NetworkInfo?) {
        // Prevent duplicate processing of network changes
        if (isHandlingNetworkChange) {
            vpnLogger.debug("Already handling network change, ignoring duplicate event.")
            return
        }
        // Only the protocol switch: AutoConnectWatcher owns standing down on Auto-Secure-off.
        val input =
            appContext.connectionPolicyInputs.from(
                networkInfo,
                activeProtocol = protocolInformation?.let { it.protocol to it.port },
            )
        if (!ConnectionPolicy.shouldSwitchProtocol(input)) {
            if (stateManager.isVPNConnected()) {
                handleNetworkChange()
            }
            return
        }
        isHandlingNetworkChange = true
        vpnLogger.info(
            "Protocol switch: ${input.activeProtocol?.first}:${input.activeProtocol?.second} -> " +
                "${input.preferredProtocol?.first}:${input.preferredProtocol?.second}",
        )
        appContext.vpnController.connectAsync(ConnectSource.App)
    }

    /**
     * Starts the budget for one connect attempt, cancelling the previous job: [connectionJob] is a
     * single field, and a leaked timer fires against whichever attempt won.
     */

    fun startConnectionJob() {
        connectionJob?.cancel()
        connectionJob =
            mainScope.launch {
                if (protocolInformation?.protocol == "wg") {
                    delay(WG_CONNECTING_WAIT.milliseconds)
                } else {
                    delay(CONNECTING_WAIT.milliseconds)
                }
                connectionTimeout()
            }
    }

    /** Every backend starts its engine through this, so there is always something to cancel. */
    protected fun launchEngineStart(block: suspend () -> Unit) {
        engineStartJob?.cancel()
        engineStartJob = mainScope.launch { block() }
    }

    /**
     * Joined, not just cancelled, or it could attach after the teardown that stopped it. Never joins
     * itself — the connect path calls disconnect.
     */
    private suspend fun cancelEngineStart() {
        val job = engineStartJob ?: return
        engineStartJob = null
        if (job === currentCoroutineContext()[Job]) {
            return
        }
        runCatching { job.cancelAndJoin() }
    }

    private suspend fun connectionTimeout() {
        vpnLogger.error("Connection timeout.")
        disconnect(
            error =
                VPNState.Error(
                    error = VPNState.ErrorType.TimeoutError,
                    "connection timeout",
                ),
        )
    }

    /**
     * Tests connectivity after a successful connection, retrying up to three times.
     *
     * @param initialWaitTime override for the initial delay. WireGuard can pass 0, since the
     * handshake already confirms the tunnel; TCP and stealth need the wait. Null uses the advance
     * parameters.
     * @param pinnedLocation pre-fetched, to avoid a database lookup here.
     */
    fun testConnectivity() {
        if (connectivityTestJob?.isActive == true) {
            vpnLogger.debug("Connectivity test already running, skipping new test.")
            return
        }
        connectionJob?.cancel()
        connectivityTestJob?.cancel()
        connectivityTestJob = null
        connectivityFailureHandled = false
        val startDelay = advanceParameterRepository.getTunnelStartDelay() ?: 500L
        val retryDelay = advanceParameterRepository.getTunnelTestRetryDelay() ?: 500L
        val maxAttempts = advanceParameterRepository.getTunnelTestAttempts() ?: 3

        connectivityTestJob =
            mainScope.launch {
                try {
                    val selectedIp = preferencesHelper.selectedIp
                    val pinnedLocation = getPinnedIpForSelectedCity()
                    val shouldCheckPinning = pinnedLocation?.second != null
                    val hasPinnedNodeMismatch =
                        shouldCheckPinning &&
                            !WindUtilities.hostnamesMatch(
                                pinnedLocation.second,
                                selectedIp,
                            )
                    // 15 seconds total timeout
                    withTimeout(15.seconds) {
                        // Initial delay before first attempt
                        delay(startDelay.milliseconds)
                        // Wait for WSNet to be fully initialized before calling bridge API
                        ensureWSNetReady()
                        configureBridgeAPI(selectedIp)
                        // Pin IP if available (WireGuard only)
                        val ipPinningFailed = attemptIpPinning(pinnedLocation, selectedIp)

                        var lastError: String?
                        var attemptCount = 0
                        var success = false

                        // Try maxAttempts times
                        while (attemptCount < maxAttempts && !success) {
                            attemptCount++
                            if (attemptCount > 1) {
                                vpnLogger.debug("Connectivity test attempt: $attemptCount/$maxAttempts")
                            }

                            val result =
                                result<String> {
                                    apiManager.getIp()
                                }

                            when (result) {
                                is CallResult.Success -> {
                                    val userIp = result.data
                                    if (Util.validIpAddress(userIp)) {
                                        val ipAddress = Util.getModifiedIpAddress(userIp.trim())
                                        preferencesHelper.userIP = ipAddress
                                        connectivityTestPassed(userIp)
                                        success = true
                                        if (hasPinnedNodeMismatch || ipPinningFailed) {
                                            val title =
                                                resourceHelper.getString(R.string.could_not_pin_ip)
                                            val description =
                                                resourceHelper.getString(R.string.favourite_node_not_available)
                                            appContext.applicationInterface.showPinnedNodeErrorDialog(
                                                title,
                                                description,
                                            )
                                        }
                                    } else {
                                        lastError = "Invalid IP address: $userIp"
                                        vpnLogger.warn("Failed Attempt: $attemptCount - $lastError")
                                        if (attemptCount < maxAttempts) {
                                            delay(retryDelay.milliseconds)
                                        }
                                    }
                                }

                                is CallResult.Error -> {
                                    lastError = result.errorMessage
                                    vpnLogger.warn("Failed Attempt: $attemptCount - $lastError")
                                    if (attemptCount < maxAttempts) {
                                        delay(retryDelay.milliseconds)
                                    }
                                }
                            }
                        }

                        if (!success) {
                            vpnLogger.error("Connectivity test failed after $attemptCount attempts")
                            failedConnectivityTest()
                        }
                    }
                } catch (_: TimeoutCancellationException) {
                    vpnLogger.error("Connectivity test timeout: exceeded 15 seconds")
                    failedConnectivityTest()
                } catch (e: Exception) {
                    vpnLogger.error("Connectivity test error: ${e.message}")
                    failedConnectivityTest()
                }
            }
    }

    fun updateState(vpnState: VPNState) {
        mainScope.launch {
            vpnState.protocolInformation = protocolInformation
            vpnState.connectionId = connectionId
            error?.let {
                if (vpnState.status == VPNState.Status.Disconnected) {
                    vpnState.error = it
                    error = null
                }
            }
            if (connectionId != null) {
                stateManager.setState(vpnState)
            }
        }
    }

    open fun connectivityTestPassed(ip: String) {
        vpnLogger.debug("Connectivity test successful: $ip")
        sessionWasEstablished = true
        updateState(VPNState(VPNState.Status.Connected, ip = ip))
        mainScope.launch {
            delay(500.milliseconds)
            reconnecting = false
        }
    }

    private fun failedConnectivityTest() {
        if (connectivityFailureHandled) {
            vpnLogger.debug("Connectivity test failure already handled; ignoring the duplicate.")
            return
        }
        connectivityFailureHandled = true
        connectivityTestJob = null
        connectionJob?.cancel()

        // The test is the only evidence a tunnel carries traffic, so a failure ends the session.
        if (reconnecting.not()) {
            mainScope.launch {
                vpnLogger.info("Connectivity test failed.")
                disconnect(
                    error =
                        VPNState.Error(
                            VPNState.ErrorType.ConnectivityTestFailed,
                            "Connectivity test failed.",
                        ),
                )
            }
        } else {
            vpnLogger.info("Connectivity test failed while reconnecting.")
            // Consider it connected and will fetch ip on app launch.
            preferencesHelper.userIP = null
            updateState(VPNState(VPNState.Status.Connected))
            mainScope.launch {
                delay(500.milliseconds)
                reconnecting = false
            }
        }
    }

    suspend fun getPinnedIpForSelectedCity(): Pair<String, String>? {
        val selectedCity = preferencesHelper.selectedCity
        val favourite =
            localDbInterface.getFavouritesAsync().firstOrNull {
                it.id == selectedCity
            }
        if (favourite == null) return null
        val pinnedIp = favourite.pinnedIp ?: return null
        val pinnedNodeIp = favourite.pinnedNodeIp ?: return null
        return Pair(pinnedIp, pinnedNodeIp)
    }

    /**
     * Waits for WSNet to be fully initialized before proceeding.
     * This prevents crashes when WSNet's native code isn't ready yet.
     */
    private suspend fun ensureWSNetReady() {
        if (!wsNetWrapper.isReady.value) {
            vpnLogger.debug("Waiting for WSNet to be fully ready...")
            withTimeoutOrNull(5.seconds) {
                wsNetWrapper.isReady.first { it }
            } ?: run {
                vpnLogger.warn("WSNet not ready after 5s, skipping bridge API call")
            }
        }
    }

    /**
     * Configures WSNet bridge API for connectivity test.
     * Sets current host for WireGuard connections and marks connection as active.
     */
    private fun configureBridgeAPI(selectedIp: String?) {
        wsNetWrapper.safeBridgeAPI()?.let { bridgeAPI ->
            val hostToSet =
                if (protocolInformation?.protocol == "wg" && !preferencesHelper.isConnectingToConfigured) {
                    selectedIp ?: ""
                } else {
                    ""
                }
            bridgeAPI.setCurrentHost(hostToSet)
            bridgeAPI.setIgnoreSslErrors(false)
            bridgeAPI.setConnectedState(true)
        }
    }

    /**
     * Attempts to pin IP for WireGuard connections.
     * @param pinnedLocation Pair of (pinnedIp, pinnedNodeIp)
     * @param selectedIp Currently selected IP
     * @return true if pinning failed, false if succeeded or not attempted
     */
    private suspend fun attemptIpPinning(
        pinnedLocation: Pair<String, String>?,
        selectedIp: String?,
    ): Boolean {
        val ip = pinnedLocation?.first ?: return false
        vpnLogger.info("Pinning IP: $ip for node: $selectedIp")
        val pinResult = result<Any> { apiManager.pinIp(ip) }
        return when (pinResult) {
            is CallResult.Success -> {
                vpnLogger.info("IP pinned successfully")
                false
            }

            is CallResult.Error -> {
                vpnLogger.error("Failed to pin IP: ${pinResult.errorMessage}")
                true
            }
        }
    }

    // ---------------------------------------------------------------- shared plumbing
    //
    // Common to all three protocol backends: which service to start, how to recognise a handover,
    // and what interface to hold while an engine negotiates.

    /** Starts the single VPN service if needed and waits for it to register, bounded by [VpnServiceGate]. */
    protected suspend fun awaitService(): WindscribeVpnService? = serviceGate.ensureRunning()

    /**
     * True when this disconnect is a handover rather than the end of a session, so the interface
     * stays established while the incoming engine attaches.
     *
     * Only errors a connect follows count, and the failover ones additionally require failover to be
     * running — nothing else drives the next attempt, and holding without one blackholes the device.
     * `UserReconnect` is exempt: `WindVpnController.connect` reconnects on its own path.
     */
    protected fun isHandover(error: VPNState.Error?): Boolean =
        when (error?.error) {
            null -> false
            VPNState.ErrorType.UserReconnect -> true
            in HANDOVER_ERRORS -> appContext.autoConnectionManager.isFailoverActive
            else -> false
        }

    /**
     * The error type already carries the user's intent, so the session end rides along with it rather
     * than being threaded through `VpnBackendHolder` and all three backends.
     */
    private fun sessionEndFor(error: VPNState.Error?): ConnectionPolicy.SessionEnd =
        when (error?.error) {
            VPNState.ErrorType.UserPause -> ConnectionPolicy.SessionEnd.UserPause
            // Without consent nothing can connect, so watching for a network to secure would be a
            // service that can only fail. Stop instead.
            VPNState.ErrorType.UserTurnOff, VPNState.ErrorType.Revoked -> ConnectionPolicy.SessionEnd.UserTurnOff
            VPNState.ErrorType.UserDisconnect -> ConnectionPolicy.SessionEnd.UserDisconnect
            in UNEXPLAINED ->
                when {
                    appContext.autoConnectionManager.isFailoverActive -> ConnectionPolicy.SessionEnd.SystemDisconnect
                    // Our session says nothing about a third party's credentials.
                    preferencesHelper.isConnectingToConfigured ->
                        when {
                            error?.error == VPNState.ErrorType.AuthenticationError -> ConnectionPolicy.SessionEnd.AuthFailed
                            sessionWasEstablished -> ConnectionPolicy.SessionEnd.SystemDisconnect
                            else -> ConnectionPolicy.SessionEnd.ConnectFailed
                        }

                    // A live session dropped, so the refresh decides whether to reconnect.
                    sessionWasEstablished -> ConnectionPolicy.SessionEnd.AwaitingSession
                    // A handshake that never came up. Waiting would reconnect into the same
                    // rejection, and the verdict would order it again on every pass.
                    else -> ConnectionPolicy.SessionEnd.ConnectFailed
                }
            // A connect that failed with no failover behind it has nothing left to try, so it ends
            // as a failure the user can see rather than as "waiting for a network to secure" — which
            // would be untrue on a network we just failed on, and would let the watcher retry at once.
            in CONNECT_FAILURES ->
                when {
                    // Failover is driving and will try the next protocol.
                    appContext.autoConnectionManager.isFailoverActive -> ConnectionPolicy.SessionEnd.SystemDisconnect
                    // A live connection dropped. "Couldn't connect" would be untrue and would
                    // suppress the auto-connect meant to bring it back.
                    sessionWasEstablished -> ConnectionPolicy.SessionEnd.SystemDisconnect
                    else -> ConnectionPolicy.SessionEnd.ConnectFailed
                }

            else -> ConnectionPolicy.SessionEnd.SystemDisconnect
        }

    /**
     * The interface to hold while an engine negotiates: the live config where there is one, a
     * default-routed blackhole on a cold connect.
     *
     * @param excludeSelf for engines that open an unprotected socket while connecting — charon's CRL
     * fetcher and the proxy transports' `WSTunnelLib` socket.
     */
    protected fun provisionalTunConfig(
        blackholeAddress: String,
        excludeSelf: Boolean,
    ): TunConfig {
        val base =
            serviceGate.current?.currentTunConfig()?.also {
                vpnLogger.debug("Holding the live TUN while the engine negotiates.")
            } ?: TunConfig.blackhole(
                sessionName = appContext.getString(R.string.app_name),
                addresses = listOf(IpNetwork(InetAddress.getByName(blackholeAddress), 32)),
                // The config this stands in for honours LAN bypass, so this must too.
                lanBypass = preferencesHelper.lanByPass,
            )
        if (!excludeSelf) {
            return base
        }
        // Returns base as-is when we are the only tunnelled app — see TunConfig.withSelfExcluded.
        return base.withSelfExcluded(appContext.packageName) ?: base.also {
            vpnLogger.warn("we are the only tunnelled app, so we cannot be excluded from the handshake interface")
        }
    }

    /** What an engine needs to come up: its opaque profile and the interface to run it on. */
    protected data class EnginePlan(
        val profile: EngineProfile,
        val tunConfig: TunConfig,
        /** Runs after the engine starts, e.g. to begin health monitoring. */
        val afterStart: () -> Unit = {},
    )

    /**
     * The connect sequence, shared by every protocol: start ctrld, ensure the service, hand the plan
     * to [TunnelManager]. Only [plan] differs per protocol, and is null when no profile is on disk.
     *
     * @param plan built on the IO dispatcher, since it parses a profile off disk.
     */
    protected suspend fun runConnect(
        engineName: String,
        tunnelManager: TunnelManager,
        plan: () -> EnginePlan?,
    ) {
        sessionWasEstablished = false
        disconnecting = false
        val attempt = connectionId
        try {
            proxyDns.startControlDIfRequired()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            // Fail closed: without a confirmed DNS proxy every app's DNS leaks or blackholes.
            vpnLogger.error("DNS proxy failed to start. Aborting connection.", e)
            disconnect(
                VPNState.Error(VPNState.ErrorType.GenericError, "DNS proxy failed to start.", true),
            )
            return
        }

        // Starting it is asynchronous, and nothing can establish a TUN before it exists.
        val service = awaitService()
        // Covers the handshake, and a cold start where the connect path had no service to block on.
        service?.beginBlockingIfRequired()
        if (service == null) {
            vpnLogger.error("VPN service did not start; aborting connection.")
            disconnect(
                VPNState.Error(VPNState.ErrorType.GenericError, "VPN service failed to start.", true),
            )
            return
        }

        withContext(Dispatchers.IO) {
            val failure =
                try {
                    val built = plan()
                    if (built == null) {
                        vpnLogger.error("No usable $engineName profile.")
                        "No usable $engineName profile."
                    } else {
                        // Cancellation cannot reach code already past its last suspension point.
                        if (disconnecting || connectionId != attempt) {
                            vpnLogger.info("$engineName attempt was superseded; not attaching it.")
                            null
                        } else {
                            // If the live TUN already covers this config, TunnelManager will not
                            // call establish() at all — the handover is just an engine swap.
                            tunnelManager.connect(built.profile, built.tunConfig)
                            built.afterStart()
                            null
                        }
                    }
                } catch (e: CancellationException) {
                    // Already being torn down; a failure here would disconnect a second time.
                    throw e
                } catch (e: Exception) {
                    vpnLogger.error("Failed to bring the $engineName engine up.", e)
                    "Could not start the $engineName engine."
                }
            // Through disconnect, or the backend stays active with its timer running and the TUN
            // unresolved. Outside the try, so a throwing teardown is not disconnected twice.
            failure?.let { reason ->
                disconnect(VPNState.Error(VPNState.ErrorType.GenericError, reason, true))
            }
        }
    }

    /**
     * The disconnect sequence, shared by every protocol: hold the TUN on a handover ([isHandover]),
     * end the session otherwise. One service outlives the engines, so nothing has to be slept out.
     */
    protected suspend fun runDisconnect(
        engineName: String,
        tunnelManager: TunnelManager,
        error: VPNState.Error?,
        onStopped: () -> Unit = {},
    ) {
        disconnecting = true
        // A connect still on its way to attaching would otherwise undo this teardown.
        cancelEngineStart()
        // Rejected credentials are the one signal the account changed under us, and the workers
        // that act on it only run if something asks.
        val handover = isHandover(error)
        val end = sessionEndFor(error)
        // Not during a handover, or the walk costs six requests nothing can act on; its last
        // attempt is not a handover, so one still runs. Nothing to ask a third party's server.
        if (!handover && !preferencesHelper.isConnectingToConfigured && error?.error in UNEXPLAINED) {
            vpnLogger.info("The tunnel stopped carrying traffic; refreshing the session to find out why.")
            appContext.workManager.updateSession()
        }
        // Blocking outranks closing but not holding: a handover is already attaching an engine and
        // its hold is bounded. Inputs are read once so the session end below cannot lose them.
        val policyInput =
            if (handover) null else appContext.connectionPolicyInputs.current()
        val hold =
            when {
                handover -> TunHold.ForHandover
                policyInput != null && ConnectionPolicy.shouldBlockOutsideVpn(policyInput, end) ->
                    TunHold.ForBlocking

                else -> TunHold.None
            }
        vpnLogger.info(
            when (hold) {
                TunHold.ForHandover -> "Handover: stopping the $engineName engine and HOLDING the TUN."
                TunHold.ForBlocking -> "Stopping the $engineName engine; HOLDING the TUN to block traffic."
                TunHold.None -> "Stopping the $engineName engine and ending the session."
            },
        )
        tunnelManager.disconnect(hold = hold, reason = StopReason.Requested)
        onStopped()
        if (policyInput != null) {
            serviceGate.current?.endSession(end, policyInput, connectionId)
        }
        // Not via the engine-state collector, which deactivate() cancels microseconds later.
        // AutoConnectionManager waits on this. Skipped for a handover, where the session continues.
        if (error?.error != VPNState.ErrorType.UserReconnect) {
            updateState(VPNState(VPNState.Status.Disconnected))
        }
        deactivate()
    }

    abstract var active: Boolean

    abstract fun activate()

    abstract fun deactivate()

    abstract fun connect(
        protocolInformation: ProtocolInformation,
        connectionId: UUID,
    )

    abstract suspend fun disconnect(error: VPNState.Error? = null)

    open fun handleNetworkChange() {}

    companion object {
        /**
         * Errors that do not say why the tunnel stopped carrying traffic — a dead location and a
         * blocked account look alike — so the session refresh decides.
         */
        private val UNEXPLAINED =
            setOf(
                VPNState.ErrorType.AuthenticationError,
                VPNState.ErrorType.BrokenTunnel,
            )

        /** Errors that mean "we tried to connect and could not". */
        private val CONNECT_FAILURES =
            setOf(
                VPNState.ErrorType.TimeoutError,
                VPNState.ErrorType.ConnectivityTestFailed,
                VPNState.ErrorType.GenericError,
            )

        /** Disconnect reasons a connect reliably follows. See [isHandover]. */
        private val HANDOVER_ERRORS =
            setOf(
                VPNState.ErrorType.UserReconnect,
                VPNState.ErrorType.TimeoutError,
                VPNState.ErrorType.ConnectivityTestFailed,
                VPNState.ErrorType.BrokenTunnel,
                VPNState.ErrorType.AuthenticationError,
                VPNState.ErrorType.WireguardAuthenticationError,
            )

        var DISCONNECT_DELAY = 1000L
        var CONNECTING_WAIT = 30 * 1000L
        var WG_CONNECTING_WAIT = 20 * 1000L

        /**
         * How long an OpenVPN attempt may sit with the server having said nothing at all. Separate
         * from [CONNECTING_WAIT], which a progressing handshake keeps in full; OpenVPN's own
         * `connect-timeout` is 120s, so it never reports inside our window.
         */
        var NO_SERVER_REPLY_WAIT = 12 * 1000L
    }
}
