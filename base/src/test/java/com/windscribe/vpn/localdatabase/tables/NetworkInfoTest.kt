/*
 * Copyright (c) 2026 Windscribe Limited.
 */

package com.windscribe.vpn.localdatabase.tables

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

/**
 * `NetworkInfoManager` publishes this through a `StateFlow` and relies on conflation to mean "the
 * network changed". Conflation compares with `equals`, so value equality here is not a style
 * question — it decides whether every collector runs once or twice per reload.
 */
class NetworkInfoTest {
    private fun sample() =
        NetworkInfo(
            networkName = "FREEDOM",
            isAutoSecureOn = true,
            isPreferredOn = false,
            protocol = "wg",
            port = "443",
        )

    @Test
    fun `two reads of the same row are equal`() {
        // Room hands back a new instance every time; the flow must not treat that as a change.
        assertEquals(sample(), sample())
        assertEquals(sample().hashCode(), sample().hashCode())
    }

    @Test
    fun `a StateFlow conflates an unchanged network`() =
        runTest {
            val flow = MutableStateFlow<NetworkInfo?>(null)
            val seen = mutableListOf<NetworkInfo?>()
            val job = launch { flow.toList(seen) }
            runCurrent() // let the collector attach and take the initial value

            flow.value = sample()
            runCurrent()
            flow.value = sample() // a second reload of the same row
            runCurrent()
            job.cancel()

            assertEquals(
                "the duplicate reload must not reach collectors",
                listOf(null, sample()),
                seen,
            )
        }

    @Test
    fun `a real change still gets through`() {
        assertNotEquals(sample(), sample().copy(isAutoSecureOn = false))
        assertNotEquals(sample(), sample().copy(protocol = "ikev2"))
        assertNotEquals(sample(), sample().copy(networkName = "OTHER"))
    }
}
