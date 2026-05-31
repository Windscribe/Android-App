package com.windscribe.vpn.commonutils

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayInputStream

class HashUtilsTest {
    @Test
    fun `sha256FromInputStream produces 34 char prefixed hex string`() {
        val hash = HashUtils.sha256FromInputStream(ByteArrayInputStream("hello".toByteArray()))

        assertTrue("Must start with 0x prefix", hash.startsWith("0x"))
        assertEquals("0x + 32 hex chars = 34", 34, hash.length)
        assertTrue("Hex chars only after prefix", hash.substring(2).all { it in '0'..'9' || it in 'a'..'f' })
    }

    @Test
    fun `sha256FromInputStream matches known truncated vector for hello`() {
        // SHA-256("hello") = 2cf24dba5fb0a30e26e83b2ac5b9e29e1b161e5c1fa7425e73043362938b9824
        // Last 16 bytes = "1b161e5c1fa7425e73043362938b9824"
        val hash = HashUtils.sha256FromInputStream(ByteArrayInputStream("hello".toByteArray()))

        assertEquals("0x1b161e5c1fa7425e73043362938b9824", hash)
    }

    @Test
    fun `sha256FromInputStream matches known truncated vector for empty input`() {
        // SHA-256("") = e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855
        // Last 16 bytes = "27ae41e4649b934ca495991b7852b855"
        val hash = HashUtils.sha256FromInputStream(ByteArrayInputStream(ByteArray(0)))

        assertEquals("0x27ae41e4649b934ca495991b7852b855", hash)
    }

    @Test
    fun `sha256FromInputStream is deterministic`() {
        val input = "windscribe-vpn-test-payload"
        val hash1 = HashUtils.sha256FromInputStream(ByteArrayInputStream(input.toByteArray()))
        val hash2 = HashUtils.sha256FromInputStream(ByteArrayInputStream(input.toByteArray()))

        assertEquals(hash1, hash2)
    }

    @Test
    fun `sha256FromInputStream differs for different input`() {
        val a = HashUtils.sha256FromInputStream(ByteArrayInputStream("a".toByteArray()))
        val b = HashUtils.sha256FromInputStream(ByteArrayInputStream("b".toByteArray()))

        assertNotEquals(a, b)
    }

    @Test
    fun `sha256FromInputStream handles input larger than 8KB buffer`() {
        // Force multiple read iterations through the 8192-byte buffer.
        val largeInput = ByteArray(8192 * 3 + 17) { (it % 256).toByte() }
        val hash = HashUtils.sha256FromInputStream(ByteArrayInputStream(largeInput))

        assertEquals(34, hash.length)
        assertTrue(hash.startsWith("0x"))
    }
}
