/*
 * Copyright (c) 2021 Windscribe Limited.
 */
package com.windscribe.vpn.serverlist.sort

import com.windscribe.vpn.serverlist.entity.Group
import java.util.Comparator

class ByLatency : Comparator<Group> {
    override fun compare(o1: Group, o2: Group): Int {
        return o1.latencyAverage - o2.latencyAverage
    }
}