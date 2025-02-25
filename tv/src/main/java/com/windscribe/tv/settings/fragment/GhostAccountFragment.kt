/*
 * Copyright (c) 2021 Windscribe Limited.
 */
package com.windscribe.tv.settings.fragment

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.windscribe.tv.R
import com.windscribe.tv.databinding.FragmentGhostAccountBinding
import com.windscribe.tv.listeners.SettingsFragmentListener
import com.windscribe.tv.settings.SettingActivity
import java.lang.ClassCastException

class GhostAccountFragment : Fragment {
    private lateinit var binding: FragmentGhostAccountBinding
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
    ): View {
        binding = FragmentGhostAccountBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        listener?.onFragmentReady(this)
        if (proUser) {
            binding.label.text = getString(R.string.ghost_account_claim)
            binding.claimAccount.visibility = View.VISIBLE
            binding.login.visibility = View.GONE
            binding.signUp.visibility = View.GONE
            binding.claimAccount.requestFocus()
        } else {
            binding.label.text = getString(R.string.ghost_account_sign_up)
            binding.claimAccount.visibility = View.GONE
            binding.login.visibility = View.VISIBLE
            binding.signUp.visibility = View.VISIBLE
            binding.signUp.requestFocus()
        }
        addFocusListener()
        addClickListener()
    }

    private fun addFocusListener() {
        val focusChangeListener = View.OnFocusChangeListener { view, hasFocus ->
            val color = if (hasFocus) R.color.colorWhite else R.color.colorWhite50
            (view as? TextView)?.setTextColor(resources.getColor(color))
        }
        with(binding) {
            claimAccount.onFocusChangeListener = focusChangeListener
            login.onFocusChangeListener = focusChangeListener
            signUp.onFocusChangeListener = focusChangeListener
        }
    }

    private fun addClickListener() {
        binding.claimAccount.setOnClickListener {
            listener?.onSignUpClick()
        }
        binding.login.setOnClickListener {
            listener?.onLoginClick()
        }
        binding.signUp.setOnClickListener {
            listener?.onSignUpClick()
        }
    }
}
