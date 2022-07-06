/*
 * Copyright (c) 2021 Windscribe Limited.
 */
package com.windscribe.tv.welcome.fragment

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.ColorStateList
import android.os.Bundle
import android.text.method.PasswordTransformationMethod
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.widget.AppCompatCheckBox
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.fragment.app.Fragment
import butterknife.BindView
import butterknife.ButterKnife
import butterknife.OnClick
import butterknife.OnFocusChange
import butterknife.OnTextChanged
import com.windscribe.tv.R

class SignUpFragment : Fragment(), WelcomeActivityCallback {
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
    @BindView(R.id.forgotPassword)
    var forgotPasswordButton: TextView? = null

    @JvmField
    @SuppressLint("NonConstantResourceId")
    @BindView(R.id.login_sign_up)
    var loginSignUpButton: TextView? = null

    @JvmField
    @SuppressLint("NonConstantResourceId")
    @BindView(R.id.username_container)
    var loginUsernameContainer: ConstraintLayout? = null

    @JvmField
    @SuppressLint("NonConstantResourceId")
    @BindView(R.id.password_edit)
    var passwordEditText: EditText? = null

    @JvmField
    @SuppressLint("NonConstantResourceId")
    @BindView(R.id.title)
    var titleView: TextView? = null

    @JvmField
    @SuppressLint("NonConstantResourceId")
    @BindView(R.id.username_edit)
    var usernameEditText: EditText? = null

    @JvmField
    @SuppressLint("NonConstantResourceId")
    @BindView(R.id.show_password)
    var showPasswordView: AppCompatCheckBox? = null
    private var isAccountSetUpLayout = false
    private var fragmentCallBack: FragmentCallback? = null
    override fun onAttach(context: Context) {
        if (activity is FragmentCallback) {
            fragmentCallBack = activity as FragmentCallback?
        }
        super.onAttach(context)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (arguments != null) {
            isAccountSetUpLayout = arguments?.getString("startFragmentName", "SignUp") == "AccountSetUp"
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_sign_up, container, false)
        ButterKnife.bind(this, view)
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        if (isAccountSetUpLayout) {
            titleView?.text = getString(R.string.account_set_up)
            forgotPasswordButton?.visibility = View.GONE
        }
        loginUsernameContainer?.requestFocus()
        passwordEditText?.transformationMethod = PasswordTransformationMethod()
        showPasswordView?.isChecked = false
    }

    override fun clearInputErrors() {
        errorView?.visibility = View.INVISIBLE
        errorView?.text = ""
    }

    @SuppressLint("NonConstantResourceId")
    @OnTextChanged(R.id.username_edit, R.id.password_edit)
    fun onInputTextChanged() {
        clearInputErrors()
    }

    override fun setLoginError(error: String) {
        errorView?.visibility = View.VISIBLE
        errorView?.text = error
    }

    override fun setPasswordError(error: String) {
        errorView?.visibility = View.VISIBLE
        errorView?.text = error
    }

    override fun setSecretCode(code: String) {}
    override fun setUsernameError(error: String) {
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
    @OnFocusChange(R.id.forgotPassword)
    fun onFocusChangeToForgotPassword() {
        resetButtonTextColor()
    }

    @SuppressLint("NonConstantResourceId")
    @OnFocusChange(R.id.password_container)
    fun onFocusChangeToPasswordContainer() {
        resetButtonTextColor()
    }

    @SuppressLint("NonConstantResourceId")
    @OnFocusChange(R.id.show_password)
    fun onFocusChangeToShowPassword() {
        resetButtonTextColor()
    }

    @SuppressLint("NonConstantResourceId")
    @OnFocusChange(R.id.username_container)
    fun onFocusChangeToUsernameContainer() {
        resetButtonTextColor()
    }

    @SuppressLint("NonConstantResourceId")
    @OnFocusChange(R.id.login_sign_up)
    fun onFocusLoginButton() {
        resetButtonTextColor()
    }

    @SuppressLint("NonConstantResourceId")
    @OnClick(R.id.forgotPassword)
    fun onForgotPasswordButtonClick() {
        fragmentCallBack!!.onForgotPasswordClick()
    }

    @SuppressLint("NonConstantResourceId")
    @OnClick(R.id.login_sign_up)
    fun onLoginButtonClick() {
        if (isAccountSetUpLayout) {
            fragmentCallBack?.onAccountClaimButtonClick(
                usernameEditText?.text.toString(),
                passwordEditText?.text.toString(), "", true
            )
        } else {
            fragmentCallBack?.onSignUpButtonClick(
                usernameEditText?.text.toString(),
                passwordEditText?.text.toString(), "", true
            )
        }
    }

    @SuppressLint("NonConstantResourceId")
    @OnClick(R.id.password_container)
    fun onPasswordContainerClick() {
        showPasswordView?.visibility = View.VISIBLE
        passwordEditText?.visibility = View.VISIBLE
        passwordEditText?.requestFocus()
    }

    @SuppressLint("NonConstantResourceId")
    @OnClick(R.id.show_password)
    fun onShowPasswordButtonClick() {
        if (showPasswordView?.isChecked == true) {
            passwordEditText?.transformationMethod = null
        } else {
            passwordEditText?.transformationMethod = PasswordTransformationMethod()
        }
        passwordEditText?.let {
            it.setSelection(it.text.length)
        }
    }

    @SuppressLint("NonConstantResourceId")
    @OnClick(R.id.username_container)
    fun onUsernameContainerClick() {
        usernameEditText?.visibility = View.VISIBLE
        usernameEditText?.requestFocus()
    }

    private fun resetButtonTextColor() {
        val normalColor = requireActivity().resources.getColor(R.color.colorWhite50)
        val focusColor = requireActivity().resources.getColor(R.color.colorWhite)
        loginSignUpButton!!.setTextColor(
            if (loginSignUpButton!!.hasFocus()) requireActivity().resources.getColor(R.color.colorWhite) else requireActivity().resources.getColor(
                R.color.colorWhite50
            )
        )
        backButton?.setTextColor(if (backButton!!.hasFocus()) focusColor else normalColor)
        forgotPasswordButton?.setTextColor(if (forgotPasswordButton?.hasFocus() == true) focusColor else normalColor)
        showPasswordView?.setTextColor(if (showPasswordView?.hasFocus() == true) focusColor else normalColor)
        showPasswordView?.buttonTintList = ColorStateList.valueOf(if (showPasswordView!!.hasFocus()) focusColor else normalColor)
    }
}
