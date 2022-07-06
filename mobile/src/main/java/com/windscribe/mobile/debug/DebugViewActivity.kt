/*
 * Copyright (c) 2021 Windscribe Limited.
 */
package com.windscribe.mobile.debug

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.lifecycle.coroutineScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import butterknife.BindView
import butterknife.OnClick
import com.windscribe.mobile.R
import com.windscribe.mobile.adapter.LogViewAdapter
import com.windscribe.mobile.base.BaseActivity
import com.windscribe.mobile.di.ActivityModule
import com.windscribe.vpn.Windscribe.Companion.appContext
import com.windscribe.vpn.constants.PreferencesKeyConstants
import javax.inject.Inject

class DebugViewActivity : BaseActivity(), DebugView {
    @JvmField
    @BindView(R.id.debugView)
    var debugView: RecyclerView? = null

    @JvmField
    @BindView(R.id.nav_title)
    var activityTitleView: TextView? = null

    @JvmField
    @BindView(R.id.progressView)
    var progressView: ConstraintLayout? = null
    private var charonLog = false

    @Inject
    lateinit var debugPresenter: DebugPresenter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setActivityModule(ActivityModule(this, this)).inject(this)
        setContentLayout(R.layout.activity_debug_view, true)
        charonLog = intent.getBooleanExtra("charonLog", false)
        setActivityTitle()
        lifecycle.coroutineScope.launchWhenCreated {
            debugPresenter.init()
        }
    }

    fun setActivityTitle() {
        activityTitleView?.text = getString(R.string.view_log)
    }

    override fun setTheme(context: Context) {
        val savedThem = appContext.preference.selectedTheme
        if (savedThem == PreferencesKeyConstants.DARK_THEME) {
            context.setTheme(R.style.DarkTheme)
        } else {
            context.setTheme(R.style.LightTheme)
        }
    }

    @OnClick(R.id.nav_button)
    fun onBackCLicked() {
        onBackPressed()
    }

    override fun setAdapter(logViewAdapter: LogViewAdapter) {
        debugView?.let {
            it.layoutManager = LinearLayoutManager(this)
            it.adapter = logViewAdapter
        }
    }

    override fun showProgress(show: Boolean) {
        if (show) {
            progressView?.visibility = View.VISIBLE
        } else {
            progressView?.visibility = View.GONE
        }
    }

    companion object {
        @JvmStatic
        fun getStartIntent(context: Context?, charonLog: Boolean): Intent {
            return Intent(context, DebugViewActivity::class.java).putExtra("charonLog", charonLog)
        }
    }
}