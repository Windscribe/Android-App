/*
 * Copyright (c) 2021 Windscribe Limited.
 */
package com.windscribe.vpn.serverlist.sort

import com.windscribe.vpn.serverlist.entity.StaticRegion
import java.util.Comparator

class ByStaticRegionName : Comparator<StaticRegion> {
    override fun compare(o1: StaticRegion, o2: StaticRegion): Int {
        return o1.cityName.compareTo(o2.cityName)
    }
}