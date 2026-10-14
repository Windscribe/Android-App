package com.windscribe.vpn.autoconnection

import androidx.fragment.app.DialogFragment
import com.windscribe.vpn.Windscribe.Companion.appContext
import com.windscribe.vpn.alert.showErrorDialog
import com.windscribe.vpn.api.IApiCallManager
import com.windscribe.vpn.apppreference.PreferencesHelper
import com.windscribe.vpn.apppreference.PreferencesKeyConstants
import com.windscribe.vpn.backend.Util
import com.windscribe.vpn.backend.VPNState
import com.windscribe.vpn.backend.utils.ConnectSource
import com.windscribe.vpn.backend.utils.WindVpnController
import com.windscribe.vpn.commonutils.ThreadSafeList
import com.windscribe.vpn.commonutils.WindUtilities
import com.windscribe.vpn.localdatabase.LocalDbInterface
import com.windscribe.vpn.localdatabase.tables.NetworkInfo
import com.windscribe.vpn.repository.CallResult
import com.windscribe.vpn.repository.ConnectionDataRepository
import com.windscribe.vpn.repository.LogRepository
import com.windscribe.vpn.state.ConnectionPolicy
import com.windscribe.vpn.state.NetworkInfoManager
import com.windscribe.vpn.state.VPNConnectionStateManager
import dagger.Lazy
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import org.slf4j.LoggerFactory
import java.util.UUID
import javax.inject.Singleton

