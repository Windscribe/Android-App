/*
 * Copyright (c) 2021 Windscribe Limited.
 */

package com.windscribe.vpn.backend

import com.windscribe.vpn.ServiceInteractor
import com.windscribe.vpn.backend.utils.ProtocolManager
import com.windscribe.vpn.constants.PreferencesKeyConstants
import com.windscribe.vpn.decoytraffic.DecoyTrafficController
import com.windscribe.vpn.state.AppLifeCycleObserver.Companion.isInForeground
import com.windscribe.vpn.state.VPNConnectionStateManager
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import java.util.concurrent.TimeUnit
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * Base class for Interfacing with VPN Modules.
 * */
@Singleton
abstract class VpnBackend(private val mainScope: CoroutineScope, val stateManager: VPNConnectionStateManager, val vpnServiceInteractor: ServiceInteractor, private val protocolManager: ProtocolManager) {

    val vpnLogger: Logger = LoggerFactory.getLogger("vpn_backend")
    var connectionJob: Job? = null
    var authFailure = false
    var reconnecting = false

    private val connectivityTestJob = CompositeDisposable()

    init {
        mainScope.launch {
            stateManager.state.collectLatest {
                if (it.status == VPNState.Status.Disconnected || it.status == VPNState.Status.Disconnecting) {
                    // Stop existing connectivity test if a disconnect is in process.
                    connectivityTestJob.clear()
                }
            }
        }
    }

    fun startConnectionJob() {
        connectionJob = mainScope.launch {
            vpnLogger.debug("Connection timer started.")
            delay(CONNECTING_WAIT)
            connectionTimeout()
        }
    }

    private suspend fun connectionTimeout() {
        vpnLogger.debug("Connection timeout.")
        deactivate()
        delay(DEACTIVATE_WAIT)
        protocolManager.protocolFailed()
    }

    /**
    Tests network connectivity after a successful VPN connection.
    Tries 3 times after 500ms delay. This delay becomes more important
    In TCP and stealth protocol.
     */
    fun testConnectivity() {
        connectionJob?.cancel()
        connectivityTestJob.clear()
        vpnLogger.debug("Testing internet connectivity.")
        connectivityTestJob.add(
                vpnServiceInteractor.apiManager
                        .checkConnectivityAndIpAddress().delay(500, TimeUnit.MILLISECONDS)
                        .retry(3)
                        .timeout(20, TimeUnit.SECONDS)
                        .delaySubscription(500, TimeUnit.MILLISECONDS)
                        .observeOn(Schedulers.io())
                        .subscribeOn(Schedulers.io())
                        .subscribe(
                                { ip ->
                                    ip.dataClass?.let { it ->
                                        if (Util.validIpAddress(it)) {
                                            val ipAddress: String = Util.getModifiedIpAddress(it.trim { it <= ' ' })
                                            vpnServiceInteractor.preferenceHelper.saveResponseStringData(
                                                    PreferencesKeyConstants.USER_IP,
                                                    ipAddress
                                            )
                                            connectivityTestPassed(it)
                                            reconnecting = false
                                        } else {
                                            failedConnectivityTest()
                                        }
                                    } ?: kotlin.run {
                                        failedConnectivityTest()
                                    }
                                },
                                {
                                    failedConnectivityTest()
                                }
                        )
        )
    }

    fun updateState(vpnState: VPNState) {
        mainScope.launch {
            if (vpnState.status == VPNState.Status.Disconnected && authFailure) {
                authFailure = false
                stateManager.setState(VPNState(VPNState.Status.Disconnected, VPNState.Error.AuthenticationError))
            } else {
                stateManager.setState(vpnState)
            }
        }
    }

    open fun connectivityTestPassed(ip: String) {
        vpnServiceInteractor.preferenceHelper.whitelistOverride = false
        vpnLogger.debug("Connectivity test successful: $ip")
        updateState(VPNState(VPNState.Status.Connected, ip = ip))
        protocolManager.onConnectionSuccessful()
    }

    private fun failedConnectivityTest() {
        // If app is in foreground, try other protocols.
        if (isInForeground && !reconnecting) {
            mainScope.launch {
                connectivityTestJob.clear()
                connectionJob?.cancel()
                deactivate()
                delay(DISCONNECT_DELAY)
                vpnLogger.debug("Connectivity test failed.")
                protocolManager.protocolFailed()
            }
        } else {
            vpnLogger.debug("Connectivity test failed in background.")
            reconnecting = false
            // Consider it connected and will fetch ip on app launch.
            vpnServiceInteractor.preferenceHelper.removeResponseData(PreferencesKeyConstants.USER_IP)
            updateState(VPNState(VPNState.Status.Connected))
        }
    }

    abstract var active: Boolean
    abstract fun activate()
    abstract fun deactivate()
    abstract fun connect()
    abstract suspend fun disconnect()

    companion object {
        var DISCONNECT_DELAY = 100L
        var CONNECTING_WAIT = 30 * 1000L
        var DEACTIVATE_WAIT = 500L
    }
}
