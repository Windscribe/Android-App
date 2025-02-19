/*
 * Copyright (c) 2021 Windscribe Limited.
 */

package com.windscribe.vpn.mocklocation

import android.content.Context
import android.location.LocationManager
import android.provider.Settings.Global
import com.windscribe.vpn.apppreference.PreferencesHelper
import com.windscribe.vpn.backend.Util
import com.windscribe.vpn.backend.VPNState
import com.windscribe.vpn.state.VPNConnectionStateManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.slf4j.LoggerFactory
import javax.inject.Singleton

@Singleton
class MockLocationManager(val context: Context, val scope: CoroutineScope, val vpnConnectionStateManager: VPNConnectionStateManager, val preferencesHelper: PreferencesHelper) {

    private val logger = LoggerFactory.getLogger("state")
    private var mockGps: MockLocationProvider? = null
    private var mockNetwork: MockLocationProvider? = null
    private var pushLocationJob: Job? = null

    fun start(lat: Double, lng: Double) {
        try {
            mockNetwork = MockLocationProvider(LocationManager.NETWORK_PROVIDER, context)
            mockGps = MockLocationProvider(LocationManager.GPS_PROVIDER, context)
        } catch (e: SecurityException) {
            logger.info(e.message)
            stop()
            return
        }
        if (pushLocationJob == null) {
            pushLocationJob = scope.launch {
                while (true) {
                    try {
                        mockNetwork?.pushLocation(lat, lng)
                    } catch (ignored: Exception) {
                    }
                    try {
                        mockGps?.pushLocation(lat, lng)
                    } catch (ignored: Exception) {
                    }
                    delay(2000)
                }
            }
        }
    }

    private fun stop() {
        pushLocationJob?.cancel()
        pushLocationJob = null
        mockGps?.shutdown()
        mockNetwork?.shutdown()
    }

    fun init() {
        scope.launch {
            vpnConnectionStateManager.state.collectLatest { state ->
                if (state.status == VPNState.Status.Disconnected || !preferencesHelper.isGpsSpoofingOn) {
                    stop()
                } else if (state.status == VPNState.Status.Connected && preferencesHelper.isGpsSpoofingOn) {
                    Util.getLastSelectedLocation(context)?.let {
                        val latitude = it.lat?.toDoubleOrNull()
                        val longitude = it.lang?.toDoubleOrNull()
                        if (latitude != null && longitude != null) {
                            start(latitude, longitude)
                        }
                    }
                }
            }
        }
    }

    companion object {

        @JvmStatic
        fun isAppSelectedInMockLocationList(applicationContext: Context): Boolean {
            return try {
                MockLocationProvider(
                    LocationManager.NETWORK_PROVIDER,
                    applicationContext
                )
                MockLocationProvider(LocationManager.GPS_PROVIDER, applicationContext)
                true
            } catch (e: SecurityException) {
                false
            }
        }

        @JvmStatic
        fun isDevModeOn(applicationContext: Context): Boolean {
            return Global.getInt(
                    applicationContext.contentResolver,
                    Global.DEVELOPMENT_SETTINGS_ENABLED, 0
            ) != 0
        }
    }
}
