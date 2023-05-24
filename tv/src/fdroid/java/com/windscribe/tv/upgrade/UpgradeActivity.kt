/*
 * Copyright (c) 2021 Windscribe Limited.
 */
package com.windscribe.tv.upgrade

import android.content.Context
import android.content.Intent
import android.os.Bundle
import com.windscribe.tv.R
import com.windscribe.tv.base.BaseActivity

class UpgradeActivity : BaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentLayout(R.layout.activity_upgrade)
    }

    companion object {
        @JvmStatic
        fun getStartIntent(context: Context): Intent {
            return Intent(context, UpgradeActivity::class.java)
        }
    }
}
