/*
 * Copyright (c) 2026 Windscribe Limited.
 */

package com.windscribe.common.tunnel

import io.mockk.mockk
import io.mockk.verify
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.FileDescriptor

/**
 * Two TUN replacements in flight at once.
 *
 * The host notifies outside its own lock, and an engine can call `requestTun` from its own thread
 * while a handover replaces the TUN, so the later establish can notify first. Acting on the older
 * notification afterwards would move the packet path back onto a descriptor the newer generation
 * has already retired.
 */
class TunReplacementOrderingTest {
    private val host = mockk<VpnHost>(relaxed = true)
    private val mux = mockk<PacketMux>(relaxed = true)
    private val manager = TunnelManager(emptyMap())

    /**
     * A live mux, without standing a session up. Whether the packet path actually moved is the
     * assertion that matters here, and with no mux both branches look identical.
     */
    @Before
    fun attachMux() {
        manager.attachHost(host)
        setMux(mux)
    }

    private fun setMux(value: PacketMux) =
        TunnelManager::class.java.getDeclaredField("mux").apply {
            isAccessible = true
            set(manager, value)
        }

    private fun replaced(
        generation: Long,
        from: VpnHost = host,
    ) = manager.onTunReplaced(from, FileDescriptor(), 1280, generation)

    @Test
    fun `a notification that arrives after a newer one does not move the packet path`() {
        replaced(generation = 2)
        replaced(generation = 1)

        verify(exactly = 1) { mux.setTun(any(), any()) }
        // Both predecessors are still released — nothing else would close the older one.
        verify { host.releasePreviousTun(2) }
        verify { host.releasePreviousTun(1) }
    }

    @Test
    fun `notifications in order are all applied`() {
        replaced(generation = 1)
        replaced(generation = 2)
        replaced(generation = 3)

        verify { host.releasePreviousTun(1) }
        verify { host.releasePreviousTun(2) }
        verify { host.releasePreviousTun(3) }
        verify(exactly = 3) { mux.setTun(any(), any()) }
        assertTrue(manager.hasTunnel)
    }

    @Test
    fun `a new service's generations are not rejected as older than the last one's`() {
        // The first service runs a session; the singleton manager remembers where it got to.
        replaced(generation = 1)
        replaced(generation = 2)
        manager.detachHost()

        // Android recreates the service. Generations are process-wide, so its first replacement is
        // 3, not 1 — a per-service counter restarting at zero is what made this a rejection, and the
        // release that came with it closed the descriptor the mux had just moved onto.
        val replacementHost = mockk<VpnHost>(relaxed = true)
        val replacementMux = mockk<PacketMux>(relaxed = true)
        manager.attachHost(replacementHost)
        setMux(replacementMux)

        replaced(generation = 3, from = replacementHost)

        verify(exactly = 1) { replacementMux.setTun(any(), any()) }
        verify { replacementHost.releasePreviousTun(3) }
    }

    @Test
    fun `a late notification from a detached service is not applied to its replacement`() {
        val replacementHost = mockk<VpnHost>(relaxed = true)
        val replacementMux = mockk<PacketMux>(relaxed = true)
        manager.detachHost()
        manager.attachHost(replacementHost)
        // A live packet path to wrongly move, or this passes for the wrong reason.
        setMux(replacementMux)

        // Still in flight when the service it came from went away, and higher than anything the new
        // session has applied — so the generation alone cannot rule it out. Only the host can.
        replaced(generation = 9, from = host)

        verify(exactly = 0) { replacementMux.setTun(any(), any()) }
        // Released through the host whose map holds it, not the current one.
        verify { host.releasePreviousTun(9) }
        verify(exactly = 0) { replacementHost.releasePreviousTun(any()) }
    }

    @Test
    fun `a repeated generation is not applied twice`() {
        replaced(generation = 4)
        replaced(generation = 4)

        verify(exactly = 1) { mux.setTun(any(), any()) }
        verify(exactly = 2) { host.releasePreviousTun(4) }
    }
}
