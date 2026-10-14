package com.windscribe.vpn.state

import com.google.gson.Gson
import com.windscribe.vpn.api.response.UserSessionResponse
import com.windscribe.vpn.apppreference.PreferencesHelper
import com.windscribe.vpn.autoconnection.AutoConnectionManager
import com.windscribe.vpn.backend.utils.ConnectSource
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
) {
    private var logger = LoggerFactory.getLogger("shortcut")
    private var initilized = false

    /**
     * Connects from outside the app — the notification, the quick tile, a shortcut, or the system
     * starting the service for Always-on.
     *
     * Goes through [AutoConnectionManager.connectInForeground] so these get the same protocol
     * failover a tap inside the app does. It advances without the dialog when there is no Activity.
     */
    fun connect(source: ConnectSource) {
        if (initilized) {
            startConnect(source)
        } else {
            load { startConnect(source) }
        }
    }

    /**
     * [connect], but suspends until the bootstrap has finished and the attempt has been dispatched —
     * for BootWorker, where returning early lets the process go with the cold start unfinished.
     *
     * @return false when nothing was dispatched: no cached session, or an account that cannot connect.
     */
    suspend fun connectNow(source: ConnectSource): Boolean {
        if (!initilized && !loadNow()) {
            return false
        }
        startConnect(source)
        return true
    }

    private fun startConnect(source: ConnectSource) {
        preferencesHelper.globalUserConnectionPreference = true
        logger.debug("Connecting from shortcut ($source).")
        scope.launch { autoConnectionManager.connectInForeground(source) }
    }

    private suspend fun getUserSession(): UserSessionResponse {
        val session = preferencesHelper.getSession
        return Gson().fromJson(session, UserSessionResponse::class.java)
    }

    fun load(callback: () -> Unit) {
        scope.launch {
            if (loadNow()) {
                callback()
            }
        }
    }

    /** @return whether the app is ready to connect. */
    private suspend fun loadNow(): Boolean {
        logger.debug("Loading user info.")
        val session =
            runCatching { getUserSession() }.getOrElse {
                logger.debug("Unable to load user info.")
                return false
            }
        var ready = false
        userRepository.get().reloadNow(session) { user ->
            if (user.accountStatus == User.AccountStatus.Okay ||
                preferencesHelper.isConnectingToConfigured
            ) {
                logger.debug("Loading network info.")
                networkInfoManager.reload()
                logger.debug("Loading connection info.")
                autoConnectionManager.reset()
                initilized = true
                ready = true
            } else {
                logger.debug("Account status is ${user.accountStatus}.")
            }
        }
        return ready
    }
}
