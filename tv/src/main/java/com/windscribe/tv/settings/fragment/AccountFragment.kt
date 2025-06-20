/*
 * Copyright (c) 2021 Windscribe Limited.
 */
package com.windscribe.tv.settings.fragment

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.windscribe.tv.R
import com.windscribe.tv.databinding.FragmentAccountBinding
import com.windscribe.tv.listeners.SettingsFragmentListener
import com.windscribe.tv.settings.SettingActivity

class AccountFragment : Fragment() {
    enum class Status {
        NOT_ADDED, NOT_CONFIRMED, NOT_ADDED_PRO, CONFIRMED
    }

    private lateinit var binding: FragmentAccountBinding
    private var listener: SettingsFragmentListener? = null
    override fun onAttach(context: Context) {
        super.onAttach(context)
        val activity: SettingActivity
        if (context is SettingActivity) {
            activity = context
            try {
                listener = activity
            } catch (e: ClassCastException) {
                throw ClassCastException("$activity must implement OnCompleteListener")
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentAccountBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        listener?.onFragmentReady(this)
        binding.confirmContainer.setOnClickListener {
            listener?.onEmailResend()
        }
        binding.emailContainer.setOnClickListener {
            listener?.onEmailClick()
        }
        binding.planContainer.setOnClickListener {
            val planText = binding.plan.text.toString()
            listener?.onUpgradeClick(planText)
        }
    }

    fun setEmail(email: String?) {
        binding.emailLabel.text = email
    }

    fun setEmailState(status: Status?, email: String?) {
        when (status) {
            Status.CONFIRMED -> {
                binding.emailLabel.text = email
                binding.confirmContainer.visibility = View.GONE
                binding.emailContainer.isFocusable = false
            }

            Status.NOT_CONFIRMED -> {
                binding.emailLabel.text = email
                binding.confirmContainer.visibility = View.VISIBLE
                binding.emailContainer.isFocusable = false
            }

            Status.NOT_ADDED_PRO, Status.NOT_ADDED -> {
                binding.emailLabel.setText(com.windscribe.vpn.R.string.add_email_pro)
                binding.confirmContainer.visibility = View.GONE
                binding.emailContainer.isFocusable = true
            }

            else -> {}
        }
    }

    fun setPlanName(planName: String?) {
        binding.planLabel.text = planName
    }

    fun setResetDate(resetDateLabel: String?, resetDate: String?) {
        binding.expiry.text = resetDate
        binding.expiryLabel.text = resetDateLabel
    }

    fun setUsername(username: String?) {
        binding.usernameLabel.text = username
    }

    fun setupLayoutForFreeUser(upgradeText: String?) {
        binding.plan.text = upgradeText
        binding.proIcon.visibility = View.GONE
        binding.planContainer.isFocusable = true
    }

    fun setupLayoutForPremiumUser(upgradeText: String?) {
        binding.plan.text = upgradeText
        binding.proIcon.visibility = View.VISIBLE
        binding.planContainer.isFocusable = false
    }
}
