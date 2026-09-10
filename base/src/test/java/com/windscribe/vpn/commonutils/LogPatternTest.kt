package com.windscribe.vpn.commonutils

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.LoggerContext
import ch.qos.logback.classic.PatternLayout
import ch.qos.logback.classic.spi.LoggingEvent
import ch.qos.logback.classic.spi.ThrowableProxy
import ch.qos.logback.core.CoreConstants
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.UUID

/**
 * The pattern in assets/logback.xml, kept in step by hand. A record has to survive being parsed:
 * everything downstream of applog.txt reads it as one JSON object per line.
 */
private const val PATTERN =
    """{"tm":"%d{yyyy-MM-dd HH:mm:ss.SSS}","lvl":"%lowerLevel","mod":"%logger{36}",""" +
        """"thr":"%replace(%thread){'DefaultDispatcher-', ''}"%cid,"msg":"%jsonMsg"}"""

class LogPatternTest {
    private fun render(
        message: String,
        throwable: Throwable? = null,
    ): JsonObject {
        val loggerContext =
            LoggerContext().apply {
                putObject(
                    CoreConstants.PATTERN_RULE_REGISTRY,
                    mutableMapOf(
                        "lowerLevel" to LowerCaseLevelConverter::class.java.name,
                        "cid" to ConnectionIdConverter::class.java.name,
                        "jsonMsg" to JsonMessageConverter::class.java.name,
                    ),
                )
            }
        val layout =
            PatternLayout().apply {
                context = loggerContext
                pattern = PATTERN
                start()
            }
        val event =
            LoggingEvent().apply {
                loggerName = "vpn"
                level = Level.DEBUG
                this.message = message
                throwable?.let { setThrowableProxy(ThrowableProxy(it)) }
            }
        val rendered = layout.doLayout(event)
        assertEquals("record must be a single line", 1, rendered.lines().count { it.isNotEmpty() })
        return JsonParser.parseString(rendered).asJsonObject
    }

    @Test
    fun `the coroutine dispatcher prefix is stripped from the thread name`() {
        val t = Thread.currentThread()
        val original = t.name
        try {
            t.name = "DefaultDispatcher-worker-28"
            assertEquals("worker-28", render("on a dispatcher")["thr"].asString)
            t.name = "OpenVPNProcessThread"
            assertEquals("OpenVPNProcessThread", render("on a native thread")["thr"].asString)
        } finally {
            t.name = original
        }
    }

    @Test
    fun `quotes in a message do not break the record`() {
        val message = """P:WARNING: linker: "/lib/arm64/libovpnexec.so" is not a directory"""
        assertEquals(message, render(message)["msg"].asString)
    }

    @Test
    fun `backslashes and control characters are escaped`() {
        val message = "path C:\\tmp\tand\na second line"
        assertEquals(message, render(message)["msg"].asString)
    }

    @Test
    fun `a throwable stays inside the message field`() {
        val msg = render("connect failed", IllegalStateException("boom"))["msg"].asString
        assertTrue(msg.startsWith("connect failed\n"))
        assertTrue(msg.contains("IllegalStateException: boom"))
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
            assertEquals("e52bf6d5", render("connecting")["cid"].asString)
        } finally {
            ConnectionLogContext.clear()
        }
    }

    @Test
    fun `a line outside an attempt carries no cid field at all`() {
        ConnectionLogContext.clear()
        val json = render("idle")
        assertFalse("an empty cid is 9 wasted bytes on every such line", json.has("cid"))
        // The field is spliced in with its own comma, so dropping it must leave the object intact.
        assertEquals("idle", json["msg"].asString)
        assertEquals("vpn", json["mod"].asString)
    }
}
