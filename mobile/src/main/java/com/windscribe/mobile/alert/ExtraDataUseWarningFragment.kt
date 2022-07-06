/*
 * Copyright (c) 2021 Windscribe Limited.
 */
package com.windscribe.mobile.alert

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.DialogFragment
import com.windscribe.mobile.R

class ExtraDataUseWarningFragment : DialogFragment() {
    private var callBack: CallBack? = null
    override fun onAttach(context: Context) {
        super.onAttach(context)
        try {
            callBack = context as CallBack
        } catch (ignored: ClassCastException) {
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NORMAL, R.style.FullScreenDialog)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        return if (activity != null) {
            val dialogView: View = inflater.inflate(R.layout.fragment_extra_data_use_warning, container, false)
            dialogView.findViewById<View>(R.id.tv_ok).setOnClickListener {
                callBack!!.turnOnDecoyTraffic()
                dismissAllowingStateLoss()
            }
            dialogView.findViewById<View>(R.id.tv_cancel).setOnClickListener { dismissAllowingStateLoss() }
            dialogView
        } else {
            super.getView()
        }
    }

    override fun onStart() {
        super.onStart()
        val width = ViewGroup.LayoutParams.MATCH_PARENT
        val height = ViewGroup.LayoutParams.MATCH_PARENT
        dialog?.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog?.window?.setLayout(width, height)
    }

    interface CallBack {
        fun turnOnDecoyTraffic()
    }
}