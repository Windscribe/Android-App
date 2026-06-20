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
         * Get raw installer package name for API calls
         * Returns the actual package name of the installer, or "none" if not available
         * Server will classify this into appropriate categories
         *
         * @return Raw installer package name (e.g., "com.android.vending") or "none" for sideloaded apps
         */
        fun getInstallerPackageName(): String {
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

            // Return "none" for null (sideloaded apps), otherwise return the package name
            return installerPackageName ?: "none"
        }
    }
