package com.windscribe.vpn.commonutils

import ch.qos.logback.classic.Level
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.UUID

class LogRecordTest {
    private fun render(
        message: String,
        throwable: Throwable? = null,
    ): List<String> =
        layout(PATTERN)
            .doLayout(event(message, throwable, logger = "wsnet-wrapper", level = Level.WARN))
            .trimEnd('\n')
            .lines()

    @Test
    fun `a rendered record comes back apart into its fields`() {
        val record = parseLogLine(render("New curl request").single())
        assertNull(record.raw)
        assertEquals("warn", record.level)
        assertEquals("wsnet-wrapper", record.module)
        assertEquals("New curl request", record.message)
        assertTrue("date is yyyy-MM-dd", record.date.matches(Regex("""\d{4}-\d{2}-\d{2}""")))
        assertTrue("time carries millis", record.time.matches(Regex("""\d{2}:\d{2}:\d{2}\.\d{3}""")))
    }

    @Test
    fun `the date is split off so rows need only carry the time`() {
        val record = parseLogLine("2026-09-21 12:54:25.272 debug [worker-14] data - hi")
        assertEquals("2026-09-21", record.date)
        assertEquals("12:54:25.272", record.time)
    }

    @Test
    fun `a stack trace is folded back into the record it belongs to`() {
        val records = parseLogLines(render("connect failed", IllegalStateException("boom")))
        assertEquals("the trace is not a row of its own", 1, records.size)
        val message = records.single().message
        assertEquals("connect failed", message.lineSequence().first())
        assertTrue("the trace came along", message.lines().size > 2)
        assertTrue(message.contains("IllegalStateException: boom"))
    }

    @Test
    fun `a log of nothing we wrote stays a run of raw lines`() {
        val charon =
            listOf(
                "00[DMN] Starting IKE charon daemon (strongSwan 5.9.14)",
                "00[LIB] providers loaded by OpenSSL: legacy default",
            )
        assertEquals(charon, parseLogLines(charon).map { it.raw })
    }

    @Test
    fun `a connection id is picked up only when the line carries one`() {
        try {
            ConnectionLogContext.set(UUID.fromString("e52bf6d5-9947-4972-8474-a2304148ad34"))
            assertEquals("e52bf6d5", parseLogLine(render("connecting").single()).connectionId)
        } finally {
            ConnectionLogContext.clear()
        }
        assertNull(parseLogLine(render("idle").single()).connectionId)
    }

    @Test
    fun `a line that is not one of our records is kept whole`() {
        val charon = "00[DMN] Starting IKE charon daemon (strongSwan 5.9.14)"
        assertEquals(charon, parseLogLine(charon).raw)
        assertEquals("", parseLogLine(charon).message)
    }

    @Test
    fun `the half record a roll leaves behind is kept whole`() {
        val truncated = "2026-09-21 12:54:25.272 debug [worke"
        assertEquals(truncated, parseLogLine(truncated).raw)
    }

    @Test
    fun `a blank line is not mistaken for a record`() {
        assertEquals("", parseLogLine("").raw)
    }

    private fun record(
        level: String,
        module: String = "vpn",
        message: String = "connecting",
    ) = LogRecord(
        time = "13:29:40.999",
        date = "2026-09-21",
        level = level,
        module = module,
        thread = "worker-14",
        connectionId = "e52bf6d5",
        message = message,
    )

    @Test
    fun `a level floor keeps everything at or louder than itself`() {
        val warn = record("warn")
        assertTrue(warn.matches(LogLevel.DEBUG, ""))
        assertTrue(warn.matches(LogLevel.WARN, ""))
        assertFalse(warn.matches(LogLevel.ERROR, ""))
        assertFalse(record("debug").matches(LogLevel.INFO, ""))
        assertTrue(record("error").matches(LogLevel.ERROR, ""))
    }

    @Test
    fun `the search reaches every field the row draws`() {
        val line = record("info", module = "wsnet-wrapper", message = "New curl request")
        assertTrue("the message", line.matches(LogLevel.DEBUG, "CURL"))
        assertTrue("the module", line.matches(LogLevel.DEBUG, "wsnet"))
        assertTrue("the thread, which is right there on the row", line.matches(LogLevel.DEBUG, "worker-14"))
        assertTrue("the time, so a moment can be jumped to", line.matches(LogLevel.DEBUG, "13:29"))
        assertTrue("the connection id", line.matches(LogLevel.DEBUG, "e52bf6d5"))
        assertTrue("the date its header shows", line.matches(LogLevel.DEBUG, "2026-09-21"))
        assertFalse(line.matches(LogLevel.DEBUG, "openvpn"))
        assertTrue("an empty query filters nothing", line.matches(LogLevel.DEBUG, ""))
    }

    @Test
    fun `the level is not text to search, it is what the floor is for`() {
        val quiet = record("error", message = "Failed to get port map")
        assertFalse(quiet.matches(LogLevel.DEBUG, "error"))
        assertTrue(quiet.matches(LogLevel.ERROR, ""))
    }

    @Test
    fun `the two filters both have to pass`() {
        val line = record("debug", message = "New curl request")
        assertTrue(line.matches(LogLevel.DEBUG, "curl"))
        assertFalse("matching the text is not enough to beat the floor", line.matches(LogLevel.WARN, "curl"))
        assertFalse("clearing the floor is not enough to match the text", record("error").matches(LogLevel.ERROR, "curl"))
    }

    @Test
    fun `a line we could not parse counts as noise`() {
        val charon = parseLogLine("00[DMN] Starting IKE charon daemon")
        assertTrue(charon.matches(LogLevel.DEBUG, ""))
        assertFalse("nothing classifies it, so raising the floor drops it", charon.matches(LogLevel.INFO, ""))
        assertTrue("its text is still searchable", charon.matches(LogLevel.DEBUG, "charon"))
    }

    @Test
    fun `a level we do not recognise is shown rather than hidden`() {
        assertTrue(record("trace").matches(LogLevel.ERROR, ""))
    }
}
