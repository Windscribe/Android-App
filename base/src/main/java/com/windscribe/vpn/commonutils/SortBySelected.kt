/*
 * Copyright (c) 2021 Windscribe Limited.
 */
package com.windscribe.vpn.commonutils

import com.windscribe.vpn.api.response.InstalledAppsData

class SortBySelected : Comparator<InstalledAppsData> {
    override fun compare(
        o1: InstalledAppsData,
        o2: InstalledAppsData,
    ): Int = o2.isChecked.compareTo(o1.isChecked)
}
