package com.windscribe.vpn.autoconnection

import android.os.Build
import androidx.fragment.app.DialogFragment
import com.windscribe.vpn.R
import com.windscribe.vpn.Windscribe
import com.windscribe.vpn.Windscribe.Companion.appContext
import com.windscribe.vpn.alert.showErrorDialog
import com.windscribe.vpn.api.IApiCallManager
import com.windscribe.vpn.api.response.GenericSuccess
import com.windscribe.vpn.apppreference.PreferencesHelper
import com.windscribe.vpn.backend.Util
import com.windscribe.vpn.backend.VPNState
import com.windscribe.vpn.backend.utils.WindVpnController
import com.windscribe.vpn.commonutils.ThreadSafeList
import com.windscribe.vpn.commonutils.WindUtilities
import com.windscribe.vpn.apppreference.PreferencesKeyConstants
import com.windscribe.vpn.encoding.encoders.Base64
import com.windscribe.vpn.localdatabase.LocalDbInterface
import com.windscribe.vpn.localdatabase.tables.NetworkInfo
import com.windscribe.vpn.repository.CallResult
import com.windscribe.vpn.repository.ConnectionDataRepository
import com.windscribe.vpn.state.NetworkInfoManager
import com.windscribe.vpn.state.VPNConnectionStateManager
import dagger.Lazy
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.suspendCancellableCoroutine
import org.slf4j.LoggerFactory
import java.io.BufferedReader
import java.io.File
import java.io.FileReader
import java.nio.charset.Charset
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
    private val preferencesHelper: PreferencesHelper
) {

    private var continuation: CancellableContinuation<Boolean>? = null
    private val logger = LoggerFactory.getLogger("vpn")
    var listOfProtocols = ThreadSafeList<ProtocolInformation>()
    private var manualProtocol: ProtocolInformation? = null
    private var preferredProtocol: Pair<String, ProtocolInformation?>? = null
    private val _nextInLineProtocol = MutableStateFlow(listOfProtocols.firstOrNull())
    val nextInLineProtocol: StateFlow<ProtocolInformation?> = _nextInLineProtocol
    private val _connectedProtocol: MutableStateFlow<ProtocolInformation?> = MutableStateFlow(null)
    val connectedProtocol: StateFlow<ProtocolInformation?> = _connectedProtocol
    private var lastProtocolLog = String()

    init {
        scope.launch {
            vpnConnectionStateManager.get().state.collectLatest { vpnState ->
                vpnState.protocolInformation?.let { protocolInformation ->
                    if (vpnState.status == VPNState.Status.Disconnected) {
                        listOfProtocols.firstOrNull {
                            it.protocol == protocolInformation.protocol && it.port == protocolInformation.port
                        }?.type = ProtocolConnectionStatus.Disconnected
                    }
                    if (vpnState.status == VPNState.Status.Connected) {
                        listOfProtocols.firstOrNull {
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
                listOfProtocols.firstOrNull {
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
        logger.info("{}", listOfProtocols)
    }

    fun stop() {
        isEnabled = false
        continuation?.let {
            it.cancel()
            continuation = null
            dismissDialog()
        }
    }

    private fun connectionAttempt(
        attempt: Int = 0,
        protocolInformation: ProtocolInformation? = null
    ): VPNState {
        return runBlocking {
            val newConnectionId = UUID.randomUUID()
            vpnController.get().connect(
                connectionId = newConnectionId,
                protocolInformation = protocolInformation,
                attempt = attempt
            )
            val vpnState = vpnConnectionStateManager.get().state
                .first {
                    if (it.connectionId == newConnectionId) {
                        if (it.error?.error == VPNState.ErrorType.AuthenticationError) {
                            logger.debug("Updating user auth credentials.")
                            if (connectionDataRepository.update()) {
                                logger.debug("Auth updated successfully.")
                                vpnController.get().connect(
                                    connectionId = newConnectionId,
                                    protocolInformation = protocolInformation,
                                    attempt = attempt
                                )
                                return@first false
                            } else {
                                logger.debug("Failed to updated auth params.")
                                return@first true
                            }
                        } else if (it.error?.error == VPNState.ErrorType.WireguardAuthenticationError) {
                            logger.debug("Trying wireguard with force init.")
                            vpnController.get().connect(
                                connectionId = newConnectionId,
                                protocolInformation = protocolInformation,
                                attempt = attempt
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
            val shouldRetryNode = attempt == 0 &&
                isAutoMode &&
                vpnState.error?.error in listOf(
                    VPNState.ErrorType.TimeoutError,
                    VPNState.ErrorType.ConnectivityTestFailed,
                    VPNState.ErrorType.AuthenticationError
                ) &&
                vpnState.status == VPNState.Status.Disconnected

            if (shouldRetryNode) {
                logger.info("Retrying with different node for error: ${vpnState.error?.error}")
                return@runBlocking connectionAttempt(
                    attempt = 1,
                    protocolInformation = protocolInformation
                )
            }

            vpnState
        }
    }

    suspend fun connectInForeground() = suspendCancellableCoroutine<Boolean> { it ->
        stop()
        this.continuation = it
        isEnabled = true
        val connectionResult = connectionAttempt()
        if (connectionResult.status == VPNState.Status.Connected) {
            isEnabled = false
            stop()
        } else if (connectionResult.error?.showError == true) {
            connectionResult.error?.message?.let { message -> showErrorDialog(message = message) }
            isEnabled = false
            stop()
        } else if (connectionResult.error?.error == VPNState.ErrorType.UserDisconnect) {
            logger.debug("user disconnect.")
            isEnabled = false
            stop()
        }
        if (WindUtilities.isOnline().not() && isEnabled) {
            logger.debug("No internet detected. existing.")
            stop()
        }
        if (isEnabled) {
            // Check if we should show manual mode failure dialog for specific errors
            val shouldShowManualModeDialog = connectionResult.error?.error in listOf(
                VPNState.ErrorType.TimeoutError,
                VPNState.ErrorType.ConnectivityTestFailed,
                VPNState.ErrorType.AuthenticationError
            ) && preferencesHelper.connectionMode != PreferencesKeyConstants.CONNECTION_MODE_AUTO

            if (shouldShowManualModeDialog) {
                logger.debug("Manual mode connection failed. Showing switch to auto dialog.")
                showManualModeFailedDialog()
            } else {
                logger.debug("Engaging auto connect.")
                listOfProtocols.firstOrNull { it.protocol == preferencesHelper.selectedProtocol }?.type =
                    ProtocolConnectionStatus.Failed
                engageAutomaticMode()
            }
        }
    }

    suspend fun changeProtocolInForeground() = suspendCancellableCoroutine<Boolean> { it ->
        isEnabled = true
        this.continuation = it
        if (isEnabled) {
            if (WindUtilities.isOnline().not()) {
                logger.debug("No internet detected. existing.")
                stop()
            } else {
                logger.debug("Engaging auto connect.")
                if (vpnConnectionStateManager.get().state.value.status == VPNState.Status.Connected) {
                    listOfProtocols.firstOrNull { it.protocol == preferencesHelper.selectedProtocol }?.type =
                        ProtocolConnectionStatus.Connected
                }
                engageConnectionChangeMode()
            }
        }
    }

    private fun reloadProtocols(): ThreadSafeList<ProtocolInformation> {
        var appSupportedProtocolOrder = if (listOfProtocols.size > 0) {
            listOfProtocols
        } else {
            if (preferencesHelper.isSuggested()){
                Util.getAppSupportedProtocolList(preferencesHelper.getDefaultProtoInfo())
            } else {
                Util.getAppSupportedProtocolList()
            }
        }
        if (preferencesHelper.connectionMode != PreferencesKeyConstants.CONNECTION_MODE_AUTO) {
            setupManualProtocol(
                preferencesHelper.savedProtocol, appSupportedProtocolOrder
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
        appSupportedProtocolOrder.filter { it.type == ProtocolConnectionStatus.NextUp }
            .forEachIterable { it.type = ProtocolConnectionStatus.Disconnected }
        appSupportedProtocolOrder[0].type = ProtocolConnectionStatus.NextUp
        val protocolLog =
            "Preferred: ${preferredProtocol ?: ""} Manual: ${manualProtocol ?: ""}"
        if (protocolLog != lastProtocolLog) {
            logger.info(protocolLog)
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
        appSupportedProtocolOrder: ThreadSafeList<ProtocolInformation>
    ): ThreadSafeList<ProtocolInformation> {
        val index =
            appSupportedProtocolOrder.indexOfFirst { it.protocol == protocolInformation.protocol }
        appSupportedProtocolOrder.removeAt(index)
        appSupportedProtocolOrder.add(0, protocolInformation)
        return appSupportedProtocolOrder
    }

    private fun setupManualProtocol(
        protocol: String,
        appSupportedProtocolOrder: List<ProtocolInformation>
    ) {
        val port: String = when (protocol) {
            PreferencesKeyConstants.PROTO_IKev2 -> preferencesHelper.iKEv2Port
            PreferencesKeyConstants.PROTO_UDP -> preferencesHelper.savedUDPPort
            PreferencesKeyConstants.PROTO_TCP -> preferencesHelper.savedTCPPort
            PreferencesKeyConstants.PROTO_STEALTH -> preferencesHelper.savedSTEALTHPort
            PreferencesKeyConstants.PROTO_WIRE_GUARD -> preferencesHelper.wireGuardPort
            PreferencesKeyConstants.PROTO_WS_TUNNEL -> preferencesHelper.savedWSTunnelPort
            else -> PreferencesKeyConstants.DEFAULT_IKEV2_PORT
        }
        manualProtocol = appSupportedProtocolOrder.firstOrNull {
            it.protocol == preferencesHelper.savedProtocol
        }
        manualProtocol?.port = port
    }

    private fun setupPreferredProtocol(
        networkInfo: NetworkInfo,
        appSupportedProtocolOrder: List<ProtocolInformation>
    ) {
        preferredProtocol = Pair(networkInfo.networkName, null)
        if (networkInfo.isPreferredOn) {
            val protocol = appSupportedProtocolOrder.firstOrNull {
                it.protocol == networkInfo.protocol
            }
            protocol?.port = networkInfo.port
            protocol?.let {
                preferredProtocol = Pair(networkInfo.networkName, protocol)
            }
        }
    }

    private fun engageAutomaticMode() {
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
                    listOfProtocols,
                    retry = { engageAutomaticMode() }
                )
            } else {
                logger.debug("Showing all protocol failed dialog.")
                reset()
                showAllProtocolFailedDialog()
            }
        }
    }

    private fun engageConnectionChangeMode() {
        listOfProtocols.filter { it.type == ProtocolConnectionStatus.NextUp }
            .forEachIterable { it.type = ProtocolConnectionStatus.Disconnected }
        val connectedProtocol =
            listOfProtocols.firstOrNull { it.type == ProtocolConnectionStatus.Connected }
        if (connectedProtocol != null) {
            val index =
                listOfProtocols.indexOfFirst { it.type == ProtocolConnectionStatus.Connected }
            if (index != -1) {
                listOfProtocols.removeAt(index)
                listOfProtocols.add(0, connectedProtocol)
            }
        }
        showConnectionChangeDialog(
            listOfProtocols,
            retry = { engageAutomaticMode() })
    }

    private fun showAllProtocolFailedDialog() {
        val launched = appContext.applicationInterface.launchFragment(
            emptyList(),
            FragmentType.AllProtocolFailed,
            autoConnectionModeCallback = object : AutoConnectionModeCallback {
                override fun onCancel() {
                    logger.debug("Cancel clicked existing auto connect.")
                    stop()
                }

                override fun onSendLogClicked() {
                    logger.debug("Send log clicked.")
                    sendLog()
                }
            })
        if (launched.not()) {
            logger.debug("App is in background. existing auto connect.")
            stop()
        }
    }

    private fun sendLog() {
        if (isEnabled) {
            continuation?.let {
                CoroutineScope(it.context).launch {
                    when (val result = sendLogToServer()) {
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

    suspend fun sendLogToServer(): CallResult<GenericSuccess> {
        return try {
            var logLine: String?
            val debugFilePath = appContext.filesDir.path + PreferencesKeyConstants.DEBUG_LOG_FILE_NAME
            val logFile = Windscribe.appContext.resources.getString(
                R.string.log_file_header,
                Build.VERSION.SDK_INT, Build.BRAND, Build.DEVICE, Build.MODEL, Build.MANUFACTURER,
                Build.VERSION.RELEASE, WindUtilities.getVersionCode()
            )
            val builder = StringBuilder()
            builder.append(logFile)
            val file = File(debugFilePath)
            val bufferedReader = BufferedReader(FileReader(file))
            while (bufferedReader.readLine().also { logLine = it } != null) {
                builder.append(logLine)
                builder.append("\n")
            }
            bufferedReader.close()
            val encodedLog = String(Base64.encode(builder.toString().toByteArray(Charset.defaultCharset())))
            return apiManager.postDebugLog(preferencesHelper.userName, encodedLog).callResult()
        } catch (ignored: Exception) {
            CallResult.Error(errorMessage = "Unable to load debug logs from disk.")
        }
    }

    private fun dismissDialog() {
        val dialog = appContext.activeActivity?.supportFragmentManager?.fragments?.firstOrNull {
            it is DialogFragment
        } as? DialogFragment
        dialog?.dismiss()
        appContext.applicationInterface.cancelDialog()
    }

    private fun contactSupport() {
        logger.debug("Showing contact support dialog.")
        val launched = appContext.applicationInterface.launchFragment(
            emptyList(),
            FragmentType.DebugLogSent,
            autoConnectionModeCallback = object : AutoConnectionModeCallback {
                override fun onCancel() {
                    logger.debug("Cancel clicked existing auto connect.")
                    stop()
                }

                override fun onContactSupportClick() {
                    stop()
                    logger.debug("On contact support clicked existing auto connect.")
                }
            })
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
            val launched = appContext.applicationInterface.launchFragment(
                emptyList(),
                FragmentType.SetupAsPreferredProtocol,
                autoConnectionModeCallback = object : AutoConnectionModeCallback {
                    override fun onCancel() {
                        logger.debug("Cancel clicked existing auto connect.")
                        stop()
                    }

                    override fun onSetAsPreferredClicked() {
                        setProtocolAsPreferred(protocolInformation)
                    }
                }, protocolInformation
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

    private fun setProtocolAsPreferred(protocolInformation: ProtocolInformation) {
        networkInfoManager.networkInfo.value?.let {
            it.protocol = protocolInformation.protocol
            it.port = protocolInformation.port
            it.isPreferredOn = true
            it.isAutoSecureOn = true
            scope.launch {
                localDbInterface.updateNetworkSync(it)
                logger.debug("Saved ${protocolInformation.protocol}:${protocolInformation.port} for SSID: ${it.networkName}")
            }
            stop()
        } ?: kotlin.run {
            stop()
        }
    }

    private fun showConnectionFailureDialog(
        protocolInformation: List<ProtocolInformation>,
        retry: () -> Unit
    ) {
        val launched = appContext.applicationInterface.launchFragment(
            protocolInformation,
            FragmentType.ConnectionFailure,
            autoConnectionModeCallback = object : AutoConnectionModeCallback {
                override fun onCancel() {
                    listOfProtocols.firstOrNull { it.protocol == preferencesHelper.selectedProtocol }?.type =
                            ProtocolConnectionStatus.NextUp
                    listOfProtocols.firstOrNull { it.protocol == preferencesHelper.selectedProtocol }?.let {
                        moveProtocolToTop(it, listOfProtocols)
                    }
                    logger.debug("Cancel clicked existing auto connect.")
                    isEnabled = false
                }

                override fun onProtocolSelect(protocolInformation: ProtocolInformation) {
                    logger.debug("Next selected protocol: ${protocolInformation.protocol}:${protocolInformation.port}")
                    if (WindUtilities.isOnline().not()) {
                        logger.debug("No internet detected. existing.")
                        stop()
                    }
                    if (isEnabled) {
                        continuation?.let {
                            CoroutineScope(it.context).launch {
                                val connectionResult =
                                    connectionAttempt(protocolInformation = protocolInformation)
                                listOfProtocols.firstOrNull {
                                    it.type == ProtocolConnectionStatus.NextUp
                                }?.type = ProtocolConnectionStatus.Disconnected
                                if (connectionResult.status == VPNState.Status.Connected) {
                                    listOfProtocols.firstOrNull { it.protocol == protocolInformation.protocol }?.type =
                                        ProtocolConnectionStatus.Connected
                                    logger.debug("Successfully found a working protocol: ${protocolInformation.protocol}:${protocolInformation.port}")
                                    if ((networkInfoManager.networkInfo.value?.port != protocolInformation.port && networkInfoManager.networkInfo.value?.protocol != protocolInformation.protocol) || networkInfoManager.networkInfo.value?.isPreferredOn == false) {
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
                                    logger.debug("Auto connect failure: ${protocolInformation.protocol}:${protocolInformation.port} ${connectionResult.error?.message}")
                                    retry()
                                }
                            }
                        }
                    }
                }
            })
        if (launched.not()) {
            logger.debug("App is in background. existing auto connect.")
            stop()
        }
    }

    private fun showConnectionChangeDialog(
        protocolInformation: List<ProtocolInformation>,
        retry: () -> Unit
    ) {
        val launched = appContext.applicationInterface.launchFragment(
            protocolInformation,
            FragmentType.ConnectionChange,
            autoConnectionModeCallback = object : AutoConnectionModeCallback {
                override fun onCancel() {
                    logger.debug("Cancel clicked existing auto connect.")
                    stop()
                }

                override fun onProtocolSelect(protocolInformation: ProtocolInformation) {
                    listOfProtocols.firstOrNull {
                        it.type == ProtocolConnectionStatus.Connected || it.type == ProtocolConnectionStatus.NextUp
                    }?.type = ProtocolConnectionStatus.Disconnected
                    logger.debug("User changed protocol: ${protocolInformation.protocol}:${protocolInformation.port}")

                    if (isEnabled) {
                        continuation?.let {
                            CoroutineScope(it.context).launch {
                                delay(300)
                                if (WindUtilities.isOnline().not()) {
                                    logger.debug("No internet detected. existing.")
                                    stop()
                                }
                                var connectionResult =
                                    connectionAttempt(protocolInformation = protocolInformation)
                                if (connectionResult.error?.error == VPNState.ErrorType.AuthenticationError) {
                                    connectionResult = connectionAttempt(1, protocolInformation)
                                }
                                if (connectionResult.status == VPNState.Status.Connected) {
                                    listOfProtocols.firstOrNull { it.protocol == protocolInformation.protocol }?.type =
                                        ProtocolConnectionStatus.Connected
                                    logger.debug("Successfully found a working protocol: ${protocolInformation.protocol}:${protocolInformation.port}")
                                    if ((networkInfoManager.networkInfo.value?.port != protocolInformation.port && networkInfoManager.networkInfo.value?.protocol != protocolInformation.protocol) || networkInfoManager.networkInfo.value?.isPreferredOn == false) {
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
                                    logger.debug("Protocol change failure: ${protocolInformation.protocol}:${protocolInformation.port} ${connectionResult.error?.message}")
                                    retry()
                                }
                            }
                        }
                    }
                }
            })
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
        val launched = appContext.applicationInterface.launchFragment(
            emptyList(),
            FragmentType.ManualModeFailed,
            autoConnectionModeCallback = object : AutoConnectionModeCallback {
                override fun onCancel() {
                    logger.debug("User cancelled manual mode switch. Stopping auto connect.")
                    stop()
                }

                override fun onSwitchToAutoMode() {
                    logger.debug("User switched to auto mode.")
                    // Switch to auto mode
                    preferencesHelper.connectionMode = PreferencesKeyConstants.CONNECTION_MODE_AUTO
                    reset()
                    listOfProtocols.firstOrNull { it.protocol == preferencesHelper.selectedProtocol }?.type =
                        ProtocolConnectionStatus.Failed
                    scope.launch {
                        connectInForeground()
                    }
                }
            })
        if (launched.not()) {
            logger.debug("App is in background. Stopping auto connect.")
            stop()
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
