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
    fun setSplitRoutingModeAdapter(
        localizeValues: Array<String>,
        selectedValue: String,
        values: Array<String>
    )

    fun setupToggleImage(resourceId: Int)
    fun showProgress(progress: Boolean)
    fun showTunnelSettingsLayout()
}