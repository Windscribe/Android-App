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
import android.widget.EditText
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.fragment.app.Fragment
import butterknife.BindView
import butterknife.ButterKnife
import butterknife.OnClick
import butterknife.OnFocusChange
import butterknife.OnTextChanged
import com.windscribe.tv.R

class TwoFactorFragment : Fragment() {
    @JvmField
    @SuppressLint("NonConstantResourceId")
    @BindView(R.id.back)
    var backButton: TextView? = null

    @JvmField
    @SuppressLint("NonConstantResourceId")
    @BindView(R.id.error)
    var errorView: TextView? = null

    @JvmField
    @SuppressLint("NonConstantResourceId")
    @BindView(R.id.two_fa_container)
    var twoFaContainer: ConstraintLayout? = null

    @JvmField
    @SuppressLint("NonConstantResourceId")
    @BindView(R.id.two_fa_edit)
    var twoFaEditTextView: EditText? = null
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
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_two_factor, container, false)
        ButterKnife.bind(this, view)
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        username = arguments?.getString("username")
        password = arguments?.getString("password")
        twoFaContainer?.requestFocus()
    }

    private fun clearInputErrors() {
        errorView?.visibility = View.INVISIBLE
        errorView?.text = ""
    }

    @SuppressLint("NonConstantResourceId")
    @OnTextChanged(R.id.two_fa_edit)
    fun onInputTextChanged() {
        clearInputErrors()
    }

    fun setTwoFaError(error: String?) {
        errorView?.visibility = View.VISIBLE
        errorView?.text = error
    }

    @SuppressLint("NonConstantResourceId")
    @OnClick(R.id.back)
    fun onBackButtonClick() {
        fragmentCallBack?.onBackButtonPressed()
    }

    @SuppressLint("NonConstantResourceId")
    @OnFocusChange(R.id.back)
    fun onFocusChangeToBack() {
        resetButtonTextColor()
    }

    @SuppressLint("NonConstantResourceId")
    @OnFocusChange(R.id.two_fa_container)
    fun onFocusChangeToTwoFaContainer() {
        resetButtonTextColor()
    }

    @SuppressLint("NonConstantResourceId")
    @OnClick(R.id.login_sign_up)
    fun onLoginButtonClick() {
        username?.let {
            password?.let { pass ->
                fragmentCallBack?.onLoginButtonClick(
                    it,
                    pass,
                    twoFaEditTextView?.text.toString()
                )
            }
        }
    }

    @SuppressLint("NonConstantResourceId")
    @OnClick(R.id.two_fa_container)
    fun onTwoFaContainerClick() {
        twoFaEditTextView?.visibility = View.VISIBLE
        twoFaEditTextView?.requestFocus()
    }

    private fun resetButtonTextColor() {
        backButton?.setTextColor(
            if (backButton?.hasFocus() == true) requireActivity().resources.getColor(R.color.colorWhite) else requireActivity().resources.getColor(
                R.color.colorWhite50
            )
        )
    }
}
