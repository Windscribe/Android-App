/*
 * Copyright (c) 2026 Windscribe Limited.
 */

package com.wireguard.android.engine;

import com.windscribe.common.tunnel.IpNetwork;
import com.windscribe.common.tunnel.TunConfig;
import com.wireguard.config.Config;
import com.wireguard.config.InetNetwork;
import com.wireguard.config.Peer;

import java.net.Inet6Address;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;

/**
 * Translates a WireGuard {@link Config} into the protocol-neutral {@link TunConfig} the host uses
 * to build the interface.
 *
 * <p>Keep the translation faithful rather than tidy: any difference in the resulting config is one
 * {@link TunConfig#coversSameInterfaceAs} notices, forcing an avoidable re-establish on handover.
 */
public final class WireGuardTunConfigMapper {

    private static final int DEFAULT_MTU = 1280;

    private WireGuardTunConfigMapper() {
    }

    public static TunConfig toTunConfig(final String sessionName, final Config config) {
        final List<IpNetwork> addresses = new ArrayList<>();
        boolean hasIpV6Address = false;
        for (final InetNetwork addr : config.getInterface().getAddresses()) {
            addresses.add(new IpNetwork(addr.getAddress(), addr.getMask()));
            if (addr.getAddress() instanceof Inet6Address) {
                hasIpV6Address = true;
            }
        }

        final List<InetAddress> dnsServers = new ArrayList<>(config.getInterface().getDnsServers());
        final List<String> searchDomains = new ArrayList<>(config.getInterface().getDnsSearchDomains());

        final List<IpNetwork> routes = new ArrayList<>();
        boolean sawDefaultRoute = false;
        for (final Peer peer : config.getPeers()) {
            for (final InetNetwork allowed : peer.getAllowedIps()) {
                if (allowed.getMask() == 0) {
                    sawDefaultRoute = true;
                }
                // ::/0 is only routable if the interface actually has an IPv6 address; adding it
                // otherwise produces an interface that blackholes v6 rather than bypassing it.
                if (!allowed.toString().equals("::/0") || hasIpV6Address) {
                    routes.add(new IpNetwork(allowed.getAddress(), allowed.getMask()));
                }
            }
        }

        // "Kill-switch" semantics from upstream: only a single peer carrying a default route may
        // capture every family. Anything else leaves the other families reachable off-tunnel.
        final boolean allowAllFamilies = !(sawDefaultRoute && config.getPeers().size() == 1);

        return new TunConfig(
                sessionName,
                addresses,
                routes,
                dnsServers,
                searchDomains,
                config.getInterface().getMtu().orElse(DEFAULT_MTU),
                new ArrayList<>(config.getInterface().getIncludedApplications()),
                new ArrayList<>(config.getInterface().getExcludedApplications()),
                allowAllFamilies);
    }
}
