/*
 * Copyright (c) 2021 Windscribe Limited.
 */
package com.windscribe.tv.adapter

import android.graphics.drawable.Drawable

class InstalledAppsData(
    val appName: String,
    val packageName: String,
    val appIconDrawable: Drawable,
    val isSystemApp: Boolean
) : Comparable<InstalledAppsData> {
    var isChecked = false
    override fun compareTo(other: InstalledAppsData): Int {
        return other.appName.compareTo(appName)
    }
}
