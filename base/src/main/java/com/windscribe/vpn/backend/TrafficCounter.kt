/*
 * Copyright (c) 2021 Windscribe Limited.
 */

package com.windscribe.vpn.backend

import android.net.TrafficStats
import com.windscribe.vpn.Windscribe
import com.windscribe.vpn.Windscribe.Companion.appContext
import com.windscribe.vpn.apppreference.PreferencesHelper
import com.windscribe.vpn.commonutils.Ext.launchPeriodicAsync
import com.windscribe.vpn.commonutils.WindUtilities
import com.windscribe.vpn.state.DeviceStateManager
import com.windscribe.vpn.state.VPNConnectionStateManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import org.slf4j.LoggerFactory
import javax.inject.Singleton

/**
Updates traffic count for VPN Service notifications.
 */
@Singleton
class TrafficCounter(
    val scope: CoroutineScope,
    val vpnConnectionStateManager: VPNConnectionStateManager,
    val preferencesHelper: PreferencesHelper,
    val deviceStateManager: DeviceStateManager,
) {
    private val logger = LoggerFactory.getLogger("vpn")
    var trafficStats = MutableSharedFlow<Traffic>()
    private var _stats = trafficStats
    private var trafficCounterJob: Job? = null
    private var lastDownload = 0L
    private var lastUpload = 0L
    private var sessionDownload = 0L
    private var sessionUpload = 0L
    private var updateIntervalInMs = 1000L

    init {
        scope.launch {
            vpnConnectionStateManager.state.collect {
                if (it.status == VPNState.Status.Connected && appContext.vpnController.vpnBackendHolder.activeBackend?.reconnecting == true) {
                    logger.debug("VPN reconnecting.")
                    startUpdatingTrafficStats()
                } else if (it.status == VPNState.Status.Connected) {
                    resetSession()
                    startUpdatingTrafficStats()
                } else {
                    stopUpdatingTrafficStats()
                }
            }
        }
        scope.launch {
            deviceStateManager.isDeviceInteractive.collect {
                if (it) {
                    startUpdatingTrafficStats()
                } else {
                    stopUpdatingTrafficStats()
                }
            }
        }
    }

    private fun startUpdatingTrafficStats() {
        stopUpdatingTrafficStats()
        if (preferencesHelper.notificationStat) {
            trafficCounterJob = scope.launchPeriodicAsync(updateIntervalInMs) {
                val requiresUpdate = lastUpload > 0 && lastDownload > 0
                if (requiresUpdate) {
                    val totalDownload = TrafficStats.getTotalRxBytes()
                    val totalUpload = TrafficStats.getTotalTxBytes()
                    val downloadDifference = (totalDownload - lastDownload).coerceAtLeast(0) / 2
                    val uploadDifference = (totalUpload - lastUpload).coerceAtLeast(0) / 2
                    sessionDownload += downloadDifference
                    sessionUpload += uploadDifference
                    lastDownload = totalDownload
                    lastUpload = totalUpload
                    val statsChanged = downloadDifference > 0 && uploadDifference > 0
                    if (statsChanged) {
                        buildTrafficHistory(sessionDownload, sessionUpload)
                    }
                } else {
                    lastDownload = TrafficStats.getTotalRxBytes()
                    lastUpload = TrafficStats.getTotalTxBytes()
                }
            }
        }
    }

    private fun stopUpdatingTrafficStats() {
        trafficCounterJob?.cancel()
        trafficCounterJob = null
    }

    private fun resetSession() {
        sessionUpload = 0
        sessionDownload = 0
        lastDownload = 0
        lastUpload = 0
    }

    private fun buildTrafficHistory(downloadDifference: Long, uploadDifference: Long) {
        scope.launch {
            val download = WindUtilities.humanReadableByteCount(
                downloadDifference,
                false,
                Windscribe.appContext.resources
            )
            val upload = WindUtilities.humanReadableByteCount(
                uploadDifference,
                false,
                Windscribe.appContext.resources
            )
            _stats.emit(
                Traffic(
                    "Out: " +
                            upload + " | " +
                            "In: " +
                            download
                )
            )
        }
    }

    fun reset(activateNotificationStats: Boolean) {
        if (activateNotificationStats && vpnConnectionStateManager.state.value.status == VPNState.Status.Connected) {
            startUpdatingTrafficStats()
        } else {
            stopUpdatingTrafficStats()
            scope.launch {
                _stats.emit(Traffic())
            }
        }
    }
}

data class Traffic(val text: String? = null)
