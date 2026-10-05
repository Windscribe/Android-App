/*
 * Copyright (c) 2026 Windscribe Limited.
 */

package com.windscribe.common.tunnel

import android.os.ParcelFileDescriptor
import android.os.SystemClock
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import io.mockk.verify
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/**
 * `onDestroy` and `onRevoke` reach [TunnelManager] on the main thread, and an engine's stop waits
 * on wg-go's own lock — which a connect in flight holds for the whole of its bring-up.
 */
class TeardownOffMainThreadTest {
    private val inStop = CountDownLatch(1)
    private val releaseStop = CountDownLatch(1)

    /** Blocks in [stop] the way wg-go does while waiting on its own tunnel lock. */
    private val engine =
        object : VpnEngine {
            override val id = "slow"

            @Volatile
            var stopped = false

            override fun start(
                profile: EngineProfile,
                host: VpnHost,
                engineFd: ParcelFileDescriptor,
            ) = Unit

            override fun stop() {
                inStop.countDown()
                check(releaseStop.await(5, TimeUnit.SECONDS)) { "stop was never released" }
                stopped = true
            }

            override fun onRevoked() = Unit
        }

    private val host = mockk<VpnHost>(relaxed = true)
    private val mux = mockk<PacketMux>(relaxed = true)
    private val manager = TunnelManager(mapOf("slow" to engine))

    @Before
    fun standUpSession() {
        mockkStatic(SystemClock::class)
        every { SystemClock.elapsedRealtime() } returns 0L
        manager.attachHost(host)
        setField("mux", mux)
        setField("activeEngine", engine)
        setField("tunnelUp", true)
    }

    @After
    fun release() {
        releaseStop.countDown()
        unmockkStatic(SystemClock::class)
    }

    private fun setField(
        name: String,
        value: Any,
    ) = TunnelManager::class.java.getDeclaredField(name).apply {
        isAccessible = true
        set(manager, value)
    }

    @Test
    fun `detachHost returns without waiting for the engine to stop`() {
        val caller =
            Thread { manager.detachHost(host) }.apply { start() }

        caller.join(TimeUnit.SECONDS.toMillis(2))
        assertEquals(
            "detachHost is still waiting on the engine's stop",
            Thread.State.TERMINATED,
            caller.state,
        )
        // Gone the moment it returns, though the stop is still running.
        assertFalse(manager.hasTunnel)
        assertEquals(null, manager.activeEngineId)
        verify { host.releaseTun() }
        assertTrue("the engine's stop never started", inStop.await(2, TimeUnit.SECONDS))

        releaseStop.countDown()
        assertTrue(waitFor { engine.stopped })
        assertTrue("the mux was never stopped", waitFor { runCatching { verify { mux.stop() } }.isSuccess })
    }

    @Test
    fun `onRevoked returns without waiting for the engine to stop`() {
        val caller =
            Thread { manager.onRevoked() }.apply { start() }

        caller.join(TimeUnit.SECONDS.toMillis(2))
        assertEquals(
            "onRevoked is still waiting on the engine's stop",
            Thread.State.TERMINATED,
            caller.state,
        )
        assertFalse(manager.hasTunnel)
        verify { host.releaseTun() }
        assertTrue("the engine's stop never started", inStop.await(2, TimeUnit.SECONDS))
    }

    @Test
    fun `a stale host does not tear down the session that replaced it`() {
        val newer = mockk<VpnHost>(relaxed = true)
        manager.attachHost(newer)

        manager.detachHost(host)

        assertTrue("the live session was torn down by a stale host", manager.hasTunnel)
        assertEquals("slow", manager.activeEngineId)
        verify(exactly = 0) { newer.releaseTun() }
        verify(exactly = 0) { mux.stop() }
    }

    private fun waitFor(condition: () -> Boolean): Boolean {
        repeat(50) {
            if (condition()) return true
            Thread.sleep(100)
        }
        return false
    }
}
