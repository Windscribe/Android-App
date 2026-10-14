package com.windscribe.vpn.wsnet

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class WsNetLogTest {
    private fun native(
        level: String,
        message: String,
    ) = """{"tm": "2026-09-21 14:30:50.757", "lvl": "$level", "mod": "wsnet", "msg": "$message"}"""

    @Test
    fun `the level on the record is the level kept`() {
        assertEquals(
            "error" to "Curl request error: SSL connect error",
            parseWsNetLog(native("error", "Curl request error: SSL connect error")),
        )
        assertEquals("info" to "wsnet version: 1.5.32", parseWsNetLog(native("info", "wsnet version: 1.5.32")))
        assertEquals("debug" to "New curl request", parseWsNetLog(native("debug", "New curl request")))
    }

    @Test
    fun `escapes in the message are undone`() {
        val (_, message) = parseWsNetLog(native("debug", """  Trying 193b2cdee:443...\n"""))!!
        assertEquals("  Trying 193b2cdee:443...\n", message)
    }

    @Test
    fun `a quote inside the message survives`() {
        val quoted = """{"tm": "x", "lvl": "debug", "mod": "wsnet", "msg": "linker: \"libovpnexec.so\" is not a directory"}"""
        assertEquals("""linker: "libovpnexec.so" is not a directory""", parseWsNetLog(quoted)?.second)
    }

    @Test
    fun `a record with no level still yields its message`() {
        assertEquals("" to "Clear DNS cache", parseWsNetLog("""{"mod": "wsnet", "msg": "Clear DNS cache"}"""))
    }

    @Test
    fun `a line that will not parse is kept whole rather than lost`() {
        assertEquals("" to "not json at all", parseWsNetLog("not json at all"))
        assertEquals("" to """{"lvl": "info"}""", parseWsNetLog("""{"lvl": "info"}"""))
    }

    @Test
    fun `only an empty message is dropped`() {
        assertNull(parseWsNetLog(native("info", "")))
    }

    @Test
    fun `curl requests are kept - they are the evidence a worker ran`() {
        assertFalse(isWsNetNoise("New curl request : https://api.windscribe.com/Session"))
        assertFalse(isWsNetNoise("New curl request : https://api.windscribe.com/MyIp"))
        assertFalse(isWsNetNoise("New curl request : https://checkip.windscribe.com/"))
        assertFalse(isWsNetNoise("Curl request error: SSL connect error"))
    }

    @Test
    fun `the bridge token line is dropped, the session token line it duplicates is not`() {
        assertTrue(isWsNetNoise("BridgeTokenRequest: token parsed successfully"))
        assertFalse(isWsNetNoise("Successfully received session token for host ''"))
    }

    @Test
    fun `the pre-existing suppressions still hold`() {
        assertTrue(isWsNetNoise("connecting to 10.0.0.1:6464/latency"))
        assertTrue(isWsNetNoise("BridgeAPI_impl::handleRequest"))
    }
}
