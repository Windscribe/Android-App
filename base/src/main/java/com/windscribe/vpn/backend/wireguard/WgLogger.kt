package com.windscribe.vpn.backend.wireguard

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader

class WgLogger {
    private var logcatProcess: Process? = null
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
}