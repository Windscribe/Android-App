/*
 * Copyright (c) 2021 Windscribe Limited.
 */

package com.windscribe.vpn.repository

import android.content.Intent
import androidx.work.WorkManager
import com.google.gson.Gson
import com.windscribe.vpn.Windscribe.Companion.appContext
import com.windscribe.vpn.api.IApiCallManager
import com.windscribe.vpn.api.response.UserSessionResponse
import com.windscribe.vpn.apppreference.PreferencesHelper
import com.windscribe.vpn.apppreference.PreferencesKeyConstants
import com.windscribe.vpn.autoconnection.AutoConnectionManager
import com.windscribe.vpn.backend.Util
import com.windscribe.vpn.backend.utils.WindVpnController
import com.windscribe.vpn.commonutils.WindUtilities
import com.windscribe.vpn.localdatabase.LocalDbInterface
import com.windscribe.vpn.model.User
import com.windscribe.vpn.services.sso.GoogleSignInManager
import com.windscribe.vpn.workers.WindScribeWorkManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import org.slf4j.LoggerFactory
import java.util.Date
import java.util.UUID
import javax.inject.Singleton

sealed class UserDataState {
    data class Loading(
        val status: String,
    ) : UserDataState()

    object Success : UserDataState()

    data class Error(
        val error: String,
    ) : UserDataState()
}

