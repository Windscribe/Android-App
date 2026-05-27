package com.windscribe.vpn.backend

import com.windscribe.common.DNSDetails
import com.windscribe.common.DnsType
import com.windscribe.vpn.Windscribe.Companion.appContext
import com.windscribe.vpn.apppreference.PreferencesHelper
import com.windscribe.vpn.apppreference.PreferencesKeyConstants
import com.windscribe.vpn.exceptions.WindScribeException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.slf4j.LoggerFactory
import java.io.File
import java.io.FileOutputStream
import java.net.DatagramSocket
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

class ProxyDNSManager(
    val scope: CoroutineScope,
    val preferenceHelper: PreferencesHelper,
) {
    companion object {
        const val LOG_PATH = "controlD.log"
        const val CONFIG_FILE = "config.toml"
    }

    private var cdLib = CdLib()
    private var controlDJob: Job? = null
    var dnsDetails: DNSDetails? = null
    var invalidConfig = false
    private var isRunning = AtomicBoolean(false)
    private val logger = LoggerFactory.getLogger("vpn")
    private val activePort = AtomicInteger(5355)

    /** Returns the port ctrld is currently configured to listen on. */
    fun getListenPort(): Int = activePort.get()

    private fun updateControlDConfig() {
        val configFile = File(appContext.filesDir, CONFIG_FILE)
        if (dnsDetails?.type == DnsType.Proxy) {
            if (!configFile.exists() || invalidConfig) {
                configFile.createNewFile()
            }
            val address = dnsDetails?.address ?: ""
            val upStreamInfo =
                "[upstream.0]\n" +
                    "bootstrap_ip = \"${dnsDetails?.ip ?: ""}\"\n" +
                    "endpoint = \"$address\"\n" +
                    "name = \"Custom DNS\"\n" +
                    "timeout = 5000\n" +
                    "type = \"${dnsDetails?.getTypeValue}\"\n" +
                    "ip_stack = \"v4\""
            val staticConfig =
                appContext.assets
                    .open("config.toml")
                    .bufferedReader()
                    .readText()
            val listenPort =
                findAvailablePort()
                    ?: throw WindScribeException("Unable to find port to start ControlD cli.")
            activePort.set(listenPort.toInt())
            val listenerInfo = staticConfig.replace("5355", listenPort)
            val configData = "$listenerInfo\n$upStreamInfo".encodeToByteArray()
            logger.debug("Configuring controlD with: $upStreamInfo \nListenPort: $listenPort")
            FileOutputStream(configFile).use {
                it.write(configData)
            }
        }
        invalidConfig = false
    }

    private fun findAvailablePort(): String? {
        var port = 5355
        repeat(20) {
            try {
                ServerSocket(port).use {
                    return "$port"
                }
            } catch (e: Exception) {
                port++
            }
        }
        return null
    }

    private fun createLogFile(): String {
        val logFile = File(appContext.filesDir, LOG_PATH)
        if (!logFile.exists()) {
            logFile.createNewFile()
        }
        return logFile.absolutePath
    }

    private fun shouldRunControlD(): Boolean =
        dnsDetails?.type == DnsType.Proxy &&
            preferenceHelper.dnsMode == PreferencesKeyConstants.DNS_MODE_CUSTOM &&
            preferenceHelper.dnsAddress != null

    suspend fun startControlDIfRequired() {
        if (shouldRunControlD() && isRunning.get().not()) {
            startControlD()
        } else if (!shouldRunControlD() && isRunning.get()) {
            stopControlD()
        }
    }

    private suspend fun startControlD() {
        updateControlDConfig()
        val logPath = createLogFile()
        val homeDir = appContext.filesDir.absolutePath
        if (controlDJob?.isActive == true) {
            logger.debug("Previous ControlD job is still running. Waiting for it to finish.")
            controlDJob?.join()
        }
        controlDJob =
            scope.launch {
                isRunning.set(true)
                logger.debug("Started ControlD.")
                // we are providing config file in home dir instead of UID
                // Pass device info directly to avoid JNI callbacks from Go goroutines
                cdLib.startCd(
                    "",
                    homeDir,
                    "doh",
                    logPath,
                    cdLib.getHostName(),
                    cdLib.getLanIP(),
                    cdLib.getMacAddress(),
                )
                logger.debug("ControlD stopped.")
                isRunning.set(false)
            }
        controlDJob?.start()
        waitForControlDReady()
    }

    /**
     * Waits for ctrld to be reachable on its listen port before returning.
     * Polls every 100ms for up to 5 seconds.
     */
    private suspend fun waitForControlDReady() {
        val port = activePort.get()
        val maxAttempts = 50
        for (i in 1..maxAttempts) {
            try {
                DatagramSocket().use { socket ->
                    socket.connect(InetSocketAddress("127.0.0.1", port))
                    socket.soTimeout = 100
                }
                // Also verify via native check
                if (cdLib.isCdRunning()) {
                    logger.debug("ControlD is ready on port $port after ${i * 100}ms.")
                    return
                }
            } catch (e: Exception) {
                // Not ready yet
            }
            delay(100)
        }
        logger.warn("ControlD may not be ready after ${maxAttempts * 100}ms on port $port. Proceeding anyway.")
    }

    suspend fun stopControlD() {
        cdLib.stopCd(true, 0)
        controlDJob?.join()
        invalidConfig = false
    }
}
