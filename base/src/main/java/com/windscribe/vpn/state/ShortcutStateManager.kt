package com.windscribe.vpn.state

import com.google.gson.Gson
import com.windscribe.vpn.api.response.UserSessionResponse
import com.windscribe.vpn.apppreference.PreferencesHelper
import com.windscribe.vpn.autoconnection.AutoConnectionManager
import com.windscribe.vpn.backend.utils.WindVpnController
import com.windscribe.vpn.constants.PreferencesKeyConstants
import com.windscribe.vpn.model.User
import com.windscribe.vpn.repository.UserRepository
import dagger.Lazy
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.slf4j.LoggerFactory

class ShortcutStateManager(
    private var scope: CoroutineScope,
    private var userRepository: Lazy<UserRepository>,
    private var autoConnectionManager: AutoConnectionManager,
    private var networkInfoManager: NetworkInfoManager,
    private val preferencesHelper: PreferencesHelper,
    private val windVpnController: WindVpnController
) {
    private var logger = LoggerFactory.getLogger("shortcut")
    private var initilized = false
    fun connect() {
        if (initilized) {
            preferencesHelper.globalUserConnectionPreference = true
            logger.debug("Connecting from shortcut.")
            windVpnController.connectAsync()
        } else {
            load {
                preferencesHelper.globalUserConnectionPreference = true
                logger.debug("Connecting from shortcut.")
                windVpnController.connectAsync()
            }
        }
    }

    private suspend fun getUserSession(): UserSessionResponse {
        val session = preferencesHelper.getResponseString(PreferencesKeyConstants.GET_SESSION)
        return Gson().fromJson(session, UserSessionResponse::class.java)
    }

    fun load(callback: () -> Unit) {
        val userRepository = userRepository.get()
        scope.launch {
            logger.debug("Loading user info.")
            kotlin.runCatching {
                getUserSession()
            }.onSuccess {
                userRepository.reload(it) { user ->
                    if (user.accountStatus == User.AccountStatus.Okay) {
                        logger.debug("Loading network info.")
                        networkInfoManager.reload(false)
                        logger.debug("Loading connection info.")
                        autoConnectionManager.reset()
                        initilized = true
                        callback()
                    } else {
                        logger.debug("Account status is ${user.accountStatus}.")
                    }
                }
            }.onFailure {
                logger.debug("Unable to load user info.")
            }
        }
    }
}