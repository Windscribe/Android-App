/*
 * Copyright (c) 2021 Windscribe Limited.
 */
package com.windscribe.tv.welcome.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.windscribe.tv.R
import com.windscribe.tv.databinding.ForgotPasswordBinding

class ForgotPasswordFragment : Fragment() {
    private lateinit var binding: ForgotPasswordBinding
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = ForgotPasswordBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.back.requestFocus()
        binding.back.setOnClickListener {
            requireActivity().onBackPressed()
        }
        binding.back.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                binding.back.setTextColor(resources.getColor(R.color.colorWhite))
            } else {
                binding.back.setTextColor(resources.getColor(R.color.colorWhite50))
            }
        }
    }
}
