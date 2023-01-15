/*
 * Copyright (c) 2021 Windscribe Limited.
 */
package com.windscribe.tv.serverlist.detail

interface DetailPresenter {
    fun init(regionId: Int)
    fun onDestroy()
}