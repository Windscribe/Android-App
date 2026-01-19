/*
 * Copyright (c) 2021 Windscribe Limited.
 */
package com.windscribe.vpn.apppreference

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore

/**
 * DataStore instance for app preferences.
 * Single instance per application context (lazy init).
 */
val Context.windscribeDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "windscribe_preferences"
)
