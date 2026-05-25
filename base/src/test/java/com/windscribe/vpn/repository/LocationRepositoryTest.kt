package com.windscribe.vpn.repository

import com.windscribe.vpn.apppreference.PreferencesHelper
import com.windscribe.vpn.backend.utils.SelectedLocationType
import com.windscribe.vpn.exceptions.WindScribeException
import com.windscribe.vpn.localdatabase.LocalDbInterface
import com.windscribe.vpn.model.User
import com.windscribe.vpn.serverlist.entity.Datacenter
import com.windscribe.vpn.serverlist.entity.DatacenterAndLocation
import com.windscribe.vpn.serverlist.entity.Location
import com.windscribe.vpn.serverlist.entity.Server
import com.windscribe.vpn.services.ping.Pinger
import dagger.Lazy
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkObject
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for LocationRepository.
 *
 * After the recent refactor:
 *  - Pinger is injected (interface), so ping logic is fakeable.
 *  - SelectedLocationType is computed inline from PreferencesHelper, so no
 *    WindUtilities static call to mock for that.
 *  - WindUtilities.deleteProfile is still touched by getAlternativeLocation();
 *    tests that exercise that path use mockkObject(WindUtilities).
 */
@OptIn(ExperimentalCoroutinesApi::class)
class LocationRepositoryTest {

    private lateinit var preferences: PreferencesHelper
    private lateinit var db: LocalDbInterface
    private lateinit var userRepository: UserRepository
    private lateinit var userRepositoryLazy: Lazy<UserRepository>
    private lateinit var userFlow: MutableStateFlow<User?>
    private lateinit var pinger: Pinger

    @Before
    fun setUp() {
        preferences = mockk(relaxed = true)
        // Default: prefs say "city location" (both isConnectingTo* are false)
        every { preferences.isConnectingToConfigured } returns false
        every { preferences.isConnectingToStaticIp } returns false
        every { preferences.selectedCity } returns 0

        db = mockk()
        // UserRepository.user is now a StateFlow<User?> — no LiveData Looper trick needed.
        userFlow = MutableStateFlow(null)
        userRepository = mockk()
        every { userRepository.user } returns userFlow
        userRepositoryLazy = mockk()
        every { userRepositoryLazy.get() } returns userRepository

        pinger = mockk()
    }

    private fun setLoggedInUser(user: User?) {
        userFlow.value = user
    }

    @After
    fun tearDown() {
        unmockkObject(com.windscribe.vpn.commonutils.WindUtilities)
    }

    private fun TestScope.buildRepo(): LocationRepository {
        return LocationRepository(this, preferences, db, userRepositoryLazy, pinger)
    }

    private fun TestScope.cleanup() = coroutineContext.cancelChildren()

    private fun datacenter(id: Int, regionId: Int = 0, tz: String = "UTC"): Datacenter =
        Datacenter().apply {
            this.id = id
            this.region_id = regionId
            val tzField = Datacenter::class.java.getDeclaredField("tz")
            tzField.isAccessible = true
            tzField.set(this, tz)
        }

    private fun server(
        id: Int,
        hostname: String = "host-$id",
        ip: String = "10.0.0.$id",
        ip2: String = ip,
        ip3: String = ip
    ): Server = Server(
        id = id,
        hostname = hostname,
        ip = ip,
        ip2 = ip2,
        ip3 = ip3,
        datacenterId = 1,
        weight = 1,
        health = 100
    )

    private fun datacenterAndLocation(
        dc: Datacenter,
        location: Location? = mockk(relaxed = true)
    ): DatacenterAndLocation =
        DatacenterAndLocation().apply {
            datacenter = dc
            this.location = location
        }

    // ---------- selectedCity flow ----------

    @Test
    fun `selectedCity flow seeds from prefs and re-emits on setSelectedCity`() = runTest {
        every { preferences.selectedCity } returns 42

        val repo = buildRepo()
        advanceUntilIdle()

        assertEquals(42, repo.selectedCity.value)

        every { preferences.selectedCity } returns 99
        repo.setSelectedCity(99)
        advanceUntilIdle()

        verify { preferences.selectedCity = 99 }
        assertEquals(99, repo.selectedCity.value)
        cleanup()
    }

