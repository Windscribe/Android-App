/*
 * Copyright (c) 2021 Windscribe Limited.
 */
package com.windscribe.mobile.robert

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import butterknife.BindView
import butterknife.OnClick
import com.windscribe.mobile.R
import com.windscribe.mobile.adapter.RobertSettingsAdapter
import com.windscribe.mobile.base.BaseActivity
import com.windscribe.mobile.custom_view.ErrorFragment
import com.windscribe.mobile.custom_view.ProgressFragment
import com.windscribe.mobile.di.ActivityModule
import javax.inject.Inject

class RobertSettingsActivity : BaseActivity(), RobertSettingsView {
    @BindView(R.id.cl_custom_rules)
    lateinit var clCustomRules: ConstraintLayout

    @BindView(R.id.nav_title)
    lateinit var activityTitleView: TextView

    @BindView(R.id.custom_rules_arrow)
    lateinit var customRulesArrow: ImageView

    @BindView(R.id.custom_rules_progress)
    lateinit var customRulesProgressView: ProgressBar

    @Inject
    lateinit var presenter: RobertSettingsPresenter

    @BindView(R.id.recycle_settings_view)
    lateinit var recyclerSettingsView: RecyclerView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setActivityModule(ActivityModule(this, this)).inject(this)
        setContentLayout(R.layout.activity_robert_settings, true)
        presenter.init()
    }

    override fun onDestroy() {
        presenter.onDestroy()
        super.onDestroy()
    }

    override fun hideProgress() {
        val fragment = supportFragmentManager.findFragmentById(R.id.cl_robert)
        if (fragment is ProgressFragment) {
            fragment.finishProgress()
        }
    }

    override fun openUrl(url: String) {
        openURLInBrowser(url)
    }

    override fun setAdapter(robertSettingsAdapter: RobertSettingsAdapter) {
        recyclerSettingsView.layoutManager = LinearLayoutManager(this)
        recyclerSettingsView.adapter = robertSettingsAdapter
    }

    override fun setTitle(title: String) {
        activityTitleView.text = title
    }

    override fun setWebSessionLoading(loading: Boolean) {
        customRulesArrow.visibility = if (loading) View.GONE else View.VISIBLE
        customRulesProgressView.visibility =
            if (loading) View.VISIBLE else View.GONE
        clCustomRules.isEnabled = !loading
    }

    override fun showError(error: String) {
        ErrorFragment.getInstance().add(error, this, R.id.cl_robert, false)
    }

    override fun showErrorDialog(error: String) {
        ErrorFragment.getInstance().add(error, this, R.id.cl_robert, true)
    }

    override fun showProgress() {
        ProgressFragment.getInstance().add(this, R.id.cl_robert, true)
    }

    override fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    @OnClick(R.id.nav_button)
    fun onBackButtonClick() {
        onBackPressed()
    }

    @OnClick(R.id.cl_custom_rules)
    fun onCustomRulesClick() {
        presenter.onCustomRulesClick()
    }

    @OnClick(R.id.learn_more)
    fun onLearnMoreClick() {
        presenter.onLearnMoreClick()
    }

    companion object {
        fun getStartIntent(context: Context?): Intent {
            return Intent(context, RobertSettingsActivity::class.java)
        }
    }
}