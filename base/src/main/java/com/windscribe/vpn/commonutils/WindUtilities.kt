/*
 * Copyright (c) 2021 Windscribe Limited.
 */

package com.windscribe.vpn.commonutils

import android.content.Context
import android.content.pm.PackageManager
import android.content.res.Resources
import android.net.ConnectivityManager
import android.net.NetworkInfo
import com.windscribe.vpn.R
import com.windscribe.vpn.Windscribe.Companion.appContext
import com.windscribe.vpn.backend.Util
import com.windscribe.vpn.backend.utils.SelectedLocationType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.math.ln
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

object WindUtilities {
    enum class ConfigType {
        OpenVPN,
        WIRE_GUARD,
    }

    suspend fun deleteProfile(context: Context): Boolean =
        withContext(Dispatchers.IO) {
            val file = File(context.filesDir, "wd.vp")
            if (file.exists()) {
                file.delete()
            } else {
                false
            }
        }

    suspend fun deleteProfileCompletely(context: Context): Unit =
        withContext(Dispatchers.IO) {
            val profileFile = File(context.filesDir, Util.VPN_PROFILE_NAME)
            if (profileFile.exists()) {
                profileFile.delete()
            }

            val locationFile = File(context.filesDir, Util.LAST_SELECTED_LOCATION)
            if (locationFile.exists()) {
                locationFile.delete()
            }
        }

    fun getConfigType(content: String): ConfigType =
        if (content.contains("[Peer]") && content.contains("[Interface]")) {
            ConfigType.WIRE_GUARD
        } else {
            ConfigType.OpenVPN
        }

    fun getSourceTypeBlocking(): SelectedLocationType {
        val isConnectingToStatic = appContext.preference.isConnectingToStaticIp
        val isConnectingToConfigured = appContext.preference.isConnectingToConfigured

        return when {
            isConnectingToConfigured -> SelectedLocationType.CustomConfiguredProfile
            isConnectingToStatic -> SelectedLocationType.StaticIp
            else -> SelectedLocationType.CityLocation
        }
    }

    fun getUnderLayNetworkInfo(): NetworkInfo? {
        val connectivityManager =
            appContext.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
        return connectivityManager?.activeNetworkInfo
    }

    fun getVersionCode(): String =
        try {
            val info =
                appContext.packageManager
                    .getPackageInfo(appContext.packageName, 0)
            info.versionCode.toString()
        } catch (_: PackageManager.NameNotFoundException) {
            ""
        }

    fun getVersionName(): String =
        try {
            appContext.packageManager.getPackageInfo(appContext.packageName, 0).versionName ?: ""
        } catch (_: PackageManager.NameNotFoundException) {
            ""
        }

    fun humanReadableByteCount(
        bytes: Long,
        speed: Boolean,
        res: Resources,
    ): String {
        val adjustedBytes = if (speed) bytes * 8 else bytes
        val unit = if (speed) 1000 else 1024

        val exp = max(0, min((ln(adjustedBytes.toDouble()) / ln(unit.toDouble())).toInt(), 3))
        val bytesUnit = (adjustedBytes / unit.toDouble().pow(exp)).toFloat()

        return if (speed) {
            when (exp) {
                0 -> res.getString(R.string.bits_per_second, bytesUnit)
                1 -> res.getString(R.string.kbits_per_second, bytesUnit)
                2 -> res.getString(R.string.mbits_per_second, bytesUnit)
                else -> res.getString(R.string.gbits_per_second, bytesUnit)
            }
        } else {
            when (exp) {
                0 -> res.getString(R.string.volume_byte, bytesUnit)
                1 -> res.getString(R.string.volume_kbyte, bytesUnit)
                2 -> res.getString(R.string.volume_mbyte, bytesUnit)
                else -> res.getString(R.string.volume_gbyte, bytesUnit)
            }
        }
    }

    fun isOnline(): Boolean {
        val activeNetworkInfo = getUnderLayNetworkInfo()
        return activeNetworkInfo?.isConnected == true
    }

    /**
     * Extracts the hostname prefix from a FQDN, ignoring the domain.
     * This allows matching servers even if the domain changes.
     *
     * Examples:
     * - "wg-012-123.whiskergalaxy.com" → "wg-012-123"
     * - "wg-012-123.windscribe.com" → "wg-012-123"
     * - "wg-012-123" → "wg-012-123"
     *
     * @param fqdn The fully qualified domain name or hostname
     * @return The hostname prefix (everything before the first dot)
     */
    fun getHostnamePrefix(fqdn: String?): String {
        if (fqdn == null) return ""
        return fqdn.substringBefore(".")
    }

    /**
     * Checks if two hostnames match based on their prefix, ignoring the domain.
     *
     * @param hostname1 First hostname (can be FQDN or prefix)
     * @param hostname2 Second hostname (can be FQDN or prefix)
     * @return True if the hostname prefixes match
     */
    fun hostnamesMatch(
        hostname1: String?,
        hostname2: String?,
    ): Boolean {
        if (hostname1 == null || hostname2 == null) return false
        return getHostnamePrefix(hostname1) == getHostnamePrefix(hostname2)
    }
}
