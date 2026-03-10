/*
 * Copyright (c) 2021 Windscribe Limited.
 */
package com.windscribe.tv.sort

import com.windscribe.vpn.serverlist.entity.LocationAndDatacenters
import java.util.Comparator

class ByLatency : Comparator<LocationAndDatacenters> {
    override fun compare(o1: LocationAndDatacenters, o2: LocationAndDatacenters): Int {
        return o1.latencyTotal - o2.latencyTotal
    }
}
