package com.windscribe.vpn.backend

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class CdLib {
    private val deviceInfo = AndroidDeviceIdentityImpl()

    @Volatile
    private var isLoaded = false

    init {
        // Load device info asynchronously to avoid blocking app startup
        CoroutineScope(Dispatchers.IO).launch {
            deviceInfo.load()
            isLoaded = true
        }
    }

    external fun startCd(cuid: String, homeDir: String, proto: String, logPath: String)
    external fun stopCd(restart: Boolean, pin: Int): Int
    external fun isCdRunning(): Boolean

    fun getHostName(): String {
        return deviceInfo.deviceHostName ?: ""
    }

    fun getLanIP(): String {
        return deviceInfo.deviceLanIp ?: ""
    }

    fun getMacAddress(): String {
        return deviceInfo.deviceMacAddress ?: ""
    }
}