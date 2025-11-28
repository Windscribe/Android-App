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
import android.widget.LinearLayout
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.viewpager2.widget.ViewPager2
import com.windscribe.tv.R
import com.windscribe.tv.databinding.FragmentSignUpBinding

class SignUpFragment : Fragment(), WelcomeActivityCallback {

    private lateinit var binding: FragmentSignUpBinding
    private var isAccountSetUpLayout = false
    private var fragmentCallBack: FragmentCallback? = null
    private lateinit var carouselHelper: CarouselHelper
    override fun onAttach(context: Context) {
        if (activity is FragmentCallback) {
            fragmentCallBack = activity as FragmentCallback?
        }
        super.onAttach(context)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (arguments != null) {
            isAccountSetUpLayout =
                arguments?.getString("startFragmentName", "SignUp") == "AccountSetUp"
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentSignUpBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Setup carousel
        carouselHelper = CarouselHelper(requireContext())
        val viewPager = binding.root.findViewById<ViewPager2>(R.id.feature_carousel)
        val indicators = binding.root.findViewById<LinearLayout>(R.id.carousel_indicators)
        carouselHelper.setupCarousel(viewPager, indicators)

        if (isAccountSetUpLayout) {
            binding.title.text = getString(com.windscribe.vpn.R.string.account_set_up)
            binding.forgotPassword.visibility = View.GONE
        }
        binding.loginSignUpContainer.requestFocus()
        binding.passwordEdit.transformationMethod = PasswordTransformationMethod()
        binding.showPassword.isChecked = false
        addFocusChangeListener()
        addClickListeners()
    }

    private fun addFocusChangeListener() {
        arrayOf(binding.usernameEdit, binding.passwordEdit).forEach {
            it.doAfterTextChanged {
                clearInputErrors()
            }
        }
        arrayOf(
            binding.back,
            binding.forgotPassword,
            binding.passwordContainer,
            binding.showPassword, binding.usernameContainer, binding.loginSignUp
        ).forEach {
            it.setOnFocusChangeListener { _, _ ->
                resetButtonTextColor()
            }
        }
    }

    private fun addClickListeners() {
        binding.back.setOnClickListener {
            fragmentCallBack?.onBackButtonPressed()
        }
        binding.forgotPassword.setOnClickListener {
            fragmentCallBack?.onForgotPasswordClick()
        }
        binding.loginSignUp.setOnClickListener {
            if (isAccountSetUpLayout) {
                fragmentCallBack?.onAccountClaimButtonClick(
                    binding.usernameEdit.text.toString(),
                    binding.passwordEdit.text.toString(), "", true
                )
            } else {
//                fragmentCallBack?.onAuthSignUpClick(
//                    binding.usernameEdit.text.toString(),
//                    binding.passwordEdit.text.toString(), ""
//                )
                fragmentCallBack?.onAuthSignUpClick(
                    binding.usernameEdit.text.toString(),
                    binding.passwordEdit.text.toString(), ""
                )
            }
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

    override fun setSecretCode(code: String) {}
    override fun setUsernameError(error: String) {
        binding.error.visibility = View.VISIBLE
        binding.error.text = error
    }

    override fun onResume() {
        super.onResume()
        carouselHelper.onResume()
    }

    override fun onPause() {
        super.onPause()
        carouselHelper.onPause()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        carouselHelper.onDestroy()
    }

    private fun resetButtonTextColor() {
        val normalColor = requireActivity().resources.getColor(R.color.colorWhite50)
        val focusColor = requireActivity().resources.getColor(R.color.colorWhite)
        binding.loginSignUp.setTextColor(
            if (binding.loginSignUp.hasFocus()) requireActivity().resources.getColor(R.color.colorWhite) else requireActivity().resources.getColor(
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
