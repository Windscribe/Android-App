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

    companion object {
        private const val HANDSHAKE_RESPONSE_RECEIVED = "Received handshake response"
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
        // Nothing to clean up
    }

    private suspend fun processHandshakeEvents(line: String) {
        if (line.contains(HANDSHAKE_RESPONSE_RECEIVED)) {
            _handshakeReceivedEvent.emit(Unit)
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