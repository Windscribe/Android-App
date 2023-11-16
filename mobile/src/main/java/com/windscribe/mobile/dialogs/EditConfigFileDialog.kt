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
import com.windscribe.mobile.R
import com.windscribe.mobile.databinding.EditConfigLayoutBinding
import com.windscribe.vpn.serverlist.entity.ConfigFile

interface EditConfigFileDialogCallback {
    fun onConfigFileUpdated(configFile: ConfigFile)
    fun onSubmitUsernameAndPassword(configFile: ConfigFile)
}

class EditConfigFileDialog : FullScreenDialog() {
    private var requestDialogCallback: EditConfigFileDialogCallback? = null
    private var binding: EditConfigLayoutBinding? = null
    override fun onAttach(context: Context) {
        super.onAttach(context)
        requestDialogCallback = context as? EditConfigFileDialogCallback
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        binding = EditConfigLayoutBinding.inflate(inflater, container, false)
        return binding?.root
    }

    override fun onDestroyView() {
        binding = null
        super.onDestroyView()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val configFile = arguments?.getSerializable(configFileKey) as? ConfigFile
        configFile?.let {
            configFile.username?.let {
                binding?.username?.setText(it)
            }
            configFile.password?.let {
                binding?.password?.setText(it)
            }
            binding?.name?.setText(configFile.name)
            binding?.rememberCheck?.setImageResource(if (configFile.isRemember) R.drawable.ic_checkmark_on else R.drawable.ic_checkmark_off)
            binding?.rememberCheck?.setOnClickListener {
                configFile.isRemember = !configFile.isRemember
                binding?.rememberCheck?.setImageResource(
                    if (configFile.isRemember) R.drawable.ic_checkmark_on else R.drawable.ic_checkmark_off
                )
            }
            binding?.requestAlertOk?.setOnClickListener {
                configFile.name = binding?.name?.text.toString()
                configFile.username = binding?.username?.text.toString()
                configFile.password = binding?.password?.text.toString()
                configFile.type = 1
                requestDialogCallback?.onConfigFileUpdated(configFile)
                dismiss()
            }
            binding?.requestAlertCancel?.setOnClickListener { dismiss() }
        }
    }

    companion object {
        const val tag: String = "EditConfigFileDialog"
        private const val configFileKey = "configFile"
        fun show(activity: AppCompatActivity, configFile: ConfigFile) {
            activity.runOnUiThread {
                kotlin.runCatching {
                    EditConfigFileDialog().apply {
                        Bundle().apply {
                            putSerializable(configFileKey, configFile)
                            arguments = this
                        }
                    }.showNow(activity.supportFragmentManager, tag)
                }
            }
        }
    }
}