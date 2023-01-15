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
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.widget.AppCompatCheckBox
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.fragment.app.Fragment
import butterknife.*
import com.windscribe.tv.R

class LoginFragment : Fragment(), WelcomeActivityCallback {
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
    @BindView(R.id.generate_code)
    var generateCode: Button? = null

    @JvmField
    @SuppressLint("NonConstantResourceId")
    @BindView(R.id.login_sign_up)
    var loginButton: TextView? = null

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
    @BindView(R.id.username_edit)
    var usernameEditText: EditText? = null

    @JvmField
    @SuppressLint("NonConstantResourceId")
    @BindView(R.id.secret_code)
    var secretCode: TextView? = null

    @JvmField
    @SuppressLint("NonConstantResourceId")
    @BindView(R.id.show_password)
    var showPasswordView: AppCompatCheckBox? = null
    private var callBack: FragmentCallback? = null
    override fun onAttach(context: Context) {
        if (activity is FragmentCallback) {
            callBack = activity as FragmentCallback?
        }
        super.onAttach(context)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_login, container, false)
        ButterKnife.bind(this, view)
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        loginUsernameContainer?.requestFocus()
        passwordEditText?.transformationMethod = PasswordTransformationMethod()
        showPasswordView?.isChecked = false
        clearInputErrors()
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

    override fun setSecretCode(code: String) {
        if (code.isEmpty()) {
            secretCode?.text = code
            secretCode?.visibility = View.GONE
            generateCode?.visibility = View.VISIBLE
            generateCode?.requestFocus()
        } else {
            generateCode?.visibility = View.GONE
            secretCode?.visibility = View.VISIBLE
            secretCode?.text = code
        }
    }

    override fun setUsernameError(error: String) {
        errorView?.visibility = View.VISIBLE
        errorView?.text = error
    }

    @SuppressLint("NonConstantResourceId")
    @OnClick(R.id.back)
    fun onBackButtonClick() {
        callBack?.onBackButtonPressed()
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
    @OnFocusChange(R.id.generate_code)
    fun onFocusGenerateCodeButton() {
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
        callBack?.onForgotPasswordClick()
    }

    @SuppressLint("NonConstantResourceId")
    @OnClick(R.id.generate_code)
    fun onGenerateCodeClick() {
        callBack?.onGenerateCodeClick()
    }

    @SuppressLint("NonConstantResourceId")
    @OnClick(R.id.login_sign_up)
    fun onLoginButtonClick() {
        callBack?.onLoginButtonClick(usernameEditText?.text.toString(), passwordEditText?.text.toString(), "")
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
        passwordEditText?.let {
            if (showPasswordView?.isChecked == true) {
                it.transformationMethod = null
            } else {
                it.transformationMethod = PasswordTransformationMethod()
            }
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
        loginButton?.setTextColor(
            if (loginButton?.hasFocus() == true) requireActivity().resources.getColor(R.color.colorWhite) else requireActivity().resources.getColor(
                R.color.colorWhite50
            )
        )
        generateCode?.setTextColor(
            if (generateCode?.hasFocus() == true) requireActivity().resources.getColor(R.color.colorWhite) else requireActivity().resources.getColor(
                R.color.colorWhite50
            )
        )
        backButton?.setTextColor(if (backButton?.hasFocus() == true) focusColor else normalColor)
        forgotPasswordButton?.setTextColor(if (forgotPasswordButton?.hasFocus() == true) focusColor else normalColor)
        showPasswordView?.let {
            it.setTextColor(if (it.hasFocus()) focusColor else normalColor)
            it.buttonTintList =
                ColorStateList.valueOf(if (it.hasFocus()) focusColor else normalColor)
        }
    }
}
