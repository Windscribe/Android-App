/*
 * Copyright (c) 2021 Windscribe Limited.
 */
package com.windscribe.tv.welcome.fragment

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import com.windscribe.tv.R
import com.windscribe.tv.databinding.FragmentTwoFactorBinding

class TwoFactorFragment : Fragment() {
    private lateinit var binding: FragmentTwoFactorBinding
    private var fragmentCallBack: FragmentCallback? = null
    private var password: String? = null
    private var username: String? = null
    override fun onAttach(context: Context) {
        if (activity is FragmentCallback) {
            fragmentCallBack = activity as FragmentCallback?
        }
        super.onAttach(context)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        binding = FragmentTwoFactorBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        username = arguments?.getString("username")
        password = arguments?.getString("password")
        binding.twoFaContainer.requestFocus()
        binding.back.setOnClickListener {
            fragmentCallBack?.onBackButtonPressed()
        }
        binding.back.setOnFocusChangeListener { _, _ ->
            resetButtonTextColor()
        }
        binding.twoFaEdit.setOnFocusChangeListener { _, _ ->
            resetButtonTextColor()
        }
        binding.twoFaContainer.setOnClickListener {
            binding.twoFaEdit.visibility = View.VISIBLE
            binding.twoFaEdit.requestFocus()
        }
        binding.loginSignUp.setOnClickListener {
            username?.let {
                password?.let { pass ->
                    fragmentCallBack?.onLoginButtonClick(
                        it, pass, binding.twoFaEdit.text.toString(), null, null
                    )
                }
            }
        }
        binding.twoFaEdit.doAfterTextChanged {
            clearInputErrors()
        }
    }

    private fun clearInputErrors() {
        binding.error.visibility = View.INVISIBLE
        binding.error.text = ""
    }

    fun setTwoFaError(error: String?) {
        binding.error.visibility = View.VISIBLE
        binding.error.text = error
    }

    private fun resetButtonTextColor() {
        binding.back.setTextColor(
            if (binding.back.hasFocus()) requireActivity().resources.getColor(R.color.colorWhite) else requireActivity().resources.getColor(
                R.color.colorWhite50
            )
        )
    }
}
