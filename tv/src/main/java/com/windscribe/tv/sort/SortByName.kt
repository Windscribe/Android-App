/*
 * Copyright (c) 2021 Windscribe Limited.
 */
package com.windscribe.tv.sort

import com.windscribe.tv.adapter.InstalledAppsData
import java.util.Comparator

class SortByName : Comparator<InstalledAppsData> {
    override fun compare(o1: InstalledAppsData, o2: InstalledAppsData): Int {
        return o1.appName.compareTo(o2.appName, ignoreCase = true)
    }
}
