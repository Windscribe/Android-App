/*
 * Copyright (c) 2021 Windscribe Limited.
 */
package com.windscribe.tv.confirmemail

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import com.windscribe.tv.R
import com.windscribe.tv.base.BaseActivity
import com.windscribe.tv.customview.ProgressFragment.Companion.instance
import com.windscribe.tv.databinding.ActivityConfirmBinding
import com.windscribe.tv.di.ActivityModule
import com.windscribe.tv.email.AddEmailActivity
import com.windscribe.vpn.Windscribe.Companion.appContext
import com.windscribe.vpn.apppreference.PreferencesKeyConstants
import javax.inject.Inject

class ConfirmActivity : BaseActivity(), ConfirmEmailView {

    private lateinit var binding: ActivityConfirmBinding

    @Inject
    lateinit var presenter: ConfirmEmailPresenter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setActivityModule(ActivityModule(this, this)).inject(this)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_confirm)
        onActivityLaunch()
        setupUI()
        presenter.init()
    }

    override fun onDestroy() {
        presenter.onDestroy()
        super.onDestroy()
    }

    private fun setupUI() {
        listOf(binding.resendEmail, binding.changeEmail, binding.back).forEach { button ->
            button.setOnFocusChangeListener { _, _ -> updateButtonFocusState() }
        }

        binding.apply {
            changeEmail.setOnClickListener { navigateToChangeEmail() }
            resendEmail.setOnClickListener { presenter.resendVerificationEmail() }
            back.setOnClickListener { finishActivity() }
        }
    }

    private fun updateButtonFocusState() {
        val focusedColor = ContextCompat.getColor(this, R.color.colorWhite)
        val unfocusedColor = ContextCompat.getColor(this, R.color.colorWhite50)

        listOf(binding.resendEmail, binding.changeEmail, binding.back).forEach { button ->
            button.setTextColor(if (button.hasFocus()) focusedColor else unfocusedColor)
        }
    }

    override fun finishActivity() {
        appContext.workManager.updateSession()
        finish()
    }

    private fun navigateToChangeEmail() {
        val startIntent = AddEmailActivity.getStartIntent(this).apply {
            putExtra("pro_user", presenter.isUserPro)
            action = PreferencesKeyConstants.ACTION_RESEND_EMAIL_FROM_ACCOUNT
        }
        startActivity(startIntent)
        finish()
    }

    override fun setReasonToConfirmEmail(reasonForConfirmEmail: String) {
        binding.title.text = reasonForConfirmEmail
    }

    override fun showEmailConfirmProgress(show: Boolean) {
        if (show) {
            instance.add(this, R.id.cl_confirm_email, true)
        } else {
            supportFragmentManager.popBackStack()
        }
    }

    override fun showToast(toast: String) {
        runOnUiThread { Toast.makeText(this, toast, Toast.LENGTH_SHORT).show() }
    }

    companion object {
        @JvmStatic
        fun getStartIntent(context: Context) = Intent(context, ConfirmActivity::class.java)
    }
}