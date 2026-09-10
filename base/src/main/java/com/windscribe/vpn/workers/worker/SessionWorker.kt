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
import com.windscribe.vpn.Windscribe.Companion.appContext
import com.windscribe.vpn.api.IApiCallManager
import com.windscribe.vpn.api.response.UserSessionResponse
import com.windscribe.vpn.apppreference.PreferencesHelper
import com.windscribe.vpn.backend.utils.SYSTEM_DISCONNECT
import com.windscribe.vpn.backend.utils.WindVpnController
import com.windscribe.vpn.constants.NetworkErrorCodes.ERROR_ACCOUNT_DELETED
import com.windscribe.vpn.constants.NetworkErrorCodes.ERROR_LICENCE_INVALIDATED
import com.windscribe.vpn.constants.NetworkErrorCodes.ERROR_LICENCE_SUSPENDED
import com.windscribe.vpn.constants.NetworkErrorCodes.ERROR_RESPONSE_SESSION_INVALID
import com.windscribe.vpn.exceptions.GenericApiException
import com.windscribe.vpn.exceptions.InvalidSessionException
import com.windscribe.vpn.exceptions.LicenceSuspendedException
import com.windscribe.vpn.localdatabase.LocalDbInterface
import com.windscribe.vpn.model.User
import com.windscribe.vpn.repository.CallResult
import com.windscribe.vpn.repository.ConnectionDataRepository
import com.windscribe.vpn.repository.ServerListRepository
import com.windscribe.vpn.repository.UserRepository
import com.windscribe.vpn.repository.WgConfigRepository
import com.windscribe.vpn.state.ConnectionPolicy.SessionEnd
import com.windscribe.vpn.state.VPNConnectionStateManager
import com.windscribe.vpn.workers.ReconnectAfterRefresh
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
        private val serverListRepository: ServerListRepository,
        private val connectionDataRepository: ConnectionDataRepository,
        private val wgConfigRepository: WgConfigRepository,
        private val vpnController: WindVpnController,
        private val vpnStateManager: VPNConnectionStateManager,
        private val reconnectAfterRefresh: ReconnectAfterRefresh,
    ) : CoroutineWorker(context, workerParams) {
        val logger: Logger = LoggerFactory.getLogger("worker")

        override suspend fun doWork(): Result {
            if (!userRepository.loggedIn()) {
                logger.warn("SessionWorker: not logged in, no session fetched.")
                return Result.failure()
            }
            return try {
                val userSession = getSession()
                val changed = userRepository.whatChanged(userSession)
                preferencesHelper.lastSessionErrorCode = NO_SESSION_ERROR
                if (updateIfRequired(changed, userSession)) {
                    Result.success(Data.Builder().putString("data", Gson().toJson(userSession)).build())
                } else {
                    // The reconnect was skipped, and success would leave a held tunnel waiting for
                    // the next periodic run.
                    logger.debug("Could not refresh what a connect reads; retrying.")
                    Result.retry()
                }
            } catch (e: Exception) {
                when (e) {
                    is InvalidSessionException -> {
                        // Include the API message: logging out wipes the stored error code, so this
                        // is the only record of why the user was signed out.
                        logger.debug("Invalid session. Now logging out: ${e.message}")
                        userRepository.logout()
                        Result.failure()
                    }

                    is LicenceSuspendedException -> {
                        // Retry, not logout: the licence code stays and the subscription can be renewed.
                        logger.debug("Licence is suspended.")
                        if (vpnStateManager.isVPNActive()) {
                            vpnController.disconnectAsync(SYSTEM_DISCONNECT)
                            appContext.applicationInterface.showLicenceSuspendedDialog()
                        }
                        Result.retry()
                    }

                    else -> {
                        logger.debug(e.message)
                        Result.retry()
                    }
                }
            }
        }

        /**
         * Suspends until the whole ordered sequence has run: WorkManager may release the process as
         * soon as `doWork` returns, stranding a tunnel waiting on a session that never settled.
         */
        private suspend fun updateIfRequired(
            changed: List<Boolean>,
            userSessionResponse: UserSessionResponse,
        ): Boolean {
            var inputsRefreshed = true
            userRepository.reloadNow(userSessionResponse) {
                // Alc changed
                if (changed[0]) {
                    logger.debug("ALC changed")
                    preferencesHelper.migrationRequired = true
                    wgConfigRepository.unregisterKey()
                }
                // User or account status changed
                if (changed[2]) {
                    logger.debug("User or account status changed.")
                    preferencesHelper.migrationRequired = true
                    wgConfigRepository.unregisterKey()
                }
                // User or account status changed or update required
                if (changed[2] || changed[3]) {
                    workManager.updateStaticIpList()
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
                if (refreshConnectionInputs(changed, userSessionResponse)) {
                    reconnectAfterRefresh.run()
                } else {
                    inputsRefreshed = false
                }
            }
            return inputsRefreshed
        }

        /**
         * Everything a connect reads, refreshed in the order it is read, before anything reconnects.
         *
         * Run here rather than enqueued: separate workers finish in an order nobody controls.
         *
         * @return whether that data can be trusted. False only when a refresh failed.
         */
        private suspend fun refreshConnectionInputs(
            changed: List<Boolean>,
            response: UserSessionResponse,
        ): Boolean {
            val accountChanged = changed[0] || changed[2] || changed[3]
            // A pending delta carries node changes, including the fd=1 force disconnect flags.
            val serverListStale = accountChanged || response.serverInventory?.hasPendingDelta() == true
            if (!serverListStale && !accountChanged) {
                // The refresh is still the answer a dropped tunnel is waiting for.
                return true
            }
            return try {
                if (serverListStale) {
                    serverListRepository.update()
                    logger.debug("Server list is current.")
                }
                if (accountChanged) {
                    connectionDataRepository.update()
                    logger.debug("Server credentials are current.")
                }
                true
            } catch (e: Exception) {
                logger.debug("Could not refresh what a connect reads: ${e.message}")
                false
            }
        }

        private suspend fun getSession(): UserSessionResponse {
            val backup = preferencesHelper.getBackupParameter()
            val response =
                apiCallManager
                    .getSessionGeneric(null, backup = backup)
                    .callResult<UserSessionResponse>()
            when (response) {
                is CallResult.Error -> {
                    preferencesHelper.lastSessionErrorCode = response.code
                    throw when (response.code) {
                        ERROR_RESPONSE_SESSION_INVALID,
                        ERROR_ACCOUNT_DELETED,
                        ERROR_LICENCE_INVALIDATED,
                        -> InvalidSessionException("Session request Success: ${response.errorMessage}")

                        ERROR_LICENCE_SUSPENDED -> LicenceSuspendedException(response.errorMessage)

                        else -> GenericApiException(response.errorMessage)
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
                    User.AccountStatus.Banned, User.AccountStatus.Expired ->
                        vpnStateManager.isVPNConnected() && !preferencesHelper.isConnectingToConfigured

                    else -> false
                }
            if (shouldDisconnect) {
                logger.info("Disconnecting due to account status: ${user.accountStatus}")
                vpnController.disconnectAsync(SYSTEM_DISCONNECT)
            }
            if (user.accountStatus == User.AccountStatus.Banned || user.accountStatus == User.AccountStatus.Expired) {
                wgConfigRepository.unregisterKey()
                // Not for a custom config: the later reconcile and app-update paths read this flag
                // to decide whether to restore that tunnel.
                if (!preferencesHelper.isConnectingToConfigured) {
                    preferencesHelper.globalUserConnectionPreference = false
                }
                // The disconnect above re-runs the policy on its way out; with none to end, nothing does.
                if (!shouldDisconnect && !vpnStateManager.isTunnelUp()) {
                    logger.info("Account is ${user.accountStatus} and no session is left to end; settling the service.")
                    vpnController.endSession(SessionEnd.SystemDisconnect)
                }
            } else if (user.accountStatus == User.AccountStatus.Okay) {
                vpnController.onAccountRecovered()
            }
        }

        companion object {
            private const val NO_SESSION_ERROR = -1
        }
    }
