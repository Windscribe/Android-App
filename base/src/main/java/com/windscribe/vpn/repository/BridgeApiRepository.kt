package com.windscribe.vpn.repository

import com.windscribe.vpn.Windscribe.Companion.appContext
import com.windscribe.vpn.apppreference.PreferencesHelper
import com.windscribe.vpn.backend.utils.SelectedLocationType
import com.windscribe.vpn.commonutils.WindUtilities
import com.windscribe.vpn.state.VPNConnectionStateManager
import com.wsnet.lib.WSNet
import com.windscribe.vpn.wsnet.WSNetWrapper
import com.wsnet.lib.WSNetBridgeAPI
import dagger.Lazy
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

class BridgeApiRepository @Inject constructor(
    private val scope: CoroutineScope,
    private val wsNetWrapper: WSNetWrapper,
    private val preferencesHelper: PreferencesHelper,
    private val locationRepository: LocationRepository,
    private val userRepository: UserRepository,
    private val vpnConnectionStateManager: VPNConnectionStateManager
) {
    private val _apiAvailable = MutableStateFlow(false)
    val apiAvailable = _apiAvailable.asSharedFlow()
    private var observerInitialized = false

    init {
        scope.launch(Dispatchers.IO) {
            // Wait for WSNet to be ready before setting up observers
            wsNetWrapper.isReady.collect { isReady ->
                if (isReady && !observerInitialized) {
                    observerInitialized = true
                    // Now we're guaranteed WSNet is ready, set up observers
                    val bridgeAPI = wsNetWrapper.safeBridgeAPI()
                    if (bridgeAPI != null) {
                        observeBridgeApi(bridgeAPI)
                    }
                    observeExtraTlsPaddingStatus()
                }
            }
        }
    }

    private fun observeBridgeApi(bridgeAPI: WSNetBridgeAPI) {
        scope.launch {
            try {
                val hasToken = bridgeAPI.hasSessionToken()
                if (hasToken && vpnConnectionStateManager.isVPNConnected()) {
                    checkAndEmitApiAvailability(ready = true)
                }
            } catch (e: Exception) {
                // JNI reference may be invalid, ignore
            }
        }
        try {
            bridgeAPI.setApiAvailableCallback { ready ->
                scope.launch {
                    checkAndEmitApiAvailability(ready)
                }
            }
        } catch (e: Exception) {
            // JNI reference may be invalid, ignore
        }
    }

    private fun observeExtraTlsPaddingStatus() {
        scope.launch {
          preferencesHelper.extraTlsPaddingEnabledFlow.collect { enabled ->
              wsNetWrapper.withWSNet { wsNet ->
                  wsNet.advancedParameters()?.let { params ->
                      params.isAPIExtraTLSPadding = enabled
                  }
              }
          }
        }
    }

    private suspend fun checkAndEmitApiAvailability(ready: Boolean) {
        if (ready) {
            // Call native method on Main thread where JNI environment is properly attached
            val settings = withContext(Dispatchers.Main) {
                wsNetWrapper.withWSNet { it.currentPersistentSettings() }
            }
            settings?.let {
                appContext.preference.wsNetSettings = it
            }
        }
        val location = locationRepository.getSelectedCityAndRegion()
        if (location == null || location.location == null) {
            _apiAvailable.emit(false)
            return
        }
        val user = userRepository.user.value ?: return
        val proUser = user.isPro
        val alcList = user.alcList?.split(",") ?: emptyList()
        val alc = alcList.contains(location.location.countryCode)
        val cityLocation =
            WindUtilities.getSourceTypeBlocking() == SelectedLocationType.CityLocation
        val activate = ready && cityLocation && (proUser || alc)
        _apiAvailable.emit(activate)
    }
}