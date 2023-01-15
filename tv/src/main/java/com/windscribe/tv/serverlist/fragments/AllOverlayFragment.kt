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
import butterknife.ButterKnife
import com.windscribe.tv.R
import com.windscribe.tv.serverlist.adapters.ServerAdapter
import com.windscribe.tv.serverlist.overlay.OverlayListener

class AllOverlayFragment : Fragment(), CustomVerticalGridView.CustomFocusListener {
    var recyclerView: CustomVerticalGridView? = null
    private var overlayListener: OverlayListener? = null
    override fun onAttach(context: Context) {
        super.onAttach(context)
        overlayListener = context as OverlayListener
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_all_overlay, container, false)
        ButterKnife.bind(this, view)
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        recyclerView = view.findViewById(R.id.all_server_view)
        recyclerView?.setNumColumns(4)
        recyclerView?.setItemViewCacheSize(12)
        recyclerView?.setCustomFocusListener(this)
        overlayListener?.onAllOverlayViewReady()
        recyclerView?.requestFocus()
    }

    fun setAllOverlayAdapter(serverAdapter: ServerAdapter?) {
        recyclerView?.adapter = serverAdapter
    }

    override fun onExit() {
        overlayListener?.onExit()
    }
}