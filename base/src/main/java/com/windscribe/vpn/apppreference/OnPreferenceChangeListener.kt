/*
 * Copyright (c) 2021 Windscribe Limited.
 */
package com.windscribe.vpn.apppreference

/**
 * Listener for preference changes.
 * Replaces Tray's OnTrayPreferenceChangeListener with a DataStore-compatible implementation.
 */
fun interface OnPreferenceChangeListener {
    /**
     * Called when any preference value changes.
     * @param key The preference key that changed (may be null if all preferences changed)
     */
    fun onPreferenceChanged(key: String?)
}
