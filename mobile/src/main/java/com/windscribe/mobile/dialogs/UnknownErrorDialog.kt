/*
 * Copyright (c) 2021 Windscribe Limited.
 */
package com.windscribe.mobile.dialogs

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import com.windscribe.mobile.databinding.UnknownErrorAlertBinding


interface UnknownErrorDialogCallback {
    fun contactSupport()
    fun exportLog()
}

class UnknownErrorDialog : FullScreenDialog() {
    private var callback: UnknownErrorDialogCallback? = null
    private var binding: UnknownErrorAlertBinding? = null
    override fun onAttach(context: Context) {
        callback = context as? UnknownErrorDialogCallback
        super.onAttach(context)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        binding = UnknownErrorAlertBinding.inflate(inflater, container, false)
        return binding?.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val error = arguments?.getString(errorKey)
        binding?.unknownErrorDescription?.text = error
        binding?.unknownErrorContactSupportButton?.setOnClickListener {
            callback?.contactSupport()
            dismiss()
        }
        binding?.unknownErrorCancelButton?.setOnClickListener {
            dismiss()
        }
    }

    override fun onDestroyView() {
        binding = null
        super.onDestroyView()
    }

    companion object {
        const val tag = "UnknownErrorDialog"
        private const val errorKey = "error"
        fun show(activity: AppCompatActivity, error: String) {
            if (activity.supportFragmentManager.findFragmentByTag(tag) != null) {
                return
            }
            activity.runOnUiThread {
                kotlin.runCatching {
                    UnknownErrorDialog().apply {
                        arguments = Bundle().apply {
                            putString(errorKey, error)
                        }
                    }.showNow(activity.supportFragmentManager, tag)
                }
            }
        }
    }
}