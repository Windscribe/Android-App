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
import com.windscribe.mobile.databinding.RateMyDialogBinding

interface RateAppDialogCallback {
    fun neverAskAgainClicked()
    fun rateLaterClicked()
    fun rateNowClicked()
}

class RateAppDialog : FullScreenDialog() {
    private var rateAppDialogCallback: RateAppDialogCallback? = null
    private var binding: RateMyDialogBinding? = null
    override fun onAttach(context: Context) {
        super.onAttach(context)
        rateAppDialogCallback = activity as RateAppDialogCallback?
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        binding = RateMyDialogBinding.inflate(inflater, container, false)
        return binding?.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding?.rateMeNow?.setOnClickListener {
            rateAppDialogCallback?.rateNowClicked()
            dismiss()
        }
        binding?.rateMeLater?.setOnClickListener {
            rateAppDialogCallback?.rateLaterClicked()
            dismiss()
        }
        binding?.neverAskAgain?.setOnClickListener {
            rateAppDialogCallback?.neverAskAgainClicked()
            dismiss()
        }
    }

    companion object {
        const val tag = "RateAppDialog"
        fun show(activity: AppCompatActivity) {
            activity.runOnUiThread {
                kotlin.runCatching {
                    RateAppDialog().showNow(activity.supportFragmentManager, tag)
                }
            }
        }
    }
}