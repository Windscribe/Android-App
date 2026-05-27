/*
 * Copyright (c) 2021 Windscribe Limited.
 */
package com.windscribe.tv.serverlist.overlay

import kotlinx.coroutines.CoroutineScope

interface OverlayPresenter {
    fun bind(
        view: OverlayView,
        scope: CoroutineScope,
    )

    fun onDestroy()

    suspend fun observeStaticRegions()

    suspend fun observeAllLocations()

    fun favouriteViewReady()

    suspend fun staticIpViewReady()

    suspend fun allLocationViewReady()

    suspend fun observeLatencyChange()
}
