/*
 * Copyright (c) 2021 Windscribe Limited.
 */

package com.windscribe.mobile.upgradeactivity

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class UpgradeActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Toast.makeText(this, getString(com.windscribe.vpn.R.string.billing_unavailable), Toast.LENGTH_SHORT).show()
        finish()
    }

    companion object {
        @JvmStatic
        fun getStartIntent(context: Context): Intent = Intent(context, UpgradeActivity::class.java)
    }
}