    @Test
    fun `setSelectedCity without arg re-emits current prefs value`() = runTest {
        every { preferences.selectedCity } returns 5

        val repo = buildRepo()
        advanceUntilIdle()
        assertEquals(5, repo.selectedCity.value)

        // Mutate prefs out-of-band, then trigger emit.
        every { preferences.selectedCity } returns 7
        repo.setSelectedCity()
        advanceUntilIdle()

        assertEquals(7, repo.selectedCity.value)
        cleanup()
    }

    // ---------- isNodeAvailable (city path + non-city short-circuit) ----------

    @Test
    fun `isNodeAvailable returns true when selectedIp matches a server`() = runTest {
        every { preferences.selectedCity } returns 7
        every { preferences.selectedIp } returns "10.0.0.2"
        coEvery { db.getServersByDatacenter(7) } returns listOf(server(1), server(2), server(3))

        val repo = buildRepo()
        assertTrue(repo.isNodeAvailable())
        cleanup()
    }

    @Test
    fun `isNodeAvailable returns false when selectedIp matches nothing`() = runTest {
        every { preferences.selectedCity } returns 7
        every { preferences.selectedIp } returns "99.99.99.99"
        coEvery { db.getServersByDatacenter(7) } returns listOf(server(1), server(2))

        val repo = buildRepo()
        assertFalse(repo.isNodeAvailable())
        cleanup()
    }

    @Test
    fun `isNodeAvailable returns true for static-ip source without checking servers`() = runTest {
        every { preferences.isConnectingToStaticIp } returns true

        val repo = buildRepo()
        assertTrue(repo.isNodeAvailable())
        coVerify(exactly = 0) { db.getServersByDatacenter(any()) }
        cleanup()
    }

    @Test
    fun `isNodeAvailable returns true for configured-profile source without checking servers`() = runTest {
        every { preferences.isConnectingToConfigured } returns true

        val repo = buildRepo()
        assertTrue(repo.isNodeAvailable())
        coVerify(exactly = 0) { db.getServersByDatacenter(any()) }
        cleanup()
    }

    @Test
    fun `isNodeAvailable returns false when db lookup throws`() = runTest {
        every { preferences.selectedCity } returns 7
        every { preferences.selectedIp } returns "10.0.0.1"
        coEvery { db.getServersByDatacenter(7) } throws RuntimeException("db down")

        val repo = buildRepo()
        assertFalse(repo.isNodeAvailable())
        cleanup()
    }

    // ---------- getSelectedCityAndRegion ----------

    @Test
    fun `getSelectedCityAndRegion returns null when selectedCity is -1`() = runTest {
        every { preferences.selectedCity } returns -1

        val repo = buildRepo()
        assertNull(repo.getSelectedCityAndRegion())
        cleanup()
    }

    @Test
    fun `getSelectedCityAndRegion returns null for non-city source`() = runTest {
        every { preferences.selectedCity } returns 7
        every { preferences.isConnectingToStaticIp } returns true

        val repo = buildRepo()
        assertNull(repo.getSelectedCityAndRegion())
        cleanup()
    }

    @Test
    fun `getSelectedCityAndRegion returns the db row for a valid city selection`() = runTest {
        every { preferences.selectedCity } returns 7
        val expected = datacenterAndLocation(datacenter(7))
        every { db.getDatacenterAndLocation(7) } returns expected

        val repo = buildRepo()
        assertEquals(expected, repo.getSelectedCityAndRegion())
        cleanup()
    }

    @Test
    fun `getSelectedCityAndRegion swallows db exception and returns null`() = runTest {
        every { preferences.selectedCity } returns 7
        every { db.getDatacenterAndLocation(7) } throws RuntimeException("db blew up")

        val repo = buildRepo()
        assertNull(repo.getSelectedCityAndRegion())
        cleanup()
    }

    // ---------- updateLocation ----------
    // updateLocation paths exercised:
    //  1. valid -> returns selectedCity.value
    //  2. invalid -> getAlternativeLocation() (we mock WindUtilities.deleteProfile)
    //  3. WindScribeException somewhere -> returns -1

    @Test
    fun `updateLocation returns selectedCity when current selection is valid`() = runTest {
        every { preferences.selectedCity } returns 42
        setLoggedInUser(mockk { every { userStatusInt } returns 1 })
        // CityLocation path: isCityAvailable requires getDatacenterAndLocation non-null + servers
        every { db.getDatacenterAndLocation(42) } returns datacenterAndLocation(datacenter(42))
        coEvery { db.getServersByDatacenter(any()) } returns listOf(server(1))

        val repo = buildRepo()
        assertEquals(42, repo.updateLocation())
        cleanup()
    }

