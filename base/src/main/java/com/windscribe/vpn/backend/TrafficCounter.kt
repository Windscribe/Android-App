/*
 * Copyright (c) 2021 Windscribe Limited.
 */

package com.windscribe.vpn.backend

import com.windscribe.vpn.Windscribe
import com.windscribe.vpn.backend.wireguard.WireguardBackend
import com.windscribe.vpn.commonutils.WindUtilities
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch

/**
Updates traffic count for VPN Services notifications.
Only works for WireGuard and OpenVPN
 */
@Singleton
class TrafficCounter(val scope: CoroutineScope) {

    var stats = MutableSharedFlow<Traffic>()
    private var trafficCounterJob: Job? = null
    private var down = 0L
    private var up = 0L
    fun update(download: Long, upload: Long) {
        down = download
        up = upload
    }

    fun start(wireguardBackend: WireguardBackend? = null) {
        trafficCounterJob = scope.launch {
            while (true) {
                if (wireguardBackend != null) {
                    val statistics = wireguardBackend.backend.getStatistics(wireguardBackend.testTunnel)
                    up = statistics.totalTx()
                    down = statistics.totalRx()
                }
                if (up > 0L || down > 0L) {
                    val download = WindUtilities.humanReadableByteCount(down, false, Windscribe.appContext.resources)
                    val upload = WindUtilities.humanReadableByteCount(up, false, Windscribe.appContext.resources)
                    stats.emit(
                            Traffic(
                                    "Out: " +
                                            upload + " | " +
                                            "In: " +
                                            download
                            )
                    )
                }
                delay(1000)
            }
        }
    }

    fun stop() {
        scope.launch {
            up = 0
            down = 0
            stats.emit(Traffic())
            trafficCounterJob?.cancel()
        }
    }
}

data class Traffic(val text: String? = null)
