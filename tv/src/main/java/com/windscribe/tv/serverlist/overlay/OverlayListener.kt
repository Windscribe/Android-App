/*
 * Copyright (c) 2021 Windscribe Limited.
 */
package com.windscribe.tv.serverlist.overlay

interface OverlayListener {
    suspend fun onAllOverlayViewReady()
    fun onExit()
    fun onFavouriteOverlayReady()
    fun onStaticOverlayReady()
    suspend fun onWindOverlayReady()
}