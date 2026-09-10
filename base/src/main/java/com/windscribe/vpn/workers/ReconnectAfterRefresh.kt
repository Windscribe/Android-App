/*
 * Copyright (c) 2026 Windscribe Limited.
 */

package com.windscribe.vpn.workers

import com.windscribe.vpn.apppreference.PreferencesHelper
import com.windscribe.vpn.autoconnection.AutoConnectionManager
import com.windscribe.vpn.backend.utils.ConnectSource
import com.windscribe.vpn.repository.LocationRepository
import com.windscribe.vpn.repository.UserRepository
import com.windscribe.vpn.state.ConnectionPolicy.ServiceMode
import com.windscribe.vpn.state.VPNConnectionStateManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.slf4j.LoggerFactory
import javax.inject.Inject
import javax.inject.Singleton

/**
 * What to do about a connection once the data it reads has been refreshed.
 *
 * Callers refresh first and call this last, so the reconnect uses current data whatever changed.
 */
@Singleton
class ReconnectAfterRefresh
    @Inject
    constructor(
        private val locationRepository: LocationRepository,
        private val userRepository: UserRepository,
        private val preferencesHelper: PreferencesHelper,
        private val vpnStateManager: VPNConnectionStateManager,
        private val autoConnectionManager: AutoConnectionManager,
        private val scope: CoroutineScope,
    ) {
        private val logger = LoggerFactory.getLogger("worker")

        suspend fun run() {
            val previousLocation = locationRepository.selectedCity.value
            val updatedLocation = locationRepository.updateLocation()
            if (!userRepository.accountStatusOkay() && !preferencesHelper.isConnectingToConfigured) {
                logger.debug("The account cannot connect; leaving the session alone.")
                return
            }
            val awaitingSession = vpnStateManager.serviceMode.value is ServiceMode.AwaitingSession
            // An auto-connected session never sets the flag, and nothing else clears AwaitingSession.
            if (!preferencesHelper.globalUserConnectionPreference && !awaitingSession) {
                return
            }
            when {
                updatedLocation != previousLocation -> {
                    locationRepository.setSelectedCity(updatedLocation)
                    connect("the selected location changed")
                }

                !locationRepository.isNodeAvailable() -> connect("the connected node is no longer eligible")

                // The drop is still owed an answer, and the refresh above was it.
                awaitingSession -> connect("the tunnel dropped and the account is fine")

                else -> logger.debug("Nothing to reconnect for.")
            }
        }

        /** Through failover. [ConnectSource.App] would be dropped while a tunnel is still up. */
        private fun connect(reason: String) {
            logger.info("Reconnecting because $reason.")
            scope.launch { autoConnectionManager.connectInForeground(ConnectSource.SessionRefresh) }
        }
    }