    // Note: updateLocation()'s `catch (e: WindScribeException) -> -1` branch is
    // effectively unreachable from the call sites — every path through
    // isLocationValid/getAlternativeLocation is wrapped in runCatching{}, which
    // converts exceptions into default values. Worth flagging as dead code but
    // there's no input we can craft to exercise it from this layer.

    @Test
    fun `updateLocation falls back to alternative when current selection is invalid`() = runTest {
        // Selection invalid: city has no servers.
        every { preferences.selectedCity } returns 42
        setLoggedInUser(mockk {
            every { userStatusInt } returns 1
            every { isPro } returns true
        })
        every { db.getDatacenterAndLocation(42) } returns datacenterAndLocation(datacenter(42))
        coEvery { db.getServersByDatacenter(42) } returns emptyList()

        // Alternative search: sister returns a valid city.
        coEvery { db.getLocationIdFromDatacenterAsync(42) } returns 100
        coEvery { db.getAllDatacentersAsync(100) } returns listOf(datacenter(7))
        coEvery { db.getServersByDatacenter(7) } returns listOf(server(1))

        // getAlternativeLocation calls WindUtilities.deleteProfile(appContext).
        // appContext is a lateinit on Windscribe.Companion; we set it to a mock so
        // argument evaluation doesn't throw, then stub the suspend call itself.
        com.windscribe.vpn.Windscribe.appContext = mockk(relaxed = true)
        mockkObject(com.windscribe.vpn.commonutils.WindUtilities)
        coEvery { com.windscribe.vpn.commonutils.WindUtilities.deleteProfile(any()) } returns true

        val repo = buildRepo()
        val result = repo.updateLocation()

        // Sister city 7 is the only candidate -- expect it back.
        assertEquals(7, result)
        // Side effects: prefs cleared
        verify { preferences.isConnectingToConfigured = false }
        verify { preferences.isConnectingToStaticIp = false }
        cleanup()
    }

    // ---------- isLocationValid path coverage via updateLocation, static-ip + configured-profile ----------

    @Test
    fun `updateLocation static-ip path uses static region lookup`() = runTest {
        every { preferences.selectedCity } returns 42
        every { preferences.isConnectingToStaticIp } returns true
        setLoggedInUser(mockk { every { userStatusInt } returns 1 })
        // Static region present -> isStaticIpAvailable returns true -> valid.
        coEvery { db.getStaticRegionByIDAsync(42) } returns mockk()

        val repo = buildRepo()
        assertEquals(42, repo.updateLocation())
        cleanup()
    }

    @Test
    fun `updateLocation configured-profile path uses config file lookup`() = runTest {
        every { preferences.selectedCity } returns 42
        every { preferences.isConnectingToConfigured } returns true
        setLoggedInUser(mockk { every { userStatusInt } returns 1 })
        // Config file present -> isConfigProfileAvailable returns true -> valid.
        coEvery { db.getConfigFileAsync(42) } returns mockk()

        val repo = buildRepo()
        assertEquals(42, repo.updateLocation())
        cleanup()
    }

    // ---------- getBestLocationAsync ----------

    @Test
    fun `getBestLocationAsync returns lowest-ping city when available`() = runTest {
        coEvery { db.getLowestPingIdAsync() } returns 9
        coEvery { db.getDatacenterByIDAsync(9) } returns datacenter(9)
        val expected = datacenterAndLocation(datacenter(9))
        every { db.getDatacenterAndLocation(9) } returns expected

        val repo = buildRepo()
        assertEquals(expected, repo.getBestLocationAsync())
        cleanup()
    }

    @Test
    fun `getBestLocationAsync throws when db has no matching location`() = runTest {
        coEvery { db.getLowestPingIdAsync() } returns 9
        coEvery { db.getDatacenterByIDAsync(9) } returns datacenter(9)
        every { db.getDatacenterAndLocation(9) } returns null
        // Random fallback also empty so we end up at -1 -> getDatacenterAndLocation(-1) -> null
        coEvery { db.getDatacentersAsync() } returns emptyList()
        setLoggedInUser(mockk { every { isPro } returns false })

        val repo = buildRepo()
        try {
            repo.getBestLocationAsync()
            error("expected exception")
        } catch (e: Exception) {
            assertTrue(e.message?.contains("Best location not found") == true)
        }
        cleanup()
    }
}
