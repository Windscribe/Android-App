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
import com.windscribe.mobile.databinding.NodeStatusLayoutBinding

interface NodeStatusDialogCallback {
    fun checkNodeStatus()
}

class NodeStatusDialog : FullScreenDialog() {
    private var callBack: NodeStatusDialogCallback? = null
    private var binding: NodeStatusLayoutBinding? = null

    override fun onAttach(context: Context) {
        super.onAttach(context)
        callBack = context as? NodeStatusDialogCallback
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        binding = NodeStatusLayoutBinding.inflate(inflater, container, false)
        return binding?.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding?.nodeStatusPrimaryButton?.setOnClickListener {
            callBack?.checkNodeStatus()
            dismiss()
        }
        binding?.nodeStatusSecondaryButton?.setOnClickListener {
            dismiss()
        }
        if (arguments?.getBoolean(staticLocation, false) == true) {
          binding?.nodeStatusPrimaryButton?.visibility = View.GONE
        }
    }

    override fun onDestroyView() {
        binding = null
        super.onDestroyView()
    }

    companion object {
        const val tag = "NodeStatusDialog"
        const val staticLocation = "IsStaticLocation"
        fun show(activity: AppCompatActivity, isStaticLocation: Boolean) {
            if (activity.supportFragmentManager.findFragmentByTag(tag) != null) {
                return
            }
            activity.runOnUiThread {
                kotlin.runCatching {
                    val dialog = NodeStatusDialog()
                    dialog.arguments = Bundle().apply { putBoolean(staticLocation, isStaticLocation) }
                    dialog.showNow(activity.supportFragmentManager, tag)
                }
            }
        }
    }
}