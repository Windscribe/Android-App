/*
 * Copyright (c) 2021 Windscribe Limited.
 */
package com.windscribe.tv.rate

import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import com.windscribe.tv.base.BaseActivity
import com.windscribe.tv.base.applyAppLocale
import com.windscribe.tv.databinding.ActivityRateMyAppBinding
import com.windscribe.vpn.constants.RateDialogConstants
import com.windscribe.vpn.constants.RateDialogConstants.PLAY_STORE_URL
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class RateMyAppActivity :
    BaseActivity(),
    RateView {
    private lateinit var binding: ActivityRateMyAppBinding

    @Inject
    lateinit var presenter: RateMyAppPresenter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        presenter.bind(this, lifecycleScope)
        applyAppLocale()
        binding = ActivityRateMyAppBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setupUI()
    }

    private fun setupUI() {
        binding.neverAskAgain.setOnClickListener {
            presenter.onNeverAskClick()
        }
        binding.rateMeLater.setOnClickListener {
            presenter.onAskMeLaterClick()
        }
        binding.rateMeNow.setOnClickListener {
            presenter.onRateNowClick()
        }
    }

    override fun onGoWindScribeActivity() {
        finish()
    }

    override fun openPlayStoreWithLink() {
        val urlIntent = Intent(Intent.ACTION_VIEW)
        urlIntent.data = Uri.parse(PLAY_STORE_URL)
        try {
            packageManager.getPackageInfo(PLAY_STORE_URL, 0)
            urlIntent.setPackage(RateDialogConstants.PLAY_STORE_PACKAGE)
        } catch (e: PackageManager.NameNotFoundException) {
            e.printStackTrace()
        }
        startActivity(urlIntent)
    }

    override fun showToast(toast: String) {
        Toast.makeText(this, toast, Toast.LENGTH_SHORT).show()
    }
}
