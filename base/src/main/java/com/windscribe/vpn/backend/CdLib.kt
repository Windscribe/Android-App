package com.windscribe.vpn.backend

class CdLib {
    private val deviceInfo = AndroidDeviceIdentityImpl()

    init {
        deviceInfo.load()
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