package com.windscribe.vpn.commonutils

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.LoggerContext
import ch.qos.logback.classic.PatternLayout
import ch.qos.logback.classic.spi.LoggingEvent
import ch.qos.logback.classic.spi.ThrowableProxy
import ch.qos.logback.core.CoreConstants
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.UUID

/**
 * The pattern in assets/logback.xml, kept in step by hand. A record has to survive being parsed
 * back: the debug tab reads applog.txt one line per record, trailing stack trace lines aside.
 */
internal const val PATTERN =
    """%d{yyyy-MM-dd HH:mm:ss.SSS} %-5lowerLevel [%replace(%thread){'DefaultDispatcher-', ''}] %logger{36}%cid - %msg%n"""

internal fun layout(pattern: String): PatternLayout {
    val loggerContext =
        LoggerContext().apply {
            putObject(
                CoreConstants.PATTERN_RULE_REGISTRY,
                mutableMapOf(
                    "lowerLevel" to LowerCaseLevelConverter::class.java.name,
                    "cid" to ConnectionIdConverter::class.java.name,
                ),
            )
        }
    return PatternLayout().apply {
        context = loggerContext
        this.pattern = pattern
        start()
    }
}

internal fun event(
    message: String,
    throwable: Throwable? = null,
    logger: String = "vpn",
    level: Level = Level.DEBUG,
) = LoggingEvent().apply {
    loggerName = logger
    this.level = level
    this.message = message
    throwable?.let { setThrowableProxy(ThrowableProxy(it)) }
}

class LogPatternTest {
    /** The id is process-wide, so a connect left running by an earlier test would bleed in. */
    @Before
    fun clearConnectionId() = ConnectionLogContext.clear()

    private fun render(
        message: String,
        throwable: Throwable? = null,
    ): List<String> = layout(PATTERN).doLayout(event(message, throwable)).trimEnd('\n').lines()

    @Test
    fun `a record without a throwable is a single line`() {
        assertEquals(1, render("New curl request").size)
    }

    @Test
    fun `the fields land in the order the parser expects`() {
        val line = render("New curl request").single()
        assertTrue(line, line.matches(Regex("""\d{4}-\d{2}-\d{2} \d{2}:\d{2}:\d{2}\.\d{3} debug +\[.+] vpn - New curl request""")))
    }

    @Test
    fun `the level is padded so the columns line up`() {
        val line = layout(PATTERN).doLayout(event("connecting", level = Level.INFO))
        assertTrue(line, line.contains("info  ["))
    }

    @Test
    fun `the coroutine dispatcher prefix is stripped from the thread name`() {
        val t = Thread.currentThread()
        val original = t.name
        try {
            t.name = "DefaultDispatcher-worker-28"
            assertEquals("worker-28", parseLogLine(render("on a dispatcher").single()).thread)
            t.name = "OpenVPNProcessThread"
            assertEquals("OpenVPNProcessThread", parseLogLine(render("on a native thread").single()).thread)
        } finally {
            t.name = original
        }
    }

    @Test
    fun `a message is written as it was logged`() {
        val message = """P:WARNING: linker: "/lib/arm64/libovpnexec.so" is not a directory"""
        assertEquals(message, parseLogLine(render(message).single()).message)
    }

    @Test
    fun `a throwable follows its record as a real stack trace`() {
        val lines = render("connect failed", IllegalStateException("boom"))
        assertTrue(lines.first().endsWith(" - connect failed"))
        assertTrue(lines.size > 2)
        assertTrue(lines[1].contains("IllegalStateException: boom"))
    }

    @Test
    fun `only an id from another attempt is worth naming`() {
        val live = UUID.fromString("3459ce49-3eb7-4073-a2af-c1de2eb42bad")
        val superseded = UUID.fromString("7cd7fa02-29ce-479e-9371-b096f55ba7b2")
        try {
            ConnectionLogContext.set(live)
            assertNull(ConnectionLogContext.differing(live))
            assertNull("a state carrying no id is the ordinary case", ConnectionLogContext.differing(null))
            assertEquals("7cd7fa02", ConnectionLogContext.differing(superseded))
        } finally {
            ConnectionLogContext.clear()
        }
    }

    @Test
    fun `the connection id is stamped on every record of an attempt`() {
        try {
            ConnectionLogContext.set(UUID.fromString("e52bf6d5-9947-4972-8474-a2304148ad34"))
            assertEquals("e52bf6d5", parseLogLine(render("connecting").single()).connectionId)
        } finally {
            ConnectionLogContext.clear()
        }
    }

    @Test
    fun `a line outside an attempt carries no cid field at all`() {
        ConnectionLogContext.clear()
        val line = render("idle").single()
        assertFalse("an empty id is wasted bytes on every such line", line.contains("#"))
        // The field is spliced in with its own space, so dropping it must leave the line parseable.
        val record = parseLogLine(line)
        assertEquals("idle", record.message)
        assertEquals("vpn", record.module)
    }
}
