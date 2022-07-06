/*
 * Copyright (c) 2021 Windscribe Limited.
 */
package com.windscribe.tv.sort

import com.windscribe.vpn.serverlist.entity.RegionAndCities
import java.util.Comparator

class ByRegionName : Comparator<RegionAndCities> {
    override fun compare(o1: RegionAndCities, o2: RegionAndCities): Int {
        return o1.region.name.compareTo(o2.region.name)
    }
}
