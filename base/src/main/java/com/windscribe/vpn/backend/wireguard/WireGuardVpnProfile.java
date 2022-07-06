/*
 * Copyright (c) 2021 Windscribe Limited.
 */

package com.windscribe.vpn.backend.wireguard;

import com.wireguard.config.BadConfigException;
import com.wireguard.config.Config;

import org.jetbrains.annotations.NotNull;
import org.strongswan.android.data.VpnProfile;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.io.Serializable;
import java.io.StringReader;

public class WireGuardVpnProfile implements Cloneable, Serializable {

    private final String mContent;

    public WireGuardVpnProfile(String content) {
        this.mContent = content;
    }

    public static Config createConfigFromString(String content) throws IOException, BadConfigException {
        Reader reader = new StringReader(content);
        try (BufferedReader bufferedReader = new BufferedReader(reader)) {
            return Config.parse(bufferedReader);
        }
    }

    public static String getHostName(String content) {
        try {
            Config config = createConfigFromString(content);
            return config.getPeers().get(0).getEndpoint().get().getHost();
        } catch (Exception e) {
            return null;
        }
    }

    public static boolean validConfig(String profileContent) {
        return profileContent.contains("[Interface]") && profileContent.contains("[Peer]");
    }

    @NotNull
    @Override
    public VpnProfile clone() {
        try {
            return (VpnProfile) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new AssertionError();
        }
    }

    public String getContent() {
        return mContent;
    }
}
