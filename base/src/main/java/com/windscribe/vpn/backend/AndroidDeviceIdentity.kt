package com.windscribe.vpn.backend

interface AndroidDeviceIdentity {
    var deviceHostName: String?
    var deviceMacAddress: String?
    var deviceLanIp: String?
    fun load()
}