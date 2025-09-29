package com.windscribe.vpn.backend.wireguard

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.util.regex.Pattern

class WgLogger {
    private var logcatProcess: Process? = null
    
    private val _failedIpFlow = MutableSharedFlow<String>(replay = 0)
    val failedIpFlow: SharedFlow<String> = _failedIpFlow
    
    private val _handshakeSuccessFlow = MutableSharedFlow<String>(replay = 0)
    val handshakeSuccessFlow: SharedFlow<String> = _handshakeSuccessFlow
    
    // Pattern to match "Received invalid response message from IP:PORT"
    private val invalidResponsePattern = Pattern.compile(
        "Received invalid response message from ([0-9]{1,3}\\.[0-9]{1,3}\\.[0-9]{1,3}\\.[0-9]{1,3}):[0-9]+"
    )
    
    // Pattern to match handshake success: "peer(+5ta…LQEE) - Received handshake response"
    private val handshakeSuccessPattern = Pattern.compile(
        "peer\\([^)]+\\) - Received handshake response"
    )
    suspend fun captureLogs(context: Context) {
        withContext(Dispatchers.IO) {
            try {
                val logFile = File(context.filesDir, "wireguard_log.txt")
                if (!logFile.exists()) {
                    logFile.createNewFile()
                }
                val process =
                    Runtime.getRuntime().exec("logcat WireGuard/GoBackend/Windscribe:D *:S")
                val bufferedReader = BufferedReader(InputStreamReader(process.inputStream))
                var line: String?
                while (bufferedReader.readLine().also { line = it } != null) {
                    line?.let {
                        appendLineToFile(logFile, it)
                        ensureMaxLines(logFile)
                        checkForInvalidResponseMessage(it)
                        checkForHandshakeSuccess(it)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun stopCapture() {
        logcatProcess?.destroy()
        logcatProcess = null
    }

    private fun appendLineToFile(file: File, line: String) {
        file.appendText("$line\n")
    }

    private fun ensureMaxLines(file: File) {
        val lines = file.readLines()
        if (lines.size > 500) {
            val trimmedLines = lines.takeLast(500)
            file.writeText(trimmedLines.joinToString("\n") + "\n")
        }
    }
    
    /**
     * Checks log line for "invalid response message" pattern and emits IP if found
     */
    private suspend fun checkForInvalidResponseMessage(logLine: String) {
        val matcher = invalidResponsePattern.matcher(logLine)
        if (matcher.find()) {
            val failedIp = matcher.group(1)
            if (failedIp != null) {
                _failedIpFlow.emit(failedIp)
            }
        }
    }
    
    /**
     * Checks log line for handshake success pattern
     */
    private suspend fun checkForHandshakeSuccess(logLine: String) {
        if (handshakeSuccessPattern.matcher(logLine).find()) {
            // Emit handshake success - can be refined to extract specific details
            _handshakeSuccessFlow.emit(logLine)
        }
    }
}