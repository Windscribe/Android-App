/*
 * Copyright (c) 2026 Windscribe Limited.
 */

package com.windscribe.common.tunnel

import java.net.InetAddress

/** An address with a prefix length. `:common` cannot see `com.wireguard.config.InetNetwork`. */
data class IpNetwork(
    val address: InetAddress,
    val prefixLength: Int,
) {
    override fun toString() = "${address.hostAddress}/$prefixLength"
}

/**
 * Protocol-neutral description of the TUN interface, which the host translates into a
 * `VpnService.Builder`.
 *
 * [coversSameInterfaceAs] is the important part: when a protocol hands over, the incoming config is
 * compared against the live one and no `establish()` happens if it is covered.
 *
 * Excluded routes are not here. They are identical for every protocol and depend on
 * `ExcludedIpHolder` in `:base`, so the host applies them while building.
 */
data class TunConfig(
    val sessionName: String,
    val addresses: List<IpNetwork>,
    val routes: List<IpNetwork>,
    val dnsServers: List<InetAddress>,
    val searchDomains: List<String> = emptyList(),
    val mtu: Int,
    val allowedApps: List<String> = emptyList(),
    val disallowedApps: List<String> = emptyList(),
    /** Set when the routes are not a single default route, so non-VPN families stay reachable. */
    val allowAllFamilies: Boolean = false,
) {
    /**
     * True when a TUN established for `this` can carry [other] without being re-established.
     * Conservative by design, so every field that changes what the kernel does must match.
     *
     * The live MTU may only be smaller: it is what apps emit, so a 1500 interface kept for a 1280
     * engine hands the new path packets too large for it.
     */
    fun coversSameInterfaceAs(other: TunConfig): Boolean =
        addresses.toSet() == other.addresses.toSet() &&
            routes.toSet() == other.routes.toSet() &&
            dnsServers.toSet() == other.dnsServers.toSet() &&
            searchDomains.toSet() == other.searchDomains.toSet() &&
            allowedApps.toSet() == other.allowedApps.toSet() &&
            disallowedApps.toSet() == other.disallowedApps.toSet() &&
            allowAllFamilies == other.allowAllFamilies &&
            mtu <= other.mtu

    /**
     * The same interface with our own package outside it, in whichever split-tunnel mode is in force.
     * `VpnService.Builder` rejects both lists together, so inclusive mode excludes by omission.
     *
     * @return null when that cannot be said: an inclusive list naming only [selfPackage], where an
     * empty allow-list would mean "no split tunnel" rather than "no apps".
     */
    fun withSelfExcluded(selfPackage: String): TunConfig? {
        if (allowedApps.isEmpty()) {
            return copy(disallowedApps = (disallowedApps + selfPackage).distinct())
        }
        val remaining = allowedApps.filterNot { it == selfPackage }
        return if (remaining.isEmpty()) null else copy(allowedApps = remaining)
    }

    companion object {
        private val DEFAULT_ROUTE_V4 = IpNetwork(InetAddress.getByName("0.0.0.0"), 0)

        /**
         * `0.0.0.0/0` with the RFC1918 ranges cut out. A route is what gets captured, so the gaps
         * are the bypass: 10/8, 172.16/12, 192.168/16, and 224/4 upwards for multicast.
         *
         * One list for both connected and blocking, so LAN bypass reaches the same devices in each.
         */
        val PUBLIC_ROUTES_V4: List<IpNetwork> =
            listOf(
                "0.0.0.0/5",
                "8.0.0.0/7",
                "11.0.0.0/8",
                "12.0.0.0/6",
                "16.0.0.0/4",
                "32.0.0.0/3",
                "64.0.0.0/2",
                "128.0.0.0/3",
                "160.0.0.0/5",
                "168.0.0.0/6",
                // 172.16.0.0/12 is the gap.
                "172.0.0.0/12",
                "172.32.0.0/11",
                "172.64.0.0/10",
                "172.128.0.0/9",
                "173.0.0.0/8",
                "174.0.0.0/7",
                "176.0.0.0/4",
                // 192.168.0.0/16 is the gap.
                "192.0.0.0/9",
                "192.128.0.0/11",
                "192.160.0.0/13",
                "192.169.0.0/16",
                "192.170.0.0/15",
                "192.172.0.0/14",
                "192.176.0.0/12",
                "192.192.0.0/10",
                "193.0.0.0/8",
                "194.0.0.0/7",
                "196.0.0.0/6",
                "200.0.0.0/5",
                "208.0.0.0/4",
            ).map { prefix ->
                val (address, length) = prefix.split("/")
                IpNetwork(InetAddress.getByName(address), length.toInt())
            }

        /**
         * A TUN that carries no engine: the blocking / DROP-state interface, and the shape a
         * session starts from.
         *
         * IPv6 needs no route to be blocked: a family with no address on the interface is dropped.
         *
         * @param lanBypass narrows the routes to [PUBLIC_ROUTES_V4] so private ranges stay on the
         * physical interface.
         * @param allowedApps split tunnel's inclusive list. Non-empty selects inclusive mode and
         * [disallowedApps] is dropped, since `VpnService.Builder` rejects both together.
         * @param disallowedApps split tunnel's exclusive list: apps that bypass the interface.
         * @param selfPackage kept outside the interface so a blocked device can still reach the API.
         * Inclusive mode states this by omission, so the package is only removed from [allowedApps].
         */
        fun blackhole(
            sessionName: String,
            addresses: List<IpNetwork>,
            mtu: Int = 1280,
            lanBypass: Boolean = false,
            allowedApps: List<String> = emptyList(),
            disallowedApps: List<String> = emptyList(),
            selfPackage: String? = null,
        ): TunConfig {
            val inclusive = allowedApps.isNotEmpty()
            return TunConfig(
                sessionName = sessionName,
                addresses = addresses,
                routes = if (lanBypass) PUBLIC_ROUTES_V4 else listOf(DEFAULT_ROUTE_V4),
                dnsServers = emptyList(),
                mtu = mtu,
                allowedApps = if (inclusive) allowedApps.filterNot { it == selfPackage } else emptyList(),
                disallowedApps =
                    if (inclusive) {
                        emptyList()
                    } else {
                        (disallowedApps + listOfNotNull(selfPackage)).distinct()
                    },
            )
        }
    }
}
