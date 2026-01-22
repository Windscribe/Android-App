/*
 * Copyright (c) 2021 Windscribe Limited.
 */

package com.windscribe.vpn.state

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import org.slf4j.LoggerFactory
import java.io.BufferedReader
import java.io.InputStreamReader
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Singleton manager that captures WSNet logs from logcat and writes them
 * to the application logger for the entire app lifecycle.
 */
@Singleton
class WSNetLogManager @Inject constructor(
    private val scope: CoroutineScope
) {
    // Use different logger tag to avoid feedback loop
    private val logger = LoggerFactory.getLogger("wsnet-capture")
    private var logCaptureJob: Job? = null
    private var process: Process? = null

    companion object {
        private const val TAG = "wsnet"
    }

    /**
     * Starts capturing WSNet logs from logcat.
     * This runs continuously for the app lifecycle.
     */
    fun start() {
        if (logCaptureJob?.isActive == true) {
            logger.debug("WSNet log capture already running")
            return
        }

        logCaptureJob = scope.launch {
            captureLogs()
        }
        logger.debug("WSNet log capture started")
    }

    /**
     * Stops capturing logs and cleans up resources.
     */
    fun stop() {
        logCaptureJob?.cancel()
        logCaptureJob = null
        process?.destroy()
        process = null
        logger.debug("WSNet log capture stopped")
    }

    private suspend fun captureLogs() {
        withContext(Dispatchers.IO) {
            try {
                // Capture only wsnet tag logs from current time (-T 1 means start from 1 line ago)
                process = Runtime.getRuntime().exec("logcat -T 1 $TAG:* *:S")
                val bufferedReader = BufferedReader(InputStreamReader(process!!.inputStream))
                var line: String?

                while (bufferedReader.readLine().also { line = it } != null) {
                    line?.let { logLine ->
                        processLogLine(logLine)
                    }
                }
            } catch (e: Exception) {
                logger.error("Error capturing WSNet logs: ${e.message}", e)
            }
        }
    }

    /**
     * Processes a single logcat line and writes it to the logger.
     * Expected format: timestamp PID-TID tag package level message
     * Example: 2026-01-22 00:57:40.935 26742-26742 wsnet com.windscribe.vpn I {"tm": "...", "lvl": "info", ...}
     */
    private fun processLogLine(line: String) {
        try {
            // Find the JSON message part (starts with '{')
            val jsonStartIndex = line.indexOf('{')
            if (jsonStartIndex == -1) {
                // Not a JSON log line, skip
                return
            }

            // Extract and parse the JSON message
            val jsonMessage = line.substring(jsonStartIndex)

            // Ignore lines ending with specific patterns (e.g., latency pings)
            if (jsonMessage.endsWith("/latency\"}")) {
                return
            }

            val json = JSONObject(jsonMessage)

            val level = json.optString("lvl", "info")
            val message = json.optString("msg", "")
            val module = json.optString("mod", "wsnet")
            val timestamp = json.optString("tm", "")

            // Log at appropriate level - using wsnet-capture tag to avoid feedback loop
            when (level.lowercase()) {
                "debug" -> logger.debug("$message")
                "info" -> logger.info("$message")
                "warn", "warning" -> logger.warn("$message")
                "error" -> logger.error("$message")
                else -> logger.info("$message")
            }
        } catch (e: Exception) {
            // If JSON parsing fails, just skip to avoid feedback loop
            logger.debug("Failed to parse wsnet log: ${e.message}")
        }
    }
}