/*
 * Copyright (c) 2021 Windscribe Limited.
 */

package com.windscribe.vpn.backend

import android.app.Activity
import android.content.Context
import com.windscribe.vpn.Windscribe
import com.windscribe.vpn.backend.utils.LastSelectedLocation
import com.windscribe.vpn.backend.utils.ProtocolConfig
import com.windscribe.vpn.constants.PreferencesKeyConstants
import com.windscribe.vpn.constants.PreferencesKeyConstants.PROTO_WIRE_GUARD
import com.wireguard.config.BadConfigException
import com.wireguard.config.Config
import inet.ipaddr.AddressStringException
import inet.ipaddr.IPAddressString
import io.reactivex.Single
import java.io.BufferedReader
import java.io.IOException
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import java.io.Reader
import java.io.StringReader


object Util {
    const val LAST_SELECTED_LOCATION = "last_selected_location.vp"
    const val VPN_PROFILE_NAME = "wd.vp"

    fun getLastSelectedLocation(context: Context): LastSelectedLocation? {
        try {
            ObjectInputStream(context.openFileInput(LAST_SELECTED_LOCATION))
                    .use {
                        val obj = it.readObject()
                        if (obj is LastSelectedLocation) {
                            return obj
                        }
                    }
        } catch (ignored: Exception) {
            return null
        }
        return null
    }

    fun getSavedLocation(): Single<LastSelectedLocation> {
        return Single.fromCallable {
            try {
                val file = ObjectInputStream(Windscribe.appContext.openFileInput(LAST_SELECTED_LOCATION))
                val obj = file.readObject()
                if (obj is LastSelectedLocation) {
                    return@fromCallable obj
                }
            } catch (ignored: Exception) {
                throw Exception()
            }
            throw Exception()
        }
    }

    inline fun <reified T> getProfile(): T? {
        try {
            val file = ObjectInputStream(Windscribe.appContext.openFileInput(VPN_PROFILE_NAME))
            val obj = file.readObject()
            file.close()
            if (obj is T) {
                return obj
            }
        } catch (ignored: Exception) {
            return null
        }
        return null
    }

    fun saveSelectedLocation(selectedLocation: LastSelectedLocation) {
        val vpnFile = ObjectOutputStream(Windscribe.appContext.openFileOutput(LAST_SELECTED_LOCATION, Activity.MODE_PRIVATE))
        vpnFile.writeObject(selectedLocation)
        vpnFile.flush()
        vpnFile.close()
    }

    fun saveProfile(profile: Any): String {
        val vpnFile = ObjectOutputStream(Windscribe.appContext.openFileOutput(VPN_PROFILE_NAME, Activity.MODE_PRIVATE))
        vpnFile.writeObject(profile)
        vpnFile.flush()
        vpnFile.close()
        return profile.toString()
    }

    fun validIpAddress(str: String): Boolean {
        val addressString = IPAddressString(str)
        return try {
            addressString.toAddress()
            true
        } catch (e: AddressStringException) {
            false
        }
    }

    fun getModifiedIpAddress(ipResponse: String): String {
        var ipAddress: String
        if (ipResponse.length >= 32) {
            ipAddress = ipResponse.replace("0000".toRegex(), "0")
            ipAddress = ipAddress.replace("000".toRegex(), "")
            ipAddress = ipAddress.replace("00".toRegex(), "")
        } else {
            ipAddress = ipResponse
        }
        return ipAddress
    }

    fun getProtocolConfigForOpenVPN(content: String): ProtocolConfig {
        val protocolConfig = ProtocolConfig(PreferencesKeyConstants.PROTO_UDP, "443", ProtocolConfig.Type.Manual)
        val serverConfigLines = content.split(System.getProperty("line.separator")).toTypedArray()
        for (serverConfigLine in serverConfigLines) {
            if (serverConfigLine.contains("remote")) {
                val splits = serverConfigLine.split(" ").toTypedArray()
                if (splits.size > 2) {
                    protocolConfig.port = splits[2]
                    return protocolConfig
                }
            }
            if (serverConfigLine.contains("proto")) {
                val splits = serverConfigLine.split(" ").toTypedArray()
                if (splits.isNotEmpty() && splits[1].contains("tcp")) {
                    protocolConfig.protocol = PreferencesKeyConstants.PROTO_TCP
                }
            }
        }
        return protocolConfig
    }

    fun getProtocolConfigForWireguard(content: String?): ProtocolConfig {
        val protocolConfig = ProtocolConfig(PROTO_WIRE_GUARD, "", ProtocolConfig.Type.Manual)
        val reader: Reader = StringReader(content)
        val bufferedReader = BufferedReader(reader)
        try {
            val config = Config.parse(bufferedReader)
            val inetEndpoint = config.peers[0].endpoint.get()
            protocolConfig.port = inetEndpoint.port.toString()
        } catch (e: IOException) {
            e.printStackTrace()
        } catch (e: BadConfigException) {
            e.printStackTrace()
        } finally {
            try {
                bufferedReader.close()
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
        return protocolConfig
    }

    fun getHostNameFromOpenVPNConfig(content: String): String? {
        val serverConfigLines = content.split(System.getProperty("line.separator")).toTypedArray()
        for (serverConfigLine in serverConfigLines) {
            val splits = serverConfigLine.split(" ").toTypedArray()
            if (serverConfigLine.contains("remote") && splits.size > 2) {
                return splits[1]
            }
        }
        return null
    }
}
