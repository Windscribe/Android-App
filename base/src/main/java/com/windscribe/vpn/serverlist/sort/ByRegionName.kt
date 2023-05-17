/*
 * Copyright (c) 2021 Windscribe Limited.
 */
package com.windscribe.vpn.serverlist.sort

import com.windscribe.vpn.serverlist.entity.Group
import java.util.Comparator

class ByRegionName : Comparator<Group> {
    override fun compare(o1: Group, o2: Group): Int {
        return o1.region.name.compareTo(o2.region.name)
    }
}