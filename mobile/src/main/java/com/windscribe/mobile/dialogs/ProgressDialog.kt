/*
 * Copyright (c) 2021 Windscribe Limited.
 */
package com.windscribe.mobile.dialogs

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import com.windscribe.mobile.databinding.FragmentProgressBinding

class ProgressDialog : FullScreenDialog() {

    private var binding: FragmentProgressBinding? = null
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        binding = FragmentProgressBinding.inflate(inflater, container, false)
        return binding?.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // Delay the progress bar visibility to avoid the flicker
        Looper.getMainLooper().let {
            Handler(it).postDelayed({
                binding?.progressBar?.visibility = View.VISIBLE
            }, 100)
        }
        arguments?.getString(progressTextKey)?.let {
            binding?.progressLabel?.text = it
        }
    }

    override fun onDestroyView() {
        binding = null
        super.onDestroyView()
    }

    fun updateProgressStatus(call: String) {
        activity?.runOnUiThread {
            binding?.progressLabel?.text = call
        }
    }

    companion object {
        private const val progressTextKey = "progressTextKey"
        const val tag = "ProgressDialog"

        @JvmStatic
        fun show(activity: AppCompatActivity, progressText: String? = null) {
            if (activity.supportFragmentManager.findFragmentByTag(tag) != null) {
                return
            }
            activity.runOnUiThread {
                kotlin.runCatching {
                    ProgressDialog().apply {
                        Bundle().apply {
                            putString(progressTextKey, progressText)
                            arguments = this
                        }
                    }.show(activity.supportFragmentManager, tag)
                }
            }
        }

        @JvmStatic
        fun hide(activity: AppCompatActivity) {
            activity.runOnUiThread {
                activity.supportFragmentManager.findFragmentByTag(tag)?.let {
                    (it as ProgressDialog).dismiss()
                }
            }
        }
    }
}