@Singleton
class AutoConnectionManager(
    private val scope: CoroutineScope,
    private val vpnConnectionStateManager: Lazy<VPNConnectionStateManager>,
    private val vpnController: Lazy<WindVpnController>,
    private val networkInfoManager: NetworkInfoManager,
    private val connectionDataRepository: ConnectionDataRepository,
    private val localDbInterface: LocalDbInterface,
    private val apiManager: IApiCallManager,
    private val preferencesHelper: PreferencesHelper,
    private val logRepository: Lazy<LogRepository>,
) {
    private var continuation: CancellableContinuation<Boolean>? = null

    @Volatile
    private var attemptJob: Job? = null
    private val logger = LoggerFactory.getLogger("vpn")
    var listOfProtocols = ThreadSafeList<ProtocolInformation>()
    private var manualProtocol: ProtocolInformation? = null
    private var preferredProtocol: Pair<String, ProtocolInformation?>? = null
    private val _nextInLineProtocol = MutableStateFlow(listOfProtocols.firstOrNull())
    val nextInLineProtocol: StateFlow<ProtocolInformation?> = _nextInLineProtocol
    private val _connectedProtocol: MutableStateFlow<ProtocolInformation?> = MutableStateFlow(null)
    val connectedProtocol: StateFlow<ProtocolInformation?> = _connectedProtocol
    private var lastProtocolLog = String()
    private var lastProtocolListLog = String()

    init {
        scope.launch {
            vpnConnectionStateManager.get().state.collectLatest { vpnState ->
                // Failover is over once something connected, whichever path got there. Left set,
                // isHandover() holds the TUN for a reconnect nothing is driving.
                if (vpnState.status == VPNState.Status.Connected && isEnabled) {
                    logger.info("Connected; auto connect is no longer driving.")
                    isEnabled = false
                }
                vpnState.protocolInformation?.let { protocolInformation ->
                    if (vpnState.status == VPNState.Status.Disconnected) {
                        listOfProtocols
                            .firstOrNull {
                                it.protocol == protocolInformation.protocol && it.port == protocolInformation.port
                            }?.type = ProtocolConnectionStatus.Disconnected
                    }
                    if (vpnState.status == VPNState.Status.Connected) {
                        listOfProtocols
                            .firstOrNull {
                                it.protocol == protocolInformation.protocol && it.port == protocolInformation.port
                            }?.let { it ->
                                listOfProtocols.firstOrNull { it.type == ProtocolConnectionStatus.Connected }?.type =
                                    ProtocolConnectionStatus.Disconnected
                                it.type = ProtocolConnectionStatus.Connected
                            }
                    }
                }
            }
        }

        // Observe network info changes via flow
        scope.launch {
            networkInfoManager.networkInfo.collectLatest { networkInfo ->
                if (isEnabled) return@collectLatest
                listOfProtocols
                    .firstOrNull {
                        it.protocol == networkInfo?.protocol
                    }?.let { protocolInfo ->
                        networkInfo?.let { info ->
                            preferredProtocol = Pair(info.networkName, protocolInfo)
                            reset()
                        }
                    }
            }
        }
    }

    fun reset() {
        listOfProtocols.clear()
        listOfProtocols = reloadProtocols()
        updateNextInLineProtocol()
        // reset() runs several times over a startup, usually with the same order.
        val listLog = "Protocol order: $listOfProtocols"
        if (listLog != lastProtocolListLog) {
            logger.debug(listLog)
            lastProtocolListLog = listLog
        }
    }

    /**
     * Stops driving *and* tells the session so. [stop] ends only this class's state; something has
     * to decide whether the held TUN is released or kept blocking, and the watchdog is 90s away.
     */
    private fun giveUp() {
        stop()
        vpnController.get().endSession(ConnectionPolicy.SessionEnd.ConnectFailed)
    }

    /** The link went away mid-walk. [stop] alone leaves the TUN held for an attempt that is not coming. */
    private fun giveUpNoNetwork() {
        stop()
        vpnController.get().endSession(ConnectionPolicy.SessionEnd.NoNetwork)
    }

    fun stop() {
        isEnabled = false
        continuation?.let {
            it.cancel()
            continuation = null
            dismissDialog()
        }
    }

    /**
     * @param allowNodeRetry whether a failure may retry once on a different node. True only for the
     * first protocol: once failover walks the list, the next protocol is the retry. Separate from
     * [attempt], which also selects the node.
     */
    private fun connectionAttempt(
        source: ConnectSource,
        attempt: Int = 0,
        protocolInformation: ProtocolInformation? = null,
        allowNodeRetry: Boolean = true,
    ): VPNState {
        attemptJob?.cancel()
        return try {
            runBlocking {
                attemptJob = coroutineContext[Job]
                val newConnectionId = UUID.randomUUID()
                // One retry each. Both paths reconnect under the same connectionId and keep
                // collecting, so without a bound a server that refuses freshly fetched credentials
                // loops here forever - two API calls and a handshake every 650ms - and failover
                // never gets to try another protocol.
                var credentialsRefreshed = false
                var keysReinitialised = false
                vpnController.get().connect(
                    source = source,
                    connectionId = newConnectionId,
                    protocolInformation = protocolInformation,
                    attempt = attempt,
                )
                val vpnState =
                    vpnConnectionStateManager
                        .get()
                        .state
                        .first {
                            if (it.connectionId == newConnectionId) {
                                if (it.error?.error == VPNState.ErrorType.AuthenticationError) {
                                    if (credentialsRefreshed) {
                                        logger.debug("Credentials already refreshed and still refused; giving up.")
                                        return@first true
                                    }
                                    credentialsRefreshed = true
                                    logger.debug("Updating user auth credentials.")
                                    if (connectionDataRepository.update()) {
                                        logger.debug("Auth updated successfully.")
                                        vpnController.get().connect(
                                            source = source,
                                            connectionId = newConnectionId,
                                            protocolInformation = protocolInformation,
                                            attempt = attempt,
                                        )
                                        return@first false
                                    } else {
                                        logger.debug("Failed to updated auth params.")
                                        return@first true
                                    }
                                } else if (it.error?.error == VPNState.ErrorType.WireguardAuthenticationError) {
                                    if (keysReinitialised) {
                                        logger.debug("Wireguard keys already re-initialised and still refused; giving up.")
                                        return@first true
                                    }
                                    keysReinitialised = true
                                    logger.debug("Trying wireguard with force init.")
                                    vpnController.get().connect(
                                        source = source,
                                        connectionId = newConnectionId,
                                        protocolInformation = protocolInformation,
                                        attempt = attempt,
                                    )
                                    return@first false
                                } else if (it.error?.error == VPNState.ErrorType.UserReconnect) {
                                    return@first false
                                } else {
                                    it.status != VPNState.Status.Connecting
                                }
                            } else {
                                false
                            }
                        }

                // Check if we should retry with different node (only in auto mode)
                val isAutoMode = preferencesHelper.connectionMode == PreferencesKeyConstants.CONNECTION_MODE_AUTO
                val shouldRetryNode =
                    allowNodeRetry &&
                        attempt == 0 &&
                        isAutoMode &&
                        vpnState.error?.error in
                        listOf(
                            VPNState.ErrorType.TimeoutError,
                            VPNState.ErrorType.ConnectivityTestFailed,
                            VPNState.ErrorType.AuthenticationError,
                        ) &&
                        vpnState.status == VPNState.Status.Disconnected

                if (shouldRetryNode) {
                    logger.info("Retrying with different node for error: ${vpnState.error?.error}")
                    // Drop the handle first: the recursive call opens with attemptJob?.cancel(),
                    // so leaving it set makes the retry cancel its own parent — which surfaces as
                    // UserDisconnect and abandons failover.
                    attemptJob = null
                    return@runBlocking connectionAttempt(
                        source = source,
                        attempt = 1,
                        protocolInformation = protocolInformation,
                    )
                }
                vpnState
            }
        } catch (_: CancellationException) {
            logger.debug("Connection attempt cancelled by a newer attempt.")
            VPNState(
                status = VPNState.Status.Disconnected,
                error = VPNState.Error(error = VPNState.ErrorType.UserDisconnect),
            )
        } finally {
            if (attemptJob?.isActive != true) attemptJob = null
        }
    }

    suspend fun connectInForeground(source: ConnectSource) =
        withContext(Dispatchers.IO) {
            // At boot the watcher and BootWorker fire milliseconds apart, and the loser's connect
            // timer would tear down the winner's session. Checked here, not by each caller.
            //
            // Only App is gated: User may be switching location, SessionRefresh moves a live session.
            val status =
                vpnConnectionStateManager
                    .get()
                    .state.value.status
            if (source == ConnectSource.App && status != VPNState.Status.Disconnected) {
                logger.debug("Ignoring an automatic connect from $source: already $status.")
                return@withContext false
            }
            // Before attempting, not after: the check below runs only once connectionAttempt has
            // already spent a full protocol budget on a network that is not there.
            if (WindUtilities.isOnline().not()) {
                logger.debug("No network; not attempting to connect.")
                stop()
                vpnController.get().endSession(ConnectionPolicy.SessionEnd.NoNetwork)
                return@withContext false
            }
            suspendCancellableCoroutine<Boolean> { it ->
                stop()
                this@AutoConnectionManager.continuation = it
                isEnabled = true
                val connectionResult = connectionAttempt(source)
                if (connectionResult.status == VPNState.Status.Connected) {
                    isEnabled = false
                    stop()
                } else if (connectionResult.error?.showError == true) {
                    connectionResult.error?.message?.let { message -> showErrorDialog(message = message) }
                    isEnabled = false
                    stop()
                    vpnController.get().endSession(ConnectionPolicy.SessionEnd.SystemDisconnect)
                } else if (connectionResult.error?.error == VPNState.ErrorType.UserDisconnect) {
                    logger.debug("user disconnect.")
                    isEnabled = false
                    stop()
                    // Giving up here too: the last disconnect was a handover, so a TUN is held for a
                    // reconnect that is not coming.
                    vpnController.get().endSession(ConnectionPolicy.SessionEnd.UserDisconnect)
                }
                if (WindUtilities.isOnline().not() && isEnabled) {
                    logger.debug("No internet detected. existing.")
                    giveUp()
                }
                if (isEnabled && preferencesHelper.isConnectingToConfigured) {
                    logger.debug("Custom config failed to connect; failover does not apply.")
                    stop()
                    connectionResult.error
                        ?.message
                        ?.takeIf { it.isNotEmpty() }
                        ?.let { message -> showErrorDialog(message = message) }
                    vpnController.get().endSession(ConnectionPolicy.SessionEnd.ConnectFailed)
                } else if (isEnabled) {
                    // Check if we should show manual mode failure dialog for specific errors
                    val shouldShowManualModeDialog =
                        connectionResult.error?.error in
                            listOf(
                                VPNState.ErrorType.TimeoutError,
                                VPNState.ErrorType.ConnectivityTestFailed,
                                VPNState.ErrorType.AuthenticationError,
                            ) &&
                            preferencesHelper.connectionMode != PreferencesKeyConstants.CONNECTION_MODE_AUTO

                    if (shouldShowManualModeDialog) {
                        logger.debug("Manual mode connection failed. Showing switch to auto dialog.")
                        showManualModeFailedDialog()
                    } else {
                        logger.debug("Engaging auto connect.")
                        listOfProtocols.firstOrNull { it.protocol == preferencesHelper.selectedProtocol }?.type =
                            ProtocolConnectionStatus.Failed
                        engageAutomaticMode(source)
                    }
                }
            }
        }

    suspend fun changeProtocolInForeground(source: ConnectSource) =
        withContext(Dispatchers.IO) {
            suspendCancellableCoroutine<Boolean> { it ->
                isEnabled = true
                this@AutoConnectionManager.continuation = it
                if (isEnabled) {
                    if (WindUtilities.isOnline().not()) {
                        logger.debug("No internet detected. existing.")
                        giveUpNoNetwork()
                    } else {
                        logger.debug("Engaging auto connect.")
                        if (vpnConnectionStateManager
                                .get()
                                .state.value.status == VPNState.Status.Connected
                        ) {
                            listOfProtocols.firstOrNull { it.protocol == preferencesHelper.selectedProtocol }?.type =
                                ProtocolConnectionStatus.Connected
                        }
                        engageConnectionChangeMode(source)
                    }
                }
            }
        }

    private fun reloadProtocols(): ThreadSafeList<ProtocolInformation> {
        var appSupportedProtocolOrder =
            if (listOfProtocols.size > 0) {
                listOfProtocols
            } else {
                if (preferencesHelper.isSuggested()) {
                    Util.getAppSupportedProtocolList(preferencesHelper.getDefaultProtoInfo())
                } else {
                    Util.getAppSupportedProtocolList()
                }
            }
        if (preferencesHelper.connectionMode != PreferencesKeyConstants.CONNECTION_MODE_AUTO) {
            setupManualProtocol(
                preferencesHelper.savedProtocol,
                appSupportedProtocolOrder,
            )
        } else {
            manualProtocol = null
        }
        networkInfoManager.networkInfo.value?.let {
            setupPreferredProtocol(it, appSupportedProtocolOrder)
        }
        manualProtocol?.let {
            appSupportedProtocolOrder = moveProtocolToTop(it, appSupportedProtocolOrder)
        }
        preferredProtocol?.second?.let {
            appSupportedProtocolOrder = moveProtocolToTop(it, appSupportedProtocolOrder)
        }
        appSupportedProtocolOrder
            .filter { it.type == ProtocolConnectionStatus.NextUp }
            .forEachIterable { it.type = ProtocolConnectionStatus.Disconnected }
        appSupportedProtocolOrder[0].type = ProtocolConnectionStatus.NextUp
        // On a fresh launch both are unset, and the list below says it anyway.
        val protocolLog =
            if (preferredProtocol == null && manualProtocol == null) {
                ""
            } else {
                "Preferred: ${preferredProtocol ?: ""} Manual: ${manualProtocol ?: ""}"
            }
        if (protocolLog.isNotEmpty() && protocolLog != lastProtocolLog) {
            logger.debug(protocolLog)
        }
        lastProtocolLog = protocolLog
        return appSupportedProtocolOrder
    }

    private fun updateNextInLineProtocol() {
        scope.launch {
            val result = runCatching { listOfProtocols.first() }
            result.getOrNull()?.let {
                _nextInLineProtocol.emit(it)
            }
        }
    }

    private fun moveProtocolToTop(
        protocolInformation: ProtocolInformation,
        appSupportedProtocolOrder: ThreadSafeList<ProtocolInformation>,
    ): ThreadSafeList<ProtocolInformation> {
        val index =
            appSupportedProtocolOrder.indexOfFirst { it.protocol == protocolInformation.protocol }
        appSupportedProtocolOrder.removeAt(index)
        appSupportedProtocolOrder.add(0, protocolInformation)
        return appSupportedProtocolOrder
    }

    private fun setupManualProtocol(
        protocol: String,
        appSupportedProtocolOrder: List<ProtocolInformation>,
    ) {
        val port: String =
            when (protocol) {
                PreferencesKeyConstants.PROTO_IKev2 -> preferencesHelper.iKEv2Port
                PreferencesKeyConstants.PROTO_UDP -> preferencesHelper.savedUDPPort
                PreferencesKeyConstants.PROTO_TCP -> preferencesHelper.savedTCPPort
                PreferencesKeyConstants.PROTO_STEALTH -> preferencesHelper.savedSTEALTHPort
                PreferencesKeyConstants.PROTO_WIRE_GUARD -> preferencesHelper.wireGuardPort
                PreferencesKeyConstants.PROTO_WS_TUNNEL -> preferencesHelper.savedWSTunnelPort
                else -> PreferencesKeyConstants.DEFAULT_IKEV2_PORT
            }
        manualProtocol =
            appSupportedProtocolOrder.firstOrNull {
                it.protocol == preferencesHelper.savedProtocol
            }
        manualProtocol?.port = port
    }

    private fun setupPreferredProtocol(
        networkInfo: NetworkInfo,
        appSupportedProtocolOrder: List<ProtocolInformation>,
    ) {
        preferredProtocol = Pair(networkInfo.networkName, null)
        if (networkInfo.isPreferredOn) {
            val protocol =
                appSupportedProtocolOrder.firstOrNull {
                    it.protocol == networkInfo.protocol
                }
            networkInfo.port?.let { protocol?.port = it }
            protocol?.let {
                preferredProtocol = Pair(networkInfo.networkName, protocol)
            }
        }
    }

    private fun engageAutomaticMode(source: ConnectSource) {
        synchronized(listOfProtocols) {
            listOfProtocols.sortBy {
                it.type
            }

            val disconnectedProtocolList =
                listOfProtocols.filter { it.type == ProtocolConnectionStatus.Disconnected }

            if (disconnectedProtocolList.isNotEmpty()) {
                val first = disconnectedProtocolList[0]
                first.type = ProtocolConnectionStatus.NextUp
                first.autoConnectTimeLeft = 10

                showConnectionFailureDialog(
                    source,
                    listOfProtocols,
                    retry = { engageAutomaticMode(source) },
                )
            } else {
                logger.debug("Showing all protocol failed dialog.")
                reset()
                // Nothing left to try, so nothing will attach to a held TUN — and the service has to
                // be told, or it sits on a bare "Disconnected" that says nothing about what happened.
                vpnController.get().endSession(ConnectionPolicy.SessionEnd.ConnectFailed)
                showAllProtocolFailedDialog()
            }
        }
    }

    private fun engageConnectionChangeMode(source: ConnectSource) {
        listOfProtocols
            .filter { it.type == ProtocolConnectionStatus.NextUp }
            .forEachIterable { it.type = ProtocolConnectionStatus.Disconnected }

        // Create a temporary display list with connected protocol at top for UI
        // Don't modify the actual listOfProtocols to avoid permanent reordering
        val displayList = ThreadSafeList<ProtocolInformation>()
        displayList.addAll(listOfProtocols)

        val connectedProtocol = displayList.firstOrNull { it.type == ProtocolConnectionStatus.Connected }
        if (connectedProtocol != null) {
            val index = displayList.indexOfFirst { it.type == ProtocolConnectionStatus.Connected }
            if (index != -1) {
                displayList.removeAt(index)
                displayList.add(0, connectedProtocol)
            }
        }

        showConnectionChangeDialog(
            source,
            displayList,
            retry = { engageAutomaticMode(source) },
        )
    }

    private fun showAllProtocolFailedDialog() {
        val launched =
            appContext.applicationInterface.launchFragment(
                emptyList(),
                FragmentType.AllProtocolFailed,
                autoConnectionModeCallback =
                    object : AutoConnectionModeCallback {
                        override fun onCancel() {
                            logger.debug("Cancel clicked existing auto connect.")
                            stop()
                        }

                        override fun onSendLogClicked() {
                            logger.debug("Send log clicked.")
                            sendLog()
                        }
                    },
            )
        if (launched.not()) {
            // No UI to show it in, but the outcome still has to reach the user: the service settles
            // into ServiceMode.Failed and the notification says which location could not be reached.
            logger.debug("Every protocol failed with no foreground UI; reporting it in the notification.")
            stop()
        }
    }

    private fun sendLog() {
        if (isEnabled) {
            continuation?.let {
                CoroutineScope(it.context).launch {
                    when (val result = logRepository.get().onSendLog()) {
                        is CallResult.Error -> {
                            logger.debug("Error sending log ${result.errorMessage}")
                            dismissDialog()
                            stop()
                        }

                        is CallResult.Success -> {
                            dismissDialog()
                            contactSupport()
                        }
                    }
                }
            }
        }
    }

    private fun dismissDialog() {
        val dialog =
            appContext.activeActivity?.supportFragmentManager?.fragments?.firstOrNull {
                it is DialogFragment
            } as? DialogFragment
        dialog?.dismiss()
        appContext.applicationInterface.cancelDialog()
    }

    private fun contactSupport() {
        logger.debug("Showing contact support dialog.")
        val launched =
            appContext.applicationInterface.launchFragment(
                emptyList(),
                FragmentType.DebugLogSent,
                autoConnectionModeCallback =
                    object : AutoConnectionModeCallback {
                        override fun onCancel() {
                            logger.debug("Cancel clicked existing auto connect.")
                            stop()
                        }

                        override fun onContactSupportClick() {
                            stop()
                            logger.debug("On contact support clicked existing auto connect.")
                        }
                    },
            )
        if (launched.not()) {
            logger.debug("App is in background. existing auto connect.")
            stop()
        }
    }

    private fun saveNetworkForFutureUse(protocolInformation: ProtocolInformation) {
        if (networkInfoManager.networkInfo.value?.isPreferredOn == true) {
            logger.debug("Preferred protocol for this network is already set. existing auto connect.")
            stop()
            return
        }
        val netWorkName = networkInfoManager.networkInfo.value?.networkName
        if (netWorkName != null) {
            logger.debug("Showing set as preferred protocol dialog.")
            val launched =
                appContext.applicationInterface.launchFragment(
                    emptyList(),
                    FragmentType.SetupAsPreferredProtocol,
                    autoConnectionModeCallback =
                        object : AutoConnectionModeCallback {
                            override fun onCancel() {
                                logger.debug("Cancel clicked existing auto connect.")
                                stop()
                            }

                            override fun onSetAsPreferredClicked() {
                                setProtocolAsPreferred(protocolInformation)
                            }
                        },
                    protocolInformation,
                )
            if (launched.not()) {
                logger.debug("App is in background. existing auto connect.")
                stop()
            }
        } else {
            logger.debug("Unable to get network name.")
        }
    }

    private var isEnabled: Boolean = false

    /**
     * True while failover is driving, i.e. a connect will follow a failure.
     *
     * Read by `VpnBackend.isHandover`: holding the TUN across a failure blackholes every other app,
     * which is only acceptable because something is about to attach to it. Nothing does on the
     * connect paths that bypass this class — notification, tile, boot, auto-connect — so a hold there
     * would blackhole the device until the watchdog expired.
     */
    val isFailoverActive: Boolean
        get() = isEnabled

    private fun setProtocolAsPreferred(protocolInformation: ProtocolInformation) {
        networkInfoManager.networkInfo.value?.let {
            val updated =
                it.copy(
                    protocol = protocolInformation.protocol,
                    port = protocolInformation.port,
                    isPreferredOn = true,
                    isAutoSecureOn = true,
                )
            scope.launch {
                localDbInterface.updateNetworkSync(updated)
                logger.debug("Saved ${protocolInformation.protocol}:${protocolInformation.port} for SSID: ${updated.networkName}")
                // Republish: the flow still holds the row as it was. Mutating it in place would
                // make `networkInfo.value` right and every collector blind.
                networkInfoManager.reload()
            }
            stop()
        } ?: kotlin.run {
            stop()
        }
    }

    /**
     * Offers the next protocol, through the dialog when there is a foreground Activity and directly
     * when there is not.
     *
     * The dialog exists so the user can intervene or cancel; its countdown otherwise just picks the
     * protocol already marked [ProtocolConnectionStatus.NextUp]. With no Activity there is nobody to
     * intervene, so the countdown's choice is taken directly.
     *
     * Driving [AutoConnectionModeCallback.onProtocolSelect] keeps both paths on the same code: it
     * marks the outcome, and on failure calls [retry], which comes back here for the next protocol.
     */
    private fun showConnectionFailureDialog(
        source: ConnectSource,
        protocolInformation: List<ProtocolInformation>,
        retry: () -> Unit,
    ) {
        val callback =
            object : AutoConnectionModeCallback {
                override fun onCancel() {
                    listOfProtocols.firstOrNull { it.protocol == preferencesHelper.selectedProtocol }?.type =
                        ProtocolConnectionStatus.NextUp
                    listOfProtocols.firstOrNull { it.protocol == preferencesHelper.selectedProtocol }?.let {
                        moveProtocolToTop(it, listOfProtocols)
                    }
                    logger.debug("Cancel clicked existing auto connect.")
                    isEnabled = false
                    // The TUN is held for a next attempt that will now never happen.
                    vpnController.get().endSession(ConnectionPolicy.SessionEnd.UserDisconnect)
                }

                override fun onProtocolSelect(protocolInformation: ProtocolInformation) {
                    logger.debug("Next selected protocol: ${protocolInformation.protocol}:${protocolInformation.port}")
                    if (WindUtilities.isOnline().not()) {
                        logger.debug("No internet detected. existing.")
                        giveUpNoNetwork()
                    }
                    if (isEnabled) {
                        continuation?.let {
                            CoroutineScope(it.context).launch {
                                val connectionResult =
                                    connectionAttempt(
                                        source = source,
                                        protocolInformation = protocolInformation,
                                        allowNodeRetry = false,
                                    )
                                listOfProtocols
                                    .firstOrNull {
                                        it.type == ProtocolConnectionStatus.NextUp
                                    }?.type = ProtocolConnectionStatus.Disconnected
                                if (connectionResult.status == VPNState.Status.Connected) {
                                    listOfProtocols.firstOrNull { it.protocol == protocolInformation.protocol }?.type =
                                        ProtocolConnectionStatus.Connected
                                    logger.debug(
                                        "Successfully found a working protocol: ${protocolInformation.protocol}:${protocolInformation.port}",
                                    )
                                    if ((
                                            networkInfoManager.networkInfo.value?.port != protocolInformation.port &&
                                                networkInfoManager.networkInfo.value?.protocol != protocolInformation.protocol
                                        ) ||
                                        networkInfoManager.networkInfo.value?.isPreferredOn == false
                                    ) {
                                        saveNetworkForFutureUse(protocolInformation)
                                    }
                                } else if (connectionResult.error?.showError == true) {
                                    showErrorDialog(connectionResult.error?.message ?: "")
                                    stop()
                                } else if (connectionResult.error?.error == VPNState.ErrorType.UserDisconnect) {
                                    isEnabled = false
                                    stop()
                                } else {
                                    listOfProtocols.firstOrNull { it.protocol == protocolInformation.protocol }?.type =
                                        ProtocolConnectionStatus.Failed
                                    logger.debug(
                                        "Auto connect failure: ${protocolInformation.protocol}:${protocolInformation.port} ${connectionResult.error?.message}",
                                    )
                                    retry()
                                }
                            }
                        }
                    }
                }
            }
        val launched =
            appContext.applicationInterface.launchFragment(
                protocolInformation,
                FragmentType.ConnectionFailure,
                autoConnectionModeCallback = callback,
            )
        if (launched) {
            return
        }
        val nextUp = protocolInformation.firstOrNull { it.type == ProtocolConnectionStatus.NextUp }
        if (nextUp != null && isEnabled) {
            logger.debug("No foreground UI; taking ${nextUp.protocol}:${nextUp.port} without the dialog.")
            callback.onProtocolSelect(nextUp)
        } else {
            logger.debug("No foreground UI and no protocol left. Ending auto connect.")
            stop()
            vpnController.get().endSession(ConnectionPolicy.SessionEnd.ConnectFailed)
        }
    }

    private fun showConnectionChangeDialog(
        source: ConnectSource,
        protocolInformation: List<ProtocolInformation>,
        retry: () -> Unit,
    ) {
        val launched =
            appContext.applicationInterface.launchFragment(
                protocolInformation,
                FragmentType.ConnectionChange,
                autoConnectionModeCallback =
                    object : AutoConnectionModeCallback {
                        override fun onCancel() {
                            logger.debug("Cancel clicked existing auto connect.")
                            stop()
                        }

                        override fun onProtocolSelect(protocolInformation: ProtocolInformation) {
                            logger.info("User changed protocol: ${protocolInformation.protocol}:${protocolInformation.port}")

                            if (isEnabled) {
                                continuation?.let {
                                    CoroutineScope(it.context).launch {
                                        delay(300)
                                        if (WindUtilities.isOnline().not()) {
                                            logger.debug("No internet detected. existing.")
                                            giveUpNoNetwork()
                                        }
                                        var connectionResult =
                                            connectionAttempt(source, protocolInformation = protocolInformation)
                                        if (connectionResult.error?.error == VPNState.ErrorType.AuthenticationError) {
                                            connectionResult = connectionAttempt(source, 1, protocolInformation)
                                        }
                                        if (connectionResult.status == VPNState.Status.Connected) {
                                            listOfProtocols.firstOrNull { it.protocol == protocolInformation.protocol }?.type =
                                                ProtocolConnectionStatus.Connected
                                            logger.debug(
                                                "Successfully found a working protocol: ${protocolInformation.protocol}:${protocolInformation.port}",
                                            )
                                            if ((
                                                    networkInfoManager.networkInfo.value?.port != protocolInformation.port &&
                                                        networkInfoManager.networkInfo.value?.protocol != protocolInformation.protocol
                                                ) ||
                                                networkInfoManager.networkInfo.value?.isPreferredOn == false
                                            ) {
                                                saveNetworkForFutureUse(protocolInformation)
                                            }
                                        } else if (connectionResult.error?.showError == true) {
                                            connectionResult.error?.message?.let { showErrorDialog(it) }
                                            stop()
                                        } else if (connectionResult.error?.error == VPNState.ErrorType.UserDisconnect) {
                                            isEnabled = false
                                            stop()
                                        } else {
                                            listOfProtocols.firstOrNull { it.protocol == protocolInformation.protocol }?.type =
                                                ProtocolConnectionStatus.Failed
                                            logger.debug(
                                                "Protocol change failure: ${protocolInformation.protocol}:${protocolInformation.port} ${connectionResult.error?.message}",
                                            )
                                            retry()
                                        }
                                    }
                                }
                            }
                        }
                    },
            )
        if (launched.not()) {
            logger.info("App is in background. existing auto connect.")
            stop()
        }
    }

    fun setSelectedProtocol(protocolInformation: ProtocolInformation) {
        logger.info("Trying to connect: $protocolInformation")
        preferencesHelper.selectedProtocol = protocolInformation.protocol
        preferencesHelper.selectedPort = protocolInformation.port
        preferencesHelper.selectedProtocolType = protocolInformation.type
        scope.launch {
            _connectedProtocol.emit(protocolInformation)
        }
    }

    private fun showManualModeFailedDialog() {
        val launched =
            appContext.applicationInterface.launchFragment(
                emptyList(),
                FragmentType.ManualModeFailed,
                autoConnectionModeCallback =
                    object : AutoConnectionModeCallback {
                        override fun onCancel() {
                            logger.debug("User cancelled manual mode switch. Stopping auto connect.")
                            giveUp()
                        }

                        override fun onSwitchToAutoMode() {
                            logger.debug("User switched to auto mode.")
                            // Switch to auto mode
                            preferencesHelper.connectionMode = PreferencesKeyConstants.CONNECTION_MODE_AUTO
                            reset()
                            listOfProtocols.firstOrNull { it.protocol == preferencesHelper.selectedProtocol }?.type =
                                ProtocolConnectionStatus.Failed
                            scope.launch {
                                connectInForeground(ConnectSource.User)
                            }
                        }
                    },
            )
        if (launched.not()) {
            logger.debug("App is in background. Stopping auto connect.")
            giveUp()
        }
    }

    private inline fun <T> List<T>.forEachIterable(block: (T) -> Unit) {
        with(iterator()) {
            while (hasNext()) {
                block(next())
            }
        }
    }
}
