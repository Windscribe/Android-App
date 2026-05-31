package com.windscribe.mobile.ui.home

import android.content.Context
import android.os.PowerManager
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.windscribe.vpn.Windscribe.Companion.appContext
import com.windscribe.vpn.apppreference.PreferencesHelper
import com.windscribe.vpn.model.User
import com.windscribe.vpn.repository.CheckUpdateRepository
import com.windscribe.vpn.repository.UserRepository
import com.windscribe.vpn.state.AppLifeCycleObserver
import com.windscribe.vpn.state.VPNConnectionStateManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.launch
import org.slf4j.LoggerFactory
import java.util.Locale
import javax.inject.Inject

sealed class HomeGoto {
    object Upgrade : HomeGoto()

    data class Expired(
        val date: String,
    ) : HomeGoto()

    object Banned : HomeGoto()

    object Downgraded : HomeGoto()

    object PowerWhitelist : HomeGoto()

    object ShareAppLink : HomeGoto()

    object LocationMaintenance : HomeGoto()

    data class EditCustomConfig(
        val id: Int,
        val connect: Boolean,
    ) : HomeGoto()

    data class UpdateAvailable(
        val latestVersion: String?,
        val force: Boolean = false,
    ) : HomeGoto()

    object MainMenu : HomeGoto()

    object None : HomeGoto()

    data class IpActionError(
        val message: String,
        val description: String,
    ) : HomeGoto()
}

sealed class UserState {
    object Loading : UserState()

    object Pro : UserState()

    object UnlimitedData : UserState()

    data class Free(
        val dataLeft: String,
        val dataLeftAngle: Float,
        val isGhost: Boolean,
        val registerDays: Long,
    ) : UserState()
}

abstract class HomeViewmodel : ViewModel() {
    abstract val goto: SharedFlow<HomeGoto>
    abstract val userState: StateFlow<UserState>
    abstract val hapticFeedbackEnabled: StateFlow<Boolean>
    abstract val showLocationLoad: StateFlow<Boolean>
    abstract val hideIp: StateFlow<Boolean>
    abstract val hideNetworkName: StateFlow<Boolean>

    abstract fun onMainMenuClick()

    abstract fun onGoToHandled()

    abstract fun onHideIpClick()

    abstract fun onHideNetworkNameClick()
}

