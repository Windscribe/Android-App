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
import com.windscribe.vpn.apppreference.PreferencesKeyConstants
import com.windscribe.vpn.backend.PlayIntegrityManager
import com.windscribe.vpn.commonutils.Ext.result
import com.windscribe.vpn.commonutils.WindUtilities
import com.windscribe.vpn.constants.AdvanceParamKeys
import org.slf4j.LoggerFactory
import java.io.BufferedReader
import java.io.File
import java.io.FileReader
import java.io.IOException
import java.nio.charset.Charset
import java.util.zip.ZipInputStream
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LogRepository
    @Inject
    constructor(
        private val preferencesHelper: PreferencesHelper,
        private val apiCallManager: IApiCallManager,
        private val playIntegrityManager: PlayIntegrityManager,
    ) {
        private val logger = LoggerFactory.getLogger("log-repo")

        fun getDebugFilePath(): String {
            val advanceParams = parseAdvanceParams(preferencesHelper.advanceParamText)
            return when {
                advanceParams[AdvanceParamKeys.SHOW_STRONG_SWAN_LOG].toBoolean() -> {
                    "${appContext.filesDir}/charon.log"
                }

                advanceParams[AdvanceParamKeys.SHOW_WG_LOG].toBoolean() -> {
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
            val logFile =
                appContext.localized.resources.getString(
                    com.windscribe.vpn.R.string.log_file_header,
                    Build.VERSION.SDK_INT,
                    Build.BRAND,
                    Build.DEVICE,
                    Build.MODEL,
                    Build.MANUFACTURER,
                    Build.VERSION.RELEASE,
                    WindUtilities.getVersionCode(),
                )
            val builder = StringBuilder()
            builder.append(logFile)
            // Oldest first. Without it a report filed just after a roll carries almost nothing.
            if (debugFilePath == appContext.filesDir.path + PreferencesKeyConstants.DEBUG_LOG_FILE_NAME) {
                builder.append(
                    readRolledLog(
                        File(appContext.filesDir.path + PreferencesKeyConstants.DEBUG_LOG_ROLLED_FILE_NAME),
                    ),
                )
            }
            val file = File(debugFilePath)
            BufferedReader(FileReader(file)).use { bufferedReader ->
                while (bufferedReader.readLine().also { logLine = it } != null) {
                    builder.append(logLine)
                    builder.append("\n")
                }
            }
            return String(
                Base64.encode(
                    builder.toString().toByteArray(Charset.defaultCharset()),
                    Base64.NO_WRAP,
                ),
            )
        }

        fun getPartialLog(): List<String> =
            try {
                File(getDebugFilePath()).readLines()
            } catch (_: IOException) {
                emptyList()
            }

        suspend fun onSendLog(): CallResult<GenericSuccess> {
            // Request Play Integrity token before reading log file
            val integrityToken = playIntegrityManager.requestIntegrityToken()
            logger.info("PlayIntegrity: reset-token android ${if (integrityToken != null) "available" else "unavailable"}")

            return result<GenericSuccess> {
                apiCallManager.postDebugLog(preferencesHelper.userName, getEncodedLog())
            }
        }
    }

/**
 * The previous log, which logback rolled into a zip, or empty when there is none. Never fails the
 * upload: an unreadable backup costs the older half of the history, not the report.
 */
internal fun readRolledLog(rolled: File): String {
    if (!rolled.isFile) {
        return ""
    }
    return runCatching {
        ZipInputStream(rolled.inputStream().buffered()).use { zip ->
            if (zip.nextEntry == null) {
                return@use ""
            }
            val text = zip.reader(Charsets.UTF_8).readText()
            if (text.isEmpty() || text.endsWith("\n")) text else text + "\n"
        }
    }.getOrElse {
        LoggerFactory.getLogger("log-repo").warn("Could not read the rolled log: ${it.message}")
        ""
    }
}
