/*
 * Copyright (c) 2021 Windscribe Limited.
 */
package com.windscribe.tv.serverlist.detail

import com.windscribe.tv.serverlist.adapters.DetailViewAdapter
import com.windscribe.tv.serverlist.overlay.LoadState

interface DetailView {
    fun onDisabledNodeClick()
    fun onNodeSelected(cityID: Int)
    fun setCount(count: String)
    fun setCountryFlagBackground(flagIconResource: Int)
    fun setDetailAdapter(detailAdapter: DetailViewAdapter)
    fun setState(state: LoadState, stateDrawable: Int, stateText: Int)
    fun setTitle(text: String)
    fun showToast(text: String)
}