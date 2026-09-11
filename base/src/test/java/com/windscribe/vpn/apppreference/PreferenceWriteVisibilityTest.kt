/*
 * Copyright (c) 2026 Windscribe Limited.
 */

package com.windscribe.vpn.apppreference

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import io.mockk.mockk
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * A write must be visible to the very next read.
 *
 * Regression: toggling Auto-Connect wrote the new value and read it straight back to decide what to
 * do. DataStore had not committed, so the policy acted on the previous value — turning the setting
 * off connected, turning it on stopped the service.
 */
class PreferenceWriteVisibilityTest {
    /** Commits nothing, ever, so only a value held in memory can answer a read. */
    private class UncommittedDataStore : DataStore<Preferences> {
        override val data: Flow<Preferences> = flowOf(emptyPreferences())

        override suspend fun updateData(transform: suspend (Preferences) -> Preferences): Preferences = awaitCancellation()
    }

    private fun helper() =
        DataStorePreferenceHelper(
            dataStore = UncommittedDataStore(),
            securePreferences = mockk(relaxed = true),
            scope = CoroutineScope(Dispatchers.Unconfined),
        )

    @Test
    fun `auto-connect reads back as it was just written`() {
        val preferences = helper()
        preferences.autoConnect = true
        assertTrue(preferences.autoConnect)
        preferences.autoConnect = false
        assertFalse(preferences.autoConnect)
    }

    /** Read by the same teardown that writes it, to decide whether the network stands down. */
    @Test
    fun `the user connection preference reads back as it was just written`() {
        val preferences = helper()
        preferences.globalUserConnectionPreference = true
        assertTrue(preferences.globalUserConnectionPreference)
        preferences.globalUserConnectionPreference = false
        assertFalse(preferences.globalUserConnectionPreference)
    }

    @Test
    fun `strings and ints read back as they were just written`() {
        val preferences = helper()
        preferences.advanceParamText = "ignore-connectivity-test"
        assertEquals("ignore-connectivity-test", preferences.advanceParamText)
        preferences.userStatus = 1
        assertEquals(1, preferences.userStatus)
    }

    /** The last write wins, not the first. */
    @Test
    fun `repeated writes read back as the latest`() {
        val preferences = helper()
        listOf(true, false, true, false).forEach { preferences.autoConnect = it }
        assertFalse(preferences.autoConnect)
    }
}
