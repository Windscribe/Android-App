/*
 * Copyright (c) 2021 Windscribe Limited.
 */
package com.windscribe.tv.welcome.fragment

import android.content.Context
import android.content.res.ColorStateList
import android.os.Bundle
import android.text.method.PasswordTransformationMethod
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.windscribe.tv.R
import com.windscribe.tv.databinding.FragmentLoginBinding

class LoginFragment : Fragment(), WelcomeActivityCallback {

    private lateinit var generateCode: Button
    private lateinit var secretCode: TextView
    private lateinit var binding: FragmentLoginBinding
    private var callBack: FragmentCallback? = null
    override fun onAttach(context: Context) {
        if (activity is FragmentCallback) {
            callBack = activity as FragmentCallback?
        }
        super.onAttach(context)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        binding = FragmentLoginBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        generateCode = view.findViewById(R.id.generate_code)
        secretCode = view.findViewById(R.id.secret_code)
        binding.loginSignUpContainer.requestFocus()
        binding.passwordEdit.transformationMethod = PasswordTransformationMethod()
        binding.showPassword.isChecked = false
        addFocusListeners()
        addClickListeners()
        clearInputErrors()
    }

    private fun addFocusListeners() {
        arrayOf(binding.usernameEdit, binding.passwordEdit).forEach {
            it.setOnFocusChangeListener { _, _ ->
                clearInputErrors()
            }
        }
        arrayOf(
            binding.back,
            binding.forgotPassword,
            binding.passwordContainer,
            binding.showPassword,
            binding.usernameContainer,
            binding.loginSignUp,
            generateCode
        ).forEach {
            it.setOnFocusChangeListener { _, _ ->
                resetButtonTextColor()
            }
        }
    }

    private fun addClickListeners() {
        binding.back.setOnClickListener {
            callBack?.onBackButtonPressed()
        }
        binding.forgotPassword.setOnClickListener {
            callBack?.onForgotPasswordClick()
        }
        generateCode.setOnClickListener {
            callBack?.onGenerateCodeClick()
        }
        binding.loginSignUp.setOnClickListener {
            callBack?.onLoginButtonClick(binding.usernameEdit.text.toString(), binding.passwordEdit.text.toString(), null, null, null)
        }
        binding.passwordContainer.setOnClickListener {
            binding.showPassword.visibility = View.VISIBLE
            binding.passwordEdit.visibility = View.VISIBLE
            binding.passwordEdit.requestFocus()
        }
        binding.showPassword.setOnClickListener {
            if (binding.showPassword.isChecked) {
                binding.passwordEdit.transformationMethod = null
            } else {
                binding.passwordEdit.transformationMethod = PasswordTransformationMethod()
            }
            binding.passwordEdit.setSelection(binding.passwordEdit.text?.length ?: 0)
        }
        binding.usernameContainer.setOnClickListener {
            binding.usernameEdit.visibility = View.VISIBLE
            binding.usernameEdit.requestFocus()
        }
    }

    override fun clearInputErrors() {
        binding.error.visibility = View.INVISIBLE
        binding.error.text = ""
    }

    override fun setLoginError(error: String) {
        binding.error.visibility = View.VISIBLE
        binding.error.text = error
    }

    override fun setPasswordError(error: String) {
        binding.error.visibility = View.VISIBLE
        binding.error.text = error
    }

    override fun setSecretCode(code: String) {
        if (code.isEmpty()) {
            secretCode.text = code
            secretCode.visibility = View.GONE
            generateCode.visibility = View.VISIBLE
            generateCode.requestFocus()
        } else {
            generateCode.visibility = View.GONE
            secretCode.visibility = View.VISIBLE
            secretCode.text = code
        }
    }

    override fun setUsernameError(error: String) {
        binding.error.visibility = View.VISIBLE
        binding.error.text = error

    }

    private fun resetButtonTextColor() {
        val normalColor = requireActivity().resources.getColor(R.color.colorWhite50)
        val focusColor = requireActivity().resources.getColor(R.color.colorWhite)
        binding.loginSignUp.setTextColor(
            if (binding.loginSignUp.hasFocus()) requireActivity().resources.getColor(R.color.colorWhite) else requireActivity().resources.getColor(
                R.color.colorWhite50
            )
        )
        generateCode.setTextColor(
            if (generateCode.hasFocus()) requireActivity().resources.getColor(R.color.colorWhite) else requireActivity().resources.getColor(
                R.color.colorWhite50
            )
        )
        binding.back.setTextColor(if (binding.back.hasFocus()) focusColor else normalColor)
        binding.forgotPassword.setTextColor(if (binding.forgotPassword.hasFocus()) focusColor else normalColor)
        binding.showPassword.setTextColor(if (binding.showPassword.hasFocus()) focusColor else normalColor)
        binding.showPassword.buttonTintList =
            ColorStateList.valueOf(if (binding.showPassword.hasFocus()) focusColor else normalColor)
    }
}
