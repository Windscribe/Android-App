/*
 * Copyright (c) 2026 Windscribe Limited.
 */

package com.windscribe.vpn.workers.worker

import android.content.Context
import androidx.work.WorkerParameters
import com.windscribe.vpn.api.IApiCallManager
import com.windscribe.vpn.api.response.GenericResponseClass
import com.windscribe.vpn.api.response.UserSessionResponse
import com.windscribe.vpn.apppreference.PreferencesHelper
import com.windscribe.vpn.backend.VPNState
import com.windscribe.vpn.backend.utils.WindVpnController
import com.windscribe.vpn.localdatabase.LocalDbInterface
import com.windscribe.vpn.model.User
import com.windscribe.vpn.repository.ConnectionDataRepository
import com.windscribe.vpn.repository.ServerListRepository
import com.windscribe.vpn.repository.UserRepository
import com.windscribe.vpn.repository.WgConfigRepository
import com.windscribe.vpn.state.VPNConnectionStateManager
import com.windscribe.vpn.workers.ReconnectAfterRefresh
import com.windscribe.vpn.workers.WindScribeWorkManager
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * The worker must not report success before the refresh it triggered has finished.
 *
 * WorkManager may release the process as soon as `doWork` returns. The ordered sequence — account
 * handling, server list, credentials, then the reconnect — runs inside the reload callback, and
 * losing it can leave a tunnel waiting on a session that never settled.
 *
 * The callback here is deliberately slow. A test that invokes it synchronously passes whether or
 * not the worker waits, which is how this went unnoticed.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class SessionWorkerAwaitsRefreshTest {
    private val preferences = mockk<PreferencesHelper>(relaxed = true)
    private val userRepository = mockk<UserRepository>(relaxed = true)
    private val apiCallManager = mockk<IApiCallManager>(relaxed = true)
    private val localDb = mockk<LocalDbInterface>(relaxed = true)
    private val vpnStateManager = mockk<VPNConnectionStateManager>(relaxed = true)

    private var callbackFinished = false

    @Before
    fun setUp() {
        every { vpnStateManager.state } returns MutableStateFlow(VPNState(VPNState.Status.Disconnected))
    }

    private fun session(): UserSessionResponse =
        UserSessionResponse().apply {
            userName = "test-user"
            userAccountStatus = 1
            isPremium = 1
        }

    private fun worker(): SessionWorker =
        SessionWorker(
            mockk<Context>(relaxed = true),
            mockk<WorkerParameters>(relaxed = true),
            preferences,
            userRepository,
            apiCallManager,
            mockk<WindScribeWorkManager>(relaxed = true),
            localDb,
            mockk<ServerListRepository>(relaxed = true),
            mockk<ConnectionDataRepository>(relaxed = true),
            mockk<WgConfigRepository>(relaxed = true),
            mockk<WindVpnController>(relaxed = true),
            vpnStateManager,
            mockk<ReconnectAfterRefresh>(relaxed = true),
        )

    @Test
    fun `doWork does not return before the refresh callback has run`() =
        runTest {
            val response = session()
            every { userRepository.loggedIn() } returns true
            every { userRepository.whatChanged(response) } returns listOf(false, false, false, false, false)
            coEvery { apiCallManager.getSessionGeneric(null, any()) } returns GenericResponseClass(response, null)
            coEvery { localDb.getAllStaticRegions() } returns emptyList()
            coEvery { userRepository.reloadNow(response, any()) } coAnswers {
                // Slower than the worker: the point is that it is waited on, not that it is quick.
                delay(500)
                secondArg<(suspend (User) -> Unit)?>()?.invoke(User(response))
                callbackFinished = true
            }

            worker().doWork()

            assertTrue(
                "doWork returned while the ordered refresh was still in flight",
                callbackFinished,
            )
        }
}
