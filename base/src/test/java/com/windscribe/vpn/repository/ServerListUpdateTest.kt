package com.windscribe.vpn.repository

import com.windscribe.vpn.api.IApiCallManager
import com.windscribe.vpn.api.response.GenericResponseClass
import com.windscribe.vpn.api.response.LocationResponse
import com.windscribe.vpn.api.response.ServerData
import com.windscribe.vpn.api.response.ServerInventory
import com.windscribe.vpn.api.response.ServerResponse
import com.windscribe.vpn.api.response.UserSessionResponse
import com.windscribe.vpn.apppreference.PreferencesHelper
import com.windscribe.vpn.localdatabase.LocalDbInterface
import dagger.Lazy
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

/** Two callers arriving together must share one locations + server-list download, not run two. */
@OptIn(ExperimentalCoroutinesApi::class)
class ServerListUpdateTest {
    private val api = mockk<IApiCallManager>(relaxed = true)
    private val db = mockk<LocalDbInterface>(relaxed = true)
    private val prefs = mockk<PreferencesHelper>(relaxed = true)

    /** Backs [PreferencesHelper.serverRevision] so the second caller sees the first one's write. */
    private var revision = 0L

    /**
     * The repository's init launches collectors that never complete, so it gets the background
     * scope; update() is still called from the test body.
     */
    private fun repository(scope: CoroutineScope): ServerListRepository {
        every { prefs.serverRevision } answers { revision }
        every { prefs.serverRevision = any() } answers { revision = firstArg() }
        every { prefs.migrationRequired } returns false
        every { prefs.getBackupParameter() } returns 0
        coEvery { db.getAllLocationsAsync() } returns emptyList()
        coEvery { db.observeAllServers() } returns kotlinx.coroutines.flow.flowOf(emptyList())
        // Slow enough that the second caller is guaranteed to arrive mid-flight.
        coEvery { api.getLocations() } coAnswers {
            delay(100)
            GenericResponseClass(
                mockk<LocationResponse>(relaxed = true).also { every { it.locations } returns emptyList() },
                null,
            )
        }
        coEvery { api.getServers(any()) } coAnswers {
            delay(100)
            GenericResponseClass(
                mockk<ServerResponse>(relaxed = true).also {
                    every { it.servers } returns emptyList()
                    every { it.revision } returns 42L
                },
                null,
            )
        }
        // The cheap branch a second caller should fall through to once the first has finished.
        coEvery { api.getSessionGeneric(null, any()) } returns
            GenericResponseClass(
                mockk<UserSessionResponse>(relaxed = true).also { every { it.serverInventory } returns null },
                null,
            )
        return ServerListRepository(
            scope,
            api,
            db,
            Lazy { mockk(relaxed = true) },
            prefs,
            mockk(relaxed = true),
        )
    }

    @Test
    fun `two concurrent updates download the server list once`() =
        runTest {
            val repo = repository(backgroundScope)
            launch { repo.update() }
            launch { repo.update() }
            advanceUntilIdle()

            coVerify(exactly = 1) { api.getLocations() }
            coVerify(exactly = 1) { api.getServers(any()) }
            assertEquals("the first caller's revision must be visible to the second", 42L, revision)
        }

    /** The delta the caller observed is the delta that gets applied: no second session fetch. */
    @Test
    fun `a supplied inventory is applied without refetching the session`() =
        runTest {
            revision = 755L
            val repo = repository(backgroundScope)
            val inventory =
                ServerInventory(
                    action = ServerInventory.ACTION_DELTA,
                    enabled =
                        listOf(
                            ServerData(
                                id = 263,
                                hostname = "us-014.windscribe.dev",
                                ip = "181.215.52.122",
                                ip2 = "181.215.52.123",
                                ip3 = "181.215.52.124",
                                datacenterId = 87,
                                weight = 1,
                                health = 0,
                                forceDisconnect = 1,
                            ),
                        ),
                    disabled = emptyList(),
                    revision = 756L,
                )

            repo.update(inventory)
            advanceUntilIdle()

            coVerify(exactly = 0) { api.getSessionGeneric(any(), any()) }
            coVerify(exactly = 1) { db.addServers(match { it.single().forceDisconnect == 1 }) }
            assertEquals("the supplied revision must be stored", 756L, revision)
        }

    @Test
    fun `a later update reuses the revision instead of refetching`() =
        runTest {
            val repo = repository(backgroundScope)
            repo.update()
            advanceUntilIdle()
            repo.update()
            advanceUntilIdle()

            coVerify(exactly = 1) { api.getLocations() }
        }
}
