/*
 * Copyright (c) 2021 Windscribe Limited.
 */
package com.windscribe.tv.moredata

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import butterknife.BindView
import butterknife.ButterKnife
import butterknife.OnClick
import butterknife.OnFocusChange
import com.windscribe.tv.R
import com.windscribe.tv.upgrade.UpgradeActivity
import com.windscribe.tv.welcome.WelcomeActivity

class GetMoreDataActivity : AppCompatActivity() {
    @JvmField
    @BindView(R.id.get_more_data)
    var getMoreDataBtn: Button? = null

    @JvmField
    @BindView(R.id.get_pro)
    var getProBtn: Button? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_get_more_data)
        ButterKnife.bind(this)
        getProBtn?.requestFocus()
    }

    @OnClick(R.id.get_more_data)
    fun moreDataClick() {
        val startIntent = WelcomeActivity.getStartIntent(this)
        startIntent.putExtra("startFragmentName", "AccountSetUp")
        startActivity(startIntent)
    }

    @OnClick(R.id.get_pro)
    fun proClick() {
        startActivity(UpgradeActivity.getStartIntent(this))
    }

    @OnFocusChange(R.id.get_more_data)
    fun onFocusChangeToGetMoreData() {
        if (getMoreDataBtn?.hasFocus() == true) {
            getMoreDataBtn?.setTextColor(resources.getColor(R.color.colorWhite))
        } else {
            getMoreDataBtn?.setTextColor(resources.getColor(R.color.colorWhite50))
        }
    }

    @OnFocusChange(R.id.get_pro)
    fun onFocusChangeToGetPro() {
        if (getProBtn?.hasFocus() == true) {
            getProBtn?.setTextColor(resources.getColor(R.color.colorWhite))
        } else {
            getProBtn?.setTextColor(resources.getColor(R.color.colorWhite50))
        }
    }

    companion object {
        @JvmStatic
        fun getStartIntent(context: Context?): Intent {
            return Intent(context, GetMoreDataActivity::class.java)
        }
    }
}
