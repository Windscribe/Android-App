/*
 * Copyright (c) 2021 Windscribe Limited.
 */
package com.windscribe.mobile.dialogs

import android.content.DialogInterface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.ColorInt
import androidx.appcompat.app.AppCompatActivity
import com.windscribe.mobile.databinding.FragmentErrorBinding

class ErrorDialog : FullScreenDialog() {

    private var binding: FragmentErrorBinding? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        binding = FragmentErrorBinding.inflate(inflater, container, false)
        return binding?.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        arguments?.getInt(backgroundColorKey)?.let {
            view.setBackgroundColor(it)
        }
        binding?.error?.text = arguments?.getString(errorKey)
        binding?.closeBtn?.requestFocus()
        binding?.closeBtn?.setOnClickListener {
            dismiss()
        }
    }

    override fun onDestroyView() {
        binding = null
        super.onDestroyView()
    }

    override fun onDismiss(dialog: DialogInterface) {
        activity?.let {
            if (arguments?.getBoolean(closeActivityKey) == true) {
                it.finish()
            }
        }
        super.onDismiss(dialog)
    }

    companion object {
        const val tag = "error_dialog"
        private const val errorKey = "error"
        private const val backgroundColorKey = "backgroundColor"
        private const val closeActivityKey = "closeActivity"

        @JvmStatic
        fun show(
            activity: AppCompatActivity,
            error: String?,
            @ColorInt backgroundColor: Int? = null,
            closeActivity: Boolean = false
        ) {
            if (activity.supportFragmentManager.findFragmentByTag(tag) != null) {
                return
            }
            activity.runOnUiThread {
                kotlin.runCatching {
                    ErrorDialog().apply {
                        Bundle().apply {
                            putString(errorKey, error)
                            backgroundColor?.let { putInt(backgroundColorKey, it) }
                            putBoolean(closeActivityKey, closeActivity)
                            arguments = this
                        }
                    }.showNow(activity.supportFragmentManager, tag)
                }
            }
        }

        fun hide(activity: AppCompatActivity) {
            activity.supportFragmentManager.findFragmentByTag(tag)?.let {
                (it as? ErrorDialog)?.dismiss()
            }
        }
    }
}