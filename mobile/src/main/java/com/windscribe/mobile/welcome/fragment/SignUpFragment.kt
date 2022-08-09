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
import android.widget.*
import androidx.appcompat.widget.AppCompatCheckBox
import androidx.appcompat.widget.AppCompatEditText
import androidx.fragment.app.Fragment
import butterknife.*
import com.windscribe.mobile.R
import java.util.concurrent.atomic.AtomicBoolean

class SignUpFragment(private var userPro: Boolean) : Fragment(), TextWatcher,
    WelcomeActivityCallback {
    @BindView(R.id.email_sub_description)
    lateinit var addEmailLabel: TextView

    @BindView(R.id.nav_button)
    lateinit var backButton: ImageButton

    @BindView(R.id.email_description)
    lateinit var emailDescriptionView: TextView

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
    lateinit var firstReferralDescriptionPrefix: TextView

    @BindView(R.id.first_referral_description)
    lateinit var firstReferralDescription: TextView

    @BindView(R.id.second_referral_description_prefix)
    lateinit var secondReferralDescriptionPrefix: TextView

    @BindView(R.id.second_referral_description)
    lateinit var secondReferralDescription: TextView

    @BindView(R.id.referral_collapse_icon)
    lateinit var referralCollapseIcon: ImageView

    @BindView(R.id.referral_username)
    lateinit var referralUsernameEditText: AppCompatEditText

    @BindView(R.id.scrollable_container)
    lateinit var scrollableContainer: ScrollView

    @BindView(R.id.referral_title)
    lateinit var referralTitle: TextView

    private var isAccountSetUpLayout = false
    private var fragmentCallBack: FragmentCallback? = null
    private var skipToHome = false
    private var showReferralViews = false
    private var referralUsernameText = ""
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
            if (userPro) {
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
    }

    override fun clearInputErrors() {
        emailDescriptionView.setTextColor(resources.getColor(R.color.colorWhite50))
        emailDescriptionView.text = getString(R.string.email_description)
        emailErrorView.visibility = View.INVISIBLE
        usernameErrorView.visibility = View.INVISIBLE
        passwordErrorView.visibility = View.INVISIBLE
        usernameEditText.setTextColor(resources.getColor(R.color.colorWhite))
        passwordEditText.setTextColor(resources.getColor(R.color.colorWhite))
        referralUsernameEditText.setTextColor(resources.getColor(R.color.colorWhite50))
        referralUsernameEditText.setText(referralUsernameText)
        referralTitle.setTextColor(resources.getColor(R.color.colorWhite50))
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
            var referral = referralUsernameEditText.text.toString().trim { it <= ' ' }
            val email = emailEditText.text.toString().trim { it <= ' ' }
            if (referral == "Please provide email first.") {
                referralUsernameEditText.setText("")
                referralUsernameText = ""
                referral = ""
            }
            if (referral.isNotEmpty() && email.isEmpty()) {
                setMissingEmailError()
                return
            }
            fragmentCallBack!!.onSignUpButtonClick(
                usernameEditText.text.toString().trim { it <= ' ' },
                passwordEditText.text.toString().trim { it <= ' ' }, email, referral, false
            )
        }
    }

    @OnClick(R.id.referral_collapse_icon)
    fun onReferralCollapseIconClick() {
        toggleReferralViewVisibility()
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
        showReferralViews = showReferralViews.not()
        if (showReferralViews) {
            referralUsernameEditText.requestFocus()
        } else {
            usernameEditText.requestFocus()
        }
    }

    private fun hideReferralView() {
        referralTitle.visibility = View.GONE
        referralCollapseIcon.visibility = View.GONE
    }

    @OnFocusChange(R.id.referral_username)
    fun focusChangedOnReferralUsername() {
        if (referralUsernameEditText.hasFocus()) {
            scrollableContainer.post { scrollableContainer.fullScroll(View.FOCUS_DOWN) }
        }
    }

    override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {}
    fun setEmailError(errorMessage: String?) {
        emailDescriptionView.setTextColor(resources.getColor(R.color.colorRed))
        emailDescriptionView.text = errorMessage
        emailErrorView.visibility = View.VISIBLE
    }

    private fun setMissingEmailError() {
        if (showReferralViews.not()) {
            toggleReferralViewVisibility()
        }
        referralUsernameEditText.setTextColor(resources.getColor(R.color.colorRed))
        referralTitle.setTextColor(resources.getColor(R.color.colorWhite))
        emailErrorView.visibility = View.VISIBLE
        val text = referralUsernameEditText.text.toString()
        if (text != getString(R.string.please_provide_email_first)) {
            referralUsernameText = text
        }
        referralUsernameEditText.setText(getString(R.string.please_provide_email_first))
        emailEditText.post { emailEditText.requestFocus() }
    }

    override fun setLoginError(error: String) {
        emailDescriptionView.setTextColor(resources.getColor(R.color.colorRed))
        emailDescriptionView.text = error
        usernameErrorView.visibility = View.VISIBLE
        passwordErrorView.visibility = View.VISIBLE
        emailErrorView.visibility = View.VISIBLE
        usernameEditText.setTextColor(resources.getColor(R.color.colorRed))
        passwordEditText.setTextColor(resources.getColor(R.color.colorRed))
    }

    override fun setPasswordError(error: String) {
        emailDescriptionView.setTextColor(resources.getColor(R.color.colorRed))
        emailDescriptionView.text = error
        passwordErrorView.visibility = View.VISIBLE
        passwordEditText.setTextColor(resources.getColor(R.color.colorRed))
    }

    override fun setUsernameError(error: String) {
        emailDescriptionView.setTextColor(resources.getColor(R.color.colorRed))
        emailDescriptionView.text = error
        usernameErrorView.visibility = View.VISIBLE
        usernameEditText.setTextColor(resources.getColor(R.color.colorRed))
    }

    private fun addEditTextChangeListener() {
        usernameEditText.addTextChangedListener(this)
        passwordEditText.addTextChangedListener(this)
        emailEditText.addTextChangedListener(this)
    }

    private fun resetNextButtonView() {
        val enable = usernameEditText.text.length > 2 && passwordEditText.text.length > 7
        signUpButton.isEnabled = enable
    }
}