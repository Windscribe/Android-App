/*
 * Copyright (c) 2026 Windscribe Limited.
 */

package com.windscribe.common.tunnel

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.net.InetAddress

/**
 * Which live interface may be kept for an incoming engine.
 *
 * The interface MTU is what apps are told to emit, not what the engine can carry, so the direction
 * that looks safe is the one that is not: keeping a 1500 interface for a 1280 engine hands the new
 * path packets it cannot take.
 */
class TunConfigMtuTest {
    private fun config(mtu: Int) =
        TunConfig(
            sessionName = "Windscribe",
            addresses = listOf(IpNetwork(InetAddress.getByName("10.0.0.2"), 32)),
            routes = listOf(IpNetwork(InetAddress.getByName("0.0.0.0"), 0)),
            dnsServers = emptyList(),
            mtu = mtu,
        )

    @Test
    fun `a 1280 interface is kept for a 1500 engine`() {
        assertTrue(config(1280).coversSameInterfaceAs(config(1500)))
    }

    @Test
    fun `a 1500 interface is rejected for a 1280 engine`() {
        assertFalse(
            "apps would emit 1500 into a path that carries 1280",
            config(1500).coversSameInterfaceAs(config(1280)),
        )
    }

    @Test
    fun `an equal mtu is kept`() {
        assertTrue(config(1280).coversSameInterfaceAs(config(1280)))
    }
}

/**
 * Excluding our own package from an interface that is already scoped by split tunnel.
 *
 * `VpnService.Builder` rejects an allow-list and a deny-list together, so in inclusive mode the
 * exclusion has to be stated by omission — appending to `disallowedApps` would silently discard the
 * user's allow-list and capture every app it named.
 */
class TunConfigSelfExclusionTest {
    private fun config(
        allowed: List<String> = emptyList(),
        disallowed: List<String> = emptyList(),
    ) = TunConfig(
        sessionName = "Windscribe",
        addresses = listOf(IpNetwork(InetAddress.getByName("10.0.0.2"), 32)),
        routes = listOf(IpNetwork(InetAddress.getByName("0.0.0.0"), 0)),
        dnsServers = emptyList(),
        mtu = 1280,
        allowedApps = allowed,
        disallowedApps = disallowed,
    )

    @Test
    fun `inclusive mode drops self from the allow-list and stays inclusive`() {
        val result =
            config(allowed = listOf("com.a", "com.windscribe.vpn", "com.b"))
                .withSelfExcluded("com.windscribe.vpn")!!
        assertEquals(listOf("com.a", "com.b"), result.allowedApps)
        assertTrue("appending here would discard the allow-list", result.disallowedApps.isEmpty())
    }

    @Test
    fun `exclusive mode appends self to the deny-list`() {
        val result = config(disallowed = listOf("com.a")).withSelfExcluded("com.windscribe.vpn")!!
        assertEquals(listOf("com.a", "com.windscribe.vpn"), result.disallowedApps)
        assertTrue(result.allowedApps.isEmpty())
    }

    @Test
    fun `no split tunnel at all just denies self`() {
        val result = config().withSelfExcluded("com.windscribe.vpn")!!
        assertEquals(listOf("com.windscribe.vpn"), result.disallowedApps)
    }

    @Test
    fun `an allow-list of only ourselves refuses rather than capturing every app`() {
        // An empty allowedApps is not "no apps" — VpnService reads it as no split tunnel at all, so
        // returning one here would widen an interface carrying a single app into one carrying the
        // whole device.
        assertNull(config(allowed = listOf("com.windscribe.vpn")).withSelfExcluded("com.windscribe.vpn"))
    }

    @Test
    fun `an allow-list with someone else in it still excludes us`() {
        val result =
            config(allowed = listOf("com.windscribe.vpn", "com.other"))
                .withSelfExcluded("com.windscribe.vpn")
        assertEquals(listOf("com.other"), result?.allowedApps)
    }

    @Test
    fun `applying it twice changes nothing, which is how callers detect it is already done`() {
        for (base in listOf(config(allowed = listOf("com.a")), config(disallowed = listOf("com.a")))) {
            val once = base.withSelfExcluded("com.windscribe.vpn")!!
            assertEquals(once, once.withSelfExcluded("com.windscribe.vpn"))
        }
    }
}
