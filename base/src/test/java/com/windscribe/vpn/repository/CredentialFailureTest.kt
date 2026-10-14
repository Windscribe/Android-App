/*
 * Copyright (c) 2026 Windscribe Limited.
 */

package com.windscribe.vpn.repository

import com.windscribe.vpn.api.IApiCallManager
import com.windscribe.vpn.api.response.ApiErrorResponse
import com.windscribe.vpn.api.response.GenericResponseClass
import com.windscribe.vpn.apppreference.PreferencesHelper
import com.windscribe.vpn.autoconnection.AutoConnectionManager
import com.windscribe.vpn.constants.NetworkErrorCodes.ERROR_UNABLE_TO_GENERATE_CREDENTIALS
import dagger.Lazy
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Test

/**
 * Our credentials failing must not end a tunnel that never used them.
 *
 * A custom config carries the user's own credentials, so a server-side failure to generate ours says
 * nothing about it — and clearing the standing intent is what stops the later reconcile paths
 * bringing it back.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class CredentialFailureTest {
    private val preferences = mockk<PreferencesHelper>(relaxed = true)
    private val apiCallManager = mockk<IApiCallManager>(relaxed = true)

    private fun repository(): ConnectionDataRepository =
        ConnectionDataRepository(
            preferences,
            apiCallManager,
            mockk<Lazy<AutoConnectionManager>>(relaxed = true),
        )

    private fun <T> unavailable() =
        GenericResponseClass<T?, ApiErrorResponse?>(
            null,
            ApiErrorResponse().apply { errorCode = ERROR_UNABLE_TO_GENERATE_CREDENTIALS },
        )

    private fun credentialsUnavailable() {
        coEvery { apiCallManager.getServerConfig() } returns unavailable()
        coEvery { apiCallManager.getServerCredentials() } returns unavailable()
        coEvery { apiCallManager.getServerCredentialsForIKev2() } returns unavailable()
    }

    @Test
    fun `a custom config keeps its intent when our credentials fail`() =
        runTest {
            credentialsUnavailable()
            every { preferences.isConnectingToConfigured } returns true

            repository().update()

            verify(exactly = 0) { preferences.globalUserConnectionPreference = false }
        }

    @Test
    fun `a Windscribe connection still stands down when our credentials fail`() =
        runTest {
            credentialsUnavailable()
            every { preferences.isConnectingToConfigured } returns false

            repository().update()

            verify { preferences.globalUserConnectionPreference = false }
        }
}
