package com.windscribe.vpn.commonutils

import org.junit.Assert.*
import org.junit.Test

/**
 * Comprehensive unit tests for WireguardUtil.
 * Tests all individual functions and edge cases for IP generation from public keys.
 */
class WireguardUtilTest {

    // ==================== parseCIDR Tests ====================

    @Test
    fun `parseCIDR should correctly parse valid CIDR notation`() {
        val cidr = WireguardUtil.parseCIDR("100.64.0.0/10")

        assertEquals(10, cidr.prefixLength)
        assertEquals(22, cidr.hostBits)
        assertEquals(0x003FFFFF, cidr.hostMask)

        // Network address: 100.64.0.0 = 0x64400000
        val expectedNetworkAddress = (100 shl 24) or (64 shl 16)
        assertEquals(expectedNetworkAddress, cidr.networkAddress)
    }

    @Test
    fun `parseCIDR should handle different prefix lengths`() {
        val cidr8 = WireguardUtil.parseCIDR("10.0.0.0/8")
        assertEquals(8, cidr8.prefixLength)
        assertEquals(24, cidr8.hostBits)
        assertEquals(0x00FFFFFF, cidr8.hostMask)

        val cidr16 = WireguardUtil.parseCIDR("172.16.0.0/12")
        assertEquals(12, cidr16.prefixLength)
        assertEquals(20, cidr16.hostBits)
        assertEquals(0x000FFFFF, cidr16.hostMask)

        val cidr24 = WireguardUtil.parseCIDR("192.168.1.0/24")
        assertEquals(24, cidr24.prefixLength)
        assertEquals(8, cidr24.hostBits)
        assertEquals(0x000000FF, cidr24.hostMask)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `parseCIDR should throw on missing slash`() {
        WireguardUtil.parseCIDR("100.64.0.0")
    }

    @Test(expected = IllegalArgumentException::class)
    fun `parseCIDR should throw on invalid prefix length format`() {
        WireguardUtil.parseCIDR("100.64.0.0/abc")
    }

    @Test(expected = IllegalArgumentException::class)
    fun `parseCIDR should throw on negative prefix length`() {
        WireguardUtil.parseCIDR("100.64.0.0/-1")
    }

    @Test(expected = IllegalArgumentException::class)
    fun `parseCIDR should throw on prefix length greater than 32`() {
        WireguardUtil.parseCIDR("100.64.0.0/33")
    }

    @Test(expected = IllegalArgumentException::class)
    fun `parseCIDR should throw on invalid IP format`() {
        WireguardUtil.parseCIDR("100.64.0/10")
    }

    // ==================== ipStringToInt Tests ====================

    @Test
    fun `ipStringToInt should convert valid IP addresses`() {
        assertEquals(0x64400000, WireguardUtil.ipStringToInt("100.64.0.0"))
        assertEquals(0x0A000000, WireguardUtil.ipStringToInt("10.0.0.0"))
        assertEquals(0xC0A80101.toInt(), WireguardUtil.ipStringToInt("192.168.1.1"))
        assertEquals(0x7F000001, WireguardUtil.ipStringToInt("127.0.0.1"))
        assertEquals(-1, WireguardUtil.ipStringToInt("255.255.255.255"))
    }

    @Test(expected = IllegalArgumentException::class)
    fun `ipStringToInt should throw on too few octets`() {
        WireguardUtil.ipStringToInt("192.168.1")
    }

    @Test(expected = IllegalArgumentException::class)
    fun `ipStringToInt should throw on too many octets`() {
        WireguardUtil.ipStringToInt("192.168.1.1.1")
    }

    @Test(expected = IllegalArgumentException::class)
    fun `ipStringToInt should throw on invalid octet values`() {
        WireguardUtil.ipStringToInt("192.168.1.abc")
    }

    @Test(expected = IllegalArgumentException::class)
    fun `ipStringToInt should throw on octet out of range`() {
        WireguardUtil.ipStringToInt("192.168.1.256")
    }

    // ==================== ipIntToString Tests ====================

    @Test
    fun `ipIntToString should convert integers to IP addresses`() {
        assertEquals("100.64.0.0", WireguardUtil.ipIntToString(0x64400000))
        assertEquals("10.0.0.0", WireguardUtil.ipIntToString(0x0A000000))
        assertEquals("192.168.1.1", WireguardUtil.ipIntToString(0xC0A80101.toInt()))
        assertEquals("127.0.0.1", WireguardUtil.ipIntToString(0x7F000001))
        assertEquals("255.255.255.255", WireguardUtil.ipIntToString(-1))
    }

    @Test
    fun `ipIntToString and ipStringToInt should be inverse operations`() {
        val testIPs = listOf(
            "100.64.0.0",
            "10.0.0.0",
            "172.16.0.0",
            "192.168.1.1",
            "127.0.0.1",
            "1.2.3.4",
            "255.255.255.255"
        )

        testIPs.forEach { ip ->
            val converted = WireguardUtil.ipIntToString(WireguardUtil.ipStringToInt(ip))
            assertEquals("Failed round-trip for $ip", ip, converted)
        }
    }

    // ==================== hashPublicKey Tests ====================

    @Test
    fun `hashPublicKey should produce consistent hash for same key`() {
        // Valid base64 WireGuard public key (32 bytes)
        val publicKey = "YWJjZGVmZ2hpamtsbW5vcHFyc3R1dnd4eXoxMjM0NTY="

        val hash1 = WireguardUtil.hashPublicKey(publicKey)
        val hash2 = WireguardUtil.hashPublicKey(publicKey)

        assertEquals("Hash should be deterministic", hash1, hash2)
    }

    @Test
    fun `hashPublicKey should produce different hashes for different keys`() {
        val key1 = "YWJjZGVmZ2hpamtsbW5vcHFyc3R1dnd4eXoxMjM0NTY="
        val key2 = "MTIzNDU2Nzg5MGFiY2RlZmdoaWprbG1ub3BxcnN0dQ=="

        val hash1 = WireguardUtil.hashPublicKey(key1)
        val hash2 = WireguardUtil.hashPublicKey(key2)

        assertNotEquals("Different keys should produce different hashes", hash1, hash2)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `hashPublicKey should throw on invalid base64`() {
        WireguardUtil.hashPublicKey("not-valid-base64!!!")
    }

    // ==================== generateIPFromHash Tests ====================

    @Test
    fun `generateIPFromHash should combine network and host portions correctly`() {
        val cidr = WireguardUtil.parseCIDR("100.64.0.0/10")

        // Test with zero hash - should give network address
        val ip1 = WireguardUtil.generateIPFromHash(cidr, 0x00000000)
        assertEquals("100.64.0.0", WireguardUtil.ipIntToString(ip1))

        // Test with all host bits set
        val ip2 = WireguardUtil.generateIPFromHash(cidr, 0xFFFFFFFF.toInt())
        assertEquals("100.127.255.255", WireguardUtil.ipIntToString(ip2))
    }

    @Test
    fun `generateIPFromHash should respect CIDR network portion`() {
        val cidr = WireguardUtil.parseCIDR("100.64.0.0/10")

        // Even with random hash, IP should always start with 100.64-127
        val hash = 0x12345678
        val ip = WireguardUtil.generateIPFromHash(cidr, hash)
        val ipString = WireguardUtil.ipIntToString(ip)

        assertTrue("IP should start with 100.", ipString.startsWith("100."))

        // Second octet should be in range 64-127 (10 bits: 01xxxxxx)
        val parts = ipString.split(".")
        val secondOctet = parts[1].toInt()
        assertTrue("Second octet should be 64-127, got $secondOctet",
            secondOctet in 64..127)
    }

    @Test
    fun `generateIPFromHash should work with different CIDR ranges`() {
        // /8 network (10.0.0.0/8)
        val cidr8 = WireguardUtil.parseCIDR("10.0.0.0/8")
        val ip8 = WireguardUtil.generateIPFromHash(cidr8, 0x01020304)
        val ipString8 = WireguardUtil.ipIntToString(ip8)
        assertTrue("IP should start with 10.", ipString8.startsWith("10."))

        // /24 network (192.168.1.0/24)
        val cidr24 = WireguardUtil.parseCIDR("192.168.1.0/24")
        val ip24 = WireguardUtil.generateIPFromHash(cidr24, 0x000000FF)
        assertEquals("192.168.1.255", WireguardUtil.ipIntToString(ip24))
    }

    // ==================== generateWireguardIP Tests (Integration) ====================

    @Test
    fun `generateWireguardIP should generate valid IP in CIDR range`() {
        val publicKey = "YWJjZGVmZ2hpamtsbW5vcHFyc3R1dnd4eXoxMjM0NTY="
        val cidr = "100.64.0.0/10"

        val ip = WireguardUtil.generateWireguardIP(publicKey, cidr)

        // Should be valid IP format
        val parts = ip.split(".")
        assertEquals(4, parts.size)

        // Should start with 100
        assertEquals("100", parts[0])

        // Second octet should be 64-127
        val secondOctet = parts[1].toInt()
        assertTrue("Second octet should be 64-127, got $secondOctet",
            secondOctet in 64..127)
    }

    @Test
    fun `generateWireguardIP should be deterministic`() {
        val publicKey = "YWJjZGVmZ2hpamtsbW5vcHFyc3R1dnd4eXoxMjM0NTY="
        val cidr = "100.64.0.0/10"

        val ip1 = WireguardUtil.generateWireguardIP(publicKey, cidr)
        val ip2 = WireguardUtil.generateWireguardIP(publicKey, cidr)

        assertEquals("Same inputs should produce same IP", ip1, ip2)
    }

    @Test
    fun `generateWireguardIP should produce different IPs for different keys`() {
        val key1 = "YWJjZGVmZ2hpamtsbW5vcHFyc3R1dnd4eXoxMjM0NTY="
        val key2 = "MTIzNDU2Nzg5MGFiY2RlZmdoaWprbG1ub3BxcnN0dQ=="
        val cidr = "100.64.0.0/10"

        val ip1 = WireguardUtil.generateWireguardIP(key1, cidr)
        val ip2 = WireguardUtil.generateWireguardIP(key2, cidr)

        assertNotEquals("Different keys should produce different IPs", ip1, ip2)
    }

    @Test
    fun `generateWireguardIP should work with different CIDR ranges`() {
        val publicKey = "YWJjZGVmZ2hpamtsbW5vcHFyc3R1dnd4eXoxMjM0NTY="

        val ip1 = WireguardUtil.generateWireguardIP(publicKey, "10.0.0.0/8")
        assertTrue(ip1.startsWith("10."))

        val ip2 = WireguardUtil.generateWireguardIP(publicKey, "172.16.0.0/12")
        assertTrue(ip2.startsWith("172."))

        val ip3 = WireguardUtil.generateWireguardIP(publicKey, "192.168.0.0/16")
        assertTrue(ip3.startsWith("192.168."))
    }

    @Test(expected = IllegalArgumentException::class)
    fun `generateWireguardIP should throw on invalid CIDR`() {
        val publicKey = "YWJjZGVmZ2hpamtsbW5vcHFyc3R1dnd4eXoxMjM0NTY="
        WireguardUtil.generateWireguardIP(publicKey, "invalid-cidr")
    }

    @Test(expected = IllegalArgumentException::class)
    fun `generateWireguardIP should throw on invalid public key`() {
        WireguardUtil.generateWireguardIP("invalid-key", "100.64.0.0/10")
    }

    // ==================== Edge Cases ====================

    @Test
    fun `CIDRBlock with slash32 should have single IP`() {
        val cidr = WireguardUtil.parseCIDR("192.168.1.1/32")
        assertEquals(0, cidr.hostBits)
        assertEquals(0, cidr.hostMask)

        // Any hash should produce the same IP (network address)
        val ip1 = WireguardUtil.generateIPFromHash(cidr, 0x00000000)
        val ip2 = WireguardUtil.generateIPFromHash(cidr, 0xFFFFFFFF.toInt())

        assertEquals("192.168.1.1", WireguardUtil.ipIntToString(ip1))
        assertEquals("192.168.1.1", WireguardUtil.ipIntToString(ip2))
    }

    @Test
    fun `CIDRBlock with slash0 should allow entire address space`() {
        val cidr = WireguardUtil.parseCIDR("0.0.0.0/0")
        assertEquals(32, cidr.hostBits)
        assertEquals(-1, cidr.hostMask)

        // Hash should determine entire IP
        val ip = WireguardUtil.generateIPFromHash(cidr, 0xC0A80101.toInt())
        assertEquals("192.168.1.1", WireguardUtil.ipIntToString(ip))
    }

    @Test
    fun `hash value larger than host mask should be masked correctly`() {
        val cidr = WireguardUtil.parseCIDR("100.64.0.0/10")

        // Even with large hash, should only use bottom 22 bits
        val largeHash = 0xFFFFFFFF.toInt()
        val ip = WireguardUtil.generateIPFromHash(cidr, largeHash)

        // Should produce 100.127.255.255 (network bits + all host bits set)
        assertEquals("100.127.255.255", WireguardUtil.ipIntToString(ip))
    }

    // ==================== Server Compatibility Tests ====================

    @Test
    fun `generateWireguardIP should match PHP server implementation`() {
        // Test with actual public key from logs
        val publicKey = "FYceno5LqCnzB47ULxVJXNCeiwT5aVeuJmDErj0CSn8="
        val cidr = "100.64.0.0/10"

        val ip = WireguardUtil.generateWireguardIP(publicKey, cidr)

        // Should be in valid range
        assertTrue("IP should start with 100.", ip.startsWith("100."))
        val parts = ip.split(".")
        val secondOctet = parts[1].toInt()
        assertTrue("Second octet should be 64-127, got $secondOctet",
            secondOctet in 64..127)

        println("Generated IP for public key $publicKey: $ip")
    }

    @Test
    fun `implementation should match PHP server algorithm`() {
        // Verify that our implementation matches the PHP server's approach:
        // $v = hash & 0x003FFFFF
        // $octet2 = 64 | (($v >> 16) & 0xFF)
        // $octet3 = ($v >> 8) & 0xFF
        // $octet4 = $v & 0xFF

        val publicKey = "YWJjZGVmZ2hpamtsbW5vcHFyc3R1dnd4eXoxMjM0NTY="
        val cidr = "100.64.0.0/10"

        // Get the hash value
        val hashValue = WireguardUtil.hashPublicKey(publicKey)

        // PHP approach: mask to 22 bits then construct octets
        val v = hashValue and 0x003FFFFF
        val phpOctet2 = 64 or ((v ushr 16) and 0xFF)
        val phpOctet3 = (v ushr 8) and 0xFF
        val phpOctet4 = v and 0xFF
        val phpIP = "100.$phpOctet2.$phpOctet3.$phpOctet4"

        // Kotlin approach: use our implementation
        val kotlinIP = WireguardUtil.generateWireguardIP(publicKey, cidr)

        assertEquals("Kotlin implementation should match PHP server", phpIP, kotlinIP)
        println("PHP approach: $phpIP")
        println("Kotlin approach: $kotlinIP")
    }
}
