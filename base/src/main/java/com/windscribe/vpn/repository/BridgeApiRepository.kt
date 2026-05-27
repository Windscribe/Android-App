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
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
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

    // Keep strong reference to prevent Java GC from collecting the object
    @Volatile
    private var bridgeAPIRef: WSNetBridgeAPI? = null

    // Keep strong reference to the callback lambda to prevent GC from collecting it
    @Volatile
    private var apiCallback: ((Boolean) -> Unit)? = null

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
        // Store strong reference to prevent GC while callback is registered
        bridgeAPIRef = bridgeAPI
        scope.launch {
            try {
                val hasToken = bridgeAPI.hasSessionToken()
                if (hasToken && vpnConnectionStateManager.isVPNConnected()) {
                    checkAndEmitApiAvailability(ready = true)
                }
            } catch (_: Exception) {
                // JNI reference may be invalid, ignore
            }
        }

        // Store callback as strong reference to prevent GC while native code holds it
        apiCallback = { ready ->
            if (scope.isActive) {  // Check if scope still valid before launching
                scope.launch {
                    checkAndEmitApiAvailability(ready)
                }
            }
        }

        try {
            bridgeAPI.setApiAvailableCallback(apiCallback!!)
        } catch (_: Exception) {
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
        val region = location?.location
        if (region == null) {
            _apiAvailable.emit(false)
            return
        }
        val user = userRepository.user.value ?: return
        val proUser = user.isPro
        val alcList = user.alcList?.split(",") ?: emptyList()
        val alc = alcList.contains(region.countryCode)
        val cityLocation =
            WindUtilities.getSourceTypeBlocking() == SelectedLocationType.CityLocation
        val activate = ready && cityLocation && (proUser || alc)
        _apiAvailable.emit(activate)
    }
}