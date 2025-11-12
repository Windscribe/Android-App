package com.windscribe.vpn.repository

import android.util.Log
import com.windscribe.vpn.Windscribe.Companion.appContext
import com.windscribe.vpn.backend.utils.SelectedLocationType
import com.windscribe.vpn.commonutils.WindUtilities
import com.wsnet.lib.WSNet
import com.wsnet.lib.WSNetBridgeAPI
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

class BridgeApiRepository @Inject constructor(
    private val scope: CoroutineScope,
    private val bridgeAPI: WSNetBridgeAPI,
    private val locationRepository: LocationRepository,
    private val userRepository: UserRepository
) {
    private val _apiAvailable = MutableStateFlow(false)
    val apiAvailable = _apiAvailable.asSharedFlow()

    init {
        scope.launch(Dispatchers.IO) {
            observeBridgeApi()
        }
    }

    private fun observeBridgeApi() {
        bridgeAPI.setApiAvailableCallback { ready ->
            scope.launch {
                if (ready) {
                    appContext.preference.wsNetSettings =
                        WSNet.instance().currentPersistentSettings()
                }
                val location = locationRepository.getSelectedCityAndRegion()
                if (location == null) {
                    _apiAvailable.emit(false)
                    return@launch
                }
                val user = userRepository.user.value ?: return@launch
                val proUser = user.isPro
                val alcList = user.alcList?.split(",") ?: emptyList()
                val alc = alcList.contains(location.region.countryCode)
                val cityLocation =
                    WindUtilities.getSourceTypeBlocking() == SelectedLocationType.CityLocation
                val activate = ready && cityLocation && (proUser || alc)
                _apiAvailable.emit(activate)
            }
        }
    }
}