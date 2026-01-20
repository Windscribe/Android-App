/*
 * Copyright (c) 2021 Windscribe Limited.
 */

package com.windscribe.vpn.repository

import android.content.Intent
import androidx.lifecycle.MutableLiveData
import androidx.work.WorkManager
import com.google.gson.Gson
import com.windscribe.vpn.Windscribe.Companion.appContext
import com.windscribe.vpn.api.IApiCallManager
import com.windscribe.vpn.api.response.UserSessionResponse
import com.windscribe.vpn.apppreference.PreferencesHelper
import com.windscribe.vpn.autoconnection.AutoConnectionManager
import com.windscribe.vpn.backend.Util
import com.windscribe.vpn.backend.utils.WindVpnController
import com.windscribe.vpn.commonutils.WindUtilities
import com.windscribe.vpn.apppreference.PreferencesKeyConstants
import com.windscribe.vpn.localdatabase.LocalDbInterface
import com.windscribe.vpn.model.User
import com.windscribe.vpn.services.sso.GoogleSignInManager
import com.windscribe.vpn.workers.WindScribeWorkManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import org.slf4j.LoggerFactory
import java.util.Date
import java.util.UUID
import javax.inject.Singleton

sealed class UserDataState {
    data class Loading(val status: String) : UserDataState()
    object Success : UserDataState()
    data class Error(val error: String) : UserDataState()
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
    private val googleSignInManager: GoogleSignInManager
) {
    var user = MutableLiveData<User>()
    private val logger = LoggerFactory.getLogger("data")
    private val _userInfo = MutableSharedFlow<User>(replay = 1)
    val userInfo: SharedFlow<User> = _userInfo

    init {
        reload()
    }

    fun reload(
        response: UserSessionResponse? = null, callback: (suspend (user: User) -> Unit)? = null
    ) {
        scope.launch(Dispatchers.IO) {
            response?.let { it ->
                preferenceHelper.getSession = Gson().toJson(it)
                val newUser = User(it)
                user.postValue(newUser)
                _userInfo.emit(newUser)
                preferenceHelper.userStatus = if (newUser.isPro) 1 else 0
                preferenceHelper.userName = newUser.userName
                callback?.invoke(newUser)
            } ?: kotlin.run {
                try {
                    val cachedSessionResponse = preferenceHelper.getSession
                    val userSession =
                        Gson().fromJson(cachedSessionResponse, UserSessionResponse::class.java)
                    user.postValue(User(userSession))
                    _userInfo.emit(User(userSession))
                } catch (ignored: Exception) {
                    logger.info("No user is logged in.")
                }
            }
        }
    }

    fun synchronizedReload() {
        try {
            logger.debug("Loading user info from cache")
            val cachedSessionResponse = preferenceHelper.getSession
            val userSession =
                Gson().fromJson(cachedSessionResponse, UserSessionResponse::class.java)
            user.postValue(User(userSession))
            _userInfo.tryEmit(User(userSession))
        } catch (ignored: Exception) {
            logger.info("No user is logged in.")
        }
    }

    fun whatChanged(userSessionResponse: UserSessionResponse): List<Boolean> {
        return user.value?.let {
            logger.debug("Comparing user information.")
            val newUser = User(userSessionResponse)
            val locationHashChanged = it.locationHash != userSessionResponse.locationHash
            val alcListChanged = it.alcList != newUser.alcList
            val userStatusChanged = it.isPro != newUser.isPro
            val accountStatusChanged = it.accountStatus != newUser.accountStatus
            val sipChanged = it.sipCount != newUser.sipCount
            val migrationRequired = preferenceHelper.migrationRequired
            val emailStatusChanged = it.emailStatus != newUser.emailStatus
            logger.debug("What changed: Server list: $locationHashChanged | Alc: $alcListChanged | Sip: $sipChanged | User Status: $userStatusChanged | Account Status: $accountStatusChanged | Migration: $migrationRequired | Email Status: $emailStatusChanged")
            return listOf(
                alcListChanged or locationHashChanged,
                sipChanged,
                userStatusChanged or accountStatusChanged or migrationRequired,
                emailStatusChanged
            )
        } ?: kotlin.run {
            logger.debug("No user information found to compare.")
            return listOf(false, false, false, false)
        }
    }

    suspend fun logout() {
        scope.launch {
            if (appContext.vpnConnectionStateManager.isVPNActive()) {
                vpnController.disconnectAsync()
            }
            googleSignInManager.signOut {
                logger.debug("Signed out from Google.")
            }
        }.invokeOnCompletion {
            WorkManager.getInstance(appContext).cancelAllWork()
            scope.launch {
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
            }
        }
    }

    private suspend fun onSessionDeleted() {
        scope.launch {
            preferenceHelper.sessionHash = null
            preferenceHelper.globalUserConnectionPreference = false
            WindUtilities.deleteProfileCompletely(appContext)
            autoConnectionManager.reset()
            preferenceHelper.clearAllData()
            localDbInterface.clearAllTables()
            appContext.activeActivity?.let {
                val intent = appContext.applicationInterface.welcomeIntent
                intent.addFlags(
                    Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_NO_HISTORY or Intent.FLAG_ACTIVITY_CLEAR_TASK
                )
                it.startActivity(intent)
                it.finish()
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

    fun prepareDashboard(firebaseToken: String?): Flow<UserDataState> = flow {
        preferenceHelper.loginTime = Date()
        emit(UserDataState.Loading("Getting session"))
        try {
            val result = apiManager.getSessionGeneric(firebaseToken).callResult<UserSessionResponse>()
            when (result) {
                is CallResult.Error -> {
                    logger.debug("Error getting session: ${result.errorMessage}")
                    emit(UserDataState.Error(result.errorMessage))
                    return@flow
                }
                is CallResult.Success -> {
                    logger.debug("Successfully added token $firebaseToken to ${result.data.userName}.")
                    if (preferenceHelper.deviceUuid == null) {
                        logger.debug("No device id is found for the current user, generating and saving UUID")
                        preferenceHelper.deviceUuid = UUID.randomUUID().toString()
                    }
                }
            }
            reload(result.data, null)
            val staticIpCount =result.data.sipCount()
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
