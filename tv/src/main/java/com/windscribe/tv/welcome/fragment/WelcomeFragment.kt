/*
 * Copyright (c) 2021 Windscribe Limited.
 */
package com.windscribe.tv.welcome.fragment

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.windscribe.tv.R
import com.windscribe.tv.databinding.FragmentWelcomeBinding

class WelcomeFragment : Fragment() {
    private lateinit var binding: FragmentWelcomeBinding
    private var fragmentCallback: FragmentCallback? = null
    override fun onAttach(context: Context) {
        if (activity is FragmentCallback) {
            fragmentCallback = activity as FragmentCallback?
        }
        super.onAttach(context)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentWelcomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.continueWithoutAccount.requestFocus()
        binding.login.setOnFocusChangeListener { v, hasFocus ->
            resetButtonTextColor()
        }
        binding.continueWithoutAccount.setOnFocusChangeListener { v, hasFocus ->
            resetButtonTextColor()
        }
        binding.login.setOnClickListener {
            fragmentCallback?.onLoginClick()
        }
        binding.continueWithoutAccount.setOnClickListener {
            fragmentCallback?.onContinueWithOutAccountClick()
        }
    }

    private fun resetButtonTextColor() {
        if (activity == null) {
            return
        }
        binding.login.setTextColor(
            if (binding.login.hasFocus()) requireActivity().resources.getColor(R.color.colorWhite) else requireActivity().resources.getColor(
                R.color.colorWhite50
            )
        )
        binding.continueWithoutAccount.setTextColor(
            if (binding.continueWithoutAccount.hasFocus()) requireActivity().resources.getColor(R.color.colorWhite) else requireActivity().resources.getColor(
                R.color.colorWhite50
            )
        )
        if (binding.continueWithoutAccount.hasFocus()) {
            binding.buttonLabel.text = ""
        } else {
            binding.buttonLabel.text = getString(com.windscribe.vpn.R.string.login_label)
        }
    }
}
