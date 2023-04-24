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
import com.windscribe.mobile.databinding.FragmentLocationPermissionRationaleBinding

interface PermissionRationaleDialogCallback {
    fun goToAppInfoSettings()
}

class LocationPermissionRationaleDialog : FullScreenDialog() {
    private var callBack: PermissionRationaleDialogCallback? = null
    private var binding: FragmentLocationPermissionRationaleBinding? = null
    override fun onAttach(context: Context) {
        super.onAttach(context)
        callBack = context as? PermissionRationaleDialogCallback
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        binding = FragmentLocationPermissionRationaleBinding.inflate(inflater, container, false)
        return binding?.root
    }

    override fun onDestroyView() {
        binding = null
        super.onDestroyView()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding?.tvOk?.setOnClickListener {
            callBack?.goToAppInfoSettings()
            dismiss()
        }
        binding?.tvCancel?.setOnClickListener { dismissAllowingStateLoss() }
    }

    companion object {
        const val tag = "LocationPermissionRationaleDialog"
        fun show(activity: AppCompatActivity) {
            if (activity.supportFragmentManager.findFragmentByTag(tag) != null) {
                return
            }
            activity.runOnUiThread {
                kotlin.runCatching {
                    LocationPermissionRationaleDialog().showNow(
                        activity.supportFragmentManager, tag
                    )
                }
            }
        }
    }
}