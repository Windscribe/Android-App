/*
 * Copyright (c) 2021 Windscribe Limited.
 */
package com.windscribe.tv.welcome.fragment

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import butterknife.BindView
import butterknife.ButterKnife
import butterknife.OnClick
import butterknife.OnFocusChange
import com.windscribe.tv.R

class ForgotPasswordFragment : Fragment() {
    @JvmField
    @SuppressLint("NonConstantResourceId")
    @BindView(R.id.back)
    var backButton: TextView? = null
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.forgot_password, container, false)
        ButterKnife.bind(this, view)
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        backButton?.requestFocus()
    }

    @SuppressLint("NonConstantResourceId")
    @OnClick(R.id.back)
    fun onBackButtonClicked() {
        requireActivity().onBackPressed()
    }

    @SuppressLint("NonConstantResourceId")
    @OnFocusChange(R.id.back)
    fun onFocusChangeToBackButton() {
        if (backButton?.hasFocus() == true) {
            backButton?.setTextColor(resources.getColor(R.color.colorWhite))
        } else {
            backButton?.setTextColor(resources.getColor(R.color.colorWhite50))
        }
    }
}
