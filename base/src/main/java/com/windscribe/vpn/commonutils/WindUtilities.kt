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
        OpenVPN, WIRE_GUARD
    }

    suspend fun deleteProfile(context: Context): Boolean = withContext(Dispatchers.IO) {
        val file = File(context.filesDir, "wd.vp")
        if (file.exists()) {
            file.delete()
        } else {
            false
        }
    }

    suspend fun deleteProfileCompletely(context: Context): Unit = withContext(Dispatchers.IO) {
        val profileFile = File(context.filesDir, Util.VPN_PROFILE_NAME)
        if (profileFile.exists()) {
            profileFile.delete()
        }

        val locationFile = File(context.filesDir, Util.LAST_SELECTED_LOCATION)
        if (locationFile.exists()) {
            locationFile.delete()
        }
    }

    fun getConfigType(content: String): ConfigType {
        return if (content.contains("[Peer]") && content.contains("[Interface]")) {
            ConfigType.WIRE_GUARD
        } else {
            ConfigType.OpenVPN
        }
    }

    fun getSourceTypeBlocking(): SelectedLocationType {
        val isConnectingToStatic = appContext.preference.isConnectingToStaticIp
        val isConnectingToConfigured = appContext.preference.isConnectingToConfiguredLocation()

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

    fun getVersionCode(): String {
        return try {
            val info = appContext.packageManager
                .getPackageInfo(appContext.packageName, 0)
            info.versionCode.toString()
        } catch (_: PackageManager.NameNotFoundException) {
            ""
        }
    }

    fun getVersionName(): String {
        return try {
            appContext.packageManager.getPackageInfo(appContext.packageName, 0).versionName ?: ""
        } catch (_: PackageManager.NameNotFoundException) {
            ""
        }
    }

    fun humanReadableByteCount(bytes: Long, speed: Boolean, res: Resources): String {
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
}