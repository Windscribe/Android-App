/*
 * Copyright (c) 2021 Windscribe Limited.
 */

package com.windscribe.vpn.backend.openvpn;

import java.io.Reader;

import de.blinkt.openvpn.core.ConfigParser;

public class OpenVPNConfigParser {

    public String getEmbeddedPassword(final Reader reader) throws Exception {
        ConfigParser configParser = new ConfigParser();
        configParser.parseConfig(reader);
        return configParser.convertProfile().mPassword;
    }

    public String getEmbeddedUsername(final Reader reader) throws Exception {
        ConfigParser configParser = new ConfigParser();
        configParser.parseConfig(reader);
        return configParser.convertProfile().mUsername;
    }
}