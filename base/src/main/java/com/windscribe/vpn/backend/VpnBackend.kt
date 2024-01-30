/*
 * Copyright (c) 2021 Windscribe Limited.
 */

package com.windscribe.vpn.backend

import com.windscribe.vpn.ServiceInteractor
import com.windscribe.vpn.autoconnection.ProtocolInformation
import com.windscribe.vpn.constants.PreferencesKeyConstants
import com.windscribe.vpn.repository.AdvanceParameterRepository
import com.windscribe.vpn.state.NetworkInfoManager
import com.windscribe.vpn.state.VPNConnectionStateManager
import io.reactivex.Single
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.*
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

/**
 * Base class for Interfacing with VPN Modules.
 * */
@Singleton
abstract class VpnBackend(
        private val mainScope: CoroutineScope,
        val stateManager: VPNConnectionStateManager,
        private val vpnServiceInteractor: ServiceInteractor,
        private val networkInfoManager: NetworkInfoManager,
        private val advanceParameterRepository: AdvanceParameterRepository
) {

    val vpnLogger: Logger = LoggerFactory.getLogger("vpn_backend")
    var connectionJob: Job? = null
    var reconnecting = false
    var protocolInformation: ProtocolInformation? = null
    var connectionId: UUID? = null
    var error: VPNState.Error? = null

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
        val preferredProtocolOn = networkInfoManager.networkInfo?.isPreferredOn ?: false
        if (preferredProtocolOn.not() && vpnServiceInteractor.preferenceHelper.getResponseString(
                        PreferencesKeyConstants.CONNECTION_MODE_KEY
                ) != PreferencesKeyConstants.CONNECTION_MODE_AUTO) {
            vpnLogger.debug("Manual connection mode selected without preferred protocol.")
            return
        }
        connectionJob = mainScope.launch {
            vpnLogger.debug("Connection timer started.")
            delay(CONNECTING_WAIT)
            connectionTimeout()
        }
    }

    private suspend fun connectionTimeout() {
        vpnLogger.debug("Connection timeout.")
        disconnect(
                error = VPNState.Error(
                        error = VPNState.ErrorType.TimeoutError,
                        "connection timeout"
                )
        )
    }

    /**
    Tests network connectivity after a successful VPN connection.
    Tries 3 times after 500ms delay. This delay becomes more important
    In TCP and stealth protocol.
     */
    fun testConnectivity() {
        connectionJob?.cancel()
        connectivityTestJob.clear()
        vpnLogger.debug("Starting connectivity test.")
        val startDelay = advanceParameterRepository.getTunnelStartDelay() ?: 500
        val retryDelay = advanceParameterRepository.getTunnelTestRetryDelay() ?: 500
        // Max Attempts = First attempt + retries
        val maxAttempts = advanceParameterRepository.getTunnelTestAttempts() ?: 3
        var maxRetries = maxAttempts
        if (maxAttempts >= 1) {
            maxRetries -= 1
        }
        var failedAttemptIndex = 0
        connectivityTestJob.add(
                Single.just(true).delay(startDelay, TimeUnit.MILLISECONDS).flatMap {
                    vpnServiceInteractor.apiManager
                            .getConnectedIp()
                            .doOnError {
                                failedAttemptIndex++
                                vpnLogger.debug("Failed Attempt: $failedAttemptIndex")
                            }.retryWhen { error ->
                                return@retryWhen error.take(maxRetries).delay(retryDelay, TimeUnit.MILLISECONDS)
                            }
                }.timeout(20, TimeUnit.SECONDS)
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
        vpnServiceInteractor.preferenceHelper.whitelistOverride = false
        vpnLogger.debug("Connectivity test successful: $ip")
        updateState(VPNState(VPNState.Status.Connected, ip = ip))
        mainScope.launch {
            delay(500)
            reconnecting = false
        }
    }

    private fun failedConnectivityTest() {
        connectivityTestJob.clear()
        connectionJob?.cancel()
        // If app is in foreground, try other protocols.
        if (reconnecting.not()) {
            mainScope.launch {
                vpnLogger.debug("Connectivity test failed.")
                disconnect(
                        error = VPNState.Error(
                                VPNState.ErrorType.ConnectivityTestFailed,
                                "Connectivity test failed."
                        )
                )
            }
        } else {
            vpnLogger.debug("Connectivity test failed in background.")
            // Consider it connected and will fetch ip on app launch.
            vpnServiceInteractor.preferenceHelper.removeResponseData(PreferencesKeyConstants.USER_IP)
            updateState(VPNState(VPNState.Status.Connected))
            mainScope.launch {
                delay(500)
                reconnecting = false
            }
        }
    }

    abstract var active: Boolean
    abstract fun activate()
    abstract fun deactivate()
    abstract fun connect(protocolInformation: ProtocolInformation, connectionId: UUID)
    abstract suspend fun disconnect(error: VPNState.Error? = null)

    companion object {
        var DISCONNECT_DELAY = 1000L
        var CONNECTING_WAIT = 30 * 1000L
        var WG_CONNECTING_WAIT = 20 * 1000L
    }
}