@Singleton
class UserRepository(
    private val scope: CoroutineScope,
    private val vpnController: WindVpnController,
    private val autoConnectionManager: AutoConnectionManager,
    private val apiManager: IApiCallManager,
    private val preferenceHelper: PreferencesHelper,
    private val localDbInterface: LocalDbInterface,
    private val workManager: WindScribeWorkManager,
    private val connectionDataRepository: ConnectionDataRepository,
    private val serverListRepository: ServerListRepository,
    private val staticIpRepository: StaticIpRepository,
    private val googleSignInManager: GoogleSignInManager,
    private val unblockWgParamsRepository: UnblockWgParamsRepository,
    private val wgConfigRepository: WgConfigRepository,
) {
    private val _user = MutableStateFlow<User?>(null)
    val user: StateFlow<User?> = _user.asStateFlow()
    private val logger = LoggerFactory.getLogger("data")
    private val logoutMutex = Mutex()

    init {
        reload()
    }

    fun reload(
        response: UserSessionResponse? = null,
        callback: (suspend (user: User) -> Unit)? = null,
    ) {
        // No explicit Dispatchers.IO — applicationScope already runs on IO in production,
        // and tests need the scope's dispatcher so advanceUntilIdle drains the launch.
        scope.launch {
            response?.let {
                preferenceHelper.getSession = Gson().toJson(it)
                val newUser = User(it)
                _user.value = newUser
                preferenceHelper.userStatus = if (newUser.isPro) 1 else 0
                preferenceHelper.userName = newUser.userName
                // Amnezia auto-enable logic
                handleAmneziaAutoEnable(it)
                callback?.invoke(newUser)
            } ?: kotlin.run {
                try {
                    val cachedSessionResponse = preferenceHelper.getSession
                    val userSession =
                        Gson().fromJson(cachedSessionResponse, UserSessionResponse::class.java)
                    _user.value = User(userSession)
                } catch (_: Exception) {
                    logger.info("No user is logged in.")
                }
            }
        }
    }

    fun whatChanged(userSessionResponse: UserSessionResponse): List<Boolean> {
        return user.value?.let {
            val newUser = User(userSessionResponse)
            val alcListChanged = it.alcList != newUser.alcList
            val userStatusChanged = it.isPro != newUser.isPro
            val accountStatusChanged = it.accountStatus != newUser.accountStatus
            val sipChanged = it.sipCount != newUser.sipCount
            val migrationRequired = preferenceHelper.migrationRequired
            val emailStatusChanged = it.emailStatus != newUser.emailStatus

            // Only log if something actually changed
            if (alcListChanged || sipChanged || userStatusChanged || accountStatusChanged || migrationRequired || emailStatusChanged) {
                logger.debug(
                    "Alc: $alcListChanged | Sip: $sipChanged | User Status: $userStatusChanged | Account Status: $accountStatusChanged | Migration: $migrationRequired | Email Status: $emailStatusChanged",
                )
            }
            return listOf(
                alcListChanged,
                sipChanged,
                userStatusChanged or accountStatusChanged,
                migrationRequired,
                emailStatusChanged,
            )
        } ?: kotlin.run {
            logger.debug("No user information found to compare.")
            return listOf(false, false, false, false, false)
        }
    }

    fun logout() {
        // Prevent concurrent logout calls from multiple SessionWorkers or UI triggers
        if (!logoutMutex.tryLock()) {
            logger.debug("Logout already in progress, skipping duplicate call")
            return
        }

        scope
            .launch {
                if (appContext.vpnConnectionStateManager.isVPNActive()) {
                    vpnController.disconnectAsync()
                }
                googleSignInManager.signOut {
                    logger.debug("Signed out from Google.")
                }
            }.invokeOnCompletion {
                WorkManager.getInstance(appContext).cancelAllWork()
                scope.launch {
                    try {
                        logger.debug("Deleting user session.")
                        var attempts = 0
                        val maxAttempts = 3
                        var success = false

                        while (attempts < maxAttempts && !success) {
                            attempts++
                            try {
                                val response = apiManager.deleteSession()
                                response.dataClass?.let {
                                    logger.debug("Successfully deleted user session: ${it.isSuccessful}")
                                    success = true
                                } ?: response.errorClass?.let {
                                    logger.debug("Error deleting session (attempt $attempts/$maxAttempts): ${it.errorMessage}")
                                }
                            } catch (e: Exception) {
                                logger.debug("Unknown error deleting session (attempt $attempts/$maxAttempts): ${e.localizedMessage}")
                            }
                        }

                        if (!success) {
                            logger.debug("Failed to delete session after $maxAttempts attempts")
                        }
                        onSessionDeleted()
                    } finally {
                        logoutMutex.unlock()
                    }
                }
            }
    }

    private fun onSessionDeleted() {
        scope.launch {
            preferenceHelper.sessionHash = null
            preferenceHelper.globalUserConnectionPreference = false
            WindUtilities.deleteProfileCompletely(appContext)
            autoConnectionManager.reset()
            preferenceHelper.clearAllData()
            localDbInterface.clearAllTables()
            appContext.activeActivity?.let {
                try {
                    val intent = appContext.applicationInterface.welcomeIntent
                    if (appContext.applicationInterface.isTV) {
                        intent.addFlags(
                            Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_NO_HISTORY or Intent.FLAG_ACTIVITY_CLEAR_TASK,
                        )
                    }
                    it.startActivity(intent)
                    it.finish()
                } catch (e: Exception) {
                    logger.error("Failed to start welcome activity: ${e.message}")
                    it.finish()
                }
            }
        }
    }

    fun loggedIn(): Boolean {
        return user.value?.let {
            return true
        } ?: false
    }

    fun accountStatusOkay(): Boolean {
        return user.value?.accountStatus?.let {
            return it == User.AccountStatus.Okay
        } ?: false
    }

    /**
     * Auto-enables/disables Amnezia based on server recommendation (only in Auto mode)
     */
    private suspend fun handleAmneziaAutoEnable(sessionResponse: UserSessionResponse) {
        // Only apply autoconfiguration if in Auto mode
        if (preferenceHelper.protocolTweaksMode != PreferencesKeyConstants.PROTOCOL_TWEAKS_AUTO) {
            return
        }
        val configId = sessionResponse.serverInventory?.amneziaWgConfigId
        if (configId.isNullOrEmpty()) {
            // No recommendation - remains in Auto mode but effectively disabled
            logger.info("Auto mode: No server recommendation for Protocol Tweaks")
        } else {
            // Server recommends a config - try to find and select it by ID
            val params = unblockWgParamsRepository.unblockWgParams.value
            val matchingPreset = params.firstOrNull { it.id == configId }

            if (matchingPreset != null) {
                logger.info("Auto mode: Selecting preset ID: ${matchingPreset.id}, title: ${matchingPreset.title}")
                unblockWgParamsRepository.setSelectedUnblockWgParam(matchingPreset.id)
            } else {
                logger.warn("Auto mode: Server recommended config_id '$configId' not found in presets")
            }
        }
    }

    /**
     * Refresh the account after a purchase: re-fetch the session, then update static IPs, connection
     * data and the server list. Emits progress via [onState]. Suspends until done; the caller decides
     * the scope (PurchaseManager runs this on the application scope so it survives screen teardown).
     * Throws on session error so the caller can surface it as a single failure state.
     */
    suspend fun refreshAccount(
        firebaseToken: String? = null,
        onState: suspend (UserDataState) -> Unit = {},
    ) {
        onState(UserDataState.Loading("Getting session"))
        wgConfigRepository.unregisterKey()
        val backup = preferenceHelper.getBackupParameter()
        val sessionResult =
            apiManager.getSessionGeneric(firebaseToken, backup = backup).callResult<UserSessionResponse>()
        val session =
            when (sessionResult) {
                is CallResult.Error -> throw Exception(sessionResult.errorMessage)
                is CallResult.Success -> sessionResult.data
            }
        reload(session, {
            preferenceHelper.migrationRequired = true
            if (session.sipCount() > 0) {
                onState(UserDataState.Loading("Getting static IPs"))
                staticIpRepository.updateFromApi()
            }
            onState(UserDataState.Loading("Getting connection data"))
            connectionDataRepository.update()
            onState(UserDataState.Loading("Getting server list"))
            try {
                serverListRepository.update()
            } catch (e: Exception) {
                logger.error("Failed to update server list: ${e.message}")
            }
        })
    }

    fun prepareDashboard(firebaseToken: String?): Flow<UserDataState> =
        flow {
            preferenceHelper.loginTime = Date()
            emit(UserDataState.Loading("Getting session"))
            unblockWgParamsRepository.update()
            try {
                val backup = preferenceHelper.getBackupParameter()
                val result = apiManager.getSessionGeneric(firebaseToken, backup = backup).callResult<UserSessionResponse>()
                when (result) {
                    is CallResult.Error -> {
                        logger.debug("Error getting session: ${result.errorMessage}")
                        emit(UserDataState.Error(result.errorMessage))
                        return@flow
                    }

                    is CallResult.Success -> {
                        logger.debug("Successfully added token to user account.")
                        if (preferenceHelper.deviceUuid == null) {
                            logger.debug("No device id is found for the current user, generating and saving UUID")
                            preferenceHelper.deviceUuid = UUID.randomUUID().toString()
                        }
                    }
                }
                reload(result.data, null)
                val staticIpCount = result.data.sipCount()
                if (staticIpCount > 0) {
                    emit(UserDataState.Loading("Getting static IPs"))
                    staticIpRepository.updateFromApi()
                }
                emit(UserDataState.Loading("Getting connection data"))
                connectionDataRepository.update()
                emit(UserDataState.Loading("Getting server list"))
                serverListRepository.update()
                if (appContext.vpnConnectionStateManager.isVPNActive()) {
                    vpnController.disconnectAsync()
                }
                Util.removeLastSelectedLocation()
                workManager.onAppStart()
                workManager.onAppMovedToForeground()
                workManager.updateNodeLatencies()
                emit(UserDataState.Success)
            } catch (e: Exception) {
                emit(UserDataState.Error(e.localizedMessage ?: "Unknown error"))
            }
        }
}
