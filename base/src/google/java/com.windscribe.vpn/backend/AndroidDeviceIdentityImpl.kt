package com.windscribe.vpn.backend

import android.content.Context
import android.os.Build
import android.provider.Settings
import com.google.android.gms.appset.AppSet
import com.google.android.gms.appset.AppSetIdInfo
import com.google.android.gms.tasks.Task
import java.net.Inet4Address
import java.net.InetAddress
import java.net.NetworkInterface
import java.net.SocketException
import java.util.*
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit.SECONDS

class AndroidDeviceIdentityImpl(private val context: Context): AndroidDeviceIdentity {

    override var deviceHostName: String? = null
    override var deviceMacAddress: String? = null
    override var deviceLanIp: String? = null
    private var lock = CountDownLatch(1)

    override fun load() {
        loadHostname()
        setLanIp()
        setMacAddress()
    }
    /**
     * Android does not provide direct api access to device hostname.
     * Device hostname is generated from device Info(bluetooth name, device name or manufacturer and model) .
     * Found value is normalized with only supporting a-z, 0-9, A-Z, - .
     */
    private fun loadHostname(): String? {
        val systemBluetoothName =
                Settings.System.getString(context.contentResolver, "bluetooth_name")
        val blueToothName = kotlin.runCatching { Settings.Secure.getString(context.contentResolver, "bluetooth_name") }.getOrNull()
        val deviceName = kotlin.runCatching { Settings.Secure.getString(context.contentResolver, "device_name") }.getOrNull()
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

    /**
     * Android does not provide access to mac address.
     * For workaround google play services uuid is formatted as mac address
     * is used. Requires: com.google.android.gms:play-services-appset:16.0.2
     * Never call from Main Thread
     */
    private fun setMacAddress() {
        val client = AppSet.getClient(context)
        val task: Task<AppSetIdInfo> = client.appSetIdInfo
        lock.await(1, SECONDS)
        task.addOnSuccessListener {
            val leastSignificant48Bits =
                    UUID.fromString(it.id).leastSignificantBits and 0xFFFFFFFFFFFFL
            val hexadecimalValue =
                    leastSignificant48Bits.toString(16).toUpperCase(Locale.ROOT).padStart(12, '0')
            val formattedMacAddress = formatAsMacAddress(hexadecimalValue)
            deviceMacAddress = formattedMacAddress
            lock.countDown()
        }
        task.addOnFailureListener {
            lock.countDown()
        }
    }

    private fun formatAsMacAddress(hexValue: String): String {
        val parts = mutableListOf<String>()
        for (i in 0 until 12 step 2) {
            parts.add(hexValue.substring(i, i + 2))
        }
        return parts.joinToString(":")
    }

    private fun formatAsHostname(hostName: String): String {
        return hostName.capitalize(Locale.ROOT)
                .replace(Regex("[^A-Za-z0-9 ]"), "")
                .replace(" ", "-")
    }
}