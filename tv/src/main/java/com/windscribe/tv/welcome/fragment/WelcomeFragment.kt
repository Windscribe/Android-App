/*
 * Copyright (c) 2021 Windscribe Limited.
 */
package com.windscribe.tv.welcome.fragment

import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.fragment.app.Fragment
import butterknife.BindView
import butterknife.ButterKnife
import butterknife.OnClick
import butterknife.OnFocusChange
import com.windscribe.tv.R

class WelcomeFragment : Fragment() {
    @JvmField
    @SuppressLint("NonConstantResourceId")
    @BindView(R.id.buttonLabel)
    var buttonLabel: TextView? = null

    @JvmField
    @SuppressLint("NonConstantResourceId")
    @BindView(R.id.login)
    var loginButton: Button? = null

    @JvmField
    @SuppressLint("NonConstantResourceId")
    @BindView(R.id.continue_without_account)
    var noAccountButton: Button? = null
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
    ): View? {
        val view = inflater.inflate(R.layout.fragment_welcome, container, false)
        ButterKnife.bind(this, view)
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        view.findViewById<View>(R.id.continue_without_account).requestFocus()
    }

    @SuppressLint("NonConstantResourceId")
    @OnFocusChange(R.id.login)
    fun onFocusChangeToLogin() {
        resetButtonTextColor()
    }

    @SuppressLint("NonConstantResourceId")
    @OnFocusChange(R.id.continue_without_account)
    fun onFocusChangeToNoAccount() {
        resetButtonTextColor()
    }

    @SuppressLint("NonConstantResourceId")
    @OnClick(R.id.login)
    fun onLoginClick() {
        fragmentCallback?.onLoginClick()
    }

    @SuppressLint("NonConstantResourceId")
    @OnClick(R.id.continue_without_account)
    fun onNoAccountClick() {
        fragmentCallback?.onContinueWithOutAccountClick()
    }

    private fun resetButtonTextColor() {
        if (activity == null) {
            return
        }
        loginButton!!.setTextColor(
            if (loginButton?.hasFocus() == true) requireActivity().resources.getColor(R.color.colorWhite) else requireActivity().resources.getColor(
                R.color.colorWhite50
            )
        )
        noAccountButton?.setTextColor(
            if (noAccountButton?.hasFocus() == true) requireActivity().resources.getColor(R.color.colorWhite) else requireActivity().resources.getColor(
                R.color.colorWhite50
            )
        )
        if (noAccountButton?.hasFocus() == true) {
            buttonLabel?.text = ""
        } else {
            buttonLabel?.text = getString(R.string.login_label)
        }
    }
}
