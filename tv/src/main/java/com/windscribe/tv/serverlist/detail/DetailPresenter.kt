/*
 * Copyright (c) 2021 Windscribe Limited.
 */
package com.windscribe.tv.serverlist.detail

import com.windscribe.tv.serverlist.detail.DetailView
import kotlinx.coroutines.CoroutineScope

interface DetailPresenter {
    fun bind(view: DetailView, scope: CoroutineScope)
    fun init(regionId: Int)
    fun onDestroy()
    suspend fun observeLatencyChange()
}