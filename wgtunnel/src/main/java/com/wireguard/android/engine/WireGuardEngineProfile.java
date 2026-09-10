/*
 * Copyright (c) 2026 Windscribe Limited.
 */

package com.wireguard.android.engine;

import com.windscribe.common.tunnel.EngineProfile;
import com.wireguard.config.Config;

/** The WireGuard-shaped payload behind the opaque {@link EngineProfile} that {@code :common} sees. */
public final class WireGuardEngineProfile implements EngineProfile {

    public static final String PROTOCOL = "wg";

    private final Config config;
    private final String tunnelName;

    public WireGuardEngineProfile(final String tunnelName, final Config config) {
        this.tunnelName = tunnelName;
        this.config = config;
    }

    @Override
    public String getProtocol() {
        return PROTOCOL;
    }

    public Config getConfig() {
        return config;
    }

    public String getTunnelName() {
        return tunnelName;
    }
}
