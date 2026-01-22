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
import com.windscribe.vpn.backend.utils.SelectedLocationType
import com.windscribe.vpn.backend.utils.VPNProfileCreator
import com.windscribe.vpn.commonutils.ResourceHelper
import com.windscribe.vpn.commonutils.WindUtilities
import com.windscribe.vpn.localdatabase.LocalDbInterface
import com.windscribe.vpn.repository.AdvanceParameterRepository
import com.windscribe.vpn.repository.CallResult
import com.windscribe.vpn.repository.UserRepository
import com.windscribe.vpn.state.DeviceStateManager
import com.windscribe.vpn.state.NetworkInfoManager
import com.windscribe.vpn.state.VPNConnectionStateManager
import com.wireguard.android.backend.GoBackend
import com.wireguard.android.backend.Tunnel.State.DOWN
import com.wireguard.android.backend.Tunnel.State.TOGGLE
import com.wireguard.android.backend.Tunnel.State.UP
import com.wireguard.config.Config
import com.wsnet.lib.WSNet
import dagger.Lazy
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.cancellable
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.util.*
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.jvm.optionals.getOrDefault
import kotlin.jvm.optionals.toSet
import kotlin.random.Random
import com.wsnet.lib.WSNetBridgeAPI
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withTimeout


@Singleton
class WireguardBackend @Inject constructor(
    var backend: GoBackend,
    var scope: CoroutineScope,
    var networkInfoManager: NetworkInfoManager,
    vpnStateManager: VPNConnectionStateManager,
    val vpnProfileCreator: VPNProfileCreator,
    val userRepository: Lazy<UserRepository>,
    val deviceStateManager: DeviceStateManager,
    val preferencesHelper: PreferencesHelper,
    advanceParameterRepository: AdvanceParameterRepository,
    val proxyDNSManager: ProxyDNSManager,
    localDbInterface: LocalDbInterface,
    val wgLogger: WgLogger,
    val wgConfigRepository: com.windscribe.vpn.repository.WgConfigRepository,
    private val wsNet: WSNet,
    private val apiManager: IApiCallManager,
    private val bridgeAPI: WSNetBridgeAPI,
    resourceHelper: ResourceHelper
) : VpnBackend(
    scope,
    vpnStateManager,
    preferencesHelper,
    networkInfoManager,
    advanceParameterRepository,
    apiManager,
    localDbInterface,
    bridgeAPI,
    resourceHelper
) {

    var service: WireGuardWrapperService? = null
    private var connectionStateJob: Job? = null

    override var active = false
    private var protectByVPN = AtomicBoolean(false)
    private var wgErrorJob: Job? = null
    private var handshakeTimeoutJob: Job? = null

    init {
        wsNet.httpNetworkManager().setWhitelistSocketsCallback { fds ->
            for (fd in fds) {
                if (active && protectByVPN.get()) {
                    service?.protect(fd)
                }
            }
        }
    }

    fun serviceCreated(vpnService: WireGuardWrapperService) {
        vpnLogger.info("WireGuard service created.")
        service = vpnService
    }

    fun serviceDestroyed() {
        vpnLogger.info("WireGuard service destroyed.")
        service = null
    }

    private val testTunnel = WireGuardTunnel(
        name = appContext.getString(R.string.app_name), config = null, state = DOWN
    )

    private var stickyDisconnectEvent = false
    override fun activate() {
        stickyDisconnectEvent = true
        vpnLogger.info("Activating wireGuard backend.")
        connectionStateJob = scope.launch {
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
        scope.launch {
            wgLogger.captureLogs(appContext)
        }
        startHandshakeTimeoutListener()
        startNetworkInfoObserver()
    }

    private fun startHandshakeTimeoutListener() {
        handshakeTimeoutJob = scope.launch {
            wgLogger.handshakeTimeoutEvent.collect { timeDifference ->
                vpnLogger.warn("Handshake timeout detected: ${timeDifference}ms. Server may have dropped the peer. Waiting for network recovery...")
                waitForNetworkAndCallPinIp()
            }
        }
    }

    private suspend fun waitForNetworkAndCallPinIp() {
        if (deviceStateManager.isOnline.value) {
            handleHandshakeTimeout()
            return
        }
        try {
            withTimeout(5_000) { // 5 seconds timeout
                deviceStateManager.isOnline.collect { isOnline ->
                    if (isOnline) {
                        handleHandshakeTimeout()
                        return@collect
                    } else {
                        vpnLogger.debug("Network not available yet, waiting...")
                    }
                }
            }
        } catch (_: TimeoutCancellationException) {
            vpnLogger.warn("Timeout waiting for network (5s), calling pin IP anyway")
            handleHandshakeTimeout()
        } catch (e: Exception) {
            vpnLogger.error("Error waiting for network: ${e.message}", e)
            handleHandshakeTimeout()
        }
    }

    private suspend fun handleHandshakeTimeout() {
        try {
            val pinnedIp = getPinnedIpForSelectedCity()?.first
            val selectedIp = preferencesHelper.selectedIp
            if (pinnedIp != null && selectedIp != null) {
                withContext(Dispatchers.Main) {
                    bridgeAPI.setConnectedState(false)
                    bridgeAPI.setCurrentHost(selectedIp)
                    bridgeAPI.setIgnoreSslErrors(true)
                    bridgeAPI.setConnectedState(true)
                }
                val pinResult = com.windscribe.vpn.commonutils.Ext.result<Any> {
                    apiManager.pinIp(pinnedIp)
                }
                when (pinResult) {
                    is CallResult.Success -> {
                        vpnLogger.info("Successfully pinned IP $pinnedIp to server $selectedIp after handshake timeout")
                    }
                    is CallResult.Error -> {
                        vpnLogger.error("Failed to pin IP after handshake timeout: ${pinResult.errorMessage}")
                    }
                }
            } else {
                vpnLogger.warn("No pinned IP available for handshake timeout recovery")
            }
        } catch (e: Exception) {
            vpnLogger.error("Error handling handshake timeout: ${e.message}", e)
        }
    }

    override fun deactivate() {
        wgErrorJob?.cancel()
        connectionStateJob?.cancel()
        handshakeTimeoutJob?.cancel()
        wgLogger.stopCapture()
        stopNetworkInfoObserver()
        active = false
        vpnLogger.debug("WireGuard backend deactivated.")
    }

    @Suppress("UNUSED_VARIABLE")
    private fun sendUdpStuffingForWireGuard(
        config: Config
    ) {
        try {
            //Open a port to send the package
            val socket = DatagramSocket(config.`interface`.listenPort.getOrDefault(0))
            val localPort = socket.localPort
            val ntpBuf = ByteArray(48)
            ntpBuf[0] = 0x23 // ntp ver=4, mode=client
            ntpBuf[2] = 0x09 // polling interval=9
            ntpBuf[3] = 0x20 // clock precision
            // repeat up to 5 times.
            val rnds = (1..5).random()
            for (i in 1 until rnds) {
                for (j in 40..47) {
                    ntpBuf[j] = Random.nextInt().toByte()
                }
                for (k in config.peers) {
                    k.endpoint.toSet().forEach {
                        val sendPacket = socket.send(
                            DatagramPacket(
                                ntpBuf, ntpBuf.size,
                                InetAddress.getByName(it.host), it.port
                            )
                        )
                    }
                }
            }
            socket.close()
        } catch (e: Exception) {
            vpnLogger.error("Can't send staffing packet! $e")
        }
    }

    override fun connect(protocolInformation: ProtocolInformation, connectionId: UUID) {
        this.protocolInformation = protocolInformation
        this.connectionId = connectionId
        startConnectionJob()
        scope.launch {
            proxyDNSManager.startControlDIfRequired()
            vpnLogger.info("Getting WireGuard profile.")
            Util.getProfile<WireGuardVpnProfile>()?.let {
                withContext(Dispatchers.IO) {
                    val content = WireGuardVpnProfile.createConfigFromString(it.content)
                    if (preferencesHelper.isAntiCensorshipOn) {
                        sendUdpStuffingForWireGuard(content)
                    }
                    vpnLogger.info("Setting WireGuard state UP.")
                    try {
                        backend.setState(testTunnel, UP, content)
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

    override fun connectivityTestPassed(ip: String) {
        super.connectivityTestPassed(ip)
        if (!reconnecting) {
            startConnectionHealthJob()
        }
    }

    private fun startConnectionHealthJob() {
        if (WindUtilities.getSourceTypeBlocking() == SelectedLocationType.CustomConfiguredProfile) {
            return
        }
    }
}
