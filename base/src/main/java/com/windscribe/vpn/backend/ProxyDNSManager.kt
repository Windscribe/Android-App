package com.windscribe.vpn.backend

import com.windscribe.common.DNSDetails
import com.windscribe.common.DnsType
import com.windscribe.vpn.Windscribe.Companion.appContext
import com.windscribe.vpn.apppreference.PreferencesHelper
import com.windscribe.vpn.constants.PreferencesKeyConstants
import ctrld_library.AppCallback
import ctrld_library.Controller
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.slf4j.LoggerFactory
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.atomic.AtomicBoolean

class ProxyDNSManager(val scope: CoroutineScope, val preferenceHelper: PreferencesHelper, private val androidDeviceIdentity: AndroidDeviceIdentity) {
    companion object {
        const val LOG_PATH = "controlD.log"
        const val CONFIG_FILE = "config.toml"
    }

    private var controller: Controller? = null
    private var controlDJob: Job? = null
    var dnsDetails: DNSDetails? = null
    var invalidConfig = false
    private var isRunning = AtomicBoolean(false)
    private val logger = LoggerFactory.getLogger("proxy_dns")

    init {
        androidDeviceIdentity.load()
    }

    private fun updateControlDConfig() {
        val configFile = File(appContext.filesDir, CONFIG_FILE)
        if (dnsDetails?.type == DnsType.Proxy) {
            if (!configFile.exists() || invalidConfig) {
                configFile.createNewFile()
            }
            val upStreamInfo = "[upstream.0]\n" + "bootstrap_ip = \"${dnsDetails?.ip ?: ""}\"\n" + "endpoint = \"${dnsDetails?.address}\"\n" + "name = \"Custom DNS\"\n" + "timeout = 5000\n" + "type = \"${dnsDetails?.getTypeValue}\"\n" + "ip_stack = \"v4\""
            val listenerInfo = appContext.assets.open("config.toml").bufferedReader().readText()
            val configData = "$listenerInfo\n$upStreamInfo".encodeToByteArray()
            logger.debug("Configuring controlD with: $upStreamInfo")
            FileOutputStream(configFile).use {
                it.write(configData)
            }
        }
        invalidConfig = false
    }

    private fun createLogFile(): String {
        val logFile = File(appContext.filesDir, LOG_PATH)
        if (!logFile.exists()) {
            logFile.createNewFile()
        }
        return logFile.absolutePath
    }

    private fun shouldRunControlD(): Boolean {
        return preferenceHelper.dnsMode == PreferencesKeyConstants.DNS_MODE_CUSTOM && preferenceHelper.dnsAddress != null
    }

    suspend fun startControlDIfRequired() {
        if (shouldRunControlD() && isRunning.get().not()) {
            startControlD()
        } else if (!shouldRunControlD() &&isRunning.get()) {
            stopControlD()
        }
    }

    private fun startControlD() {
        updateControlDConfig()
        val logPath = createLogFile()
        val homeDir = appContext.filesDir.absolutePath
        val callback = object : AppCallback {
            override fun exit(p0: String?) {
                logger.debug("ControlD exited.")
            }

            override fun hostname(): String {
                return androidDeviceIdentity.deviceHostName ?: ""
            }

            override fun lanIp(): String {
                return androidDeviceIdentity.deviceLanIp ?: ""
            }

            override fun macAddress(): String {
                return androidDeviceIdentity.deviceMacAddress ?: ""
            }
        }
        controlDJob = scope.launch {
            isRunning.set(true)
            controller = Controller(callback)
            logger.debug("Started ControlD.")
            // we are providing config file in home dir instead of UID
            controller?.start("", homeDir, "doh", 0, logPath)
            logger.debug("ControlD stopped.")
            isRunning.set(false)
        }
        controlDJob?.start()
    }

    suspend fun stopControlD() {
        logger.debug("Stopping ControlD.")
        controller?.stop(true, 0)
        controlDJob?.join()
        controller = null
        invalidConfig = false
    }
}