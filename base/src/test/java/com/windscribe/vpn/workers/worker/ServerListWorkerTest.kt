/*
 * Copyright (c) 2026 Windscribe Limited.
 */

package com.windscribe.vpn.workers.worker

import android.content.Context
import androidx.work.ListenableWorker.Result
import androidx.work.WorkerParameters
import com.windscribe.vpn.repository.ServerListRepository
import com.windscribe.vpn.repository.UserRepository
import com.windscribe.vpn.workers.ReconnectAfterRefresh
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.coVerifyOrder
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ServerListWorkerTest {
    private lateinit var context: Context
    private lateinit var workerParameters: WorkerParameters
    private lateinit var serverListRepository: ServerListRepository
    private lateinit var userRepository: UserRepository
    private lateinit var reconnectAfterRefresh: ReconnectAfterRefresh

    @Before
    fun setUp() {
        context = mockk(relaxed = true)
        workerParameters = mockk(relaxed = true)
        serverListRepository = mockk(relaxed = true)
        userRepository = mockk(relaxed = true)
        reconnectAfterRefresh = mockk(relaxed = true)
    }

    private fun TestScope.worker(): ServerListWorker =
        ServerListWorker(context, workerParameters, serverListRepository, userRepository, reconnectAfterRefresh, this)

    /** The list is what the reconnect decision reads, so it lands before the decision is made. */
    @Test
    fun `the list is current before anything reconnects`() =
        runTest {
            every { userRepository.loggedIn() } returns true
            coEvery { serverListRepository.update() } just Runs

            val result = worker().doWork()
            advanceUntilIdle()

            assertEquals(Result.success(), result)
            coVerifyOrder {
                serverListRepository.update()
                reconnectAfterRefresh.run()
            }
        }

    @Test
    fun `a failed refresh decides nothing`() =
        runTest {
            every { userRepository.loggedIn() } returns true
            coEvery { serverListRepository.update() } throws IllegalStateException("no network")

            val result = worker().doWork()
            advanceUntilIdle()

            assertEquals(Result.failure(), result)
            coVerify(exactly = 0) { reconnectAfterRefresh.run() }
        }

    @Test
    fun `a logged out user refreshes nothing`() =
        runTest {
            every { userRepository.loggedIn() } returns false

            assertEquals(Result.failure(), worker().doWork())
            coVerify(exactly = 0) { serverListRepository.update() }
        }
}
