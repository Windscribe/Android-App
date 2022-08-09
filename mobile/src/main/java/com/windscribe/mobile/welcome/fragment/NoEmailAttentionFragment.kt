/*
 * Copyright (c) 2021 Windscribe Limited.
 */
package com.windscribe.mobile.welcome.fragment

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import butterknife.ButterKnife
import butterknife.OnClick
import com.windscribe.mobile.R

class NoEmailAttentionFragment : Fragment {
    private var accountClaim = false
    private var isPro = false
    private var fragmentCallBack: FragmentCallback? = null
    private var password = ""
    private var username = ""

    constructor(accountClaim: Boolean, username: String, password: String, isPro: Boolean) {
        this.accountClaim = accountClaim
        this.username = username
        this.password = password
        this.isPro = isPro
    }

    constructor()

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
        val view = inflater.inflate(R.layout.fragment_no_email_attention, container, false)
        ButterKnife.bind(this, view)
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val warningText = view.findViewById<TextView>(R.id.warningText)
        if (isPro) {
            warningText.setText(R.string.warning_no_email_pro_account)
        }
    }

    @OnClick(R.id.backButton)
    fun onBackButtonClicked() {
        fragmentCallBack?.onBackButtonPressed()
    }

    @OnClick(R.id.continue_without_email)
    fun onContinueWithoutEmailButtonClicked() {
        if (accountClaim) {
            fragmentCallBack?.onAccountClaimButtonClick(username, password, "", true)
        } else {
            fragmentCallBack?.onSignUpButtonClick(username, password, "", "", true)
        }
    }
}