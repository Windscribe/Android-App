/*
 * Copyright (c) 2021 Windscribe Limited.
 */
package com.windscribe.mobile.dialogs


import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.setFragmentResult
import com.windscribe.mobile.databinding.BackgroundLocationPermissionDialogBinding
import com.windscribe.mobile.utils.PermissionManagerImpl


class BackgroundLocationPermissionDialog : FullScreenDialog() {
    private var binding: BackgroundLocationPermissionDialogBinding? = null

    override fun onCreateView(
            inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        binding = BackgroundLocationPermissionDialogBinding.inflate(inflater, container, false)
        return binding?.root
    }

    override fun onDestroyView() {
        binding = null
        super.onDestroyView()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding?.tvOk?.setOnClickListener {
            setFragmentResult(PermissionManagerImpl.resultKey, Bundle().apply { putBoolean(PermissionManagerImpl.okButtonKey, true) })
            dismiss()
        }
        binding?.tvCancel?.setOnClickListener {
            setFragmentResult(PermissionManagerImpl.resultKey, Bundle())
            dismissAllowingStateLoss()
        }
        dialog?.setOnCancelListener {
            setFragmentResult(PermissionManagerImpl.resultKey, Bundle())
        }
    }
}