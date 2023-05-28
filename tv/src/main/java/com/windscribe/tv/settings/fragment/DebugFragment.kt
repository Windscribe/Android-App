/*
 * Copyright (c) 2021 Windscribe Limited.
 */
package com.windscribe.tv.settings.fragment

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.leanback.widget.VerticalGridView
import butterknife.BindView
import butterknife.ButterKnife
import com.windscribe.tv.R
import com.windscribe.tv.adapter.DebugViewAdapter
import com.windscribe.tv.listeners.SettingsFragmentListener
import com.windscribe.tv.settings.SettingActivity
import java.lang.ClassCastException

class DebugFragment : Fragment() {
    @JvmField
    @BindView(R.id.progress_bar)
    var progressBar: ProgressBar? = null

    @JvmField
    @BindView(R.id.progress_text)
    var progressText: TextView? = null

    @JvmField
    @BindView(R.id.debug_recycle_view)
    var recyclerView: VerticalGridView? = null
    private var listener: SettingsFragmentListener? = null
    override fun onAttach(context: Context) {
        super.onAttach(context)
        val activity: SettingActivity
        if (context is SettingActivity) {
            activity = context
            try {
                listener = activity
            } catch (e: ClassCastException) {
                throw ClassCastException("$activity must implement OnCompleteListener")
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_debug, container, false)
        ButterKnife.bind(this, view)
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        listener?.onFragmentReady(this)
    }

    fun showLoading(loadingText: String, error: String) {
        if (loadingText.isNotEmpty() && error.isEmpty()) {
            recyclerView?.visibility = View.GONE
            progressBar?.visibility = View.VISIBLE
            progressText?.visibility = View.VISIBLE
            progressText?.text = loadingText
        } else if (loadingText.isEmpty() && error.isEmpty()) {
            recyclerView?.visibility = View.VISIBLE
            progressBar?.visibility = View.GONE
            progressText?.visibility = View.GONE
        } else {
            recyclerView?.visibility = View.GONE
            progressBar?.visibility = View.INVISIBLE
            progressText?.visibility = View.VISIBLE
            progressText?.text = error
        }
    }

    fun showLogs(log: List<String>) {
        if (log.isNotEmpty()) {
            recyclerView?.visibility = View.VISIBLE
            val debugViewAdapter = DebugViewAdapter(log)
            recyclerView?.windowAlignmentOffsetPercent = 50f
            recyclerView?.scrollToPosition(log.size)
            recyclerView?.adapter = debugViewAdapter
        }
    }
}
