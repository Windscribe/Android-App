/*
 * Copyright (c) 2021 Windscribe Limited.
 */
package com.windscribe.vpn.serverlist.sort

import com.windscribe.vpn.serverlist.entity.City
import java.util.Comparator

class ByCityName : Comparator<City> {
    override fun compare(o1: City, o2: City): Int {
        return (o1.nodeName + o1.nickName).compareTo(o2.nodeName + o2.nickName)
    }
}