/*
 * Copyright (c) 2024 Windscribe Limited.
 */
package com.windscribe.vpn.installer

import android.content.Context
import android.os.Build
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Enum representing different app installer sources
 */
enum class AppInstaller {
    GOOGLE_PLAY,
    FDROID,
    AMAZON,
    SAMSUNG,
    HUAWEI,
    UNKNOWN,
}

/**
 * Detector class to identify where the app was installed from
 * Can be injected via Hilt
 */
@Singleton
class AppInstallerDetector
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
    ) {
        /**
         * Detects the installer source of the application
         *
         * @return AppInstaller enum indicating the installation source
         */
        private fun getInstaller(): AppInstaller {
            val packageName = context.packageName
            val installerPackageName =
                try {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                        // Android 11+ (API 30+)
                        context.packageManager.getInstallSourceInfo(packageName).installingPackageName
                    } else {
                        // Below Android 11
                        @Suppress("DEPRECATION")
                        context.packageManager.getInstallerPackageName(packageName)
                    }
                } catch (e: Exception) {
                    null
                }

            return when (installerPackageName) {
                "com.android.vending" -> AppInstaller.GOOGLE_PLAY
                "org.fdroid.fdroid" -> AppInstaller.FDROID
                "org.fdroid.fdroid.privileged" -> AppInstaller.FDROID
                "com.amazon.venezia" -> AppInstaller.AMAZON
                "com.sec.android.app.samsungapps" -> AppInstaller.SAMSUNG
                "com.huawei.appmarket" -> AppInstaller.HUAWEI
                else -> AppInstaller.UNKNOWN
            }
        }

        /**
         * Get installer identifier for API calls
         * Returns lowercase string representation suitable for server-side comparison
         *
         * @return Lowercase installer identifier (e.g., "google_play", "fdroid", "unknown")
         */
        fun getInstallerIdentifier(): String =
            when (getInstaller()) {
                AppInstaller.GOOGLE_PLAY -> "google_play"
                AppInstaller.FDROID -> "fdroid"
                AppInstaller.AMAZON -> "amazon"
                AppInstaller.SAMSUNG -> "samsung"
                AppInstaller.HUAWEI -> "huawei"
                AppInstaller.UNKNOWN -> "unknown"
            }
    }
