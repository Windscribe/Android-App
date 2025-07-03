/*
 * Copyright (c) 2021 Windscribe Limited.
 */
package com.windscribe.tv.serverlist.overlay

interface OverlayPresenter {
    fun onDestroy()
    suspend fun observeStaticRegions()
    suspend fun observeAllLocations()
    fun favouriteViewReady()
    suspend fun staticIpViewReady()
    suspend fun allLocationViewReady()
    suspend fun windLocationViewReady()
    suspend fun observeLatencyChange()
}