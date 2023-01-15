/*
 * Copyright (c) 2021 Windscribe Limited.
 */
package com.windscribe.tv.welcome.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import butterknife.ButterKnife
import com.windscribe.tv.R

class NoEmailAttentionFragment : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_no_email_attention, container, false)
        ButterKnife.bind(this, view)
        return view
    }
}
