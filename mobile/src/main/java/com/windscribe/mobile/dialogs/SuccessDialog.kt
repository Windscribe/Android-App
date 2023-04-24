/*
 * Copyright (c) 2021 Windscribe Limited.
 */
package com.windscribe.mobile.dialogs

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import com.windscribe.mobile.databinding.FragmentSuccessBinding

class SuccessDialog : FullScreenDialog() {

    private var binding: FragmentSuccessBinding? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        binding = FragmentSuccessBinding.inflate(inflater, container, false)
        return binding?.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        arguments?.getInt(backgroundColorKey)?.let {
            view.setBackgroundColor(it)
        }
        binding?.message?.text = arguments?.getString(messageKey)
        binding?.closeBtn?.setOnClickListener {
            dismiss()
        }
    }

    override fun onDestroyView() {
        binding = null
        super.onDestroyView()
    }

    companion object {
        const val tag = "success_dialog"
        private const val backgroundColorKey = "backgroundColor"
        private const val messageKey = "message"
        fun show(activity: AppCompatActivity, message: String?, backgroundColor: Int? = null) {
            if (activity.supportFragmentManager.findFragmentByTag(tag) != null) {
                return
            }
            activity.runOnUiThread {
                kotlin.runCatching {
                    SuccessDialog().apply {
                        Bundle().apply {
                            putString(messageKey, message)
                            backgroundColor?.let { putInt(backgroundColorKey, it) }
                            arguments = this
                        }
                    }.showNow(activity.supportFragmentManager, tag)
                }
            }
        }
    }
}