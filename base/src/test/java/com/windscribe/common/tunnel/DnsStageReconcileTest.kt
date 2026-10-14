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

    private val stateField =
        TunnelManager::class.java
            .getDeclaredField("dnsStageState")
            .apply { isAccessible = true }

    private val stateClass = Class.forName("com.windscribe.common.tunnel.TunnelManager\$DnsStageState")

    private val stateConstructor =
        stateClass
            .getDeclaredConstructor(PacketMux::class.java, Integer::class.java)
            .apply { isAccessible = true }

    /** Records [port] as already installed in [into], as a prior connect would have left it. */
    private fun installed(
        into: PacketMux?,
        port: Int?,
    ) = stateField.set(manager, into?.let { stateConstructor.newInstance(it, port) })

    private fun installedPort(): Any? =
        stateField.get(manager)?.let {
            stateClass.getDeclaredMethod("getPort").apply { isAccessible = true }.invoke(it)
        }

    private fun installedMux(): Any? =
        stateField.get(manager)?.let {
            stateClass.getDeclaredMethod("getMux").apply { isAccessible = true }.invoke(it)
        }

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
        installed(into = mux, port = 5355)

        reconcile.invoke(manager, host, mux)

        verify(exactly = 0) { mux.setDnsStage(any()) }
    }

    @Test
    fun `a restarted proxy on a new port rebuilds the stage`() {
        usingProxy(port = 41999)
        installed(into = mux, port = 5355)

        reconcile.invoke(manager, host, mux)

        verify(exactly = 1) { mux.setDnsStage(any()) }
    }

    @Test
    fun `turning the proxy off drops the stage`() {
        passingThrough()
        installed(into = mux, port = 5355)

        reconcile.invoke(manager, host, mux)

        verify(exactly = 1) { mux.setDnsStage(null) }
    }

    @Test
    fun `turning the proxy on wires a stage where there was none`() {
        usingProxy(port = 5355)
        installed(into = null, port = null)

        reconcile.invoke(manager, host, mux)

        verify(exactly = 1) { mux.setDnsStage(any()) }
        // Recorded, so the next handover with the same port does not restart it.
        assert(installedPort() == 5355)
        assert(installedMux() === mux)
    }

    /**
     * A reconnect inside a live process: beginBlocking builds a fresh mux with no stage while ctrld
     * keeps running on the port the previous session recorded. Matching on the port alone read that
     * as "already intercepting" and left every query going straight out the tunnel, so custom DNS
     * silently stopped applying after the first connect of the process.
     */
    @Test
    fun `a replacement mux is wired even when the proxy kept its port`() {
        val replacement = mockk<PacketMux>(relaxed = true)
        usingProxy(port = 5355)
        installed(into = mux, port = 5355)

        reconcile.invoke(manager, host, replacement)

        verify(exactly = 1) { replacement.setDnsStage(any()) }
        verify(exactly = 0) { mux.setDnsStage(any()) }
        assert(installedMux() === replacement)
    }
}
