/*
 * Copyright (c) 2021 Windscribe Limited.
 */

package com.windscribe.vpn.workers.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.WorkerParameters
import com.google.gson.Gson
import com.windscribe.vpn.api.IApiCallManager
import com.windscribe.vpn.api.response.UserSessionResponse
import com.windscribe.vpn.apppreference.PreferencesHelper
import com.windscribe.vpn.backend.utils.WindVpnController
import com.windscribe.vpn.constants.NetworkErrorCodes
import com.windscribe.vpn.exceptions.GenericApiException
import com.windscribe.vpn.exceptions.InvalidSessionException
import com.windscribe.vpn.localdatabase.LocalDbInterface
import com.windscribe.vpn.model.User
import com.windscribe.vpn.repository.CallResult
import com.windscribe.vpn.repository.LocationRepository
import com.windscribe.vpn.repository.UserRepository
import com.windscribe.vpn.repository.WgConfigRepository
import com.windscribe.vpn.state.VPNConnectionStateManager
import com.windscribe.vpn.workers.WindScribeWorkManager
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import org.slf4j.Logger
import org.slf4j.LoggerFactory

@HiltWorker
class SessionWorker
    @AssistedInject
    constructor(
        @Assisted context: Context,
        @Assisted workerParams: WorkerParameters,
        private val preferencesHelper: PreferencesHelper,
        private val userRepository: UserRepository,
        private val apiCallManager: IApiCallManager,
        private val workManager: WindScribeWorkManager,
        private val localDbInterface: LocalDbInterface,
        private val locationRepository: LocationRepository,
        private val wgConfigRepository: WgConfigRepository,
        private val vpnController: WindVpnController,
        private val vpnStateManager: VPNConnectionStateManager,
    ) : CoroutineWorker(context, workerParams) {
        val logger: Logger = LoggerFactory.getLogger("worker")

        override suspend fun doWork(): Result {
            if (!userRepository.loggedIn()) return Result.failure()
            return try {
                val userSession = getSession()
                val changed = userRepository.whatChanged(userSession)
                updateIfRequired(changed, userSession)
                Result.success(Data.Builder().putString("data", Gson().toJson(userSession)).build())
            } catch (e: Exception) {
                if (e is InvalidSessionException) {
                    logger.debug("Invalid session. Now logging out.")
                    userRepository.logout()
                    Result.failure()
                } else {
                    logger.debug(e.message)
                    Result.retry()
                }
            }
        }

        private fun updateIfRequired(
            changed: List<Boolean>,
            userSessionResponse: UserSessionResponse,
        ) {
            userRepository.reload(userSessionResponse) {
                // Alc changed
                if (changed[0]) {
                    logger.debug("ALC changed")
                    preferencesHelper.migrationRequired = true
                    wgConfigRepository.unregisterKey()
                    workManager.updateCredentialsUpdate()
                }
                // User or account status changed
                if (changed[2]) {
                    logger.debug("User or account status changed.")
                    preferencesHelper.migrationRequired = true
                    wgConfigRepository.unregisterKey()
                }
                // Update server list when: server list changed, account changed, or migration required
                // All these cases set migrationRequired = true which forces full server refresh
                if (changed[0] || changed[2] || changed[3]) {
                    workManager.updateServerList()
                }

                // User or account status changed or update required
                if (changed[2] || changed[3]) {
                    workManager.updateStaticIpList()
                    workManager.updateCredentialsUpdate()
                    workManager.updateNotifications()
                }
                val storedSip = localDbInterface.getAllStaticRegions()
                val hasStaticCredential = storedSip.firstOrNull()?.credentials
                val storedSipCount = storedSip.size
                // Static ip changed
                if (storedSipCount != it.sipCount || (it.sipCount > 0 && hasStaticCredential == null)) {
                    logger.debug("Static ip changed.")
                    workManager.updateStaticIpList()
                }
                handleAccountStatusChange(it)
                if (!changed[0] && !changed[2] && !changed[3]) {
                    handleTunnelRecovery()
                }
            }
        }

        private fun handleTunnelRecovery() {
            val vpnState = vpnStateManager.state.value
            if (vpnState.error?.error == com.windscribe.vpn.backend.VPNState.ErrorType.BrokenTunnel &&
                preferencesHelper.globalUserConnectionPreference &&
                vpnState.status == com.windscribe.vpn.backend.VPNState.Status.Disconnected
            ) {
                logger.info("Reconnecting after tunnel recovery")
                vpnController.connectAsync()
            }
        }

        private suspend fun getSession(): UserSessionResponse {
            val backup = preferencesHelper.getBackupParameter()
            val response = apiCallManager.getSessionGeneric(null, backup = backup).callResult<UserSessionResponse>()
            when (response) {
                is CallResult.Error -> {
                    if (response.code == NetworkErrorCodes.ERROR_RESPONSE_SESSION_INVALID) {
                        throw InvalidSessionException("Session request Success: Invalid session.")
                    } else {
                        throw GenericApiException(response.errorMessage)
                    }
                }

                is CallResult.Success -> {
                    return response.data
                }
            }
        }

        private fun handleAccountStatusChange(user: User) {
            val shouldDisconnect =
                when (user.accountStatus) {
                    User.AccountStatus.Banned -> vpnStateManager.isVPNConnected()
                    User.AccountStatus.Expired -> vpnStateManager.isVPNConnected() && !preferencesHelper.isConnectingToConfigured
                    else -> false
                }
            if (shouldDisconnect) {
                logger.info("Disconnecting due to account status: ${user.accountStatus}")
                vpnController.disconnectAsync()
            }
            if (user.accountStatus == User.AccountStatus.Banned || user.accountStatus == User.AccountStatus.Expired) {
                wgConfigRepository.unregisterKey()
                preferencesHelper.globalUserConnectionPreference = false
            }
        }
    }
