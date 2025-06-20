package com.windscribe.mobile.upgradeactivity

import android.os.Bundle
import androidx.databinding.DataBindingUtil
import com.windscribe.mobile.R
import com.windscribe.mobile.databinding.ActivityUpgradeSuccessBinding
import com.windscribe.mobile.upgradeactivity.UpgradeActivity.getStartIntent
import com.windscribe.vpn.constants.NetworkKeyConstants

class UpgradeSuccessActivity: BaseActivity() {

    private lateinit var binding: ActivityUpgradeSuccessBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_upgrade_success);
        setContentLayout(false)
        addClickListeners()
    }

    private fun addClickListeners() {
        binding.closeBtn.setOnClickListener {
            finish()
        }
        binding.startUsingPro.setOnClickListener {
            if (intent.getBooleanExtra("isGhostAccount", false)) {
                val startIntent = getStartIntent(this)
                startIntent.putExtra("startFragmentName", "AccountSetUp")
                startActivity(startIntent)
                finish()
            } else {
                finish()
            }
        }
        binding.discord.onClick {
            openURLInBrowser(NetworkKeyConstants.URL_DISCORD)
        }
        binding.reddit.onClick {
            openURLInBrowser(NetworkKeyConstants.URL_REDDIT)
        }
        binding.youtube.onClick {
            openURLInBrowser(NetworkKeyConstants.URL_YOUTUBE)
        }
        binding.x.onClick {
            openURLInBrowser(NetworkKeyConstants.URL_X)
        }
    }
}