package com.windscribe.vpn.repository

import com.windscribe.vpn.Windscribe.Companion.appContext
import com.windscribe.vpn.backend.utils.SelectedLocationType
import com.windscribe.vpn.commonutils.WindUtilities
import com.windscribe.vpn.state.VPNConnectionStateManager
import com.wsnet.lib.WSNet
import com.wsnet.lib.WSNetBridgeAPI
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

class BridgeApiRepository @Inject constructor(
    private val scope: CoroutineScope,
    private val bridgeAPI: WSNetBridgeAPI,
    private val locationRepository: LocationRepository,
    private val userRepository: UserRepository,
    private val vpnConnectionStateManager: VPNConnectionStateManager
) {
    private val _apiAvailable = MutableStateFlow(false)
    val apiAvailable = _apiAvailable.asSharedFlow()

    init {
        scope.launch(Dispatchers.IO) {
            observeBridgeApi()
        }
    }

    private fun observeBridgeApi() {
        scope.launch {
            val hasToken = bridgeAPI.hasSessionToken()
            if (hasToken && vpnConnectionStateManager.isVPNConnected()) {
                checkAndEmitApiAvailability(ready = true)
            }
        }

        bridgeAPI.setApiAvailableCallback { ready ->
            scope.launch {
                checkAndEmitApiAvailability(ready)
            }
        }
    }

    private suspend fun checkAndEmitApiAvailability(ready: Boolean) {
        if (ready) {
            // Call native method on Main thread where JNI environment is properly attached
            val settings = withContext(Dispatchers.Main) {
                try {
                    WSNet.instance().currentPersistentSettings()
                } catch (e: Exception) {
                    null
                }
            }
            settings?.let {
                appContext.preference.wsNetSettings = it
            }
        }
        val location = locationRepository.getSelectedCityAndRegion()
        if (location == null || location.region == null) {
            _apiAvailable.emit(false)
            return
        }
        val user = userRepository.user.value ?: return
        val proUser = user.isPro
        val alcList = user.alcList?.split(",") ?: emptyList()
        val alc = alcList.contains(location.region.countryCode)
        val cityLocation =
            WindUtilities.getSourceTypeBlocking() == SelectedLocationType.CityLocation
        val activate = ready && cityLocation && (proUser || alc)
        _apiAvailable.emit(activate)
    }
}