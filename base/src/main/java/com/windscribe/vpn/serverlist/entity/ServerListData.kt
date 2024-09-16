/*
 * Copyright (c) 2021 Windscribe Limited.
 */
package com.windscribe.vpn.serverlist.entity

class ServerListData {
    var bestLocation: CityAndRegion? = null
    var serverListHash: String? = null
    var favourites: List<Favourite> = ArrayList()
    var flags: Map<String, Int>? = null
        get() = if (field == null) {
            HashMap()
        } else field
    var pingTimes: List<PingTime> = ArrayList()
    var isProUser = false
    private var showLatencyInMs = false
    var isShowLocationHealthEnabled = false
        private set
    val isShowLatencyInBar: Boolean
        get() = !showLatencyInMs

    fun setShowLatencyInMs(showLatencyInMs: Boolean) {
        this.showLatencyInMs = showLatencyInMs
    }

    fun setShowLocationHealth(showLocationHealth: Boolean) {
        isShowLocationHealthEnabled = showLocationHealth
    }
}
