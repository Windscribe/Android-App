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
import com.windscribe.mobile.databinding.FragmentExtraDataUseWarningBinding

interface ExtraDataUseWarningDialogCallBack {
    fun turnOnDecoyTraffic()
}

class ExtraDataUseWarningDialog : FullScreenDialog() {
    private var extraDataUseWarningDialogCallBack: ExtraDataUseWarningDialogCallBack? = null
    private var binding: FragmentExtraDataUseWarningBinding? = null
    override fun onAttach(context: Context) {
        super.onAttach(context)
        extraDataUseWarningDialogCallBack = context as? ExtraDataUseWarningDialogCallBack
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        binding = FragmentExtraDataUseWarningBinding.inflate(inflater, container, false)
        return binding?.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding?.tvOk?.setOnClickListener {
            extraDataUseWarningDialogCallBack?.turnOnDecoyTraffic()
            dismiss()
        }
        binding?.tvCancel?.setOnClickListener { dismiss() }
    }

    override fun onDestroyView() {
        binding = null
        super.onDestroyView()
    }

    companion object {
        const val tag = "ExtraDataUseWarningDialog"
        fun show(activity: AppCompatActivity) {
            if (activity.supportFragmentManager.findFragmentByTag(tag) != null) {
                return
            }
            activity.runOnUiThread {
                kotlin.runCatching {
                    ExtraDataUseWarningDialog().showNow(activity.supportFragmentManager, tag)
                }
            }
        }
    }
}