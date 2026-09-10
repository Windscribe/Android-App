/*
 * Copyright (c) 2026 Windscribe Limited.
 */

package com.windscribe.vpn.repository

import com.windscribe.vpn.api.response.UserSessionResponse
import com.windscribe.vpn.apppreference.PreferencesHelper
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * That [UserRepository.reloadNow] actually runs the callback it is waiting on.
 *
 * Cancellation propagation — a cancelled caller cancelling the application-scope job it started —
 * is deliberately not asserted here. `backgroundScope` work that has been abandoned is not driven
 * to completion by `advanceUntilIdle`, so the harness cannot tell the two implementations apart,
 * and a test that passes either way would read as proof without being any.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ReloadNowTest {
    private val preferences = mockk<PreferencesHelper>(relaxed = true)

    private fun repository(scope: CoroutineScope): UserRepository {
        every { preferences.getSession } returns "{}"
        return UserRepository(
            scope,
            mockk(relaxed = true),
            mockk(relaxed = true),
            mockk(relaxed = true),
            preferences,
            mockk(relaxed = true),
            mockk(relaxed = true),
            mockk(relaxed = true),
            mockk(relaxed = true),
            mockk(relaxed = true),
            mockk(relaxed = true),
            mockk(relaxed = true),
            mockk(relaxed = true),
        )
    }

    @Test
    fun `the callback runs when nothing cancels it`() =
        runTest {
            val repository = repository(backgroundScope)
            var callbackFinished = false

            repository.reloadNow(UserSessionResponse().apply { userName = "u" }) {
                callbackFinished = true
            }
            advanceUntilIdle()

            assertTrue("the test harness never reaches the callback at all", callbackFinished)
        }
}
