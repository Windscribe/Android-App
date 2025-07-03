/*
 * Copyright (c) 2021 Windscribe Limited.
 */
package com.windscribe.tv.email

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.databinding.DataBindingUtil
import com.windscribe.tv.R
import com.windscribe.tv.base.BaseActivity
import com.windscribe.tv.confirmemail.ConfirmActivity
import com.windscribe.tv.customview.ProgressFragment.Companion.instance
import com.windscribe.tv.databinding.ActivityAddEmailAddressBinding
import com.windscribe.tv.di.ActivityModule
import com.windscribe.tv.windscribe.WindscribeActivity
import com.windscribe.vpn.constants.PreferencesKeyConstants
import org.slf4j.LoggerFactory
import javax.inject.Inject

class AddEmailActivity : BaseActivity(), AddEmailView {

    @Inject
    lateinit var presenter: AddEmailPresenter

    private lateinit var binding: ActivityAddEmailAddressBinding

    private val logger = LoggerFactory.getLogger("basic")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setActivityModule(ActivityModule(this, this)).inject(this)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_add_email_address)
        onActivityLaunch()
        setupUI()
    }

    private fun setupUI() {
        binding.emailContainer.requestFocus()
        setTextFieldsFromStartingPoint()
        binding.back.setOnClickListener {
            logger.info("User clicked to skip adding email address...")
            presenter.onSkipEmailClicked()
        }
        binding.addEmail.setOnClickListener {
            onAddEmailClick()
        }
        binding.emailContainer.setOnClickListener {
            binding.emailEdit.visibility = View.VISIBLE
            binding.emailEdit.requestFocus()
        }
        binding.back.setOnFocusChangeListener { _, _ ->
            resetButtonTextColor()
        }
    }

    override fun onDestroy() {
        presenter.onDestroy()
        super.onDestroy()
    }

    override fun decideActivity() {
        if (intent != null && intent.action != null) {
            when (intent.action) {
                PreferencesKeyConstants.ACTION_ADD_EMAIL_FROM_ACCOUNT -> {
                    goToConfirmEmailActivity()
                }

                PreferencesKeyConstants.ACTION_RESEND_EMAIL_FROM_ACCOUNT -> {
                    gotoAccountActivity()
                }

                else -> {
                    gotoWindscribeActivity()
                }
            }
        }
    }

    override fun decideActivityForSkipButton() {
        if (intent != null && intent.action != null) {
            when (intent.action) {
                PreferencesKeyConstants.ACTION_ADD_EMAIL_FROM_ACCOUNT -> {
                    finish()
                }

                PreferencesKeyConstants.ACTION_RESEND_EMAIL_FROM_ACCOUNT -> {
                    finish()
                }

                else -> {
                    gotoWindscribeActivity()
                }
            }
        }
    }

    private fun gotoAccountActivity() {
        overridePendingTransition(R.anim.fade_in, R.anim.fade_out)
        finish()
    }

    private fun gotoWindscribeActivity() {
        startActivity(WindscribeActivity.getStartIntent(this))
        finish()
    }

    override fun hideSoftKeyboard() {
        val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(
            window
                .decorView.windowToken,
            InputMethodManager.HIDE_NOT_ALWAYS
        )
    }

    private fun onAddEmailClick() {
        val email = binding.emailEdit.text
        if (intent != null && intent.action != null && (intent.action == PreferencesKeyConstants.ACTION_RESEND_EMAIL_FROM_ACCOUNT)
        ) {
            email?.let {
                presenter.onResendEmail(it.toString())
            }
        } else {
            presenter.onAddEmailClicked(email.toString())
        }
    }

    override fun prepareUiForApiCallFinished() {
        logger.info("Preparing ui for api call finished...")
        supportFragmentManager.popBackStack()
    }

    override fun prepareUiForApiCallStart() {
        logger.info("Preparing ui for api call start...")
        instance.add(this, R.id.cl_add_email, true)
    }

    override fun showInputError(errorText: String) {
        Toast.makeText(this, errorText, Toast.LENGTH_SHORT).show()
    }

    override fun showToast(toastString: String) {
        Toast.makeText(this, toastString, Toast.LENGTH_SHORT).show()
    }

    private fun resetButtonTextColor() {
        binding.back.setTextColor(
            if (binding.back.hasFocus()) resources.getColor(R.color.colorWhite) else resources.getColor(
                R.color.colorWhite50
            )
        )
    }

    private fun goToConfirmEmailActivity() {
        startActivity(ConfirmActivity.getStartIntent(this))
        finish()
    }

    private fun setTextFieldsFromStartingPoint() {
        val action = intent.action ?: return
        val proUser = intent.getBooleanExtra("pro_user", false)
        binding.title.setText(if (proUser) com.windscribe.vpn.R.string.pro_reason_to_add_email else com.windscribe.vpn.R.string.free_reason_to_add_email)
        when (action) {
            PreferencesKeyConstants.ACTION_RESEND_EMAIL_FROM_ACCOUNT -> {
                binding.back.text = getString(com.windscribe.vpn.R.string.back_uppercase)
            }

            PreferencesKeyConstants.ACTION_ADD_EMAIL_FROM_ACCOUNT -> {
                binding.addEmail.text = getString(com.windscribe.vpn.R.string.add_email_pro)
                binding.back.text = getString(com.windscribe.vpn.R.string.back_uppercase)
            }

            else -> {
                binding.addEmail.text = getString(com.windscribe.vpn.R.string.add_email_pro)
                binding.back.text = getString(com.windscribe.vpn.R.string.skip)
            }
        }
    }

    companion object {
        const val Email_Tag = "email_tag"

        @JvmStatic
        fun getStartIntent(context: Context?): Intent {
            return Intent(context, AddEmailActivity::class.java)
        }
    }
}
