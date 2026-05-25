package com.windscribe.vpn.backend.wireguard

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.withContext
import org.slf4j.LoggerFactory
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader

class WgLogger {
    private val _handshakeReceivedEvent = MutableSharedFlow<Unit>(replay = 0)
    val handshakeReceivedEvent: SharedFlow<Unit> = _handshakeReceivedEvent

    private val _handshakeFailureEvent = MutableSharedFlow<Unit>(replay = 0)
    val handshakeFailureEvent: SharedFlow<Unit> = _handshakeFailureEvent

    private val logger = LoggerFactory.getLogger("vpn")

    @Volatile
    private var activeProcess: Process? = null

    @Volatile
    private var activeReader: BufferedReader? = null

    companion object {
        private const val HANDSHAKE_RESPONSE_RECEIVED = "Received handshake response"
        private const val HANDSHAKE_NOT_COMPLETE = "Handshake did not complete after 5 seconds, retrying"
        private const val MAX_LINES = 500
        private const val TRIM_CHECK_INTERVAL = 100
    }

    suspend fun captureLogs(context: Context) {
        // Make sure no previous capture is still running before starting a new one.
        stopCapture()
        withContext(Dispatchers.IO) {
            val logFile = File(context.filesDir, "wireguard_log.txt")
            if (!logFile.exists()) {
                logFile.createNewFile()
            }
            val process = try {
                Runtime.getRuntime().exec("logcat -T 1 WireGuard/GoBackend/Windscribe:D *:S")
            } catch (e: Exception) {
                logger.warn("WgLogger failed to start logcat: ${e.message}")
                return@withContext
            }
            activeProcess = process
            val reader = BufferedReader(InputStreamReader(process.inputStream))
            activeReader = reader
            var linesSinceTrim = 0
            try {
                while (true) {
                    val line = reader.readLine() ?: break
                    appendLineToFile(logFile, line)
                    processHandshakeEvents(line)
                    if (++linesSinceTrim >= TRIM_CHECK_INTERVAL) {
                        linesSinceTrim = 0
                        ensureMaxLines(logFile)
                    }
                }
            } catch (_: Exception) {
                // Reader closed by stopCapture(), or process died — expected on shutdown.
            } finally {
                try { reader.close() } catch (_: Exception) {}
                try { process.destroy() } catch (_: Exception) {}
                if (activeProcess === process) activeProcess = null
                if (activeReader === reader) activeReader = null
            }
        }
    }

    fun stopCapture() {
        val reader = activeReader
        val process = activeProcess
        activeReader = null
        activeProcess = null
        try { process?.destroy() } catch (_: Exception) {}
        try { reader?.close() } catch (_: Exception) {}
    }

    private suspend fun processHandshakeEvents(line: String) {
        when {
            line.contains(HANDSHAKE_RESPONSE_RECEIVED) -> {
                _handshakeReceivedEvent.emit(Unit)
            }
            line.contains(HANDSHAKE_NOT_COMPLETE) -> {
                _handshakeFailureEvent.emit(Unit)
            }
        }
    }

    private fun appendLineToFile(file: File, line: String) {
        file.appendText("$line\n")
    }

    private fun ensureMaxLines(file: File) {
        val lines = file.readLines()
        if (lines.size > MAX_LINES) {
            val trimmedLines = lines.takeLast(MAX_LINES)
            file.writeText(trimmedLines.joinToString("\n") + "\n")
        }
    }
}