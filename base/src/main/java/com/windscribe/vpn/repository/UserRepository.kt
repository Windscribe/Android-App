/*
 * Copyright (c) 2021 Windscribe Limited.
 */

package com.windscribe.vpn.repository

import android.content.Intent
import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.work.WorkManager
import com.google.gson.Gson
import com.windscribe.vpn.ServiceInteractor
import com.windscribe.vpn.Windscribe.Companion.appContext
import com.windscribe.vpn.api.response.UserSessionResponse
import com.windscribe.vpn.backend.utils.ProtocolManager
import com.windscribe.vpn.backend.utils.WindVpnController
import com.windscribe.vpn.commonutils.WindUtilities
import com.windscribe.vpn.constants.PreferencesKeyConstants
import com.windscribe.vpn.model.User
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.rx2.await
import org.slf4j.LoggerFactory

@Singleton
class UserRepository(private val scope: CoroutineScope, private val serviceInteractor: ServiceInteractor, private val vpnController: WindVpnController, private val protocolManager: ProtocolManager) {
    var user = MutableLiveData<User>()
    private val logger = LoggerFactory.getLogger("user_repo")

    init {
        logger.debug("Starting user repository.")
        reload()
    }

    fun reload(response: UserSessionResponse? = null, callback:(suspend (user:User)->Unit)? = null) {
        scope.launch(Dispatchers.IO) {
            response?.let { it ->
                serviceInteractor.preferenceHelper.saveResponseStringData(PreferencesKeyConstants.GET_SESSION, Gson().toJson(it))
                val newUser = User(it)
                user.postValue(newUser)
                serviceInteractor.preferenceHelper.userStatus = if (newUser.isPro) 1 else 0
                serviceInteractor.preferenceHelper.userName = newUser.userName
                callback?.invoke(newUser)
            } ?: kotlin.run {
                try {
                    logger.debug("Loading user info from cache")
                    val cachedSessionResponse = serviceInteractor.preferenceHelper.getResponseString(PreferencesKeyConstants.GET_SESSION)
                    val userSession = Gson().fromJson(cachedSessionResponse, UserSessionResponse::class.java)
                    user.postValue(User(userSession))
                } catch (e: Exception) {
                    logger.debug("Error loading user info: ${e.message}")
                }
            }
        }
    }

    fun synchronizedReload(){
        try {
            logger.debug("Loading user info from cache")
            val cachedSessionResponse = serviceInteractor.preferenceHelper.getResponseString(PreferencesKeyConstants.GET_SESSION)
            val userSession = Gson().fromJson(cachedSessionResponse, UserSessionResponse::class.java)
            user.postValue(User(userSession))
        } catch (e: Exception) {
            logger.debug("Error loading user info: ${e.message}")
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
            val migrationRequired = serviceInteractor.preferenceHelper.migrationRequired
            val emailStatusChanged = it.emailStatus != newUser.emailStatus
            logger.info("What changed: Server list: $locationHashChanged | Alc: $alcListChanged | Sip: $sipChanged | User Status: $userStatusChanged | Account Status: $accountStatusChanged | Migration: $migrationRequired | Email Status: $emailStatusChanged")
            return listOf(alcListChanged or locationHashChanged, sipChanged, userStatusChanged or accountStatusChanged or migrationRequired, emailStatusChanged)
        } ?: kotlin.run {
            logger.debug("No user information found to compare.")
            return listOf(false, false, false, false)
        }
    }

    suspend fun logout() {
        scope.launch {
            if (appContext.vpnConnectionStateManager.isVPNActive()) {
                vpnController.disconnect()
            }
        }.invokeOnCompletion {
            WorkManager.getInstance(appContext).cancelAllWork()
            scope.launch {
                logger.debug("Deleting user session.")
                try {
                    val response = serviceInteractor.apiManager.deleteSession().retry(3).await()
                    response.dataClass?.let {
                        logger.debug("Successfully deleted user session:" + it.isSuccessful)
                    } ?: response.errorClass?.let {
                        logger.debug("Error deleting session: ${it.errorMessage}")
                    }
                } catch (e: Exception) {
                    logger.debug("Unknown error deleting session.${e.localizedMessage}")
                }
                onSessionDeleted()
            }
        }
    }

    private suspend fun onSessionDeleted() {
        scope.launch {
            serviceInteractor.preferenceHelper.sessionHash = null
            serviceInteractor.preferenceHelper.globalUserConnectionPreference = false
            WindUtilities.deleteProfileCompletely(appContext).await()
            protocolManager.loadProtocolConfigs()
            serviceInteractor.clearData()
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

    fun loggedIn():Boolean{
        return user.value?.let {
            return true
        }?:false
    }

    fun accountStatusOkay():Boolean{
        return user.value?.accountStatus?.let {
            return it == User.AccountStatus.Okay
        }?:false
    }
}
