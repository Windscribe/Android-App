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
import com.windscribe.tv.databinding.FragmentDebugBinding
import com.windscribe.tv.listeners.SettingsFragmentListener
import com.windscribe.tv.settings.SettingActivity
import java.lang.ClassCastException

class DebugFragment : Fragment() {
    private lateinit var binding: FragmentDebugBinding
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
    ): View {
        binding = FragmentDebugBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        listener?.onFragmentReady(this)
    }

    fun showLoading(loadingText: String, error: String) {
        if (loadingText.isNotEmpty() && error.isEmpty()) {
            binding.debugRecycleView.visibility = View.GONE
            binding.progressBar.visibility = View.VISIBLE
            binding.progressText.visibility = View.VISIBLE
            binding.progressText.text = loadingText
        } else if (loadingText.isEmpty() && error.isEmpty()) {
            binding.debugRecycleView.visibility = View.VISIBLE
            binding.progressBar.visibility = View.GONE
            binding.progressText.visibility = View.GONE
        } else {
            binding.debugRecycleView.visibility = View.GONE
            binding.progressBar.visibility = View.INVISIBLE
            binding.progressText.visibility = View.VISIBLE
            binding.progressText.text = error
        }
    }

    fun showLogs(log: List<String>) {
        if (log.isNotEmpty()) {
            binding.debugRecycleView.visibility = View.VISIBLE
            val debugViewAdapter = DebugViewAdapter(log)
            binding.debugRecycleView.windowAlignmentOffsetPercent = 50f
            binding.debugRecycleView.scrollToPosition(log.size)
            binding.debugRecycleView.adapter = debugViewAdapter
        }
    }
}
