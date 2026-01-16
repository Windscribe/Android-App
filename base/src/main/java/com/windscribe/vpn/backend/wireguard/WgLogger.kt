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

class WgLogger {
    private var logcatProcess: Process? = null
    private var handshakeNotCompleteTimestamp: Long? = null

    private val _handshakeTimeoutEvent = MutableSharedFlow<Long>(replay = 0)
    val handshakeTimeoutEvent: SharedFlow<Long> = _handshakeTimeoutEvent

    companion object {
        private const val HANDSHAKE_NOT_COMPLETE = "Handshake did not complete"
        private const val HANDSHAKE_RESPONSE_RECEIVED = "Received handshake response"
        private const val TIMEOUT_THRESHOLD_MS = 180_000L // 3 Minutes
    }

    suspend fun captureLogs(context: Context) {
        withContext(Dispatchers.IO) {
            try {
                val logFile = File(context.filesDir, "wireguard_log.txt")
                if (!logFile.exists()) {
                    logFile.createNewFile()
                }
                val process =
                    Runtime.getRuntime().exec("logcat -T 1 WireGuard/GoBackend/Windscribe:D *:S")
                val bufferedReader = BufferedReader(InputStreamReader(process.inputStream))
                var line: String?
                while (bufferedReader.readLine().also { line = it } != null) {
                    line?.let {
                        appendLineToFile(logFile, it)
                        processHandshakeEvents(it)
                        ensureMaxLines(logFile)
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
        handshakeNotCompleteTimestamp = null
    }

    private suspend fun processHandshakeEvents(line: String) {
        when {
            line.contains(HANDSHAKE_NOT_COMPLETE) -> {
                if (handshakeNotCompleteTimestamp == null) {
                    handshakeNotCompleteTimestamp = System.currentTimeMillis()
                    Log.d(
                        "WgLogger",
                        "First handshake not complete detected at: $handshakeNotCompleteTimestamp"
                    )
                }
            }

            line.contains(HANDSHAKE_RESPONSE_RECEIVED) -> {
                // Check if we have a previous "handshake not complete" event
                handshakeNotCompleteTimestamp?.let { startTime ->
                    val currentTime = System.currentTimeMillis()
                    val timeDifference = currentTime - startTime
                    if (timeDifference > TIMEOUT_THRESHOLD_MS) {
                        Log.w(
                            "WgLogger",
                            "Handshake timeout detected: ${timeDifference}ms > ${TIMEOUT_THRESHOLD_MS}ms"
                        )
                        _handshakeTimeoutEvent.emit(timeDifference)
                    }
                    handshakeNotCompleteTimestamp = null
                }
            }
        }
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
}