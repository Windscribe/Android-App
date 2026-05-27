/*
 * Copyright (c) 2021 Windscribe Limited.
 */

package com.windscribe.vpn.backend.wireguard

import com.wireguard.config.BadConfigException
import com.wireguard.config.Config
import org.strongswan.android.data.VpnProfile
import java.io.BufferedReader
import java.io.IOException
import java.io.Serializable
import java.io.StringReader

class WireGuardVpnProfile(val content: String) : Cloneable, Serializable {

    public override fun clone(): VpnProfile {
        try {
            return super.clone() as VpnProfile
        } catch (e: CloneNotSupportedException) {
            throw AssertionError()
        }
    }

    companion object {
        @JvmStatic
        @Throws(IOException::class, BadConfigException::class)
        fun createConfigFromString(content: String): Config {
            StringReader(content).use { reader ->
                BufferedReader(reader).use { bufferedReader ->
                    return Config.parse(bufferedReader)
                }
            }
        }

        @JvmStatic
        fun getHostName(content: String): String? {
            return try {
                val config = createConfigFromString(content)
                config.peers[0].endpoint.get().host
            } catch (e: Exception) {
                null
            }
        }

        @JvmStatic
        fun validConfig(profileContent: String): Boolean {
            return profileContent.contains("[Interface]") && profileContent.contains("[Peer]")
        }
    }
}
