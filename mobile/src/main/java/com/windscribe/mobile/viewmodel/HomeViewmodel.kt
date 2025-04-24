package com.windscribe.mobile.viewmodel

import android.content.Context
import android.os.Build
import android.os.PowerManager
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.windscribe.vpn.Windscribe.Companion.appContext
import com.windscribe.vpn.apppreference.PreferencesHelper
import com.windscribe.vpn.repository.UserRepository
import com.windscribe.vpn.state.VPNConnectionStateManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.slf4j.LoggerFactory
import java.util.Locale

sealed class HomeGoto {
    object Upgrade : HomeGoto()
    data class Expired(val date: String) : HomeGoto()
    object Banned : HomeGoto()
    object PowerWhitelist : HomeGoto()
    object ShareAppLink : HomeGoto()
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
    abstract val goto: StateFlow<HomeGoto>
    abstract val userState: StateFlow<UserState>
    abstract fun clearGoTo()
}

class HomeViewmodelImpl(
    private val vpnConnectionStateManager: VPNConnectionStateManager,
    private val userRepository: UserRepository,
    private val preferences: PreferencesHelper
) : HomeViewmodel() {

    private val _goto = MutableStateFlow<HomeGoto>(HomeGoto.None)
    override val goto: StateFlow<HomeGoto> = _goto
    private val _userState = MutableStateFlow<UserState>(UserState.Loading)
    override val userState: StateFlow<UserState> = _userState
    private val logger = LoggerFactory.getLogger("basic")

    init {
        fetchUserState()
        observeConnectionCount()
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
//                if (user is UserState.Free && !user.isGhost && user.registerDays > 30 && count >= 10 && preferences.alreadyShownShareAppLink.not()) {
//                    _goto.emit(HomeGoto.ShareAppLink)
//                }
                if (user is UserState.Free && !user.isGhost && user.registerDays > 30 && count >= 1) {
                    _goto.emit(HomeGoto.ShareAppLink)
                }
            }
        }
    }

    private fun fetchUserState() {
        viewModelScope.launch {
            userRepository.userInfo.collect {
                if (it.isPro) {
                    _userState.emit(UserState.Pro)
                } else if (it.maxData == -1L) {
                    _userState.emit(UserState.UnlimitedData)
                } else {
                    val dataLeft = it.maxData - it.dataUsed
                    val angle = (dataLeft.toFloat() / it.maxData.toFloat()) * 360f
                    logger.info("Data left: $dataLeft, Angle: $angle Max: ${it.maxData}")
                    _userState.emit(
                        UserState.Free(
                            String.format(
                                Locale.getDefault(),
                                "%.2f GB",
                                dataLeft.toDouble() / (1024 * 1024 * 1024)
                            ),
                            angle, it.isGhost, it.daysRegisteredSince
                        )
                    )
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

    override fun clearGoTo() {
        viewModelScope.launch {
            _goto.emit(HomeGoto.None)
        }
    }
}