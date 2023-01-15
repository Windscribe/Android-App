/*
 * Copyright (c) 2021 Windscribe Limited.
 */
package com.windscribe.vpn.serverlist.sort

import com.windscribe.vpn.serverlist.entity.ConfigFile
import java.util.Comparator

class ByConfigName : Comparator<ConfigFile> {
    override fun compare(o1: ConfigFile, o2: ConfigFile): Int {
        return o1.name.compareTo(o2.name)
    }
}