/*
 * Copyright (c) 2026 Windscribe Limited.
 */

package com.windscribe.common.tunnel

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.net.InetAddress

/**
 * The blocking TUN and the LAN-bypass allowed-IP list are the same route set, and it is a set nobody
 * can eyeball: thirty prefixes whose whole job is the three gaps between them. These assert the
 * property rather than restating the list, so a typo in one prefix fails rather than reads fine.
 */
class TunConfigRoutesTest {
    private fun asLong(ip: String): Long {
        val bytes = InetAddress.getByName(ip).address
        return bytes.fold(0L) { acc, b -> (acc shl 8) or (b.toLong() and 0xFF) }
    }

    /** The prefixes as half-open [start, end) intervals over the 32-bit address space. */
    private fun intervals(): List<Pair<Long, Long>> =
        TunConfig.PUBLIC_ROUTES_V4
            .map { route ->
                val start = asLong(route.address.hostAddress!!)
                start to start + (1L shl (32 - route.prefixLength))
            }.sortedBy { it.first }

    @Test
    fun `covers every address that is not private or multicast`() {
        val merged = mutableListOf<Pair<Long, Long>>()
        intervals().forEach { (start, end) ->
            val last = merged.lastOrNull()
            // Overlapping prefixes would silently mask a missing one, so require they only ever abut.
            assertFalse("prefixes overlap at ${asIp(start)}", last != null && start < last.second)
            if (last != null && start == last.second) {
                merged[merged.size - 1] = last.first to end
            } else {
                merged.add(start to end)
            }
        }

        val gaps =
            merged.zipWithNext { a, b -> a.second to b.first } +
                listOf(merged.last().second to (1L shl 32))

        assertEquals(
            "the gaps are what stays on the physical interface",
            listOf(
                asLong("10.0.0.0") to asLong("11.0.0.0"),
                asLong("172.16.0.0") to asLong("172.32.0.0"),
                asLong("192.168.0.0") to asLong("192.169.0.0"),
                asLong("224.0.0.0") to (1L shl 32),
            ),
            gaps,
        )
        assertEquals("coverage starts at 0.0.0.0", 0L, merged.first().first)
    }

    @Test
    fun `blackhole routes everything unless lan bypass is on`() {
        val addresses = listOf(IpNetwork(InetAddress.getByName("172.16.252.2"), 32))

        val blocking = TunConfig.blackhole("s", addresses)
        assertEquals(listOf(IpNetwork(InetAddress.getByName("0.0.0.0"), 0)), blocking.routes)

        val bypassing = TunConfig.blackhole("s", addresses, lanBypass = true)
        assertEquals(TunConfig.PUBLIC_ROUTES_V4, bypassing.routes)
        assertTrue("a blackhole must not hand apps a resolver", bypassing.dnsServers.isEmpty())
    }

    // ------------------------------------------------------------------ split tunnel

    private val self = "com.windscribe.vpn"

    private fun blocked(
        allowedApps: List<String> = emptyList(),
        disallowedApps: List<String> = emptyList(),
    ) = TunConfig.blackhole(
        "s",
        listOf(IpNetwork(InetAddress.getByName("172.16.252.2"), 32)),
        allowedApps = allowedApps,
        disallowedApps = disallowedApps,
        selfPackage = self,
    )

    @Test
    fun `split tunnel off blocks everything except us`() {
        val config = blocked()
        assertEquals(emptyList<String>(), config.allowedApps)
        assertEquals(listOf(self), config.disallowedApps)
    }

    @Test
    fun `exclusive mode keeps the user's apps outside the block`() {
        val config = blocked(disallowedApps = listOf("com.example.bank", "com.example.tv"))
        assertEquals(emptyList<String>(), config.allowedApps)
        assertEquals(listOf("com.example.bank", "com.example.tv", self), config.disallowedApps)
    }

    @Test
    fun `inclusive mode blocks only the listed apps`() {
        val config = blocked(allowedApps = listOf("com.example.browser"))
        assertEquals(listOf("com.example.browser"), config.allowedApps)
        // Everything unlisted is already outside the interface, us included.
        assertEquals(emptyList<String>(), config.disallowedApps)
    }

    @Test
    fun `the two lists are never both set`() {
        // VpnService.Builder throws on a config carrying both, and every add is wrapped in
        // runCatching, so a mix would fail silently and half-apply.
        val config = blocked(allowedApps = listOf("com.example.browser"), disallowedApps = listOf("com.example.bank"))
        assertTrue(config.allowedApps.isEmpty() || config.disallowedApps.isEmpty())
        assertEquals(listOf("com.example.browser"), config.allowedApps)
    }

    @Test
    fun `we are never captured by our own block`() {
        // A blocked device that cannot reach the API can never connect its way out.
        assertTrue(blocked().disallowedApps.contains(self))
        assertTrue(blocked(disallowedApps = listOf("com.example.bank")).disallowedApps.contains(self))
        // Inclusive mode says it by omission — and drops us from the list if the user put us there.
        assertFalse(blocked(allowedApps = listOf("com.example.browser", self)).allowedApps.contains(self))
    }

    @Test
    fun `a narrowed blackhole is not covered by a default-routed one`() {
        val addresses = listOf(IpNetwork(InetAddress.getByName("172.16.252.2"), 32))
        val wide = TunConfig.blackhole("s", addresses)
        val narrow = TunConfig.blackhole("s", addresses, lanBypass = true)
        // Otherwise toggling the preference mid-session would skip the re-establish that applies it.
        assertFalse(wide.coversSameInterfaceAs(narrow))
        assertFalse(narrow.coversSameInterfaceAs(wide))
    }

    private fun asIp(value: Long) = listOf(24, 16, 8, 0).joinToString(".") { ((value shr it) and 0xFF).toString() }
}
