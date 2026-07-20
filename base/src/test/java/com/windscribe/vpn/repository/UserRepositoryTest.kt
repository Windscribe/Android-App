package com.windscribe.vpn.repository

import com.windscribe.vpn.api.IApiCallManager
import com.windscribe.vpn.api.response.UserSessionResponse
import com.windscribe.vpn.apppreference.PreferencesHelper
import com.windscribe.vpn.apppreference.PreferencesKeyConstants
import com.windscribe.vpn.autoconnection.AutoConnectionManager
import com.windscribe.vpn.backend.utils.WindVpnController
import com.windscribe.vpn.localdatabase.LocalDbInterface
import com.windscribe.vpn.localdatabase.tables.UnBlockWgParam
import com.windscribe.vpn.model.User
import com.windscribe.vpn.services.sso.GoogleSignInManager
import com.windscribe.vpn.workers.WindScribeWorkManager
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for UserRepository.
 *
 * After the LiveData→StateFlow migration, this repo exposes `user: StateFlow<User?>` only.
 * Tests just read `repo.user.value` — no LiveData mocking required.
 *
 * Coverage scope:
 *  - `reload(response)` happy path: prefs written, state updated, callback fired, Amnezia.
 *  - `reload(null)` cached path: deserializes from prefs OR no-ops on missing/invalid JSON.
 *  - `whatChanged()` field-diff matrix.
 *  - `loggedIn()` / `accountStatusOkay()` simple state derivations.
 *  - `handleAmneziaAutoEnable` (exercised through `reload(response)`).
 *
 * Out of scope: `logout()`, `onSessionDeleted()`, `prepareDashboard()`. They reach
 * through Windscribe.appContext (vpnConnectionStateManager, activeActivity,
 * applicationInterface) and a static `WindUtilities.deleteProfileCompletely`.
 * Testable later if those reach-throughs get extracted into interfaces.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class UserRepositoryTest {
    private lateinit var preferences: PreferencesHelper
    private lateinit var unblockWgParamsRepository: UnblockWgParamsRepository
    private lateinit var unblockWgParams: MutableStateFlow<List<UnBlockWgParam>>

    // Unused-by-the-tested-methods, but constructor needs them.
    private lateinit var vpnController: WindVpnController
    private lateinit var autoConnectionManager: AutoConnectionManager
    private lateinit var apiManager: IApiCallManager
    private lateinit var localDbInterface: LocalDbInterface
    private lateinit var workManager: WindScribeWorkManager
    private lateinit var connectionDataRepository: ConnectionDataRepository
    private lateinit var serverListRepository: ServerListRepository
    private lateinit var staticIpRepository: StaticIpRepository
    private lateinit var googleSignInManager: GoogleSignInManager
    private lateinit var wgConfigRepository: WgConfigRepository

    @Before
    fun setUp() {
        preferences = mockk(relaxed = true)
        // Default: empty session string -> Gson.fromJson returns null -> User(null) crashes,
        // so the init { reload() } path is expected to fall into the catch.
        every { preferences.getSession } returns ""
        // Default: not in Auto mode, so handleAmneziaAutoEnable short-circuits.
        every { preferences.protocolTweaksMode } returns "manual"

        unblockWgParams = MutableStateFlow(emptyList())
        unblockWgParamsRepository = mockk(relaxed = true)
        every { unblockWgParamsRepository.unblockWgParams } returns unblockWgParams

        vpnController = mockk(relaxed = true)
        autoConnectionManager = mockk(relaxed = true)
        apiManager = mockk(relaxed = true)
        localDbInterface = mockk(relaxed = true)
        workManager = mockk(relaxed = true)
        connectionDataRepository = mockk(relaxed = true)
        serverListRepository = mockk(relaxed = true)
        staticIpRepository = mockk(relaxed = true)
        googleSignInManager = mockk(relaxed = true)
        wgConfigRepository = mockk(relaxed = true)
    }

    private fun TestScope.buildRepo(): UserRepository =
        UserRepository(
            scope = this,
            vpnController = vpnController,
            autoConnectionManager = autoConnectionManager,
            apiManager = apiManager,
            preferenceHelper = preferences,
            localDbInterface = localDbInterface,
            workManager = workManager,
            connectionDataRepository = connectionDataRepository,
            serverListRepository = serverListRepository,
            staticIpRepository = staticIpRepository,
            googleSignInManager = googleSignInManager,
            unblockWgParamsRepository = unblockWgParamsRepository,
            wgConfigRepository = wgConfigRepository,
        )

    private fun TestScope.cleanup() = coroutineContext.cancelChildren()

    /**
     * Builds a UserSessionResponse mock with the fields commonly accessed via the User wrapper.
     */
    private fun sessionResponse(
        userName: String? = "alice",
        isPremium: Int = 0,
        emailStatus: Int = 0,
        accountStatusInt: Int = 1,
        alcList: List<String>? = null,
        sipCount: Int = 0,
        amneziaWgConfigId: String? = null,
    ): UserSessionResponse =
        mockk(relaxed = true) {
            every { this@mockk.userName } returns userName
            every { this@mockk.isPremium } returns isPremium
            every { this@mockk.emailStatus } returns emailStatus
            every { this@mockk.userAccountStatus } returns accountStatusInt
            every { this@mockk.alcList } returns alcList
            // User.sipCount reads sessionResponse.sip?.count — build a nested Sip mock.
            every { sip } returns mockk { every { count } returns sipCount }
            if (amneziaWgConfigId != null) {
                every { serverInventory } returns
                    mockk inventory@{
                        every { this@inventory.amneziaWgConfigId } returns amneziaWgConfigId
                    }
            } else {
                every { serverInventory } returns null
            }
        }

    // ---------- reload(response) happy path ----------

    @Test
    fun `reload with response persists session JSON and updates user state`() =
        runTest {
            val response = sessionResponse(userName = "alice", isPremium = 1)

            val repo = buildRepo()
            repo.reload(response)
            advanceUntilIdle()

            // Session JSON written to prefs.
            verify { preferences.getSession = any() }
            // userStatus = 1 because isPro (isPremium = 1).
            verify { preferences.userStatus = 1 }
            verify { preferences.userName = "alice" }
            // State updated.
            assertEquals("alice", repo.user.value?.userName)
            cleanup()
        }

    @Test
    fun `reload with non-pro user writes userStatus 0`() =
        runTest {
            val response = sessionResponse(userName = "bob", isPremium = 0)

            val repo = buildRepo()
            repo.reload(response)
            advanceUntilIdle()

            verify { preferences.userStatus = 0 }
            verify { preferences.userName = "bob" }
            cleanup()
        }

    @Test
    fun `reload with response invokes the callback with the new User`() =
        runTest {
            val response = sessionResponse(userName = "carol", isPremium = 1)
            var callbackUser: User? = null

            val repo = buildRepo()
            repo.reload(response) { user -> callbackUser = user }
            advanceUntilIdle()

            assertEquals("carol", callbackUser?.userName)
            assertTrue(callbackUser?.isPro == true)
            cleanup()
        }

    // ---------- reload(null) cached path ----------

    @Test
    fun `reload with no arg deserializes cached session from prefs`() =
        runTest {
            val cached = sessionResponse(userName = "dave")
            // Real Gson serialization round-trip won't work with a mockk — but we can stub the JSON
            // and let reload's Gson.fromJson produce a real UserSessionResponse from a real JSON
            // string. Use a minimal but valid JSON.
            every { preferences.getSession } returns """{"username":"dave","is_premium":0}"""

            val repo = buildRepo()
            advanceUntilIdle() // init { reload() } already fired

            assertEquals("dave", repo.user.value?.userName)
            cleanup()
        }

    @Test
    fun `reload with no arg and null cached session leaves state null`() =
        runTest {
            every { preferences.getSession } returns null

            val repo = buildRepo()
            advanceUntilIdle()

            assertNull(repo.user.value)
            cleanup()
        }

    @Test
    fun `reload with no arg and malformed JSON leaves state null`() =
        runTest {
            every { preferences.getSession } returns "not-json-at-all"

            val repo = buildRepo()
            advanceUntilIdle()

            // Gson.fromJson throws, caught silently.
            assertNull(repo.user.value)
            cleanup()
        }

    // ---------- Amnezia auto-enable (exercised via reload(response)) ----------

    @Test
    fun `Amnezia auto-enable short-circuits when not in Auto mode`() =
        runTest {
            every { preferences.protocolTweaksMode } returns "manual"
            val response = sessionResponse(amneziaWgConfigId = "config-1")

            val repo = buildRepo()
            repo.reload(response)
            advanceUntilIdle()

            coVerify(exactly = 0) { unblockWgParamsRepository.setSelectedUnblockWgParam(any()) }
            cleanup()
        }

    @Test
    fun `Amnezia auto-enable selects matching preset when in Auto mode`() =
        runTest {
            every { preferences.protocolTweaksMode } returns PreferencesKeyConstants.PROTOCOL_TWEAKS_AUTO
            val preset =
                mockk<UnBlockWgParam> {
                    every { id } returns "config-1"
                    every { title } returns "Preset 1"
                }
            unblockWgParams.value = listOf(preset)
            val response = sessionResponse(amneziaWgConfigId = "config-1")

            val repo = buildRepo()
            repo.reload(response)
            advanceUntilIdle()

            coVerify(exactly = 1) { unblockWgParamsRepository.setSelectedUnblockWgParam("config-1") }
            cleanup()
        }

    @Test
    fun `Amnezia auto-enable does nothing when server recommends unknown config`() =
        runTest {
            every { preferences.protocolTweaksMode } returns PreferencesKeyConstants.PROTOCOL_TWEAKS_AUTO
            unblockWgParams.value = listOf(mockk { every { id } returns "config-A" })
            val response = sessionResponse(amneziaWgConfigId = "config-XYZ")

            val repo = buildRepo()
            repo.reload(response)
            advanceUntilIdle()

            coVerify(exactly = 0) { unblockWgParamsRepository.setSelectedUnblockWgParam(any()) }
            cleanup()
        }

    @Test
    fun `Amnezia auto-enable does nothing when server returns null config`() =
        runTest {
            every { preferences.protocolTweaksMode } returns PreferencesKeyConstants.PROTOCOL_TWEAKS_AUTO
            val response = sessionResponse(amneziaWgConfigId = null)

            val repo = buildRepo()
            repo.reload(response)
            advanceUntilIdle()

            coVerify(exactly = 0) { unblockWgParamsRepository.setSelectedUnblockWgParam(any()) }
            cleanup()
        }

    // ---------- loggedIn / accountStatusOkay ----------

    @Test
    fun `loggedIn returns false when no user set`() =
        runTest {
            every { preferences.getSession } returns null

            val repo = buildRepo()
            advanceUntilIdle()

            assertFalse(repo.loggedIn())
            cleanup()
        }

    @Test
    fun `loggedIn returns true after a reload populates user state`() =
        runTest {
            val repo = buildRepo()
            repo.reload(sessionResponse(userName = "eve"))
            advanceUntilIdle()

            assertTrue(repo.loggedIn())
            cleanup()
        }

    @Test
    fun `accountStatusOkay is false when no user set`() =
        runTest {
            every { preferences.getSession } returns null

            val repo = buildRepo()
            advanceUntilIdle()

            assertFalse(repo.accountStatusOkay())
            cleanup()
        }

    @Test
    fun `accountStatusOkay is true when account status is Okay (1)`() =
        runTest {
            val repo = buildRepo()
            repo.reload(sessionResponse(accountStatusInt = 1))
            advanceUntilIdle()

            assertTrue(repo.accountStatusOkay())
            cleanup()
        }

    @Test
    fun `accountStatusOkay is false when account status is not Okay`() =
        runTest {
            val repo = buildRepo()
            // accountStatusInt = 2 = Expired, 3 = Banned (anything not 1)
            repo.reload(sessionResponse(accountStatusInt = 2))
            advanceUntilIdle()

            assertFalse(repo.accountStatusOkay())
            cleanup()
        }

    // ---------- whatChanged ----------

    @Test
    fun `whatChanged returns all-false when no user is loaded`() =
        runTest {
            every { preferences.getSession } returns null

            val repo = buildRepo()
            advanceUntilIdle()

            val diff = repo.whatChanged(sessionResponse())
            assertEquals(listOf(false, false, false, false, false), diff)
            cleanup()
        }

    @Test
    fun `whatChanged returns all-false when comparing identical responses`() =
        runTest {
            val repo = buildRepo()
            repo.reload(sessionResponse(userName = "x", isPremium = 0, sipCount = 0))
            advanceUntilIdle()

            // Same shape -> no diffs flagged.
            every { preferences.migrationRequired } returns false
            val diff = repo.whatChanged(sessionResponse(userName = "x", isPremium = 0, sipCount = 0))
            assertEquals(false, diff[0]) // alc
            assertEquals(false, diff[1]) // sip
            assertEquals(false, diff[2]) // user-or-account status
            assertEquals(false, diff[3]) // migration
            assertEquals(false, diff[4]) // email
            cleanup()
        }

    @Test
    fun `whatChanged flags user status change when isPremium flips`() =
        runTest {
            val repo = buildRepo()
            repo.reload(sessionResponse(isPremium = 0))
            advanceUntilIdle()

            every { preferences.migrationRequired } returns false
            val diff = repo.whatChanged(sessionResponse(isPremium = 1))
            assertTrue("Expected user/account status changed", diff[2])
            cleanup()
        }

    @Test
    fun `whatChanged flags sip change when sipCount differs`() =
        runTest {
            val repo = buildRepo()
            repo.reload(sessionResponse(sipCount = 0))
            advanceUntilIdle()

            every { preferences.migrationRequired } returns false
            val diff = repo.whatChanged(sessionResponse(sipCount = 3))
            assertTrue("Expected sip changed", diff[1])
            cleanup()
        }

    @Test
    fun `whatChanged surfaces migrationRequired from prefs`() =
        runTest {
            val repo = buildRepo()
            repo.reload(sessionResponse())
            advanceUntilIdle()

            every { preferences.migrationRequired } returns true
            val diff = repo.whatChanged(sessionResponse())
            assertTrue("Expected migrationRequired flagged", diff[3])
            cleanup()
        }
}
