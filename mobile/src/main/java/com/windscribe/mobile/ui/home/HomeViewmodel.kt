package com.windscribe.mobile.ui.home

import android.content.Context
import android.os.Build
import android.os.PowerManager
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.windscribe.mobile.ui.connection.ToastMessage
import com.windscribe.vpn.Windscribe.Companion.appContext
import com.windscribe.vpn.apppreference.PreferencesHelper
import com.windscribe.vpn.model.User
import com.windscribe.vpn.repository.UserRepository
import com.windscribe.vpn.state.VPNConnectionStateManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import net.grandcentrix.tray.core.OnTrayPreferenceChangeListener
import org.slf4j.LoggerFactory
import java.util.Locale

sealed class HomeGoto {
    object Upgrade : HomeGoto()
    data class Expired(val date: String) : HomeGoto()
    object Banned : HomeGoto()
    object PowerWhitelist : HomeGoto()
    object ShareAppLink : HomeGoto()
    object LocationMaintenance : HomeGoto()
    data class EditCustomConfig(val id: Int, val connect: Boolean) : HomeGoto()
    object MainMenu : HomeGoto()
    object None : HomeGoto()
}

sealed class UserState() {
    object Loading : UserState()
    object Pro : UserState()
    object UnlimitedData : UserState()
    data class Free(
        val dataLeft: String,
        val dataLeftAngle: Float,
        val isGhost: Boolean,
        val registerDays: Long
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

class HomeViewmodelImpl(
    private val vpnConnectionStateManager: VPNConnectionStateManager,
    private val userRepository: UserRepository,
    private val preferences: PreferencesHelper
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

    private val trayPreferenceChangeListener = OnTrayPreferenceChangeListener {
        _hapticFeedbackEnabled.value = preferences.isHapticFeedbackEnabled
        _showLocationLoad.value = preferences.isShowLocationHealthEnabled
    }
    private val logger = LoggerFactory.getLogger("basic")

    init {
        fetchUserState()
        observeConnectionCount()
        fetchUserPreferences()
    }

    private fun observeConnectionCount() {
        viewModelScope.launch(Dispatchers.IO) {
            vpnConnectionStateManager.connectionCount.collectLatest { count ->
                val showCount = preferences.getPowerWhiteListDialogCount()
                val askForPowerWhiteListPermission =
                    count > 1 && !isIgnoringBatteryOptimizations(appContext) && showCount < 3
                if (askForPowerWhiteListPermission && !isIgnoringBatteryOptimizations(appContext) && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    _goto.emit(HomeGoto.PowerWhitelist)
                }
                val user = _userState.value
                if (user is UserState.Free && !user.isGhost && user.registerDays > 30 && count >= 10 && preferences.alreadyShownShareAppLink.not()) {
                    preferences.alreadyShownShareAppLink = true
                    _goto.emit(HomeGoto.ShareAppLink)
                }
            }
        }
    }

    private fun fetchUserPreferences() {
        preferences.addObserver(trayPreferenceChangeListener)
    }

    private fun fetchUserState() {
        viewModelScope.launch {
            userRepository.userInfo.collectLatest {
                if (it.isPro) {
                    _userState.emit(UserState.Pro)
                } else if (it.maxData == -1L) {
                    _userState.emit(UserState.UnlimitedData)
                } else {
                    var dataLeft = it.maxData - it.dataUsed
                    if (dataLeft < 0) {
                        dataLeft = 0
                    }
                    val angle = (dataLeft.toFloat() / it.maxData.toFloat()) * 360f
                    logger.info("Data left: $dataLeft, Angle: $angle Max: ${it.maxData}")
                    _userState.emit(
                        UserState.Free(
                            String.format(
                                Locale.getDefault(),
                                "%.2f\nGB",
                                dataLeft.toDouble() / (1024 * 1024 * 1024)
                            ),
                            angle, it.isGhost, it.daysRegisteredSince
                        )
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
        }
    }

    private fun isIgnoringBatteryOptimizations(context: Context): Boolean {
        val manager =
            context.applicationContext.getSystemService(Context.POWER_SERVICE) as PowerManager
        val name = context.applicationContext.packageName
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return manager.isIgnoringBatteryOptimizations(name)
        }
        return true
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

    override fun onCleared() {
        preferences.removeObserver(trayPreferenceChangeListener)
        viewModelScope.launch {
            _goto.emit(HomeGoto.None)
        }
        super.onCleared()
    }

    override fun onGoToHandled() {
        viewModelScope.launch {
            _goto.emit(HomeGoto.None)
        }
    }
}