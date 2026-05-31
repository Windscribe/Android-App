package com.windscribe.vpn.backend.utils

import com.windscribe.vpn.apppreference.PreferencesKeyConstants
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ProtocolConfigTest {
    @Test
    fun `heading maps every supported protocol`() {
        assertEquals(
            "UDP",
            ProtocolConfig(PreferencesKeyConstants.PROTO_UDP, "443", ProtocolConfig.Type.Auto).heading,
        )
        assertEquals(
            "TCP",
            ProtocolConfig(PreferencesKeyConstants.PROTO_TCP, "443", ProtocolConfig.Type.Auto).heading,
        )
        assertEquals(
            "Stealth",
            ProtocolConfig(PreferencesKeyConstants.PROTO_STEALTH, "443", ProtocolConfig.Type.Auto).heading,
        )
        assertEquals(
            "WStunnel",
            ProtocolConfig(PreferencesKeyConstants.PROTO_WS_TUNNEL, "443", ProtocolConfig.Type.Auto).heading,
        )
        assertEquals(
            "WireGuard",
            ProtocolConfig(PreferencesKeyConstants.PROTO_WIRE_GUARD, "443", ProtocolConfig.Type.Auto).heading,
        )
    }

    @Test
    fun `heading falls back to IKEv2 for ikev2 protocol and unknown values`() {
        assertEquals(
            "IKEv2",
            ProtocolConfig(PreferencesKeyConstants.PROTO_IKev2, "500", ProtocolConfig.Type.Auto).heading,
        )
        assertEquals(
            "IKEv2",
            ProtocolConfig("totally-bogus-protocol", "1", ProtocolConfig.Type.Auto).heading,
        )
        assertEquals(
            "IKEv2",
            ProtocolConfig("", "1", ProtocolConfig.Type.Auto).heading,
        )
    }

    @Test
    fun `equals is reflexive, symmetric, and field-sensitive`() {
        val a = ProtocolConfig("udp", "443", ProtocolConfig.Type.Auto)
        val b = ProtocolConfig("udp", "443", ProtocolConfig.Type.Auto)
        val differentProtocol = ProtocolConfig("tcp", "443", ProtocolConfig.Type.Auto)
        val differentPort = ProtocolConfig("udp", "1194", ProtocolConfig.Type.Auto)
        val differentType = ProtocolConfig("udp", "443", ProtocolConfig.Type.Manual)

        assertTrue(a == a)
        assertEquals(a, b)
        assertEquals(b, a)
        assertNotEquals(a, differentProtocol)
        assertNotEquals(a, differentPort)
        assertNotEquals(a, differentType)
        assertFalse(a.equals(null))
        assertFalse(a.equals("udp:443"))
    }

    @Test
    fun `hashCode is consistent with equals`() {
        val a = ProtocolConfig("udp", "443", ProtocolConfig.Type.Auto)
        val b = ProtocolConfig("udp", "443", ProtocolConfig.Type.Auto)

        assertEquals(a.hashCode(), b.hashCode())
    }

    @Test
    fun `toString includes protocol and port`() {
        val cfg = ProtocolConfig("wg", "51820", ProtocolConfig.Type.Preferred)

        assertEquals("Protocol Config: wg:51820", cfg.toString())
    }

    @Test
    fun `mutable protocol and port are reflected in heading and toString`() {
        val cfg = ProtocolConfig("udp", "443", ProtocolConfig.Type.Auto)

        cfg.protocol = "tcp"
        cfg.port = "1194"

        assertEquals("TCP", cfg.heading)
        assertEquals("Protocol Config: tcp:1194", cfg.toString())
    }
}
