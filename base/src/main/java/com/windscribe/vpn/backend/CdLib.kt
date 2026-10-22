package com.windscribe.vpn.backend

import androidx.annotation.Keep
import com.windscribe.vpn.Windscribe.Companion.appContext
import com.wireguard.android.backend.GoBackend
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * ctrld.
 *
 * Constructing this loads libwg-go.so, so never do it on the main thread — see
 * [GoBackend.loadLibrary]. ctrld starts before any VPN engine, so the load cannot be left to the
 * WireGuard backend, which a connection on another protocol never resolves.
 */
@Keep
class CdLib {
    private val deviceInfo = AndroidDeviceIdentityImpl()

    init {
        GoBackend.loadLibrary(appContext)
        // Load device info asynchronously to avoid blocking app startup
        CoroutineScope(Dispatchers.IO).launch {
            deviceInfo.load()
        }
    }

    external fun startCd(
        cuid: String,
        homeDir: String,
        proto: String,
        logPath: String,
        hostName: String,
        lanIp: String,
        macAddress: String,
    )

    external fun stopCd(
        restart: Boolean,
        pin: Int,
    ): Int

    external fun isCdRunning(): Boolean

    fun getHostName(): String = deviceInfo.deviceHostName ?: ""

    fun getLanIP(): String = deviceInfo.deviceLanIp ?: ""

    fun getMacAddress(): String = deviceInfo.deviceMacAddress ?: ""
}
