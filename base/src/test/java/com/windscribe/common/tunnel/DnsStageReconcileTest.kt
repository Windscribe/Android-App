/*
 * Copyright (c) 2026 Windscribe Limited.
 */

package com.windscribe.common.tunnel

import com.windscribe.common.DNSDetails
import com.windscribe.common.DnsType
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Before
import org.junit.Test

/**
 * The DNS stage against a mux that outlives the connect that built it.
 *
 * Mode and port are decided per connect — the user can change DNS while connected, and
 * ProxyDNSManager stops ctrld or binds a new random port on the next attempt — but a handover
 * reuses the TUN and its mux. A stage left over from the previous connect sends queries to a
 * listener that has moved or gone, or misses that one is now expected.
 *
 * Driven by reflection: `common` is a separate module, so the reconcile is not visible here.
 */
class DnsStageReconcileTest {
    private val host = mockk<VpnHost>(relaxed = true)
    private val mux = mockk<PacketMux>(relaxed = true)
    private val manager = TunnelManager(emptyMap())

    private val reconcile =
        TunnelManager::class.java
            .getDeclaredMethod("reconcileDnsStage", VpnHost::class.java, PacketMux::class.java)
            .apply { isAccessible = true }

    private fun key(): Any? =
        TunnelManager::class.java
            .getDeclaredField("dnsStageKey")
            .apply { isAccessible = true }
            .get(manager)

    private fun setKey(value: Any?) =
        TunnelManager::class.java
            .getDeclaredField("dnsStageKey")
            .apply { isAccessible = true }
            .set(manager, value)

    private fun usingProxy(port: Int) {
        every { host.dnsDetails() } returns DNSDetails(type = DnsType.Proxy)
        every { host.controlDPort() } returns port
    }

    private fun passingThrough() {
        every { host.dnsDetails() } returns DNSDetails(type = DnsType.Plain)
    }

    @Before
    fun attach() {
        manager.attachHost(host)
    }

    @Test
    fun `an unchanged decision leaves the stage alone`() {
        usingProxy(port = 5355)
        setKey(5355)

        reconcile.invoke(manager, host, mux)

        verify(exactly = 0) { mux.setDnsStage(any()) }
    }

    @Test
    fun `a restarted proxy on a new port rebuilds the stage`() {
        usingProxy(port = 41999)
        setKey(5355)

        reconcile.invoke(manager, host, mux)

        verify(exactly = 1) { mux.setDnsStage(any()) }
    }

    @Test
    fun `turning the proxy off drops the stage`() {
        passingThrough()
        setKey(5355)

        reconcile.invoke(manager, host, mux)

        verify(exactly = 1) { mux.setDnsStage(null) }
    }

    @Test
    fun `turning the proxy on wires a stage where there was none`() {
        usingProxy(port = 5355)
        setKey(null)

        reconcile.invoke(manager, host, mux)

        verify(exactly = 1) { mux.setDnsStage(any()) }
        // Recorded, so the next handover with the same port does not restart it.
        assert(key() == 5355)
    }
}
