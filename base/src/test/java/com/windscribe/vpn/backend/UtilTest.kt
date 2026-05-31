package com.windscribe.vpn.backend

import com.windscribe.vpn.apppreference.PreferencesKeyConstants
import com.windscribe.vpn.autoconnection.ProtocolConnectionStatus
import com.windscribe.vpn.autoconnection.ProtocolInformation
import com.windscribe.vpn.serverlist.entity.Server
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class UtilTest {
    private fun server(
        id: Int,
        weight: Int,
    ): Server =
        Server(
            id = id,
            hostname = "host-$id",
            ip = "10.0.0.$id",
            ip2 = "10.0.0.$id",
            ip3 = "10.0.0.$id",
            datacenterId = 1,
            weight = weight,
            health = 100,
        )

    // ---------- getRandomNode ----------

    @Test
    fun `getRandomNode returns 0 for single-node list`() {
        val nodes = listOf(server(1, 1))

        repeat(20) {
            assertEquals(0, Util.getRandomNode(lastUsedIndex = -1, attempt = 0, nodes = nodes))
        }
    }

    @Test
    fun `getRandomNode returns valid index within list bounds`() {
        val nodes = (0..4).map { server(it, weight = 1) }

        repeat(200) {
            val index = Util.getRandomNode(lastUsedIndex = -1, attempt = 0, nodes = nodes)
            assertTrue("Index $index out of bounds for size ${nodes.size}", index in nodes.indices)
        }
    }

    @Test
    fun `getRandomNode with attempt greater than zero avoids lastUsedIndex when possible`() {
        // Two nodes — with attempt > 0 the picker should never return lastUsedIndex
        // because the retry loop runs until a different index is chosen.
        val nodes = listOf(server(0, 1), server(1, 1))
        val lastUsed = 0

        repeat(100) {
            val index = Util.getRandomNode(lastUsedIndex = lastUsed, attempt = 1, nodes = nodes)
            assertEquals(1, index)
        }
    }

    @Test
    fun `getRandomNode honours weight distribution`() {
        // Heavily-weighted node should dominate selections.
        val nodes =
            listOf(
                server(0, weight = 1),
                server(1, weight = 99),
            )

        var heavyHits = 0
        val trials = 2000
        repeat(trials) {
            if (Util.getRandomNode(lastUsedIndex = -1, attempt = 0, nodes = nodes) == 1) heavyHits++
        }

        // With 99/100 weight, expect well over 80% selection of the heavy node.
        assertTrue("Heavy node only chosen $heavyHits / $trials times", heavyHits > trials * 0.8)
    }

    // ---------- validIpAddress ----------

    @Test
    fun `validIpAddress accepts standard IPv4`() {
        assertTrue(Util.validIpAddress("127.0.0.1"))
        assertTrue(Util.validIpAddress("10.0.0.1"))
        assertTrue(Util.validIpAddress("255.255.255.255"))
        assertTrue(Util.validIpAddress("0.0.0.0"))
    }

    @Test
    fun `validIpAddress accepts IPv6`() {
        assertTrue(Util.validIpAddress("::1"))
        assertTrue(Util.validIpAddress("2001:db8::1"))
        assertTrue(Util.validIpAddress("fe80::1"))
    }

    @Test
    fun `validIpAddress rejects malformed input`() {
        assertFalse(Util.validIpAddress("not.an.ip.address"))
        assertFalse(Util.validIpAddress("256.256.256.256"))
        assertFalse(Util.validIpAddress("hello world"))
    }

    // ---------- getModifiedIpAddress ----------

    @Test
    fun `getModifiedIpAddress returns input unchanged when shorter than 32 chars`() {
        val short = "1.2.3.4"
        assertEquals(short, Util.getModifiedIpAddress(short))
    }

    @Test
    fun `getModifiedIpAddress collapses zero runs in long strings`() {
        // Length 32+ triggers the replacement chain: 0000→0, then 000→"", then 00→"".
        val padded = "2001000000000000abcd000000001234"
        val result = Util.getModifiedIpAddress(padded)

        // Just verify the transformation happened — exact output is the
        // documented (lossy) behaviour of this legacy helper.
        assertTrue("Long input should be transformed", result != padded)
        assertFalse("Result should not contain '0000' run", result.contains("0000"))
    }

    // ---------- getProtocolLabel ----------

    @Test
    fun `getProtocolLabel maps every known protocol`() {
        assertEquals("IKEv2", Util.getProtocolLabel(PreferencesKeyConstants.PROTO_IKev2))
        assertEquals("UDP", Util.getProtocolLabel(PreferencesKeyConstants.PROTO_UDP))
        assertEquals("TCP", Util.getProtocolLabel(PreferencesKeyConstants.PROTO_TCP))
        assertEquals("Stealth", Util.getProtocolLabel(PreferencesKeyConstants.PROTO_STEALTH))
        assertEquals("WireGuard", Util.getProtocolLabel(PreferencesKeyConstants.PROTO_WIRE_GUARD))
        assertEquals("WStunnel", Util.getProtocolLabel(PreferencesKeyConstants.PROTO_WS_TUNNEL))
    }

    @Test
    fun `getProtocolLabel falls back to IKEv2 for unknown values`() {
        assertEquals("IKEv2", Util.getProtocolLabel("nonexistent"))
        assertEquals("IKEv2", Util.getProtocolLabel(""))
    }

    // ---------- getHostNameFromOpenVPNConfig ----------

    @Test
    fun `getHostNameFromOpenVPNConfig extracts hostname from remote line`() {
        val config =
            """
            client
            dev tun
            remote us-east.windscribe.com 443 udp
            cipher AES-256-GCM
            """.trimIndent()

        assertEquals("us-east.windscribe.com", Util.getHostNameFromOpenVPNConfig(config))
    }

    @Test
    fun `getHostNameFromOpenVPNConfig returns null when no remote line has port and protocol`() {
        // Helper requires splits.size > 2 — a bare "remote host" line is skipped.
        val config =
            """
            client
            remote single-token
            """.trimIndent()

        assertNull(Util.getHostNameFromOpenVPNConfig(config))
    }

    @Test
    fun `getHostNameFromOpenVPNConfig returns null when no remote line present`() {
        val config =
            """
            client
            dev tun
            cipher AES-256-GCM
            """.trimIndent()

        assertNull(Util.getHostNameFromOpenVPNConfig(config))
    }

    // ---------- buildProtocolInformation (with provided list, no appContext) ----------

    @Test
    fun `buildProtocolInformation updates port and timeout on matching entry`() {
        val list =
            listOf(
                ProtocolInformation(
                    PreferencesKeyConstants.PROTO_UDP,
                    "443",
                    "udp",
                    ProtocolConnectionStatus.Disconnected,
                ),
                ProtocolInformation(
                    PreferencesKeyConstants.PROTO_TCP,
                    "443",
                    "tcp",
                    ProtocolConnectionStatus.Disconnected,
                ),
            )

        val result = Util.buildProtocolInformation(list, PreferencesKeyConstants.PROTO_TCP, "1194")

        assertNotNull(result)
        assertEquals(PreferencesKeyConstants.PROTO_TCP, result.protocol)
        assertEquals("1194", result.port)
        assertEquals(10, result.autoConnectTimeLeft)
    }
}
