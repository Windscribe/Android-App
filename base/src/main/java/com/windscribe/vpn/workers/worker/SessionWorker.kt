/*
 * Copyright (c) 2021 Windscribe Limited.
 */

package com.windscribe.vpn.workers.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.WorkerParameters
import com.google.gson.Gson
import com.windscribe.vpn.Windscribe.Companion.appContext
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
import com.windscribe.vpn.state.PreferenceChangeObserver
import com.windscribe.vpn.state.VPNConnectionStateManager
import com.windscribe.vpn.workers.WindScribeWorkManager
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import javax.inject.Inject

class SessionWorker(context: Context, workerParams: WorkerParameters) : CoroutineWorker(context, workerParams) {

    val logger: Logger = LoggerFactory.getLogger("worker")

    @Inject
    lateinit var preferencesHelper: PreferencesHelper

    @Inject
    lateinit var userRepository: UserRepository

    @Inject
    lateinit var apiCallManager: IApiCallManager

    @Inject
    lateinit var workManager: WindScribeWorkManager

    @Inject
    lateinit var localDbInterface: LocalDbInterface

    @Inject
    lateinit var preferenceChangeObserver: PreferenceChangeObserver

    @Inject
    lateinit var locationRepository: LocationRepository

    @Inject
    lateinit var wgConfigRepository: WgConfigRepository

    @Inject
    lateinit var vpnController: WindVpnController

    @Inject
    lateinit var vpnStateManager: VPNConnectionStateManager

    init {
        appContext.applicationComponent.inject(this)
    }

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

    private suspend fun updateIfRequired(changed: List<Boolean>, userSessionResponse: UserSessionResponse) {
        userRepository.reload(userSessionResponse) {
            if (changed[0]) {
                workManager.updateServerList()
            }
            val storedSipCount = localDbInterface.getAllStaticRegions().size
            logger.debug("Sip: stored: $storedSipCount updated: ${it.sipCount}")
            if (storedSipCount != it.sipCount) {
                workManager.updateStaticIpList()
            }
            val forceUpdate = inputData.getBoolean("forceUpdate", false)
            if (changed[2] || forceUpdate) {
                workManager.updateServerList()
                workManager.updateStaticIpList()
                workManager.updateCredentialsUpdate()
                workManager.updateNotifications()
            }
            if (changed[3]) {
                preferenceChangeObserver.postEmailStatusChange()
            }
            handleAccountStatusChange(it)
        }
    }

    private suspend fun getSession(): UserSessionResponse {
        val response = apiCallManager.getSessionGeneric(null).callResult<UserSessionResponse>()
        when(response) {
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
        logger.info("User account status: ${user.accountStatus} is VPN Connected: ${vpnStateManager.isVPNConnected()}")
        val shouldDisconnect = when (user.accountStatus) {
            User.AccountStatus.Banned -> vpnStateManager.isVPNConnected()
            User.AccountStatus.Expired -> vpnStateManager.isVPNConnected() && !preferencesHelper.isConnectingToConfiguredLocation()
            else -> false
        }
        if (shouldDisconnect) {
            logger.info("Disconnecting...")
            vpnController.disconnectAsync()
        }
        if (user.accountStatus == User.AccountStatus.Banned || user.accountStatus == User.AccountStatus.Expired) {
            wgConfigRepository.deleteKeys()
            preferencesHelper.globalUserConnectionPreference = false
        }
    }
}
