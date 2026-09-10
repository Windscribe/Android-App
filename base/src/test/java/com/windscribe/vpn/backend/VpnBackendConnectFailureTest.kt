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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.UUID

/**
 * What is left behind when an engine never comes up.
 *
 * Publishing Disconnected on its own left the backend active, its connect timer running and the
 * held TUN unresolved. In foreground failover the next protocol takes over, and the abandoned
 * timer later disconnects the shared TunnelManager under the engine that did succeed — so these
 * two exits have to go through the same typed disconnect as every other failure in the method.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class VpnBackendConnectFailureTest {
    private val serviceGate = mockk<VpnServiceGate>(relaxed = true)
    private val stateManager = mockk<VPNConnectionStateManager>(relaxed = true)

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
        val disconnects = mutableListOf<VPNState.Error?>()
        override var active: Boolean = true

        override fun activate() = Unit

        override fun deactivate() = Unit

        override fun connect(
            protocolInformation: ProtocolInformation,
            connectionId: UUID,
        ) = Unit

        override suspend fun disconnect(error: VPNState.Error?) {
            disconnects += error
        }

        suspend fun start(
            tunnelManager: TunnelManager,
            plan: () -> EnginePlan?,
        ) = runConnect("Test", tunnelManager, plan)
    }

    /** The backend's init launches collectors that never complete, so it gets the background scope. */
    private fun backend(scope: CoroutineScope): Backend {
        every { stateManager.state } returns MutableStateFlow(VPNState(VPNState.Status.Disconnected))
        coEvery { serviceGate.ensureRunning() } returns mockk<WindscribeVpnService>(relaxed = true)
        return Backend(scope, stateManager, serviceGate)
    }

    @Test
    fun `no usable profile disconnects instead of only publishing Disconnected`() =
        runTest {
            val backend = backend(backgroundScope)

            backend.start(mockk(relaxed = true)) { null }
            advanceUntilIdle()

            assertEquals(1, backend.disconnects.size)
            assertEquals(VPNState.ErrorType.GenericError, backend.disconnects.single()?.error)
        }

    @Test
    fun `an engine that throws on start disconnects`() =
        runTest {
            val backend = backend(backgroundScope)
            val tunnelManager =
                mockk<TunnelManager>(relaxed = true).also {
                    every { it.connect(any(), any()) } throws IllegalStateException("engine refused")
                }

            backend.start(tunnelManager) { mockk(relaxed = true) }
            advanceUntilIdle()

            assertEquals(1, backend.disconnects.size)
            val reason = backend.disconnects.single()?.message
            assertTrue(
                "the engine may have partly attached, and only disconnect detaches it",
                reason != null && reason.contains("Test"),
            )
        }

    @Test
    fun `a plan that comes up disconnects nothing`() =
        runTest {
            val backend = backend(backgroundScope)

            backend.start(mockk(relaxed = true)) { mockk(relaxed = true) }
            advanceUntilIdle()

            assertTrue(backend.disconnects.isEmpty())
        }
}
