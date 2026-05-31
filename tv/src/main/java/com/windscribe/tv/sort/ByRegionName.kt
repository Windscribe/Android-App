/*
 * Copyright (c) 2021 Windscribe Limited.
 */
package com.windscribe.tv.sort

import com.windscribe.vpn.serverlist.entity.LocationAndDatacenters
import java.util.Comparator

class ByRegionName : Comparator<LocationAndDatacenters> {
    override fun compare(
        o1: LocationAndDatacenters,
        o2: LocationAndDatacenters,
    ): Int =
        o1.location
            ?.name
            .orEmpty()
            .compareTo(o2.location?.name.orEmpty())
}
