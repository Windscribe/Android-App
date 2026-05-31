/*
 * Copyright (c) 2021 Windscribe Limited.
 */

package com.windscribe.vpn.backend.openvpn

import de.blinkt.openvpn.core.ConfigParser
import java.io.Reader

class OpenVPNConfigParser {
    @Throws(Exception::class)
    fun getEmbeddedPassword(reader: Reader): String {
        val configParser = ConfigParser()
        configParser.parseConfig(reader)
        return configParser.convertProfile().mPassword
    }

    @Throws(Exception::class)
    fun getEmbeddedUsername(reader: Reader): String {
        val configParser = ConfigParser()
        configParser.parseConfig(reader)
        return configParser.convertProfile().mUsername
    }
}
