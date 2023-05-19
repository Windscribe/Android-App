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
import com.windscribe.mobile.databinding.UsernameAndPasswordRequestLayoutBinding
import com.windscribe.vpn.serverlist.entity.ConfigFile

class UsernameAndPasswordRequestDialog : FullScreenDialog() {
    private var requestDialogCallback: EditConfigFileDialogCallback? = null
    private var binding: UsernameAndPasswordRequestLayoutBinding? = null
    override fun onAttach(context: Context) {
        super.onAttach(context)
        requestDialogCallback = context as? EditConfigFileDialogCallback
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        binding = UsernameAndPasswordRequestLayoutBinding.inflate(inflater, container, false)
        return binding?.root
    }

    override fun onDestroyView() {
        binding = null
        super.onDestroyView()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val configFile = arguments?.getSerializable(configFileKey) as? ConfigFile
        configFile?.let {
            configFile.username?.let {
                binding?.username?.setText(it)
            }
            configFile.password?.let {
                binding?.password?.setText(it)
            }
            binding?.rememberCheck?.setImageResource(if (configFile.isRemember) R.drawable.ic_checkmark_on else R.drawable.ic_checkmark_off)
            binding?.rememberCheck?.setOnClickListener {
                configFile.isRemember = !configFile.isRemember
                binding?.rememberCheck?.setImageResource(
                    if (configFile.isRemember) R.drawable.ic_checkmark_on else R.drawable.ic_checkmark_off
                )
            }
            binding?.requestAlertOk?.setOnClickListener {
                configFile.username = binding?.username?.text.toString()
                configFile.password = binding?.password?.text.toString()
                requestDialogCallback?.onSubmitUsernameAndPassword(configFile)
            }
            binding?.requestAlertCancel?.setOnClickListener {
                dismiss()
            }
        }
    }

    companion object {
        const val tag = "UsernameAndPasswordRequestDialog"
        private const val configFileKey = "configFile"

        fun show(activity: AppCompatActivity, configFile: ConfigFile) {
            activity.runOnUiThread {
                kotlin.runCatching {
                    UsernameAndPasswordRequestDialog().apply {
                        arguments = Bundle().apply {
                            putSerializable(configFileKey, configFile)
                        }
                    }.showNow(activity.supportFragmentManager, tag)
                }
            }
        }
    }
}