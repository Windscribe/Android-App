/*
 * Copyright (c) 2026 Windscribe Limited.
 */

package com.windscribe.common.tunnel

import android.os.ParcelFileDescriptor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.net.InetAddress
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/**
 * The main thread reads [TunnelManager.hasTunnel] on every `onStartCommand`, and a connect can sit
 * in DNS for tens of seconds before it has anything to establish.
 */
class ConnectLockContentionTest {
    private val inPrepare = CountDownLatch(1)
    private val releasePrepare = CountDownLatch(1)

    /** Blocks in [prepare] the way a peer endpoint's DNS lookup does. */
    private val engine =
        object : VpnEngine {
            override val id = "slow"

            @Volatile
            var started = false

            override fun prepare(profile: EngineProfile) {
                inPrepare.countDown()
                check(releasePrepare.await(5, TimeUnit.SECONDS)) { "prepare was never released" }
            }

            override fun start(
                profile: EngineProfile,
                host: VpnHost,
                engineFd: ParcelFileDescriptor,
            ) {
                started = true
            }

            override fun stop() = Unit

            override fun onRevoked() = Unit
        }

    private val manager = TunnelManager(mapOf("slow" to engine))

    private val profile =
        object : EngineProfile {
            override val protocol = "slow"
        }

    private val tunConfig =
        TunConfig.blackhole(
            sessionName = "test",
            addresses = listOf(IpNetwork(InetAddress.getByName("172.16.252.2"), 32)),
            lanBypass = false,
            allowedApps = emptyList(),
            disallowedApps = emptyList(),
            selfPackage = "com.windscribe.test",
        )

    @Test
    fun `a connect stalled in prepare does not block the state reads or a disconnect`() {
        // No host attached: connect throws once it takes the monitor, which is what is under test.
        val connect =
            Thread {
                runCatching { manager.connect(profile, tunConfig) }
            }.apply { start() }
        assertTrue("connect never reached prepare", inPrepare.await(5, TimeUnit.SECONDS))

        val reads =
            Thread {
                manager.hasTunnel
                manager.activeEngineId
                manager.disconnect(TunHold.None)
            }.apply { start() }

        reads.join(TimeUnit.SECONDS.toMillis(2))
        assertEquals(
            "state reads and disconnect are still waiting on the connect",
            Thread.State.TERMINATED,
            reads.state,
        )

        releasePrepare.countDown()
        connect.join(TimeUnit.SECONDS.toMillis(5))
    }
}
