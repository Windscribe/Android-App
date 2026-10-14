/*
 * Copyright (c) 2026 Windscribe Limited.
 */

package com.windscribe.vpn.workers.worker

import android.content.Context
import androidx.work.ListenableWorker
import androidx.work.WorkerParameters
import com.windscribe.vpn.backend.VPNState
import com.windscribe.vpn.backend.tunnel.VpnServiceGate
import com.windscribe.vpn.backend.tunnel.WindscribeVpnService
import com.windscribe.vpn.backend.utils.ConnectSource
import com.windscribe.vpn.state.ConnectionPolicyInputs
import com.windscribe.vpn.state.ShortcutStateManager
import com.windscribe.vpn.state.VPNConnectionStateManager
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

/**
 * Boot work must not report success before the connect it dispatched reaches the service.
 *
 * WorkManager may release the process as soon as `doWork` returns, so a cold start still walking
 * through session and network initialization is simply lost — and lost having recorded success,
 * which is what stops it being retried.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class BootWorkerHandoffTest {
    private val policyInputs = mockk<ConnectionPolicyInputs>(relaxed = true)
    private val shortcutStateManager = mockk<ShortcutStateManager>(relaxed = true)
    private val stateManager = mockk<VPNConnectionStateManager>(relaxed = true)
    private val serviceGate = mockk<VpnServiceGate>(relaxed = true)

    @Before
    fun setUp() {
        every { stateManager.state } returns MutableStateFlow(VPNState(VPNState.Status.Disconnected))
    }

    private fun worker(): BootWorker =
        BootWorker(
            mockk<Context>(relaxed = true),
            mockk<WorkerParameters>(relaxed = true).also {
                every { it.inputData } returns
                    androidx.work.Data
                        .Builder()
                        .putString(BootWorker.KEY_REASON, BootWorker.REASON_BOOT)
                        .build()
            },
            policyInputs,
            shortcutStateManager,
            stateManager,
            serviceGate,
        )

    /** The policy has to want a connect, or the worker returns before any of this matters. */
    private fun policyWantsConnect() {
        coEvery { policyInputs.current() } returns
            mockk(relaxed = true) {
                every { autoStartOnBoot } returns true
            }
    }

    @Test
    fun `a connect that reaches the service succeeds`() =
        runTest {
            policyWantsConnect()
            coEvery { shortcutStateManager.connectNow(ConnectSource.App) } returns true
            coEvery { serviceGate.await(any()) } returns mockk<WindscribeVpnService>(relaxed = true)

            val result = worker().doWork()

            coVerify { shortcutStateManager.connectNow(ConnectSource.App) }
            assertEquals(ListenableWorker.Result.success(), result)
        }

    @Test
    fun `a connect that never reaches the service is retried, not recorded as done`() =
        runTest {
            policyWantsConnect()
            coEvery { shortcutStateManager.connectNow(ConnectSource.App) } returns true
            coEvery { serviceGate.await(any()) } returns null

            val result = worker().doWork()

            assertEquals(ListenableWorker.Result.retry(), result)
        }

    @Test
    fun `nothing to connect with fails rather than waiting out the timeout`() =
        runTest {
            policyWantsConnect()
            coEvery { shortcutStateManager.connectNow(ConnectSource.App) } returns false

            val result = worker().doWork()

            assertEquals(ListenableWorker.Result.failure(), result)
            coVerify(exactly = 0) { serviceGate.await(any()) }
        }
}
