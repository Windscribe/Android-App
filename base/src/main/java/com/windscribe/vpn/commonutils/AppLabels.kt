/*
 * Copyright (c) 2021 Windscribe Limited.
 */
package com.windscribe.vpn.commonutils

import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager

private const val CHUNK = 50

/**
 * App labels keyed by package name, for the split tunnel lists.
 *
 * [PackageManager.getApplicationLabel] opens the target app's resources, and the framework keeps
 * those only weakly. Reading a few hundred in one pass leaves every APK mapped at once, because
 * nothing about the Java heap prompts a collection: on a device with 500 apps `.apk mmap` peaks
 * between 114 MB and 290 MB depending on when a collection happens to land. Releasing between
 * chunks holds it near 40 MB, and makes it the same on every run.
 */
fun PackageManager.labelsByPackage(apps: List<ApplicationInfo>): Map<String, String> {
    val labels = HashMap<String, String>(apps.size)
    apps.chunked(CHUNK).forEach { chunk ->
        chunk.forEach { labels[it.packageName] = getApplicationLabel(it).toString() }
        Runtime.getRuntime().gc()
    }
    return labels
}
