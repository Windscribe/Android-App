package com.windscribe.vpn.workers.worker

import android.content.Context
import androidx.work.ListenableWorker.Result
import androidx.work.WorkerParameters
import com.windscribe.vpn.repository.NotificationRepository
import com.windscribe.vpn.repository.UserRepository
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.just
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for [NotificationWorker].
 *
 * The worker was crashing because `notificationRepository.update()` was called without any guard;
 * any exception escaping it (including the WSNet-backed API path) propagated out of `doWork()`.
 * It now mirrors the other workers (StaticIpWorker / SessionWorker / RobertSyncWorker):
 *  - not logged in            -> Result.failure(), repo never touched
 *  - logged in, update OK     -> Result.success()
 *  - logged in, update throws -> Result.failure() (regression guard for the crash)
 *
 * Context / WorkerParameters are never read by doWork(), so relaxed mocks are sufficient.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class NotificationWorkerTest {
    private lateinit var context: Context
    private lateinit var workerParams: WorkerParameters
    private lateinit var notificationRepository: NotificationRepository
    private lateinit var userRepository: UserRepository

    @Before
    fun setUp() {
        context = mockk(relaxed = true)
        workerParams = mockk(relaxed = true)
        notificationRepository = mockk(relaxed = true)
        userRepository = mockk(relaxed = true)
    }

    private fun buildWorker(): NotificationWorker = NotificationWorker(context, workerParams, notificationRepository, userRepository)

    @Test
    fun `returns failure and skips update when user is not logged in`() =
        runTest {
            coEvery { userRepository.loggedIn() } returns false

            val result = buildWorker().doWork()

            assertEquals(Result.failure(), result)
            coVerify(exactly = 0) { notificationRepository.update() }
        }

    @Test
    fun `returns success when logged in and update succeeds`() =
        runTest {
            coEvery { userRepository.loggedIn() } returns true
            coEvery { notificationRepository.update() } just Runs

            val result = buildWorker().doWork()

            assertEquals(Result.success(), result)
            coVerify(exactly = 1) { notificationRepository.update() }
        }

    @Test
    fun `returns failure instead of crashing when update throws`() =
        runTest {
            coEvery { userRepository.loggedIn() } returns true
            coEvery { notificationRepository.update() } throws IllegalStateException("WSNet not initialized")

            val result = buildWorker().doWork()

            assertEquals(Result.failure(), result)
            coVerify(exactly = 1) { notificationRepository.update() }
        }
}
