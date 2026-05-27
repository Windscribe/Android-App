package com.windscribe.vpn.repository

import com.windscribe.vpn.api.IApiCallManager
import com.windscribe.vpn.api.response.ApiErrorResponse
import com.windscribe.vpn.api.response.GenericResponseClass
import com.windscribe.vpn.api.response.GetMyIpResponse
import com.windscribe.vpn.apppreference.PreferencesHelper
import com.windscribe.vpn.backend.VPNState
import com.windscribe.vpn.state.VPNConnectionStateManager
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for IpRepository.
 *
 * The repo's `state` is now a StateFlow built declaratively from:
 *   merge(events, vpnState. Map { Refresh })
 *     .onStart { emit(Refresh) }
 *     .mapLatest { branch on vpn status }
 *     .stateIn(scope, Eagerly, Loading())
 *
 * Test approach:
 *  - Set up mocks + initial VPN/online state BEFORE constructing the repo.
 *  - Construct the repo (this immediately starts the eager state flow).
 *  - advanceUntilIdle() to let `mapLatest` resolve.
 *  - For the Disconnected branch (500ms delay), advanceTimeBy(500).
 *  - Read `repo.state.value` and assert.
 *
 * StateFlow gives us late-subscribers-see-current-value semantics, so no
 * UNDISPATCHED collector dance is required. The test scope still needs
 * cancelChildren() to clean up the perpetual stateIn collector.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class IpRepositoryTest {
    private lateinit var preferences: PreferencesHelper
    private lateinit var apiCallManager: IApiCallManager
    private lateinit var connectionStateManager: VPNConnectionStateManager
    private lateinit var connectionState: MutableStateFlow<VPNState>
    private lateinit var isOnline: MutableStateFlow<Boolean>

    @Before
    fun setUp() {
        preferences = mockk(relaxed = true)
        apiCallManager = mockk()
        connectionStateManager = mockk()
        connectionState = MutableStateFlow(VPNState(VPNState.Status.Disconnected))
        every { connectionStateManager.state } returns connectionState
        isOnline = MutableStateFlow(false)
    }

    private fun ipResponse(userIp: String): GenericResponseClass<GetMyIpResponse?, ApiErrorResponse?> {
        val response = GetMyIpResponse::class.java.getDeclaredConstructor().newInstance()
        GetMyIpResponse::class.java.getDeclaredField("userIp").apply {
            isAccessible = true
            set(response, userIp)
        }
        return GenericResponseClass(response, null)
    }

    private fun apiError(
        code: Int,
        message: String,
    ): GenericResponseClass<GetMyIpResponse?, ApiErrorResponse?> {
        val err = ApiErrorResponse::class.java.getDeclaredConstructor().newInstance()
        ApiErrorResponse::class.java.declaredFields
            .firstOrNull { it.name.equals("errorCode", ignoreCase = true) }
            ?.apply {
                isAccessible = true
                set(err, code)
            }
        ApiErrorResponse::class.java.declaredFields
            .firstOrNull { it.name.equals("errorMessage", ignoreCase = true) }
            ?.apply {
                isAccessible = true
                set(err, message)
            }
        return GenericResponseClass(null, err)
    }

    private fun TestScope.buildRepository(): IpRepository =
        IpRepository(this, preferences, apiCallManager, connectionStateManager, isOnline)

    private fun TestScope.cleanup() = coroutineContext.cancelChildren()

    /**
     * Disconnected branch waits 500ms before refreshing. Tests that exercise the
     * initial refresh through the Disconnected path must advance past this delay.
     */
    private suspend fun TestScope.settleStartup() {
        advanceTimeBy(501)
        advanceUntilIdle()
    }

    // ---------- Disconnected branch (delay 500ms, then refreshIp) ----------

    @Test
    fun `disconnected online success stores trimmed IP and emits Success`() =
        runTest {
            connectionState.value = VPNState(VPNState.Status.Disconnected)
            isOnline.value = true
            coEvery { apiCallManager.getApiIp() } returns ipResponse("  1.2.3.4  ")

            val repo = buildRepository()
            settleStartup()

            verify { preferences.userIP = "1.2.3.4" }
            assertEquals(RepositoryState.Success("1.2.3.4"), repo.state.value)

            cleanup()
        }

    @Test
    fun `state starts as Loading before the 500ms delay elapses`() =
        runTest {
            connectionState.value = VPNState(VPNState.Status.Disconnected)
            isOnline.value = true
            coEvery { apiCallManager.getApiIp() } returns ipResponse("9.9.9.9")

            val repo = buildRepository()
            // advanceTimeBy + runCurrent (NOT advanceUntilIdle) — we want to stop
            // strictly before the 500ms delay fires. advanceUntilIdle would happily
            // jump past the pending delay and run the refresh.
            advanceTimeBy(499)
            runCurrent()

            assertTrue(
                "Expected Loading before 500ms delay, got ${repo.state.value}",
                repo.state.value is RepositoryState.Loading,
            )

            settleStartup()
            cleanup()
        }

    @Test
    fun `disconnected API error falls back to stored IP`() =
        runTest {
            connectionState.value = VPNState(VPNState.Status.Disconnected)
            isOnline.value = true
            coEvery { apiCallManager.getApiIp() } returns apiError(701, "boom")
            every { preferences.userIP } returns "5.6.7.8"

            val repo = buildRepository()
            settleStartup()

            assertEquals(RepositoryState.Success("5.6.7.8"), repo.state.value)
            cleanup()
        }

    @Test
    fun `disconnected and offline uses stored IP without calling API`() =
        runTest {
            connectionState.value = VPNState(VPNState.Status.Disconnected)
            isOnline.value = false
            every { preferences.userIP } returns "172.16.0.1"

            val repo = buildRepository()
            settleStartup()

            assertEquals(RepositoryState.Success("172.16.0.1"), repo.state.value)
            coVerify(exactly = 0) { apiCallManager.getApiIp() }
            cleanup()
        }

    @Test
    fun `disconnected, offline, no stored IP emits Error`() =
        runTest {
            connectionState.value = VPNState(VPNState.Status.Disconnected)
            isOnline.value = false
            every { preferences.userIP } returns null

            val repo = buildRepository()
            settleStartup()

            assertEquals(RepositoryState.Error<String>("No saved ip found."), repo.state.value)
            cleanup()
        }

    @Test
    fun `API exception treated as error and falls back to stored IP`() =
        runTest {
            connectionState.value = VPNState(VPNState.Status.Disconnected)
            isOnline.value = true
            coEvery { apiCallManager.getApiIp() } throws RuntimeException("network blew up")
            every { preferences.userIP } returns "8.8.8.8"

            val repo = buildRepository()
            settleStartup()

            assertEquals(RepositoryState.Success("8.8.8.8"), repo.state.value)
            cleanup()
        }

    // ---------- Connected branch (loads from storage, no delay, no API call) ----------

    @Test
    fun `connected branch loads from storage and skips API`() =
        runTest {
            connectionState.value = VPNState(VPNState.Status.Connected)
            every { preferences.userIP } returns "100.64.0.1"

            val repo = buildRepository()
            advanceUntilIdle()

            assertEquals(RepositoryState.Success("100.64.0.1"), repo.state.value)
            coVerify(exactly = 0) { apiCallManager.getApiIp() }
            cleanup()
        }

    @Test
    fun `connected with no stored IP emits Error`() =
        runTest {
            connectionState.value = VPNState(VPNState.Status.Connected)
            every { preferences.userIP } returns null

            val repo = buildRepository()
            advanceUntilIdle()

            assertEquals(RepositoryState.Error<String>("No saved ip found."), repo.state.value)
            cleanup()
        }

    // ---------- VPN state transitions drive refreshes ----------

    @Test
    fun `vpn transition disconnected to connected switches to storage IP`() =
        runTest {
            connectionState.value = VPNState(VPNState.Status.Disconnected)
            isOnline.value = true
            coEvery { apiCallManager.getApiIp() } returns ipResponse("1.1.1.1")
            every { preferences.userIP } returns "storage.ip.is.here"

            val repo = buildRepository()
            settleStartup()
            assertEquals(RepositoryState.Success("1.1.1.1"), repo.state.value)

            // Connect: should switch to storage value (no API call this time).
            connectionState.value = VPNState(VPNState.Status.Connected)
            advanceUntilIdle()
            assertEquals(RepositoryState.Success("storage.ip.is.here"), repo.state.value)

            cleanup()
        }

    @Test
    fun `vpn transition connected to disconnected refreshes from API after delay`() =
        runTest {
            connectionState.value = VPNState(VPNState.Status.Connected)
            every { preferences.userIP } returns "old.cached.ip"
            isOnline.value = true
            coEvery { apiCallManager.getApiIp() } returns ipResponse("2.2.2.2")

            val repo = buildRepository()
            advanceUntilIdle()
            assertEquals(RepositoryState.Success("old.cached.ip"), repo.state.value)

            // Disconnect: should wait 500ms then refresh from API.
            connectionState.value = VPNState(VPNState.Status.Disconnected)
            advanceTimeBy(499)
            runCurrent()
            // Still showing pre-disconnect value because delay hasn't elapsed.
            assertEquals(RepositoryState.Success("old.cached.ip"), repo.state.value)

            advanceTimeBy(2)
            advanceUntilIdle()
            assertEquals(RepositoryState.Success("2.2.2.2"), repo.state.value)

            cleanup()
        }

    @Test
    fun `mapLatest cancels in-flight disconnect refresh when a new vpn state arrives`() =
        runTest {
            connectionState.value = VPNState(VPNState.Status.Disconnected)
            isOnline.value = true
            coEvery { apiCallManager.getApiIp() } returns ipResponse("should.not.appear")
            every { preferences.userIP } returns "storage.value"

            val repo = buildRepository()

            // Half-way through the 500ms delay, flip to Connected. The pending
            // disconnect refresh should be cancelled and the Connected branch wins.
            advanceTimeBy(250)
            connectionState.value = VPNState(VPNState.Status.Connected)
            advanceUntilIdle()

            assertEquals(RepositoryState.Success("storage.value"), repo.state.value)
            coVerify(exactly = 0) { apiCallManager.getApiIp() }

            cleanup()
        }

    // ---------- update() manual refresh ----------

    @Test
    fun `update emits a refresh that retriggers the branch logic`() =
        runTest {
            connectionState.value = VPNState(VPNState.Status.Disconnected)
            isOnline.value = true
            coEvery { apiCallManager.getApiIp() } returnsMany
                listOf(
                    ipResponse("1.1.1.1"),
                    ipResponse("2.2.2.2"),
                )

            val repo = buildRepository()
            settleStartup()
            assertEquals(RepositoryState.Success("1.1.1.1"), repo.state.value)

            repo.update()
            settleStartup()
            assertEquals(RepositoryState.Success("2.2.2.2"), repo.state.value)

            cleanup()
        }

    // ---------- IPv6 truncation ----------

    @Test
    fun `long IPv6-shaped response has zero runs collapsed before storage`() =
        runTest {
            connectionState.value = VPNState(VPNState.Status.Disconnected)
            isOnline.value = true
            // 32+ chars triggers the zero-run collapsing branch.
            val longIp = "2001000000000000abcd000000001234"
            coEvery { apiCallManager.getApiIp() } returns ipResponse(longIp)

            val repo = buildRepository()
            settleStartup()

            // The expected mangled output per the implementation's regex chain:
            //   "0000" -> "0", then "000" -> "", then "00" -> ""
            val expected =
                longIp
                    .replace("0000".toRegex(), "0")
                    .replace("000".toRegex(), "")
                    .replace("00".toRegex(), "")
            verify { preferences.userIP = expected }
            assertEquals(RepositoryState.Success(expected), repo.state.value)

            cleanup()
        }
}
