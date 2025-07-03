/*
 * Copyright (c) 2021 Windscribe Limited.
 */
package com.windscribe.tv.moredata

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import com.windscribe.tv.R
import com.windscribe.tv.databinding.ActivityGetMoreDataBinding
import com.windscribe.tv.upgrade.UpgradeActivity
import com.windscribe.tv.welcome.WelcomeActivity

class GetMoreDataActivity : AppCompatActivity() {

    private lateinit var binding: ActivityGetMoreDataBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_get_more_data)
        setupUI()
    }

    private fun setupUI() {
        binding.getMoreData.requestFocus()
        binding.getMoreData.setOnClickListener {
            val startIntent = WelcomeActivity.getStartIntent(this)
            startIntent.putExtra("startFragmentName", "AccountSetUp")
            startActivity(startIntent)
        }
        binding.getPro.setOnClickListener {
            startActivity(UpgradeActivity.getStartIntent(this))
        }
        binding.getMoreData.setOnFocusChangeListener { _, _ ->
            if (binding.getMoreData.hasFocus()) {
                binding.getMoreData.setTextColor(resources.getColor(R.color.colorWhite))
            } else {
                binding.getMoreData.setTextColor(resources.getColor(R.color.colorWhite50))
            }
        }
        binding.getPro.setOnFocusChangeListener { _, _ ->
            if (binding.getPro.hasFocus()) {
                binding.getPro.setTextColor(resources.getColor(R.color.colorWhite))
            } else {
                binding.getPro.setTextColor(resources.getColor(R.color.colorWhite50))
            }
        }

    }

    companion object {
        @JvmStatic
        fun getStartIntent(context: Context?): Intent {
            return Intent(context, GetMoreDataActivity::class.java)
        }
    }
}
