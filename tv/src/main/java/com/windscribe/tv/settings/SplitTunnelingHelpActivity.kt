/*
 * Copyright (c) 2021 Windscribe Limited.
 */
package com.windscribe.tv.settings

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.windscribe.tv.R
import com.windscribe.tv.base.applyAppLocale

class SplitTunnelingHelpActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        applyAppLocale()
        setContentView(R.layout.activity_split_tunneling_help)
    }

    companion object {
        @JvmStatic
        fun getStartIntent(context: Context): Intent {
            return Intent(context, SplitTunnelingHelpActivity::class.java)
        }
    }
}
