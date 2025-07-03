/*
 * Copyright (c) 2021 Windscribe Limited.
 */
package com.windscribe.tv.serverlist.fragments

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.windscribe.tv.serverlist.overlay.OverlayListener
import androidx.leanback.widget.VerticalGridView
import com.windscribe.tv.R
import com.windscribe.tv.serverlist.adapters.FavouriteAdapter

class FavouriteFragment : Fragment() {
    private var overlayListener: OverlayListener? = null
    private var serverView: VerticalGridView? = null
    override fun onAttach(context: Context) {
        super.onAttach(context)
        overlayListener = context as OverlayListener
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_favourite, container, false)
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        serverView = view.findViewById(R.id.all_server_view)
        serverView?.itemAnimator = null
        serverView?.setNumColumns(1)
        overlayListener?.onFavouriteOverlayReady()
        serverView?.requestFocus()
    }

    fun setAdapter(adapter: FavouriteAdapter) {
        serverView?.adapter = adapter
    }
}