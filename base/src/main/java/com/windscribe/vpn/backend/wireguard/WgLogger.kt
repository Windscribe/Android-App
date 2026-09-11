package com.windscribe.vpn.backend.wireguard

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.withContext
import org.slf4j.LoggerFactory
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader

class WgLogger {
    // Never suspending: emit runs on the thread reading logcat, and a slow collector would park it.
    private val _handshakeReceivedEvent =
        MutableSharedFlow<Unit>(extraBufferCapacity = EVENT_BUFFER, onBufferOverflow = BufferOverflow.DROP_OLDEST)
    val handshakeReceivedEvent: SharedFlow<Unit> = _handshakeReceivedEvent

    private val logger = LoggerFactory.getLogger("vpn")

    @Volatile
    private var activeProcess: Process? = null

    @Volatile
    private var activeReader: BufferedReader? = null

    companion object {
        private const val HANDSHAKE_RESPONSE_RECEIVED = "Received handshake response"
        private const val MAX_LINES = 500
        private const val TRIM_CHECK_INTERVAL = 100
        private const val EVENT_BUFFER = 8
    }

    suspend fun captureLogs(context: Context) {
        // Make sure no previous capture is still running before starting a new one.
        stopCapture()
        withContext(Dispatchers.IO) {
            val logFile = File(context.filesDir, "wireguard_log.txt")
            if (!logFile.exists()) {
                logFile.createNewFile()
            }
            val process =
                try {
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
                try {
                    reader.close()
                } catch (_: Exception) {
                }
                try {
                    process.destroy()
                } catch (_: Exception) {
                }
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
        try {
            process?.destroy()
        } catch (_: Exception) {
        }
        try {
            reader?.close()
        } catch (_: Exception) {
        }
    }

    private fun processHandshakeEvents(line: String) {
        if (line.contains(HANDSHAKE_RESPONSE_RECEIVED)) {
            _handshakeReceivedEvent.tryEmit(Unit)
        }
    }

    private fun appendLineToFile(
        file: File,
        line: String,
    ) {
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
