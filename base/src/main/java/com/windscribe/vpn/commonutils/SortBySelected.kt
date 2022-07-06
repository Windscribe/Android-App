/*
 * Copyright (c) 2021 Windscribe Limited.
 */
package com.windscribe.vpn.commonutils

import com.windscribe.vpn.api.response.InstalledAppsData
import java.lang.Boolean
import java.util.Comparator

class SortBySelected : Comparator<InstalledAppsData> {
    override fun compare(o1: InstalledAppsData, o2: InstalledAppsData): Int {
        return Boolean.compare(o2.isChecked, o1.isChecked)
    }
}