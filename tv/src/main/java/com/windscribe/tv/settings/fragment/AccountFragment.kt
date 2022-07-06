/*
 * Copyright (c) 2021 Windscribe Limited.
 */
package com.windscribe.tv.settings.fragment

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.fragment.app.Fragment
import butterknife.BindView
import butterknife.ButterKnife
import butterknife.OnClick
import com.windscribe.tv.R
import com.windscribe.tv.listeners.SettingsFragmentListener
import com.windscribe.tv.settings.SettingActivity
import java.lang.ClassCastException

class AccountFragment : Fragment() {
    enum class Status {
        NOT_ADDED, NOT_CONFIRMED, NOT_ADDED_PRO, CONFIRMED
    }

    @JvmField
    @BindView(R.id.confirmContainer)
    var confirmContainer: ConstraintLayout? = null

    @JvmField
    @BindView(R.id.emailContainer)
    var emailContainer: ConstraintLayout? = null

    @JvmField
    @BindView(R.id.email_underline_mask)
    var emailTextView: TextView? = null

    @JvmField
    @BindView(R.id.expiryLabel)
    var expiryLabel: TextView? = null

    @JvmField
    @BindView(R.id.plan)
    var planCase: TextView? = null

    @JvmField
    @BindView(R.id.planLabel)
    var planTextView: TextView? = null

    @JvmField
    @BindView(R.id.planContainer)
    var playContainer: ConstraintLayout? = null

    @JvmField
    @BindView(R.id.proIcon)
    var proIcon: ImageView? = null

    @JvmField
    @BindView(R.id.expiry)
    var resetTextView: TextView? = null

    @JvmField
    @BindView(R.id.username)
    var userNameTextView: TextView? = null
    private var listener: SettingsFragmentListener? = null
    override fun onAttach(context: Context) {
        super.onAttach(context)
        val activity: SettingActivity
        if (context is SettingActivity) {
            activity = context
            try {
                listener = activity
            } catch (e: ClassCastException) {
                throw ClassCastException("$activity must implement OnCompleteListener")
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_account, container, false)
        ButterKnife.bind(this, view)
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        listener?.onFragmentReady(this)
    }

    fun setEmail(email: String?) {
        emailTextView?.text = email
    }

    fun setEmailState(status: Status?, email: String?) {
        when (status) {
            Status.CONFIRMED -> {
                emailTextView?.text = email
                confirmContainer?.visibility = View.GONE
                emailContainer?.isFocusable = false
            }
            Status.NOT_CONFIRMED -> {
                emailTextView?.text = email
                confirmContainer?.visibility = View.VISIBLE
                emailContainer?.isFocusable = false
            }
            Status.NOT_ADDED_PRO, Status.NOT_ADDED -> {
                emailTextView?.setText(R.string.add_email_pro)
                confirmContainer?.visibility = View.GONE
                emailContainer?.isFocusable = true
            }
        }
    }

    fun setPlanName(planName: String?) {
        planTextView?.text = planName
    }

    fun setResetDate(resetDateLabel: String?, resetDate: String?) {
        resetTextView?.text = resetDate
        expiryLabel?.text = resetDateLabel
    }

    fun setUsername(username: String?) {
        userNameTextView?.text = username
    }

    fun setupLayoutForFreeUser(upgradeText: String?) {
        planCase?.text = upgradeText
        proIcon?.visibility = View.GONE
        playContainer?.isFocusable = true
    }

    fun setupLayoutForPremiumUser(upgradeText: String?) {
        planCase?.text = upgradeText
        proIcon?.visibility = View.VISIBLE
        playContainer?.isFocusable = false
    }

    @OnClick(R.id.confirmContainer)
    fun onConfirmClick() {
        listener?.onEmailResend()
    }

    @OnClick(R.id.emailContainer)
    fun onEmailClick() {
        listener?.onEmailClick()
    }

    @OnClick(R.id.planContainer)
    fun onPlanClick() {
        val planText = planCase?.text.toString()
        listener?.onUpgradeClick(planText)
    }
}
