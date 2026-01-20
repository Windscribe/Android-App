/*
 * Copyright (c) 2021 Windscribe Limited.
 */

package com.windscribe.vpn.backend

import android.app.Activity
import android.content.Context
import com.windscribe.vpn.R
import com.windscribe.vpn.Windscribe
import com.windscribe.vpn.Windscribe.Companion.appContext
import com.windscribe.vpn.autoconnection.ProtocolConnectionStatus
import com.windscribe.vpn.autoconnection.ProtocolInformation
import com.windscribe.vpn.backend.utils.LastSelectedLocation
import com.windscribe.vpn.commonutils.ThreadSafeList
import com.windscribe.vpn.apppreference.PreferencesKeyConstants
import com.windscribe.vpn.apppreference.PreferencesKeyConstants.PROTO_WIRE_GUARD
import com.windscribe.vpn.exceptions.WindScribeException
import com.windscribe.vpn.serverlist.entity.Node
import com.wireguard.config.BadConfigException
import com.wireguard.config.Config
import inet.ipaddr.AddressStringException
import inet.ipaddr.IPAddressString
import java.io.BufferedReader
import java.io.File
import java.io.FileNotFoundException
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

    suspend fun getSavedLocationAsync(): LastSelectedLocation {
        return try {
            ObjectInputStream(Windscribe.appContext.openFileInput(LAST_SELECTED_LOCATION)).use {
                val obj = it.readObject()
                if (obj is LastSelectedLocation) {
                    obj
                } else {
                    throw WindScribeException("Invalid location found.")
                }
            }
        } catch (ignored: FileNotFoundException) {
            throw WindScribeException("No saved location")
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
        val vpnFile = ObjectOutputStream(
            Windscribe.appContext.openFileOutput(
                LAST_SELECTED_LOCATION,
                Activity.MODE_PRIVATE
            )
        )
        vpnFile.writeObject(selectedLocation)
        vpnFile.flush()
        vpnFile.close()
    }

    fun removeLastSelectedLocation() {
        val file = File(appContext.filesDir, LAST_SELECTED_LOCATION)
        file.delete()
    }

    fun saveProfile(profile: Any): String {
        val vpnFile = ObjectOutputStream(
            Windscribe.appContext.openFileOutput(
                VPN_PROFILE_NAME,
                Activity.MODE_PRIVATE
            )
        )
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

    fun getProtocolInformationFromOpenVPNConfig(content: String): ProtocolInformation {
        val protocolInformation = buildProtocolInformation(
            null,
            PreferencesKeyConstants.PROTO_UDP,
            "443"
        )
        val serverConfigLines = content.split(System.getProperty("line.separator")).toTypedArray()
        for (serverConfigLine in serverConfigLines) {
            if (serverConfigLine.contains("remote")) {
                val splits = serverConfigLine.split(" ").toTypedArray()
                if (splits.size > 2) {
                    protocolInformation.port = splits[2]
                    return protocolInformation
                }
            }
            if (serverConfigLine.contains("proto")) {
                val splits = serverConfigLine.split(" ").toTypedArray()
                if (splits.isNotEmpty() && splits[1].contains("tcp")) {
                    protocolInformation.protocol = PreferencesKeyConstants.PROTO_TCP
                }
            }
        }
        return protocolInformation
    }

    fun getProtocolInformationFromWireguardConfig(content: String?): ProtocolInformation {
        val protocolInformation =
            buildProtocolInformation(null, PROTO_WIRE_GUARD, "")
        val reader: Reader = StringReader(content)
        val bufferedReader = BufferedReader(reader)
        try {
            val config = Config.parse(bufferedReader)
            val inetEndpoint = config.peers[0].endpoint.get()
            protocolInformation.port = inetEndpoint.port.toString()
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
        return protocolInformation
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

    fun getAppSupportedProtocolList(suggestedProtocol: Pair<String, String>? = null): ThreadSafeList<ProtocolInformation> {
        val protocol1 = ProtocolInformation(
            PreferencesKeyConstants.PROTO_IKev2,
            PreferencesKeyConstants.DEFAULT_IKEV2_PORT,
            appContext.getString(R.string.iKEV2_description),
            ProtocolConnectionStatus.Disconnected
        )
        val protocol2 = ProtocolInformation(
            PreferencesKeyConstants.PROTO_UDP,
            PreferencesKeyConstants.DEFAULT_UDP_LEGACY_PORT,
            appContext.getString(R.string.Udp_description),
            ProtocolConnectionStatus.Disconnected
        )
        val protocol3 = ProtocolInformation(
            PreferencesKeyConstants.PROTO_TCP,
            PreferencesKeyConstants.DEFAULT_TCP_LEGACY_PORT,
            appContext.getString(R.string.Tcp_description),
            ProtocolConnectionStatus.Disconnected
        )
        val protocol4 = ProtocolInformation(
            PreferencesKeyConstants.PROTO_STEALTH,
            PreferencesKeyConstants.DEFAULT_STEALTH_LEGACY_PORT,
            appContext.getString(R.string.Stealth_description),
            ProtocolConnectionStatus.Disconnected
        )
        val protocol5 = ProtocolInformation(
            PROTO_WIRE_GUARD,
            PreferencesKeyConstants.DEFAULT_WIRE_GUARD_PORT,
            appContext.getString(R.string.Wireguard_description),
            ProtocolConnectionStatus.Disconnected
        )
        val protocol6 = ProtocolInformation(
            PreferencesKeyConstants.PROTO_WS_TUNNEL,
            PreferencesKeyConstants.DEFAULT_WS_TUNNEL_LEGACY_PORT,
            appContext.getString(R.string.WSTunnel_description),
            ProtocolConnectionStatus.Disconnected
        )
        val supportedProtocoList =  ThreadSafeList<ProtocolInformation>().apply {
            add(protocol1)
            add(protocol2)
            add(protocol3)
            add(protocol4)
            add(protocol5)
            add(protocol6)
        }
        val suggestedProtocolFromApp = supportedProtocoList.firstOrNull { it.protocol == suggestedProtocol?.first }
        if(suggestedProtocolFromApp != null && suggestedProtocol != null){
            val index = supportedProtocoList.indexOfFirst { it.protocol == suggestedProtocol.first }
            supportedProtocoList.removeAt(index)
            suggestedProtocolFromApp.port = suggestedProtocol.second
            supportedProtocoList.add(0, suggestedProtocolFromApp)
        }
        return supportedProtocoList
    }

    fun buildProtocolInformation(
        protocolInformationList: List<ProtocolInformation>?,
        protocol: String,
        port: String
    ): ProtocolInformation {
        val list = protocolInformationList ?: getAppSupportedProtocolList()
        return list.firstOrNull { it.protocol == protocol }?.apply {
            this.port = port
            this.autoConnectTimeLeft = 10
        } ?: getAppSupportedProtocolList().first()
    }

    fun getProtocolLabel(protocol: String): String {
        return when (protocol) {
            PreferencesKeyConstants.PROTO_IKev2 -> "IKEv2"
            PreferencesKeyConstants.PROTO_UDP -> "UDP"
            PreferencesKeyConstants.PROTO_TCP -> "TCP"
            PreferencesKeyConstants.PROTO_STEALTH -> "Stealth"
            PROTO_WIRE_GUARD -> "WireGuard"
            PreferencesKeyConstants.PROTO_WS_TUNNEL -> "WStunnel"
            else -> "IKEv2"
        }
    }

    /**
     * @return Random node index based on weight ignoring the last attempted node.
     */
    fun getRandomNode(lastUsedIndex: Int, attempt: Int, nodes: List<Node>): Int {
        if (nodes.size == 1) {
            return 0
        }
        var index = getRandomNode(nodes)
        while (lastUsedIndex == index && attempt > 0) {
            index = getRandomNode(nodes)
        }
        return index
    }

    private fun getRandomNode(nodes: List<Node>): Int {
        var bestNode = 0
        var weightCounter = Math.random() * (nodes.sumOf { it.weight })
        for (node in nodes) {
            weightCounter -= node.weight
            if (weightCounter <= 0.0) {
                bestNode = nodes.indexOf(node)
                break
            }
        }
        return bestNode
    }
}
