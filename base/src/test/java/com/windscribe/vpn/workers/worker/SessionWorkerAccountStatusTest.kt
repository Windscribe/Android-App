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
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test

/**
 * What a lapsed Windscribe account does to a tunnel the user supplied themselves.
 *
 * The account cannot connect on our locations, but a custom config is not ours to end — and the
 * standing intent is what the later reconcile and app-update paths read to decide whether to
 * restore and watch it. Exercised through the worker rather than the pure policy, because the
 * defect was an assignment beside the policy call, not in it.
 */
class SessionWorkerAccountStatusTest {
    private val context = mockk<Context>(relaxed = true)
    private val workerParameters = mockk<WorkerParameters>(relaxed = true)
    private val preferences = mockk<PreferencesHelper>(relaxed = true)
    private val userRepository = mockk<UserRepository>(relaxed = true)
    private val apiCallManager = mockk<IApiCallManager>(relaxed = true)
    private val workManager = mockk<WindScribeWorkManager>(relaxed = true)
    private val localDb = mockk<LocalDbInterface>(relaxed = true)
    private val serverListRepository = mockk<ServerListRepository>(relaxed = true)
    private val connectionDataRepository = mockk<ConnectionDataRepository>(relaxed = true)
    private val reconnectAfterRefresh = mockk<ReconnectAfterRefresh>(relaxed = true)
    private val wgConfigRepository = mockk<WgConfigRepository>(relaxed = true)
    private val vpnController = mockk<WindVpnController>(relaxed = true)
    private val vpnStateManager = mockk<VPNConnectionStateManager>(relaxed = true)

    @Before
    fun setUp() {
        every { vpnStateManager.state } returns MutableStateFlow(VPNState(VPNState.Status.Disconnected))
        every { vpnStateManager.isVPNConnected() } returns true
        every { vpnStateManager.isTunnelUp() } returns true
    }

    /** 3 is Banned, 2 is Expired, 1 is Okay. */
    private fun session(accountStatus: Int): UserSessionResponse =
        UserSessionResponse().apply {
            userName = "test-user"
            userAccountStatus = accountStatus
            isPremium = 1
        }

    private fun arrange(response: UserSessionResponse) {
        every { userRepository.loggedIn() } returns true
        every { userRepository.whatChanged(response) } returns listOf(false, false, false, false, false)
        coEvery { apiCallManager.getSessionGeneric(null, any()) } returns GenericResponseClass(response, null)
        coEvery { localDb.getAllStaticRegions() } returns emptyList()
        coEvery { userRepository.reloadNow(response, any()) } coAnswers {
            secondArg<(suspend (User) -> Unit)?>()?.invoke(User(response))
        }
    }

    private fun TestScope.buildWorker(): SessionWorker =
        SessionWorker(
            context,
            workerParameters,
            preferences,
            userRepository,
            apiCallManager,
            workManager,
            localDb,
            serverListRepository,
            connectionDataRepository,
            wgConfigRepository,
            vpnController,
            vpnStateManager,
            reconnectAfterRefresh,
        )

    @Test
    fun `a banned account leaves a custom config connected and keeps its intent`() =
        runTest {
            val response = session(accountStatus = 3)
            arrange(response)
            every { preferences.isConnectingToConfigured } returns true

            buildWorker().doWork()

            verify(exactly = 0) { preferences.globalUserConnectionPreference = false }
            coVerify(exactly = 0) { vpnController.disconnectAsync(any(), any(), any()) }
        }

    @Test
    fun `an expired account leaves a custom config connected and keeps its intent`() =
        runTest {
            val response = session(accountStatus = 2)
            arrange(response)
            every { preferences.isConnectingToConfigured } returns true

            buildWorker().doWork()

            verify(exactly = 0) { preferences.globalUserConnectionPreference = false }
        }

    @Test
    fun `a banned account still clears the intent of a Windscribe connection`() =
        runTest {
            val response = session(accountStatus = 3)
            arrange(response)
            every { preferences.isConnectingToConfigured } returns false

            buildWorker().doWork()

            verify { preferences.globalUserConnectionPreference = false }
        }
}
