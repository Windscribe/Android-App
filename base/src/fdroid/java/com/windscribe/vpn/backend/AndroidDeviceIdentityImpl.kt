package com.windscribe.vpn.backend

import android.os.Build
import android.provider.Settings
import com.windscribe.vpn.Windscribe.Companion.appContext
import java.net.Inet4Address
import java.net.InetAddress
import java.net.NetworkInterface
import java.net.SocketException
import java.util.*

class AndroidDeviceIdentityImpl(): AndroidDeviceIdentity {

    override var deviceHostName: String? = null
    override var deviceMacAddress: String? = null
    override var deviceLanIp: String? = null

    override fun load() {
        loadHostname()
        setLanIp()
        deviceMacAddress = "Android"
    }
    /**
     * Android does not provide direct api access to device hostname.
     * Device hostname is generated from device Info(bluetooth name, device name or manufacturer and model) .
     * Found value is normalized with only supporting a-z, 0-9, A-Z, - .
     */
    private fun loadHostname(): String? {
        val systemBluetoothName =
                Settings.System.getString(appContext.contentResolver, "bluetooth_name")
        val blueToothName = kotlin.runCatching { Settings.Secure.getString(appContext.contentResolver, "bluetooth_name") }.getOrNull()
        val deviceName = kotlin.runCatching { Settings.Secure.getString(appContext.contentResolver, "device_name") }.getOrNull()
        val hostName = if (!systemBluetoothName.isNullOrEmpty()) {
            systemBluetoothName
        } else if (!blueToothName.isNullOrEmpty()) {
            blueToothName
        } else if (!deviceName.isNullOrEmpty()) {
            deviceName
        } else {
            "${Build.MANUFACTURER} ${Build.MODEL}"
        }
        deviceHostName = formatAsHostname(hostName)
        return deviceHostName
    }

    private fun setLanIp() {
        try {
            val interfaces: Enumeration<NetworkInterface> = NetworkInterface.getNetworkInterfaces()
            while (interfaces.hasMoreElements()) {
                val iface: NetworkInterface = interfaces.nextElement()
                val addresses: Enumeration<InetAddress> = iface.inetAddresses
                while (addresses.hasMoreElements()) {
                    val addr: InetAddress = addresses.nextElement()
                    if (!addr.isLoopbackAddress && !addr.isLinkLocalAddress && addr is Inet4Address && iface.name.startsWith("wlan0")) {
                        deviceLanIp = addr.hostAddress
                    }
                }
            }
        } catch (ignored: SocketException) {
        }
    }
    private fun formatAsHostname(hostName: String): String {
        return hostName.capitalize(Locale.ROOT)
                .replace(Regex("[^A-Za-z0-9 ]"), "")
                .replace(" ", "-")
    }
}