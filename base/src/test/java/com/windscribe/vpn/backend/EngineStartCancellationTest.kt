/*
 * Copyright (c) 2026 Windscribe Limited.
 */

package com.windscribe.vpn.backend

import com.windscribe.common.tunnel.TunnelManager
import com.windscribe.vpn.api.IApiCallManager
import com.windscribe.vpn.apppreference.PreferencesHelper
import com.windscribe.vpn.autoconnection.ProtocolInformation
import com.windscribe.vpn.backend.tunnel.VpnServiceGate
import com.windscribe.vpn.backend.tunnel.WindscribeVpnService
import com.windscribe.vpn.commonutils.ResourceHelper
import com.windscribe.vpn.localdatabase.LocalDbInterface
import com.windscribe.vpn.repository.AdvanceParameterRepository
import com.windscribe.vpn.state.NetworkInfoManager
import com.windscribe.vpn.state.VPNConnectionStateManager
import com.windscribe.vpn.wsnet.WSNetWrapper
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.UUID

/**
 * An engine start that is still on its way when the session ends.
 *
 * It suspends several times before attaching — starting ctrld, waiting for the service, building
 * the plan off disk — so a teardown can finish while it is parked. Cancellation covers the parked
 * cases; the identity check before attaching covers the rest, since cancellation cannot reach code
 * already past its last suspension point.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class EngineStartCancellationTest {
    private val serviceGate = mockk<VpnServiceGate>(relaxed = true)
    private val stateManager = mockk<VPNConnectionStateManager>(relaxed = true)
    private val tunnelManager = mockk<TunnelManager>(relaxed = true)

    private class Backend(
        scope: CoroutineScope,
        stateManager: VPNConnectionStateManager,
        serviceGate: VpnServiceGate,
    ) : VpnBackend(
            scope,
            stateManager,
            mockk<PreferencesHelper>(relaxed = true),
            mockk<NetworkInfoManager>(relaxed = true),
            mockk<AdvanceParameterRepository>(relaxed = true),
            mockk<IApiCallManager>(relaxed = true),
            mockk<LocalDbInterface>(relaxed = true),
            mockk<WSNetWrapper>(relaxed = true),
            mockk<ResourceHelper>(relaxed = true),
            serviceGate,
            mockk(relaxed = true),
        ) {
        override var active: Boolean = true

        override fun activate() = Unit

        override fun deactivate() = Unit

        override fun connect(
            protocolInformation: ProtocolInformation,
            connectionId: UUID,
        ) = Unit

        val disconnects = mutableListOf<VPNState.Error?>()

        override suspend fun disconnect(error: VPNState.Error?) {
            disconnects += error
        }

        suspend fun start(
            tunnelManager: TunnelManager,
            plan: () -> EnginePlan?,
        ) = runConnect("Test", tunnelManager, plan)

        /** Stands in for what the real backends do when the user disconnects mid-connect. */
        fun markDisconnecting() {
            disconnecting = true
        }
    }

    private fun backend(scope: CoroutineScope): Backend {
        every { stateManager.state } returns MutableStateFlow(VPNState(VPNState.Status.Disconnected))
        coEvery { serviceGate.ensureRunning() } returns mockk<WindscribeVpnService>(relaxed = true)
        return Backend(scope, stateManager, serviceGate)
    }

    @Test
    fun `an attempt superseded by a newer connection id does not attach`() =
        runTest {
            val backend = backend(backgroundScope)
            backend.connectionId = UUID.randomUUID()

            backend.start(tunnelManager) {
                // A newer connect lands while this one is building its plan.
                backend.connectionId = UUID.randomUUID()
                mockk(relaxed = true)
            }
            advanceUntilIdle()

            verify(exactly = 0) { tunnelManager.connect(any(), any()) }
        }

    @Test
    fun `an attempt overtaken by a disconnect does not attach`() =
        runTest {
            val backend = backend(backgroundScope)
            backend.connectionId = UUID.randomUUID()

            backend.start(tunnelManager) {
                backend.markDisconnecting()
                mockk(relaxed = true)
            }
            advanceUntilIdle()

            verify(exactly = 0) { tunnelManager.connect(any(), any()) }
        }

    @Test
    fun `a cancelled attempt neither attaches nor disconnects`() =
        runTest {
            val backend = backend(backgroundScope)
            backend.connectionId = UUID.randomUUID()

            // Parked waiting for the service when the disconnect arrives — a real suspension point
            // on the way to attaching, which is the whole reason cancellation is needed at all.
            coEvery { serviceGate.ensureRunning() } coAnswers {
                delay(10_000)
                mockk<WindscribeVpnService>(relaxed = true)
            }
            val started = backgroundScope.launch { backend.start(tunnelManager) { mockk(relaxed = true) } }
            advanceTimeBy(100)
            started.cancelAndJoin()
            advanceUntilIdle()

            // The disconnect that cancelled it is already tearing it down. Nothing here may report
            // a failure on top of that. Today the cancellation lands before the guarded region, so
            // this pins the behaviour rather than the rethrow that would catch it if it did not.
            assertTrue(backend.disconnects.isEmpty())
            verify(exactly = 0) { tunnelManager.connect(any(), any()) }
        }

    @Test
    fun `an attempt nothing overtook attaches`() =
        runTest {
            val backend = backend(backgroundScope)
            backend.connectionId = UUID.randomUUID()

            backend.start(tunnelManager) { mockk(relaxed = true) }
            advanceUntilIdle()

            verify(exactly = 1) { tunnelManager.connect(any(), any()) }
        }
}
