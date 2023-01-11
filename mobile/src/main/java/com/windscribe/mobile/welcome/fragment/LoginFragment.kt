/*
 * Copyright (c) 2021 Windscribe Limited.
 */
package com.windscribe.mobile.welcome.fragment

import android.content.Context
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.text.method.PasswordTransformationMethod
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.widget.AppCompatCheckBox
import androidx.fragment.app.Fragment
import butterknife.BindView
import butterknife.ButterKnife
import butterknife.OnCheckedChanged
import butterknife.OnClick
import com.windscribe.mobile.R
import java.util.concurrent.atomic.AtomicBoolean

class LoginFragment : Fragment(), TextWatcher, WelcomeActivityCallback {
    @BindView(R.id.loginButton)
    lateinit var loginButton: Button

    @BindView(R.id.password)
    lateinit var passwordEditText: EditText

    @BindView(R.id.password_error)
    lateinit var passwordErrorView: ImageView

    @BindView(R.id.password_visibility_toggle)
    lateinit var passwordVisibilityToggle: AppCompatCheckBox

    @BindView(R.id.nav_title)
    lateinit var titleView: TextView

    @BindView(R.id.two_fa)
    lateinit var twoFaEditText: EditText

    @BindView(R.id.two_fa_error)
    lateinit var twoFaErrorView: ImageView

    @BindView(R.id.two_fa_hint)
    lateinit var twoFaHintView: TextView

    @BindView(R.id.twoFaToggle)
    lateinit var twoFaToggle: Button

    @BindView(R.id.username)
    lateinit var usernameEditText: EditText

    @BindView(R.id.username_error)
    lateinit var usernameErrorView: ImageView

    @BindView(R.id.two_fa_description)
    lateinit var twoFaDescriptionView: TextView

    private val ignoreEditTextChange = AtomicBoolean(false)
    private var fragmentCallBack: FragmentCallback? = null
    override fun onAttach(context: Context) {
        if (activity is FragmentCallback) {
            fragmentCallBack = activity as FragmentCallback
        }
        super.onAttach(context)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_login, container, false)
        ButterKnife.bind(this, view)
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        titleView.text = getString(R.string.login)
        addEditTextChangeListener()
    }

    override fun afterTextChanged(s: Editable) {
        resetNextButtonView()
    }

    override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {
        if (!ignoreEditTextChange.getAndSet(false)) {
            clearInputErrors()
        }
    }

    override fun clearInputErrors() {
        twoFaDescriptionView.text = getString(R.string.two_fa_description)
        twoFaDescriptionView.setTextColor(resources.getColor(R.color.colorWhite50))
        twoFaErrorView.visibility = View.INVISIBLE
        usernameErrorView.visibility = View.INVISIBLE
        passwordErrorView.visibility = View.INVISIBLE
        usernameEditText.setTextColor(resources.getColor(R.color.colorWhite))
        passwordEditText.setTextColor(resources.getColor(R.color.colorWhite))
    }

    @OnClick(R.id.forgot_password)
    fun onForgotPasswordClick() {
        fragmentCallBack?.onForgotPasswordClick()
    }

    @OnClick(R.id.loginButton)
    fun onLoginButtonClick() {
        fragmentCallBack?.onLoginButtonClick(usernameEditText.text.toString()
            .trim { it <= ' ' },
            passwordEditText.text.toString().trim { it <= ' ' },
            twoFaEditText.text.toString().trim { it <= ' ' })
    }

    @OnClick(R.id.nav_button)
    fun onNavButtonClick() {
        requireActivity().onBackPressed()
    }

    @OnCheckedChanged(R.id.password_visibility_toggle)
    fun onPasswordVisibilityToggleChanged() {
        ignoreEditTextChange.set(true)
        if (passwordVisibilityToggle.isChecked) {
            passwordEditText.transformationMethod = null
        } else {
            passwordEditText.transformationMethod = PasswordTransformationMethod()
        }
        passwordEditText.setSelection(passwordEditText.text.length)
    }

    override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {}

    @OnClick(R.id.twoFaToggle)
    fun onTwoFaToggleClick() {
        if (twoFaEditText.visibility != View.VISIBLE) {
            setTwoFaVisibility(View.VISIBLE)
        }
    }

    @OnClick(R.id.two_fa_hint)
    fun onTwoFaHintClick() {
        if (twoFaEditText.visibility == View.VISIBLE) {
            setTwoFaVisibility(View.GONE)
        }
    }

    override fun setLoginError(error: String) {
        twoFaDescriptionView.visibility = View.VISIBLE
        twoFaDescriptionView.text = error
        twoFaDescriptionView.setTextColor(resources.getColor(R.color.colorRed))
        usernameErrorView.visibility = View.VISIBLE
        passwordErrorView.visibility = View.VISIBLE
        usernameEditText.setTextColor(resources.getColor(R.color.colorRed))
        passwordEditText.setTextColor(resources.getColor(R.color.colorRed))
    }

    override fun setPasswordError(error: String) {
        twoFaDescriptionView.visibility = View.VISIBLE
        twoFaDescriptionView.text = error
        twoFaDescriptionView.setTextColor(resources.getColor(R.color.colorRed))
        passwordErrorView.visibility = View.VISIBLE
        passwordEditText.setTextColor(resources.getColor(R.color.colorRed))
    }

    fun setTwoFaError(errorMessage: String) {
        twoFaDescriptionView.visibility = View.VISIBLE
        twoFaDescriptionView.text = errorMessage
        twoFaDescriptionView.setTextColor(resources.getColor(R.color.colorRed))
        twoFaErrorView.visibility = View.VISIBLE
    }

    fun setTwoFaVisibility(visibility: Int) {
        twoFaDescriptionView.visibility = visibility
        twoFaEditText.visibility = visibility
        twoFaHintView.visibility = visibility
        twoFaToggle.visibility =
            if (visibility == View.VISIBLE) View.GONE else View.VISIBLE
    }

    override fun setUsernameError(error: String) {
        twoFaDescriptionView.visibility = View.VISIBLE
        twoFaDescriptionView.text = error
        twoFaDescriptionView.setTextColor(resources.getColor(R.color.colorRed))
        usernameErrorView.visibility = View.VISIBLE
        usernameEditText.setTextColor(resources.getColor(R.color.colorRed))
    }

    private fun addEditTextChangeListener() {
        usernameEditText.addTextChangedListener(this)
        passwordEditText.addTextChangedListener(this)
        twoFaEditText.addTextChangedListener(this)
    }

    private fun resetNextButtonView() {
        val enable = usernameEditText.text.length > 2 && passwordEditText.text.length > 3
        loginButton.isEnabled = enable
    }
}