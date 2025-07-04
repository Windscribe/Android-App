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
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.windscribe.tv.R
import com.windscribe.tv.serverlist.adapters.ServerAdapter
import com.windscribe.tv.serverlist.fragments.CustomVerticalGridView.CustomFocusListener
import com.windscribe.tv.serverlist.overlay.OverlayListener
import kotlinx.coroutines.launch

class WindOverlayFragment : Fragment() {
    private var recyclerView: CustomVerticalGridView? = null
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
        val view = inflater.inflate(R.layout.fragment_wind_overlay, container, false)
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        recyclerView = view.findViewById(R.id.all_server_view)
        recyclerView?.setNumColumns(4)
        recyclerView?.setCustomFocusListener(object : CustomFocusListener {
            override fun onExit() {
                overlayListener?.onExit()
            }
        })
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.CREATED) {
                overlayListener?.onWindOverlayReady()
                recyclerView?.requestFocus()
            }
        }
    }

    fun setWindOverlayAdapter(serverAdapter: ServerAdapter) {
        recyclerView?.adapter = serverAdapter
    }
}