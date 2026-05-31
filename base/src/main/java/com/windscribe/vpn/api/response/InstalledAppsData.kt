/*
 * Copyright (c) 2021 Windscribe Limited.
 */

package com.windscribe.vpn.api.response

import androidx.annotation.Keep

@Keep
class InstalledAppsData(
    var appName: String,
    var packageName: String,
) : Comparable<InstalledAppsData> {
    var isChecked = false

    var isSystemApp = false

    override fun compareTo(other: InstalledAppsData): Int = other.appName.compareTo(appName)
}
