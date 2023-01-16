/*
 * Copyright (c) 2021 Windscribe Limited.
 */

package com.windscribe.vpn.backend.wireguard

import android.content.Context
import android.os.Build
import android.os.PowerManager
import com.windscribe.vpn.ServiceInteractor
import com.windscribe.vpn.Windscribe.Companion.appContext
import com.windscribe.vpn.autoconnection.ProtocolInformation
import com.windscribe.vpn.backend.Util
import com.windscribe.vpn.backend.VPNState
import com.windscribe.vpn.backend.VPNState.Status.Connecting
import com.windscribe.vpn.backend.VPNState.Status.Disconnected
import com.windscribe.vpn.backend.VpnBackend
import com.windscribe.vpn.backend.utils.SelectedLocationType
import com.windscribe.vpn.backend.utils.VPNProfileCreator
import com.windscribe.vpn.commonutils.WindUtilities
import com.windscribe.vpn.constants.NetworkErrorCodes.EXPIRED_OR_BANNED_ACCOUNT
import com.windscribe.vpn.model.User
import com.windscribe.vpn.repository.CallResult
import com.windscribe.vpn.repository.UserRepository
import com.windscribe.vpn.state.DeviceStateManager
import com.windscribe.vpn.state.NetworkInfoManager
import com.windscribe.vpn.state.VPNConnectionStateManager
import com.wireguard.android.backend.GoBackend
import com.wireguard.android.backend.GoBackend.wgGetConfig
import com.wireguard.android.backend.Tunnel.State.*
import dagger.Lazy
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.cancellable
import kotlinx.coroutines.flow.collectIndexed
import kotlinx.coroutines.flow.collectLatest
import java.util.*
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Singleton
class WireguardBackend(
    var backend: GoBackend,
    var scope: CoroutineScope,
    var networkInfoManager: NetworkInfoManager,
    var vpnStateManager: VPNConnectionStateManager,
    var serviceInteractor: ServiceInteractor,
    val vpnProfileCreator: VPNProfileCreator,
    val userRepository: Lazy<UserRepository>,
    val deviceStateManager: DeviceStateManager
) : VpnBackend(scope, vpnStateManager, serviceInteractor) {

    var service: WireGuardWrapperService? = null
    var connectionStateJob: Job? = null
    private var connectionHealthJob: Job? = null
    private var deviceIdleJob: Job? = null
    private var appActivationJob: Job? = null
    private var healthJob: Job? = null
    override var active = false
    private val maxHandshakeTimeInSeconds = 180L

    fun serviceCreated(vpnService: WireGuardWrapperService) {
        vpnLogger.debug("WireGuard service created.")
        service = vpnService
    }

    fun serviceDestroyed() {
        vpnLogger.debug("WireGuard service destroyed.")
        service = null
    }

    val testTunnel = WireGuardTunnel(
            name = "windscribe-wireguard",
            config = null,
            state = DOWN
    )

    private var stickyDisconnectEvent = false
    override fun activate() {
        stickyDisconnectEvent = true
        vpnLogger.debug("Activating wireGuard backend.")
        connectionStateJob = scope.launch {
            testTunnel.stateFlow.cancellable().collectLatest {
                vpnLogger.debug("WireGuard tunnel state changed to ${it.name}")
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
        vpnLogger.debug("WireGuard backend activated.")
        active = true
    }

    override fun deactivate() {
        connectionStateJob?.cancel()
        active = false
        vpnLogger.debug("WireGuard backend deactivated.")
    }

    override fun connect(protocolInformation: ProtocolInformation, connectionId: UUID) {
        this.protocolInformation = protocolInformation
        this.connectionId = connectionId
        startConnectionJob()
        scope.launch {
            vpnLogger.debug("Getting WireGuard profile.")
            Util.getProfile<WireGuardVpnProfile>()?.let {
                withContext(Dispatchers.IO) {
                    val content = WireGuardVpnProfile.createConfigFromString(it.content)
                    vpnLogger.debug("Setting WireGuard state UP.")
                    try {
                        backend.setState(testTunnel, UP, content)
                    } catch (e: Exception) {
                        vpnLogger.error("Exception while setting WireGuard state UP.", e)
                        updateState(VPNState(Disconnected))
                    }
                }
            } ?: kotlin.run {
                vpnLogger.debug("Failed to get WireGuard profile.")
                updateState(VPNState(Disconnected))
            }
        }
    }

    override suspend fun disconnect(error: VPNState.Error?) {
        this.error = error
        deviceIdleJob?.cancel()
        appActivationJob?.cancel()
        healthJob?.cancel()
        connectionHealthJob?.cancel()
        connectionJob?.cancel()
        vpnLogger.debug("Stopping WireGuard service.")
        service?.close()
        delay(20)
        vpnLogger.debug("Setting WireGuard tunnel state down.")
        backend.setState(testTunnel, DOWN, null)
        delay(DISCONNECT_DELAY)
        vpnLogger.debug("Deactivating WireGuard backend.")
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
        connectionHealthJob = scope.launch {
            while (true) {
                delay(1000 * 60)
                vpnLogger.debug("Checking tunnel health.")
                healthJob?.cancel()
                healthJob = scope.launch { checkTunnelHealth() }
            }
        }
        deviceIdleJob = scope.launch {
            deviceStateManager.isDeviceInteractive.collectLatest {
                if (it) {
                    vpnLogger.debug("Device state changed: Checking tunnel health.")
                    healthJob?.cancel()
                    healthJob = scope.launch { checkLastHandshake() }
                }
            }
        }
        appActivationJob = scope.launch {
            appContext.appLifeCycleObserver.appActivationState.collectIndexed { index, value ->
                if (index > 0) {
                    vpnLogger.debug("App state changed: Checking tunnel health.")
                    healthJob?.cancel()
                    healthJob = scope.launch { checkTunnelHealth() }
                }
            }
        }
    }

    private suspend fun checkLastHandshake() {
        backend.handshakeNSecAgo()?.let { lastHandshakeTimeInSeconds ->
            vpnLogger.debug("Last Wg handshake $lastHandshakeTimeInSeconds seconds ago")
            if (active && WindUtilities.isOnline() && lastHandshakeTimeInSeconds > maxHandshakeTimeInSeconds && userRepository.get() != null && userRepository.get().user.value?.accountStatus == User.AccountStatus.Okay) {
                checkTunnelHealth()
            }
        } ?: kotlin.run {
            vpnLogger.debug("Unable to get handshake time from wg binary..")
        }
    }

    private suspend fun checkTunnelHealth() {
        vpnLogger.debug("Requesting new interface address.")
        Util.getProfile<WireGuardVpnProfile>()?.content?.let {
            vpnLogger.debug("Creating config from saved params")
            val pm = appContext.getSystemService(Context.POWER_SERVICE) as PowerManager
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                vpnLogger.debug(
                    "Power options: Interactive:${pm.isInteractive} Power Save mode: ${pm.isPowerSaveMode} Ignore battery optimization: ${
                        pm.isIgnoringBatteryOptimizations(
                            appContext.packageName
                        )
                    } Device Idle: ${pm.isDeviceIdleMode}"
                )
            }
            try {
                val config = WireGuardVpnProfile.createConfigFromString(it)
                when (val response = vpnProfileCreator.updateWireGuardConfig(config)) {
                    is CallResult.Success -> {
                        if (config.`interface`.addresses.first() != response.data.`interface`.addresses.first()) {
                            vpnLogger.debug("${config.`interface`.addresses.first()} > ${response.data.`interface`.addresses.first()}")
                            reconnecting = true
                            backend.setState(testTunnel, UP, response.data)
                        } else {
                            vpnLogger.debug("Interface address unchanged.")
                        }
                    }
                    is CallResult.Error -> {
                        vpnLogger.debug("Failed to create wg params :$response")
                        when (response.code) {
                            EXPIRED_OR_BANNED_ACCOUNT -> {
                                appContext.vpnController.disconnectAsync()
                            }
                            else -> {}
                        }
                    }
                }
            }catch (e:Exception){
                vpnLogger.debug("Failed to create wg params :${e.message}")
            }
        }
    }

    private fun GoBackend.handshakeNSecAgo(): Long? {
        if (currentTunnelHandle != -1) {
            val config = wgGetConfig(currentTunnelHandle)
            val lines = config?.split("\n")
            val timeInSecondsPassed = lines?.firstOrNull {
                it.startsWith("last_handshake_time_sec")
            }?.split("=")?.get(1)?.toLong()
            val currentTimeSeconds = TimeUnit.SECONDS.convert(System.currentTimeMillis(), TimeUnit.MILLISECONDS)
            return timeInSecondsPassed?.let {
                currentTimeSeconds - it
            }
        }
        return null
    }
}
