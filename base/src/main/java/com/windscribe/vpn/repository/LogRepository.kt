/*
 * Copyright (c) 2021 Windscribe Limited.
 */
package com.windscribe.vpn.repository

import android.os.Build
import android.util.Base64
import com.windscribe.vpn.Windscribe.Companion.appContext
import com.windscribe.vpn.api.IApiCallManager
import com.windscribe.vpn.api.response.GenericSuccess
import com.windscribe.vpn.apppreference.PreferencesHelper
import com.windscribe.vpn.commonutils.Ext.result
import com.windscribe.vpn.commonutils.WindUtilities
import com.windscribe.vpn.constants.PreferencesKeyConstants
import java.io.BufferedReader
import java.io.File
import java.io.FileReader
import java.io.IOException
import java.nio.charset.Charset
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LogRepository @Inject constructor(
    private val preferencesHelper: PreferencesHelper,
    private val apiCallManager: IApiCallManager
) {

    fun getDebugFilePath(): String {
        val advanceParams = parseAdvanceParams(preferencesHelper.advanceParamText)
        return when {
            advanceParams["showStrongSwanLog"].toBoolean() -> {
                "${appContext.filesDir}/charon.log"
            }

            advanceParams["showWgLog"].toBoolean() -> {
                "${appContext.filesDir}/wireguard_log.txt"
            }

            else -> {
                appContext.filesDir.path + PreferencesKeyConstants.DEBUG_LOG_FILE_NAME
            }
        }
    }

    private fun parseAdvanceParams(text: String): Map<String, String> {
        val map = mutableMapOf<String, String>()
        if (text.isNotEmpty()) {
            val lines = text.split("\n").filter { it.isNotEmpty() }
            for (line in lines) {
                val kv = line.split("=")
                if (kv.size == 2) {
                    map[kv[0]] = kv[1]
                }
            }
        }
        return map
    }

    private fun getEncodedLog(): String {
        var logLine: String?
        val debugFilePath = getDebugFilePath()
        val logFile = appContext.resources.getString(
            com.windscribe.vpn.R.string.log_file_header,
            Build.VERSION.SDK_INT, Build.BRAND, Build.DEVICE, Build.MODEL, Build.MANUFACTURER,
            Build.VERSION.RELEASE, WindUtilities.getVersionCode()
        )
        val builder = StringBuilder()
        builder.append(logFile)
        val file = File(debugFilePath)
        val bufferedReader = BufferedReader(FileReader(file))
        while (bufferedReader.readLine().also { logLine = it } != null) {
            builder.append(logLine)
            builder.append("\n")
        }
        bufferedReader.close()
        return String(
            Base64.encode(
                builder.toString().toByteArray(Charset.defaultCharset()),
                Base64.NO_WRAP
            )
        )
    }

    fun getPartialLog(): List<String> {
        return try {
            File(getDebugFilePath()).readLines()
        } catch (_: IOException) {
            emptyList()
        }
    }

    suspend fun onSendLog(): CallResult<GenericSuccess> {
        return result<GenericSuccess> {
            apiCallManager.postDebugLog(preferencesHelper.userName, getEncodedLog())
        }
    }
}
