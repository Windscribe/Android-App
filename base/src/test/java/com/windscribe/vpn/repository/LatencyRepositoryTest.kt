package com.windscribe.vpn.repository

import com.windscribe.vpn.apppreference.PreferencesHelper
import com.windscribe.vpn.localdatabase.LocalDbInterface
import com.windscribe.vpn.serverlist.entity.ConfigFile
import com.windscribe.vpn.serverlist.entity.Datacenter
import com.windscribe.vpn.serverlist.entity.Favourite
import com.windscribe.vpn.serverlist.entity.PingTime
import com.windscribe.vpn.serverlist.entity.StaticRegion
import com.windscribe.vpn.services.ping.Pinger
import com.windscribe.vpn.state.VPNConnectionStateManager
import dagger.Lazy
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for LatencyRepository.
 *
 * After the recent refactor, the repo only depends on:
 *  - PreferencesHelper (interface)
 *  - LocalDbInterface (interface)
 *  - Lazy<VPNConnectionStateManager>
 *  - Pinger (interface)
 *  - StateFlow<Boolean> isOnline
 *
 * No statics, no globals, no Ping/Socket calls — Pinger is the only IO seam.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class LatencyRepositoryTest {

    private lateinit var preferences: PreferencesHelper
    private lateinit var db: LocalDbInterface
    private lateinit var vpnConnectionStateManager: VPNConnectionStateManager
    private lateinit var vpnConnectionStateManagerLazy: Lazy<VPNConnectionStateManager>
    private lateinit var pinger: Pinger
    private lateinit var isOnline: MutableStateFlow<Boolean>

    @Before
    fun setUp() {
        preferences = mockk(relaxed = true)
        every { preferences.userIP } returns "100.1.1.1"

        db = mockk()
        // Sensible defaults so any unexpected path doesn't NPE.
        coEvery { db.getAllPingsAsync() } returns emptyList()
        coEvery { db.getPingableDatacenters() } returns emptyList()
        coEvery { db.getFavouritesAsync() } returns emptyList()
        coEvery { db.getAllStaticRegions() } returns emptyList()
        coEvery { db.getAllConfigs() } returns emptyList()
        coEvery { db.addPing(any()) } returns Unit
        coEvery { db.getLowestPingIdAsync() } returns 0

        vpnConnectionStateManager = mockk()
        every { vpnConnectionStateManager.isVPNActive() } returns false
        vpnConnectionStateManagerLazy = mockk()
        every { vpnConnectionStateManagerLazy.get() } returns vpnConnectionStateManager

        pinger = mockk()
        // Default: any ping returns 42ms.
        coEvery { pinger.ping(any(), any()) } returns 42

        isOnline = MutableStateFlow(true)
    }

    private fun TestScope.buildRepo(): LatencyRepository =
        LatencyRepository(preferences, db, vpnConnectionStateManagerLazy, pinger, isOnline)

    private fun TestScope.cleanup() = coroutineContext.cancelChildren()

    private fun datacenter(id: Int, regionId: Int = 1): Datacenter =
        Datacenter().apply {
            this.id = id
            this.region_id = regionId
        }

    private fun pingRow(
        id: Int,
        ip: String = "100.1.1.1",
        updatedAt: Long = System.currentTimeMillis(),
        pingTime: Int = 50
    ): PingTime = PingTime().apply {
        ping_id = id
        this.ip = ip
        this.updatedAt = updatedAt
        this.pingTime = pingTime
    }

    // ---------- updateAllServerLatencies: filter logic ----------

    @Test
    fun `skips datacenters with a recent valid ping from the same user IP`() = runTest {
        coEvery { db.getAllPingsAsync() } returns listOf(
            pingRow(id = 1, ip = "100.1.1.1", pingTime = 50)
        )
        coEvery { db.getPingableDatacenters() } returns listOf(datacenter(1), datacenter(2))
        coEvery { db.getPingIpAndHost(1) } returns Pair("ip-1", "host-1")
        coEvery { db.getPingIpAndHost(2) } returns Pair("ip-2", "host-2")

        val repo = buildRepo()
        repo.updateAllServerLatencies()
        advanceUntilIdle()

        // Only datacenter 2 should be pinged; 1 is filtered out.
        coVerify(exactly = 0) { pinger.ping("ip-1", any()) }
        coVerify(exactly = 1) { pinger.ping("ip-2", any()) }
        cleanup()
    }

    @Test
    fun `re-pings cities whose cached ping was from a different IP`() = runTest {
        coEvery { db.getAllPingsAsync() } returns listOf(
            pingRow(id = 1, ip = "OLD-IP", pingTime = 50)
        )
        coEvery { db.getPingableDatacenters() } returns listOf(datacenter(1))
        coEvery { db.getPingIpAndHost(1) } returns Pair("ip-1", "host-1")

        buildRepo().updateAllServerLatencies()
        advanceUntilIdle()

        coVerify(exactly = 1) { pinger.ping("ip-1", any()) }
        cleanup()
    }

    @Test
    fun `re-pings cities whose cached ping is stale`() = runTest {
        val tenMinutesAgo = System.currentTimeMillis() - 10 * 60_000
        coEvery { db.getAllPingsAsync() } returns listOf(
            pingRow(id = 1, ip = "100.1.1.1", updatedAt = tenMinutesAgo, pingTime = 50)
        )
        coEvery { db.getPingableDatacenters() } returns listOf(datacenter(1))
        coEvery { db.getPingIpAndHost(1) } returns Pair("ip-1", "host-1")

        buildRepo().updateAllServerLatencies()
        advanceUntilIdle()

        coVerify(exactly = 1) { pinger.ping("ip-1", any()) }
        cleanup()
    }

    @Test
    fun `re-pings cities whose previous ping was -1`() = runTest {
        coEvery { db.getAllPingsAsync() } returns listOf(
            pingRow(id = 1, ip = "100.1.1.1", pingTime = -1)
        )
        coEvery { db.getPingableDatacenters() } returns listOf(datacenter(1))
        coEvery { db.getPingIpAndHost(1) } returns Pair("ip-1", "host-1")

        buildRepo().updateAllServerLatencies()
        advanceUntilIdle()

        coVerify(exactly = 1) { pinger.ping("ip-1", any()) }
        cleanup()
    }

    @Test
    fun `skips cities when currentIp is null even if cached pings exist`() = runTest {
        every { preferences.userIP } returns null
        coEvery { db.getAllPingsAsync() } returns listOf(
            pingRow(id = 1, ip = "100.1.1.1", pingTime = 50)
        )
        coEvery { db.getPingableDatacenters() } returns listOf(datacenter(1))
        coEvery { db.getPingIpAndHost(1) } returns Pair("ip-1", "host-1")

        buildRepo().updateAllServerLatencies()
        advanceUntilIdle()

        // currentIp == null short-circuits the "valid" filter -> re-ping.
        coVerify(exactly = 1) { pinger.ping("ip-1", any()) }
        cleanup()
    }

    // ---------- updateAllServerLatencies: persistence + event ----------

    @Test
    fun `persists each successful ping result and emits a Servers event`() = runTest {
        coEvery { db.getPingableDatacenters() } returns listOf(datacenter(1), datacenter(2))
        coEvery { db.getPingIpAndHost(1) } returns Pair("ip-1", "host-1")
        coEvery { db.getPingIpAndHost(2) } returns Pair("ip-2", "host-2")
        coEvery { pinger.ping("ip-1", any()) } returns 30
        coEvery { pinger.ping("ip-2", any()) } returns 60
        coEvery { db.getLowestPingIdAsync() } returns 1

        val repo = buildRepo()
        val before = repo.latencyEvent.value
        val changed = repo.updateAllServerLatencies()
        advanceUntilIdle()

        assertTrue("Expected the call to report change=true", changed)
        coVerify(exactly = 2) { db.addPing(any()) }
        verify { preferences.lowestPingId = 1 }
        verify { preferences.pingTestRequired = false }
        // Event boolean flipped.
        assertNotEquals(before, repo.latencyEvent.value)
        assertEquals(LatencyRepository.LatencyType.Servers, repo.latencyEvent.value.second)
        cleanup()
    }

    @Test
    fun `skips cities with no ping ip + host row in DB`() = runTest {
        coEvery { db.getPingableDatacenters() } returns listOf(datacenter(1), datacenter(2))
        coEvery { db.getPingIpAndHost(1) } returns null            // no row -> pingJob returns null
        coEvery { db.getPingIpAndHost(2) } returns Pair("ip-2", "host-2")

        buildRepo().updateAllServerLatencies()
        advanceUntilIdle()

        coVerify(exactly = 0) { pinger.ping("ip-1", any()) }
        coVerify(exactly = 1) { pinger.ping("ip-2", any()) }
        cleanup()
    }

    // ---------- updateFavouriteCityLatencies ----------

    @Test
    fun `updateFavouriteCityLatencies pings each favourite and emits Servers event`() = runTest {
        coEvery { db.getFavouritesAsync() } returns listOf(Favourite(10), Favourite(20))
        coEvery { db.getDatacenterByIDAsync(10) } returns datacenter(10)
        coEvery { db.getDatacenterByIDAsync(20) } returns datacenter(20)
        coEvery { db.getPingIpAndHost(10) } returns Pair("ip-10", "host-10")
        coEvery { db.getPingIpAndHost(20) } returns Pair("ip-20", "host-20")

        val repo = buildRepo()
        val before = repo.latencyEvent.value
        val changed = repo.updateFavouriteCityLatencies()
        advanceUntilIdle()

        assertTrue(changed)
        coVerify(exactly = 2) { db.addPing(any()) }
        assertEquals(LatencyRepository.LatencyType.Servers, repo.latencyEvent.value.second)
        assertNotEquals(before, repo.latencyEvent.value)
        cleanup()
    }

    @Test
    fun `updateFavouriteCityLatencies skips favourites whose datacenter lookup throws`() = runTest {
        coEvery { db.getFavouritesAsync() } returns listOf(Favourite(10), Favourite(20))
        coEvery { db.getDatacenterByIDAsync(10) } throws RuntimeException("not found")
        coEvery { db.getDatacenterByIDAsync(20) } returns datacenter(20)
        coEvery { db.getPingIpAndHost(20) } returns Pair("ip-20", "host-20")

        buildRepo().updateFavouriteCityLatencies()
        advanceUntilIdle()

        coVerify(exactly = 0) { pinger.ping("ip-10", any()) }
        coVerify(exactly = 1) { pinger.ping("ip-20", any()) }
        cleanup()
    }

    @Test
    fun `updateFavouriteCityLatencies returns false when no favourites`() = runTest {
        coEvery { db.getFavouritesAsync() } returns emptyList()

        val changed = buildRepo().updateFavouriteCityLatencies()
        advanceUntilIdle()

        assertFalse(changed)
        cleanup()
    }

    // ---------- updateStaticIpLatency ----------

    @Test
    fun `updateStaticIpLatency pings each region and emits StaticIp event`() = runTest {
        val region1 = mockk<StaticRegion> {
            every { id } returns 100
            every { ipId } returns 1
            every { getStaticIpNode() } returns mockk { every { ip } returns "static-1" }
        }
        val region2 = mockk<StaticRegion> {
            every { id } returns 200
            every { ipId } returns 2
            every { getStaticIpNode() } returns mockk { every { ip } returns "static-2" }
        }
        coEvery { db.getAllStaticRegions() } returns listOf(region1, region2)

        val repo = buildRepo()
        val before = repo.latencyEvent.value
        val changed = repo.updateStaticIpLatency()
        advanceUntilIdle()

        assertTrue(changed)
        coVerify(exactly = 1) { pinger.ping("static-1", any()) }
        coVerify(exactly = 1) { pinger.ping("static-2", any()) }
        coVerify(exactly = 2) { db.addPing(any()) }
        assertEquals(LatencyRepository.LatencyType.StaticIp, repo.latencyEvent.value.second)
        assertNotEquals(before, repo.latencyEvent.value)
        cleanup()
    }

    @Test
    fun `updateStaticIpLatency does NOT update lowestPingId (Servers-only side effect)`() = runTest {
        val region = mockk<StaticRegion> {
            every { id } returns 100
            every { ipId } returns 1
            every { getStaticIpNode() } returns mockk { every { ip } returns "static-1" }
        }
        coEvery { db.getAllStaticRegions() } returns listOf(region)

        buildRepo().updateStaticIpLatency()
        advanceUntilIdle()

        // The Servers-only branch shouldn't fire for StaticIp.
        coVerify(exactly = 0) { db.getLowestPingIdAsync() }
        verify(exactly = 0) { preferences.lowestPingId = any() }
        cleanup()
    }

    @Test
    fun `updateStaticIpLatency returns false when there are no regions`() = runTest {
        coEvery { db.getAllStaticRegions() } returns emptyList()

        assertFalse(buildRepo().updateStaticIpLatency())
        cleanup()
    }

    // ---------- updateConfigLatencies ----------

    @Test
    fun `updateConfigLatencies aborts entirely when skipPing is true (VPN active)`() = runTest {
        every { vpnConnectionStateManager.isVPNActive() } returns true
        // skipPing throws before either field is read; the mock just needs to exist.
        coEvery { db.getAllConfigs() } returns listOf(mockk<ConfigFile>(relaxed = true))

        val changed = buildRepo().updateConfigLatencies()
        advanceUntilIdle()

        assertFalse(changed)
        coVerify(exactly = 0) { pinger.ping(any(), any()) }
        cleanup()
    }

    @Test
    fun `updateConfigLatencies aborts entirely when offline`() = runTest {
        isOnline.value = false
        coEvery { db.getAllConfigs() } returns listOf(mockk<ConfigFile>(relaxed = true))

        assertFalse(buildRepo().updateConfigLatencies())
        advanceUntilIdle()
        coVerify(exactly = 0) { pinger.ping(any(), any()) }
        cleanup()
    }

    @Test
    fun `updateConfigLatencies returns false when there are no configs`() = runTest {
        coEvery { db.getAllConfigs() } returns emptyList()

        assertFalse(buildRepo().updateConfigLatencies())
        cleanup()
    }

    // ---------- skipPing integration via updateAllServerLatencies ----------

    @Test
    fun `pings happen normally when VPN is inactive and online`() = runTest {
        every { vpnConnectionStateManager.isVPNActive() } returns false
        isOnline.value = true
        coEvery { db.getPingableDatacenters() } returns listOf(datacenter(1))
        coEvery { db.getPingIpAndHost(1) } returns Pair("ip-1", "host-1")

        buildRepo().updateAllServerLatencies()
        advanceUntilIdle()

        coVerify(exactly = 1) { pinger.ping("ip-1", any()) }
        cleanup()
    }

    // ---------- latencyEvent details ----------

    @Test
    fun `event boolean flips on each successful update`() = runTest {
        coEvery { db.getPingableDatacenters() } returns listOf(datacenter(1))
        coEvery { db.getPingIpAndHost(1) } returns Pair("ip-1", "host-1")

        val repo = buildRepo()
        val initial = repo.latencyEvent.value.first
        repo.updateAllServerLatencies()
        advanceUntilIdle()
        val afterFirst = repo.latencyEvent.value.first
        assertEquals(!initial, afterFirst)

        repo.updateAllServerLatencies()
        advanceUntilIdle()
        val afterSecond = repo.latencyEvent.value.first
        assertEquals(initial, afterSecond)
        cleanup()
    }

    @Test
    fun `event does not flip when the ping batch was empty`() = runTest {
        coEvery { db.getPingableDatacenters() } returns emptyList()
        coEvery { db.getAllStaticRegions() } returns emptyList()

        val repo = buildRepo()
        val before = repo.latencyEvent.value
        repo.updateAllServerLatencies()
        advanceUntilIdle()

        assertEquals(before, repo.latencyEvent.value)
        cleanup()
    }

    // ---------- ping result is what gets persisted ----------

    @Test
    fun `pinger result is what gets persisted to addPing`() = runTest {
        coEvery { db.getPingableDatacenters() } returns listOf(datacenter(7))
        coEvery { db.getPingIpAndHost(7) } returns Pair("ip-7", "host-7")
        coEvery { pinger.ping("ip-7", any()) } returns 123

        val captured = slot<PingTime>()
        coEvery { db.addPing(capture(captured)) } returns Unit

        buildRepo().updateAllServerLatencies()
        advanceUntilIdle()

        assertEquals(7, captured.captured.ping_id)
        assertEquals(123, captured.captured.pingTime)
        assertFalse("Datacenter pings are non-static", captured.captured.isStatic)
        cleanup()
    }
}
