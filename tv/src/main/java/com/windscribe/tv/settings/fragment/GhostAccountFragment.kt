/*
 * Copyright (c) 2021 Windscribe Limited.
 */
package com.windscribe.tv.settings.fragment

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.fragment.app.Fragment
import butterknife.BindView
import butterknife.ButterKnife
import butterknife.OnClick
import butterknife.OnFocusChange
import com.windscribe.tv.R
import com.windscribe.tv.listeners.SettingsFragmentListener
import com.windscribe.tv.settings.SettingActivity
import java.lang.ClassCastException

class GhostAccountFragment : Fragment {
    @JvmField
    @BindView(R.id.claimAccount)
    var claimAccountButton: Button? = null

    @JvmField
    @BindView(R.id.label)
    var labelView: TextView? = null

    @JvmField
    @BindView(R.id.login)
    var loginButton: Button? = null

    @JvmField
    @BindView(R.id.sign_up)
    var signUpButton: Button? = null
    private var listener: SettingsFragmentListener? = null
    private var proUser = false

    constructor(proUser: Boolean) {
        this.proUser = proUser
    }

    constructor()

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
        val view = inflater.inflate(R.layout.fragment_ghost_account, container, false)
        ButterKnife.bind(this, view)
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        listener?.onFragmentReady(this)
        if (proUser) {
            labelView?.text = getString(R.string.ghost_account_claim)
            claimAccountButton?.visibility = View.VISIBLE
            loginButton?.visibility = View.GONE
            signUpButton?.visibility = View.GONE
            claimAccountButton?.requestFocus()
        } else {
            labelView?.text = getString(R.string.ghost_account_sign_up)
            claimAccountButton?.visibility = View.GONE
            loginButton?.visibility = View.VISIBLE
            signUpButton?.visibility = View.VISIBLE
            signUpButton?.requestFocus()
        }
    }

    @OnClick(R.id.claimAccount)
    fun onClaimAccountClick() {
        listener?.onSignUpClick()
    }

    @OnFocusChange(R.id.claimAccount)
    fun onFocusChangeToClaimAccount() {
        if (claimAccountButton?.hasFocus() == true) {
            claimAccountButton?.setTextColor(resources.getColor(R.color.colorWhite))
        } else {
            claimAccountButton?.setTextColor(resources.getColor(R.color.colorWhite50))
        }
    }

    @OnFocusChange(R.id.login)
    fun onFocusChangeToLogin() {
        if (loginButton?.hasFocus() == true) {
            loginButton?.setTextColor(resources.getColor(R.color.colorWhite))
        } else {
            loginButton?.setTextColor(resources.getColor(R.color.colorWhite50))
        }
    }

    @OnFocusChange(R.id.sign_up)
    fun onFocusChangeToSignUp() {
        if (signUpButton?.hasFocus() == true) {
            signUpButton?.setTextColor(resources.getColor(R.color.colorWhite))
        } else {
            signUpButton?.setTextColor(resources.getColor(R.color.colorWhite50))
        }
    }

    @OnClick(R.id.login)
    fun onLoginClick() {
        listener?.onLoginClick()
    }

    @OnClick(R.id.sign_up)
    fun onSignUpClick() {
        listener?.onSignUpClick()
    }
}
