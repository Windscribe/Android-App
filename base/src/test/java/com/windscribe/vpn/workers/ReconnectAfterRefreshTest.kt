/*
 * Copyright (c) 2026 Windscribe Limited.
 */

package com.windscribe.vpn.workers

import com.windscribe.vpn.apppreference.PreferencesHelper
import com.windscribe.vpn.autoconnection.AutoConnectionManager
import com.windscribe.vpn.backend.utils.ConnectSource
import com.windscribe.vpn.repository.LocationRepository
import com.windscribe.vpn.repository.UserRepository
import com.windscribe.vpn.state.ConnectionPolicy.ServiceMode
import com.windscribe.vpn.state.VPNConnectionStateManager
import com.windscribe.vpn.state.WatchReason
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ReconnectAfterRefreshTest {
    private lateinit var locationRepository: LocationRepository
    private lateinit var userRepository: UserRepository
    private lateinit var preferences: PreferencesHelper
    private lateinit var vpnStateManager: VPNConnectionStateManager
    private lateinit var autoConnectionManager: AutoConnectionManager

    @Before
    fun setUp() {
        locationRepository = mockk(relaxed = true)
        userRepository = mockk(relaxed = true)
        preferences = mockk(relaxed = true)
        vpnStateManager = mockk(relaxed = true)
        autoConnectionManager = mockk(relaxed = true)
        every { userRepository.accountStatusOkay() } returns true
        every { preferences.globalUserConnectionPreference } returns true
        every { locationRepository.selectedCity } returns MutableStateFlow(7)
        coEvery { locationRepository.updateLocation() } returns 7
        coEvery { locationRepository.isNodeAvailable() } returns true
        every { vpnStateManager.serviceMode } returns MutableStateFlow(null)
    }

    private fun TestScope.subject() =
        ReconnectAfterRefresh(locationRepository, userRepository, preferences, vpnStateManager, autoConnectionManager, this)

    // SessionRefresh, not App: two of the three reasons to reconnect describe a tunnel that is
    // still up, and connectInForeground drops an App connect unless the status is Disconnected.
    private fun expectConnects(times: Int) =
        coVerify(exactly = times) { autoConnectionManager.connectInForeground(ConnectSource.SessionRefresh) }

    @Test
    fun `a location the user no longer has access to is replaced and reconnected`() =
        runTest {
            coEvery { locationRepository.updateLocation() } returns 9

            subject().run()
            advanceUntilIdle()

            coVerify { locationRepository.setSelectedCity(9) }
            expectConnects(1)
        }

    @Test
    fun `a node that is no longer eligible reconnects`() =
        runTest {
            coEvery { locationRepository.isNodeAvailable() } returns false

            subject().run()
            advanceUntilIdle()

            expectConnects(1)
        }

    /** The drop is still owed an answer, and the refresh that just ran was it. */
    @Test
    fun `a tunnel waiting on the refresh reconnects`() =
        runTest {
            every { vpnStateManager.serviceMode } returns MutableStateFlow(ServiceMode.AwaitingSession)

            subject().run()
            advanceUntilIdle()

            expectConnects(1)
        }

    @Test
    fun `nothing changed and nothing was waiting, so nothing reconnects`() =
        runTest {
            every { vpnStateManager.serviceMode } returns MutableStateFlow(ServiceMode.Watching(WatchReason.NetworkNotSecured))

            subject().run()
            advanceUntilIdle()

            expectConnects(0)
        }

    @Test
    fun `an account that cannot connect reconnects nothing`() =
        runTest {
            every { userRepository.accountStatusOkay() } returns false
            coEvery { locationRepository.isNodeAvailable() } returns false

            subject().run()
            advanceUntilIdle()

            expectConnects(0)
        }

    @Test
    fun `a custom config reconnects whatever the account says`() =
        runTest {
            every { userRepository.accountStatusOkay() } returns false
            every { preferences.isConnectingToConfigured } returns true
            every { vpnStateManager.serviceMode } returns MutableStateFlow(ServiceMode.AwaitingSession)

            subject().run()
            advanceUntilIdle()

            expectConnects(1)
        }

    @Test
    fun `a user who asked us to stop reconnects nothing`() =
        runTest {
            every { preferences.globalUserConnectionPreference } returns false
            coEvery { locationRepository.isNodeAvailable() } returns false

            subject().run()
            advanceUntilIdle()

            expectConnects(0)
        }
}