@HiltViewModel
class HomeViewmodelImpl
    @Inject
    constructor(
        private val vpnConnectionStateManager: VPNConnectionStateManager,
        private val userRepository: UserRepository,
        private val preferences: PreferencesHelper,
        private val checkUpdateRepository: CheckUpdateRepository,
        private val appLifeCycleObserver: AppLifeCycleObserver,
    ) : HomeViewmodel() {
        private val _goto = MutableSharedFlow<HomeGoto>(replay = 0)
        override val goto: SharedFlow<HomeGoto> = _goto
        private val _userState = MutableStateFlow<UserState>(UserState.Loading)
        override val userState: StateFlow<UserState> = _userState
        private val _hapticFeedbackEnabled = MutableStateFlow(preferences.isHapticFeedbackEnabled)
        override val hapticFeedbackEnabled: StateFlow<Boolean> = _hapticFeedbackEnabled
        private val _showLocationLoad = MutableStateFlow(preferences.isShowLocationHealthEnabled)
        override val showLocationLoad: StateFlow<Boolean> = _showLocationLoad
        private val _hideIp = MutableStateFlow(preferences.blurIp)
        override val hideIp: StateFlow<Boolean> = _hideIp
        private val _hideNetworkName = MutableStateFlow(preferences.blurNetworkName)
        override val hideNetworkName: StateFlow<Boolean> = _hideNetworkName

        private val logger = LoggerFactory.getLogger("basic")

        init {
            fetchUserState()
            observeConnectionCount()
            observePreferences()
            observeUpdateAvailable()
        }

        private fun observePreferences() {
            viewModelScope.launch(Dispatchers.IO) {
                preferences.isHapticFeedbackEnabledFlow.collectLatest { isEnabled ->
                    _hapticFeedbackEnabled.value = isEnabled
                }
            }
            viewModelScope.launch(Dispatchers.IO) {
                preferences.isShowLocationHealthEnabledFlow.collectLatest { isEnabled ->
                    _showLocationLoad.value = isEnabled
                }
            }
        }

        private fun observeUpdateAvailable() {
            viewModelScope.launch {
                checkUpdateRepository.updateAvailable.collectLatest { update ->
                    if (update == null || !update.isUpdateAvailable) return@collectLatest
                    // Force bypasses the soft-prompt rate limit.
                    if (update.isForceUpgrade) {
                        _goto.emit(HomeGoto.UpdateAvailable(update.latestVersion, force = true))
                        return@collectLatest
                    }
                    if (checkUpdateRepository.shouldShowPrompt()) {
                        checkUpdateRepository.recordPromptShown()
                        _goto.emit(HomeGoto.UpdateAvailable(update.latestVersion, force = false))
                    }
                }
            }
            // Re-present the force gate on every foreground while force is still asserted.
            viewModelScope.launch {
                appLifeCycleObserver.appActivationState.drop(1).collectLatest {
                    val update = checkUpdateRepository.updateAvailable.value ?: return@collectLatest
                    if (update.isUpdateAvailable && update.isForceUpgrade) {
                        _goto.emit(HomeGoto.UpdateAvailable(update.latestVersion, force = true))
                    }
                }
            }
        }

        private fun observeConnectionCount() {
            viewModelScope.launch(Dispatchers.IO) {
                vpnConnectionStateManager.connectionCount.collectLatest { count ->
                    val showCount = preferences.powerWhiteListDialogCount
                    val askForPowerWhiteListPermission =
                        count > 1 && !isIgnoringBatteryOptimizations(appContext) && showCount < 3
                    if (askForPowerWhiteListPermission &&
                        !isIgnoringBatteryOptimizations(appContext)
                    ) {
                        _goto.emit(HomeGoto.PowerWhitelist)
                    }
                    val user = _userState.value
                    if (user is UserState.Free &&
                        !user.isGhost &&
                        user.registerDays > 30 &&
                        count >= 10 &&
                        preferences.alreadyShownShareAppLink.not()
                    ) {
                        preferences.alreadyShownShareAppLink = true
                        _goto.emit(HomeGoto.ShareAppLink)
                    }
                }
            }
        }

        private fun fetchUserState() {
            viewModelScope.launch {
                userRepository.user.filterNotNull().collectLatest {
                    if (it.isPro) {
                        _userState.emit(UserState.Pro)
                    } else if (it.maxData == -1L) {
                        _userState.emit(UserState.UnlimitedData)
                    } else {
                        var dataLeft = it.maxData - it.dataUsed
                        if (dataLeft < 0) {
                            dataLeft = 0
                        }
                        val angle =
                            if (it.maxData > 0) {
                                (dataLeft.toFloat() / it.maxData.toFloat()) * 360f
                            } else {
                                0f
                            }
                        logger.info("Data left: $dataLeft, Angle: $angle Max: ${it.maxData}")
                        _userState.emit(
                            UserState.Free(
                                String.format(
                                    Locale.getDefault(),
                                    "%.2f\nGB",
                                    dataLeft.toDouble() / (1024 * 1024 * 1024),
                                ),
                                angle,
                                it.isGhost,
                                it.daysRegisteredSince,
                            ),
                        )
                    }
                    showUserStatus(it)
                }
            }
        }

        private fun showUserStatus(user: User) {
            viewModelScope.launch {
                val previousAccountStatus = preferences.getPreviousAccountStatus(user.userName)
                if (previousAccountStatus != user.accountStatusToInt) {
                    preferences.setPreviousAccountStatus(user.userName, user.accountStatusToInt)
                    if (user.accountStatus == User.AccountStatus.Banned) {
                        _goto.emit(HomeGoto.Banned)
                    } else if (user.accountStatus == User.AccountStatus.Expired) {
                        val resetDate = user.nextResetDate() ?: ""
                        _goto.emit(HomeGoto.Expired(resetDate))
                    }
                }

                // Check for user status changes (pro to free downgrade)
                val previousUserStatus = preferences.getPreviousUserStatus(user.userName)
                val currentUserStatus = user.userStatusInt
                if (previousUserStatus == -1) {
                    // First time tracking this user's status, set baseline without showing popup
                    preferences.setPreviousUserStatus(user.userName, currentUserStatus)
                } else if (previousUserStatus != currentUserStatus) {
                    // User was pro (1) and now is free (0) - downgrade detected
                    if (previousUserStatus == 1 && currentUserStatus == 0) {
                        _goto.emit(HomeGoto.Downgraded)
                    }
                    preferences.setPreviousUserStatus(user.userName, currentUserStatus)
                }
            }
        }

        private fun isIgnoringBatteryOptimizations(context: Context): Boolean {
            val manager =
                context.applicationContext.getSystemService(Context.POWER_SERVICE) as PowerManager
            return manager.isIgnoringBatteryOptimizations(context.applicationContext.packageName)
        }

        override fun onMainMenuClick() {
            viewModelScope.launch {
                _goto.emit(HomeGoto.MainMenu)
            }
        }

        override fun onHideIpClick() {
            _hideIp.value = !_hideIp.value
            preferences.blurIp = _hideIp.value
        }

        override fun onHideNetworkNameClick() {
            _hideNetworkName.value = !_hideNetworkName.value
            preferences.blurNetworkName = _hideNetworkName.value
        }

        override fun onGoToHandled() {
            viewModelScope.launch {
                _goto.emit(HomeGoto.None)
            }
        }
    }
