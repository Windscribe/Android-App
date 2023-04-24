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
import com.windscribe.mobile.databinding.LocationPermissionAlertBinding

interface LocationPermissionDialogCallback {
    fun onRequestPermission(requestCode: Int)
}

class LocationPermissionDialog : FullScreenDialog() {
    private var locationPermissionDialogCallback: LocationPermissionDialogCallback? = null
    private var binding: LocationPermissionAlertBinding? = null

    override fun onAttach(context: Context) {
        super.onAttach(context)
        locationPermissionDialogCallback = context as? LocationPermissionDialogCallback?
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        binding = LocationPermissionAlertBinding.inflate(inflater, container, false)
        return binding?.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding?.tvOk?.setOnClickListener {
            locationPermissionDialogCallback?.onRequestPermission(
                arguments?.getInt(requestCodeKey) ?: 0
            )
            dismiss()
        }
        binding?.tvCancel?.setOnClickListener {
            dismissAllowingStateLoss()
        }
    }

    override fun onDestroyView() {
        binding = null
        super.onDestroyView()
    }

    companion object {
        const val tag = "LocationPermissionDialog"
        private const val requestCodeKey = "requestCode"
        fun show(activity: AppCompatActivity, requestCode: Int) {
            if (activity.supportFragmentManager.findFragmentByTag(tag) != null) {
                return
            }
            activity.runOnUiThread {
                kotlin.runCatching {
                    LocationPermissionDialog().apply {
                        Bundle().apply {
                            putInt(requestCodeKey, requestCode)
                            arguments = this
                        }
                    }.showNow(activity.supportFragmentManager, tag)
                }
            }
        }
    }
}