/*
 * Copyright (c) 2021 Windscribe Limited.
 */
package com.windscribe.tv.confirmemail

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import butterknife.BindView
import butterknife.OnClick
import butterknife.OnFocusChange
import com.windscribe.tv.R
import com.windscribe.tv.base.BaseActivity
import com.windscribe.tv.customview.ProgressFragment.Companion.instance
import com.windscribe.tv.di.ActivityModule
import com.windscribe.tv.email.AddEmailActivity
import com.windscribe.vpn.Windscribe.Companion.appContext
import com.windscribe.vpn.constants.PreferencesKeyConstants
import javax.inject.Inject

class ConfirmActivity : BaseActivity(), ConfirmEmailView {
    @JvmField
    @BindView(R.id.back)
    var back: TextView? = null

    @JvmField
    @BindView(R.id.change_email)
    var changeEmail: Button? = null

    @Inject
    lateinit var presenter: ConfirmEmailPresenter

    @JvmField
    @BindView(R.id.resend_email)
    var resendEmail: Button? = null

    @JvmField
    @BindView(R.id.title)
    var title: TextView? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setActivityModule(ActivityModule(this, this)).inject(this)
        setContentLayout(R.layout.activity_confirm)
        presenter.init()
    }

    override fun onDestroy() {
        presenter.onDestroy()
        super.onDestroy()
    }

    override fun finishActivity() {
        appContext.workManager.updateSession()
        finish()
    }

    @OnFocusChange(R.id.resend_email, R.id.change_email, R.id.back)
    fun onButtonFocusChange() {
        val focusedColor = resources.getColor(R.color.colorWhite)
        val unfocusedColor = resources.getColor(R.color.colorWhite50)
        resendEmail?.setTextColor(if (resendEmail?.hasFocus() == true) focusedColor else unfocusedColor)
        changeEmail?.setTextColor(if (changeEmail?.hasFocus() == true) focusedColor else unfocusedColor)
        back?.setTextColor(if (back?.hasFocus() == true) focusedColor else unfocusedColor)
    }

    @SuppressLint("NonConstantResourceId")
    @OnClick(R.id.change_email)
    fun onChangeEmailClicked() {
        val startIntent = AddEmailActivity.getStartIntent(this)
        startIntent.putExtra("pro_user", presenter.isUserPro)
        startIntent.action = PreferencesKeyConstants.ACTION_RESEND_EMAIL_FROM_ACCOUNT
        startActivity(startIntent)
        finish()
    }

    @SuppressLint("NonConstantResourceId")
    @OnClick(R.id.back)
    fun onCloseClicked() {
        finishActivity()
    }

    @SuppressLint("NonConstantResourceId")
    @OnClick(R.id.resend_email)
    fun onResendEmailClicked() {
        presenter.resendVerificationEmail()
    }

    override fun setReasonToConfirmEmail(reasonForConfirmEmail: String) {
        title?.text = reasonForConfirmEmail
    }

    override fun showEmailConfirmProgress(show: Boolean) {
        if (show) {
            instance.add(this, R.id.cl_confirm_email, true)
        } else {
            supportFragmentManager.popBackStack()
        }
    }

    override fun showToast(toast: String) {
        runOnUiThread { Toast.makeText(this@ConfirmActivity, toast, Toast.LENGTH_SHORT).show() }
    }

    companion object {
        @JvmStatic
        fun getStartIntent(context: Context?): Intent {
            return Intent(context, ConfirmActivity::class.java)
        }
    }
}
