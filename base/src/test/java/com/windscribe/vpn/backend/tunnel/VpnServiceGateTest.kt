/*
 * Copyright (c) 2026 Windscribe Limited.
 */

package com.windscribe.vpn.backend.tunnel

import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The window between `stopSelf()` and `onDestroy()`. A caller landing in it must not be given the
 * outgoing service, whose pending teardown closes the TUN it is about to establish.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class VpnServiceGateTest {
    private val gate = VpnServiceGate()
    private val outgoing = mockk<WindscribeVpnService>(relaxed = true)
    private val replacement = mockk<WindscribeVpnService>(relaxed = true)

    @Test
    fun `a running service is handed straight back`() =
        runTest {
            gate.attach(outgoing)
            assertSame(outgoing, gate.await())
        }

    @Test
    fun `teardown can still reach a stopping service`() {
        gate.attach(outgoing)
        gate.markStopping()
        assertSame("endSession and protect still need it", outgoing, gate.current)
        assertTrue(gate.isStopping)
    }

    @Test
    fun `a stopping service is never handed out to start a session on`() =
        runTest {
            gate.attach(outgoing)
            gate.markStopping()

            val waiter = async { gate.await(timeoutMs = 1_000) }
            advanceTimeBy(1_001)
            advanceUntilIdle()

            assertNull("would have closed the new TUN on its way out", waiter.await())
        }

    @Test
    fun `the wait ends when a replacement attaches`() =
        runTest {
            gate.attach(outgoing)
            gate.markStopping()

            val waiter = async { gate.await(timeoutMs = 1_000) }
            gate.detach(outgoing)
            gate.attach(replacement)
            advanceUntilIdle()

            assertSame(replacement, waiter.await())
        }

    @Test
    fun `the wait ends when the same instance is revived by a start command`() =
        runTest {
            gate.attach(outgoing)
            gate.markStopping()

            val waiter = async { gate.await(timeoutMs = 1_000) }
            // A start command arriving before the pending stop takes effect: onCreate does not run
            // again, so onStartCommand is the only thing that can clear this.
            gate.attach(outgoing)
            advanceUntilIdle()

            assertSame(outgoing, waiter.await())
        }

    @Test
    fun `a late onDestroy from the outgoing service does not clear its replacement`() {
        gate.attach(outgoing)
        gate.markStopping()
        gate.attach(replacement)

        gate.detach(outgoing)

        assertSame("the identity check is what keeps the live service attached", replacement, gate.current)
    }
}
