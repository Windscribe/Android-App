/*
 * Copyright (c) 2021 Windscribe Limited.
 */
package com.windscribe.mobile.splittunneling

import com.windscribe.mobile.adapter.InstalledAppsAdapter

interface SplitTunnelingView {
    val splitRoutingModes: Array<String>
    fun hideTunnelSettingsLayout()
    fun restartConnection()
    fun setRecyclerViewAdapter(mAdapter: InstalledAppsAdapter)
    fun setSplitModeTextView(mode: String, textDescription: Int)
    fun setSplitRoutingModeAdapter(modes: Array<String>, savedMode: String)
    fun setupToggleImage(resourceId: Int)
    fun showProgress(progress: Boolean)
    fun showTunnelSettingsLayout()
}