/*
 * Copyright (c) 2021 Windscribe Limited.
 */

package com.windscribe.vpn.backend.ikev2

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
import com.windscribe.vpn.commonutils.ResourceHelper
import com.windscribe.vpn.localdatabase.LocalDbInterface
import com.windscribe.vpn.repository.AdvanceParameterRepository
import com.windscribe.vpn.state.NetworkInfoManager
import com.windscribe.vpn.state.VPNConnectionStateManager
import com.windscribe.vpn.wsnet.WSNetWrapper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
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
class IKev2VpnBackend @Inject constructor(
        var scope: CoroutineScope,
        var networkInfoManager: NetworkInfoManager,
        vpnStateManager: VPNConnectionStateManager,
        var preferencesHelper: PreferencesHelper,
        advanceParameterRepository: AdvanceParameterRepository,
        val proxyDNSManager: ProxyDNSManager,
        apiManager: IApiCallManager,
        localDbInterface: LocalDbInterface,
        wsNetWrapper: WSNetWrapper,
        resourceHelper: ResourceHelper
) : VpnBackend(scope, vpnStateManager, preferencesHelper, networkInfoManager, advanceParameterRepository, apiManager, localDbInterface, wsNetWrapper, resourceHelper) {

    var service: CharonVpnServiceWrapper? = null  // Direct reference like WireGuard
    private var connectionStateJob: Job? = null
    override var active = false

    // StateFlow-based tunnel like WireGuard
    private val ikev2Tunnel = IKev2Tunnel()

    // Sticky flag to prevent initial DOWN state from triggering disconnect (like WireGuard)
    private var stickyDisconnectEvent = false

    private var strongswanInitialized = false

    fun serviceCreated(charonVpnServiceWrapper: CharonVpnServiceWrapper) {
        vpnLogger.info("IKEv2 service created.")
        service = charonVpnServiceWrapper
        initStrongswan()
    }

    private fun initStrongswan() {
        if (strongswanInitialized) {
            vpnLogger.debug("StrongSwan already initialized, skipping.")
            return
        }

        vpnLogger.info("Initializing StrongSwan for the first time.")
        StrongSwanApplication.setContext(appContext)
        StrongSwanApplication.setService(CharonVpnServiceWrapper::class.java)
        init(appContext)
        strongswanInitialized = true
    }

    fun serviceDestroyed() {
        vpnLogger.info("IKEv2 service destroyed.")
        service = null
    }

    /**
     * Returns the tunnel object for state reporting from CharonVpnService
     */
    fun getTunnel() = ikev2Tunnel

    override fun activate() {
        stickyDisconnectEvent = true  // Prevent initial DOWN from triggering disconnect
        vpnLogger.info("Activating IKEv2 backend.")
        // Start observing tunnel state changes (like WireGuard does)
        connectionStateJob = scope.launch {
            ikev2Tunnel.stateFlow.cancellable().collectLatest { state ->
                vpnLogger.info("IKEv2 tunnel state changed to ${state.name}")
                when (state) {
                    IKev2Tunnel.State.DOWN -> {
                        // Only disconnect if not the initial activation event and not reconnecting
                        if (!stickyDisconnectEvent && !reconnecting) {
                            connectionJob?.cancel()
                            updateState(VPNState(Disconnected))
                        }
                    }
                    IKev2Tunnel.State.CONNECTING -> {
                        updateState(VPNState(Connecting))
                    }
                    IKev2Tunnel.State.CONNECTED -> {
                        testConnectivity()
                    }
                    IKev2Tunnel.State.DISCONNECTING -> {
                        updateState(VPNState(VPNState.Status.Disconnecting))
                    }
                }
                stickyDisconnectEvent = false  // Reset after first event
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

    override fun connect(protocolInformation: ProtocolInformation, connectionId: UUID) {
        this.protocolInformation = protocolInformation
        this.connectionId = connectionId
        vpnLogger.info("Connecting to IKEv2 Service.")
        startConnectionJob()
        scope.launch {
            proxyDNSManager.startControlDIfRequired()

            // Start the CharonVpnServiceWrapper service (if not already started)
            val context = appContext.applicationContext
            val intent = android.content.Intent(context, CharonVpnServiceWrapper::class.java)

            try {
                vpnLogger.info("Starting CharonVpnServiceWrapper service.")
                androidx.core.content.ContextCompat.startForegroundService(context, intent)

                // Give service time to start and call serviceCreated()
                delay(100)

                // Now trigger connection
                vpnLogger.info("Triggering connection via service.")
                service?.connect() ?: run {
                    vpnLogger.error("Service is null after starting! Cannot connect.")
                }
            } catch (e: Exception) {
                vpnLogger.error("Failed to start CharonVpnServiceWrapper: ${e.message}")
            }
        }
    }

    override suspend fun disconnect(error: VPNState.Error?) {
        this.error = error
        if (proxyDNSManager.invalidConfig){
            proxyDNSManager.stopControlD()
        }
        connectionJob?.cancel()
        vpnLogger.info("Stopping IKEv2 service.")

        // Stop the VPN tunnel by setting next profile to null
        vpnLogger.info("Setting next profile to null to stop tunnel.")
        service?.setNextProfile(null)

        delay(20)  // Short delay for profile to stop

        // Then close the service cleanly (WireGuard's approach - no startForegroundService!)
        vpnLogger.info("Closing CharonVpnServiceWrapper.")
        service?.close()

        delay(DISCONNECT_DELAY)
        vpnLogger.info("Deactivating IKEv2 backend.")
        deactivate()
    }

    private fun checkLogFileSize(){
        val logFile = File(appContext.filesDir.absolutePath + File.separator + CharonVpnService.LOG_FILE)
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
