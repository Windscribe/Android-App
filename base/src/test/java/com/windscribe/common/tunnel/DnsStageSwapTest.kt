/*
 * Copyright (c) 2026 Windscribe Limited.
 */

package com.windscribe.common.tunnel

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import io.mockk.verifyOrder
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Test

/**
 * Swapping the DNS stage under a running mux.
 *
 * The stage is decided per connect and the mux is per session, so a handover replaces it while the
 * pumps are reading. Nothing here touches a descriptor, so it runs on the JVM; the wiring the stage
 * is started with is the part worth pinning.
 */
class DnsStageSwapTest {
    private val listener = mockk<PacketMux.Listener>(relaxed = true)
    private val mux = PacketMux(listener, null, false)

    /** [PacketMux.setDnsStage] only starts a stage on a running mux. */
    private fun markRunning() =
        PacketMux::class.java.getDeclaredField("running").apply {
            isAccessible = true
            setBoolean(mux, true)
        }

    private fun stage(): PacketMux.DnsStage = mockk(relaxed = true)

    private fun liveStage(): PacketMux.DnsStage? =
        PacketMux::class.java
            .getDeclaredField("dnsStage")
            .apply { isAccessible = true }
            .get(mux) as PacketMux.DnsStage?

    @Test
    fun `a new stage is started with both sinks`() {
        markRunning()
        val incoming = stage()

        mux.setDnsStage(incoming)

        // Answering into the TUN and passing through to the engine are separate sinks; a stage given
        // only one of them silently loses whichever direction it was missing.
        verify { incoming.start(any(), any()) }
        assertSame(incoming, liveStage())
    }

    @Test
    fun `the outgoing stage is stopped before the incoming one is started`() {
        markRunning()
        val outgoing = stage()
        mux.setDnsStage(outgoing)
        val incoming = stage()

        mux.setDnsStage(incoming)

        // A query handed to a stopped listener is lost; a moment of pass-through is not.
        verifyOrder {
            outgoing.stop()
            incoming.start(any(), any())
        }
    }

    @Test
    fun `a stage that will not start leaves pass-through rather than a dead one`() {
        markRunning()
        val broken = stage().also { every { it.start(any(), any()) } throws IllegalStateException("no ctrld") }

        mux.setDnsStage(broken)

        assertNull("a stage that failed to start must never be offered a packet", liveStage())
    }

    @Test
    fun `dropping the stage stops it and leaves nothing behind`() {
        markRunning()
        val outgoing = stage()
        mux.setDnsStage(outgoing)

        mux.setDnsStage(null)

        verify { outgoing.stop() }
        assertNull(liveStage())
    }

    @Test
    fun `setting the same stage again does not restart it`() {
        markRunning()
        val current = stage()
        mux.setDnsStage(current)

        mux.setDnsStage(current)

        verify(exactly = 1) { current.start(any(), any()) }
        verify(exactly = 0) { current.stop() }
    }
}
