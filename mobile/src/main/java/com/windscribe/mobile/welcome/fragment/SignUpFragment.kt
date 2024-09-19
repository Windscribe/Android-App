/*
 * Copyright (c) 2021 Windscribe Limited.
 */
package com.windscribe.mobile.welcome.fragment

import android.content.Context
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.text.method.PasswordTransformationMethod
import android.util.Patterns
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.widget.AppCompatCheckBox
import androidx.appcompat.widget.AppCompatEditText
import androidx.fragment.app.Fragment
import butterknife.BindView
import butterknife.ButterKnife
import butterknife.OnCheckedChanged
import butterknife.OnClick
import com.windscribe.mobile.R
import java.util.concurrent.atomic.AtomicBoolean

class SignUpFragment : Fragment(), TextWatcher,
    WelcomeActivityCallback {
    @BindView(R.id.email_sub_description)
    lateinit var addEmailLabel: TextView

    @BindView(R.id.nav_button)
    lateinit var backButton: ImageButton

    @BindView(R.id.username_error_description)
    lateinit var usernameErrorDescription: TextView

    @BindView(R.id.email_description)
    lateinit var emailDescriptionView: TextView

    @BindView(R.id.password_error_description)
    lateinit var passwordErrorDescription: TextView

    @BindView(R.id.password_description)
    lateinit var passwordDescription: TextView

    @BindView(R.id.email)
    lateinit var emailEditText: EditText

    @BindView(R.id.email_error)
    lateinit var emailErrorView: ImageView

    @BindView(R.id.password)
    lateinit var passwordEditText: EditText

    @BindView(R.id.password_error)
    lateinit var passwordErrorView: ImageView

    @BindView(R.id.password_visibility_toggle)
    lateinit var passwordVisibilityToggle: AppCompatCheckBox

    @BindView(R.id.set_up_later_button)
    lateinit var setUpButton: Button

    @BindView(R.id.loginButton)
    lateinit var signUpButton: Button

    @BindView(R.id.nav_title)
    lateinit var titleView: TextView

    @BindView(R.id.username)
    lateinit var usernameEditText: EditText

    @BindView(R.id.username_error)
    lateinit var usernameErrorView: ImageView

    @BindView(R.id.first_referral_description_prefix)
    lateinit var firstReferralDescriptionPrefix: ImageView

    @BindView(R.id.first_referral_description)
    lateinit var firstReferralDescription: TextView

    @BindView(R.id.second_referral_description_prefix)
    lateinit var secondReferralDescriptionPrefix: ImageView

    @BindView(R.id.second_referral_description)
    lateinit var secondReferralDescription: TextView

    @BindView(R.id.referral_collapse_icon)
    lateinit var referralCollapseIcon: ImageView

    @BindView(R.id.referral_username)
    lateinit var referralUsernameEditText: AppCompatEditText

    @BindView(R.id.voucher_collapse_icon)
    lateinit var voucherCollapseIcon: ImageView

    @BindView(R.id.voucher_hint)
    lateinit var voucherHint: TextView

    @BindView(R.id.confirm_email_explainer)
    lateinit var confirmEmailExplainer: TextView

    @BindView(R.id.scrollable_container)
    lateinit var scrollableContainer: ScrollView

    @BindView(R.id.referral_title)
    lateinit var referralTitle: TextView

    @BindView(R.id.bottom_focus)
    lateinit var bottomFocusView: ImageView

    @BindView(R.id.voucher)
    lateinit var voucher: AppCompatEditText

    private var isUserPro = false
    private var isAccountSetUpLayout = false
    private var fragmentCallBack: FragmentCallback? = null
    private var skipToHome = false
    private var showReferralViews = false
    private var showVoucherViews = false
    private val ignoreEditTextChange = AtomicBoolean(false)

    override fun onAttach(context: Context) {
        if (activity is FragmentCallback) {
            fragmentCallBack = activity as FragmentCallback
        }
        super.onAttach(context)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (arguments != null) {
            isAccountSetUpLayout =
                requireArguments().getString("startFragmentName", "SignUp") == "AccountSetUp"
            skipToHome = requireArguments().getBoolean("skipToHome", false)
            isUserPro = requireArguments().getBoolean("userPro", false)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_sign_up, container, false)
        ButterKnife.bind(this, view)
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        if (isAccountSetUpLayout) {
            titleView.text = getString(R.string.account_set_up)
            setUpButton.visibility = View.VISIBLE
            if (isUserPro) {
                addEmailLabel.visibility = View.GONE
            }
            if (skipToHome) {
                backButton.visibility = View.INVISIBLE
            }
            hideReferralView()
        } else {
            titleView.text = getString(R.string.sign_up)
            setUpButton.visibility = View.GONE
        }
        addEditTextChangeListener()
    }

    override fun afterTextChanged(s: Editable) {
        if (!ignoreEditTextChange.getAndSet(false)) {
            resetNextButtonView()
        }
    }

    override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {
        if (!ignoreEditTextChange.getAndSet(false)) {
            clearInputErrors()
        }
        resetReferralUsernameView()
    }

    private fun resetReferralUsernameView() {
        if (Patterns.EMAIL_ADDRESS.matcher(emailEditText.text)
                .matches() && referralUsernameEditText.text.toString() != getString(R.string.please_provide_email_first)
        ) {
            referralUsernameEditText.setTextColor(resources.getColor(R.color.colorWhite50))
            referralUsernameEditText.isEnabled = true
        } else if (Patterns.EMAIL_ADDRESS.matcher(emailEditText.text)
                .matches() && referralUsernameEditText.text.toString() == getString(R.string.please_provide_email_first)
        ) {
            referralUsernameEditText.setTextColor(resources.getColor(R.color.colorWhite50))
            referralUsernameEditText.setText("")
            referralUsernameEditText.isEnabled = true
        } else {
            referralUsernameEditText.setTextColor(resources.getColor(R.color.colorRed))
            referralUsernameEditText.setText(getString(R.string.please_provide_email_first))
            referralUsernameEditText.isEnabled = false
        }
    }

    override fun clearInputErrors() {
        emailDescriptionView.setTextColor(resources.getColor(R.color.colorWhite50))
        emailDescriptionView.text = getString(R.string.email_description)
        emailErrorView.visibility = View.INVISIBLE
        usernameErrorView.visibility = View.INVISIBLE
        passwordErrorView.visibility = View.INVISIBLE
        usernameEditText.setTextColor(resources.getColor(R.color.colorWhite))
        passwordEditText.setTextColor(resources.getColor(R.color.colorWhite))
        referralTitle.setTextColor(resources.getColor(R.color.colorWhite50))
        usernameErrorDescription.visibility = View.GONE
        usernameErrorDescription.text = ""
        passwordErrorDescription.visibility = View.GONE
        passwordErrorDescription.text = ""
        passwordDescription.visibility = View.VISIBLE
    }

    @OnClick(R.id.nav_button, R.id.set_up_later_button)
    fun onNavButtonClick() {
        if (skipToHome) {
            fragmentCallBack?.onSkipToHomeClick()
        } else {
            requireActivity().onBackPressed()
        }
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

    @OnClick(R.id.loginButton)
    fun onSignUpButtonClick() {
        if (isAccountSetUpLayout) {
            fragmentCallBack?.onAccountClaimButtonClick(
                usernameEditText.text.toString().trim { it <= ' ' },
                passwordEditText.text.toString().trim { it <= ' ' },
                emailEditText.text.toString().trim { it <= ' ' },
                false
            )
        } else {
            val referral = referralUsernameEditText.text.toString().trim { it <= ' ' }
            val email = emailEditText.text.toString().trim { it <= ' ' }
            fragmentCallBack?.onSignUpButtonClick(
                usernameEditText.text.toString().trim { it <= ' ' },
                passwordEditText.text.toString().trim { it <= ' ' },
                email,
                referral,
                false,
                voucher.text.toString().trim {it <= ' '},
            )
        }
    }

    @OnClick(R.id.referral_collapse_icon)
    fun onReferralCollapseIconClick() {
        toggleReferralViewVisibility()
    }

    @OnClick(R.id.voucher_collapse_icon)
    fun onVoucherCollapseIconClick() {
        toggleVoucherViewVisibility()
    }

    private fun toggleVoucherViewVisibility() {
        voucherCollapseIcon.rotation = if (showVoucherViews.not()) {
            0F
        } else {
            180F
        }
        voucher.visibility =
            if (!showVoucherViews) View.VISIBLE else View.GONE
        voucherHint.visibility =
            if (!showVoucherViews) View.VISIBLE else View.GONE
        showVoucherViews = showVoucherViews.not()
        if (showVoucherViews) {
            bottomFocusView.requestFocus()
        } else {
            usernameEditText.requestFocus()
        }
    }

    private fun toggleReferralViewVisibility() {
        referralCollapseIcon.rotation = if (showReferralViews.not()) {
            0F
        } else {
            180F
        }
        firstReferralDescriptionPrefix.visibility =
            if (!showReferralViews) View.VISIBLE else View.GONE
        firstReferralDescription.visibility =
            if (!showReferralViews) View.VISIBLE else View.GONE
        secondReferralDescriptionPrefix.visibility =
            if (!showReferralViews) View.VISIBLE else View.GONE
        secondReferralDescription.visibility =
            if (!showReferralViews) View.VISIBLE else View.GONE
        referralUsernameEditText.visibility =
            if (!showReferralViews) View.VISIBLE else View.GONE
        confirmEmailExplainer.visibility =
            if (!showReferralViews) View.VISIBLE else View.GONE
        showReferralViews = showReferralViews.not()
        if (showReferralViews) {
            bottomFocusView.requestFocus()
        } else {
            usernameEditText.requestFocus()
        }
    }

    private fun hideReferralView() {
        referralTitle.visibility = View.GONE
        referralCollapseIcon.visibility = View.GONE
    }

    override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {}
    fun setEmailError(errorMessage: String?) {
        emailDescriptionView.setTextColor(resources.getColor(R.color.colorRed))
        emailDescriptionView.text = errorMessage
        emailErrorView.visibility = View.VISIBLE
    }

    override fun setLoginError(error: String) {
        emailDescriptionView.setTextColor(resources.getColor(R.color.colorRed))
        emailDescriptionView.text = error
        usernameEditText.error = error
        usernameErrorView.visibility = View.VISIBLE
        passwordErrorView.visibility = View.VISIBLE
        emailErrorView.visibility = View.VISIBLE
        usernameEditText.setTextColor(resources.getColor(R.color.colorRed))
        passwordEditText.setTextColor(resources.getColor(R.color.colorRed))
    }

    override fun setPasswordError(error: String) {
        passwordErrorDescription.visibility = View.VISIBLE
        passwordErrorDescription.setTextColor(resources.getColor(R.color.colorRed))
        passwordErrorDescription.text = error
        passwordErrorView.visibility = View.VISIBLE
        passwordEditText.setTextColor(resources.getColor(R.color.colorRed))
        passwordDescription.visibility = View.GONE
    }

    override fun setUsernameError(error: String) {
        usernameErrorDescription.visibility = View.VISIBLE
        usernameErrorDescription.setTextColor(resources.getColor(R.color.colorRed))
        usernameErrorDescription.text = error
        usernameErrorView.visibility = View.VISIBLE
        usernameEditText.setTextColor(resources.getColor(R.color.colorRed))
    }

    private fun addEditTextChangeListener() {
        usernameEditText.addTextChangedListener(this)
        passwordEditText.addTextChangedListener(this)
        emailEditText.addTextChangedListener(this)
    }

    override fun onDestroyView() {
        usernameEditText.removeTextChangedListener(this)
        passwordEditText.removeTextChangedListener(this)
        emailEditText.removeTextChangedListener(this)
        super.onDestroyView()
    }

    private fun resetNextButtonView() {

    }

    companion object {
        fun newInstance(userPro: Boolean): SignUpFragment {
            val args = Bundle()
            args.putBoolean("userPro", userPro)
            val fragment = SignUpFragment()
            fragment.arguments = args
            return fragment
        }
    }
}