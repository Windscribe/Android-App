/*
 * Copyright (c) 2021 Windscribe Limited.
 */
package com.windscribe.vpn.backend.openvpn

import android.content.Context
import com.windscribe.vpn.Windscribe.Companion.appContext
import com.windscribe.vpn.constants.VpnPreferenceConstants
import com.windscribe.vpn.errormodel.WindError.Companion.instance
import java.io.BufferedReader
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.InputStream
import java.io.InputStreamReader
import java.nio.charset.StandardCharsets
import org.slf4j.Logger
import org.slf4j.LoggerFactory

object WindStunnelUtility {
    var logger: Logger = LoggerFactory.getLogger("vpn")
    val isStunnelRunning: Boolean
        get() = File(appContext.filesDir.path + "/" + VpnPreferenceConstants.STUNNEL_PID)
                .exists()

    /**
     * Starts Stunnel binary process
     * @return true if process started successfully.
     * */
    fun startLocalTun(): Boolean {
        return if (!isStunnelRunning) {
            val filePath = appContext.filesDir.path + "/"
            val sTunnelLibPath = File(appContext.applicationInfo.nativeLibraryDir,
                    "libstunnel_42.so").path
            try {
                val process = Runtime.getRuntime()
                        .exec(sTunnelLibPath + " " + filePath + VpnPreferenceConstants.STUNNEL_CONFIG_FILE)
                process.waitFor()
                // Log if there is any error opening the tunnel
                val inReader = InputStreamReader(process.errorStream)
                val reader = BufferedReader(inReader)
                inReader.close()
                reader.close()
                if (process.exitValue() != 0) {
                    logger.debug("S_TUNNEL TUN FAILED: Process exit value: ${process.exitValue()}")
                }
                process.exitValue() == 0
            } catch (e: Exception) {
                logger.debug("S_TUNNEL TUN FAILED: " + instance.convertErrorToString(e))
                false
            }
        } else {
            logger.debug("S_TUNNEL TUN FAILED: Already running")
            false
        }
    }

    fun stopLocalTunFromAppContext(context: Context) {
        if (isStunnelRunning) {
            var pid = ""
            val filePath = context.filesDir.path + "/"
            try {
                FileInputStream(filePath + VpnPreferenceConstants.STUNNEL_PID).use { inputStream ->
                    val buffer = readInputStream(inputStream)
                    if (buffer != null) {
                        pid = String(buffer, StandardCharsets.UTF_8)
                    }
                    if (pid.trim { it <= ' ' } != "") {
                        try {
                            val mExitProcess = Runtime.getRuntime().exec("kill $pid")
                            mExitProcess.waitFor()
                        } finally {
                            if (isStunnelRunning) {
                                File(filePath + VpnPreferenceConstants.STUNNEL_PID).delete()
                            }
                        }
                    }
                }
            } catch (ignored: Exception) {
            }
        }
    }

    private fun readInputStream(inputStream: InputStream): ByteArray? {
        return try {
            val buf = ByteArray(512)
            var len: Int
            val out = ByteArrayOutputStream()
            while (inputStream.read(buf).also { len = it } > -1) {
                out.write(buf, 0, len)
            }
            out.toByteArray()
        } catch (e: Exception) {
            null
        }
    }
}