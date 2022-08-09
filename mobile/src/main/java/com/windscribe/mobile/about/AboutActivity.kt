/*
 * Copyright (c) 2021 Windscribe Limited.
 */
package com.windscribe.mobile.about

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.HapticFeedbackConstants
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import butterknife.BindView
import butterknife.OnClick
import com.windscribe.mobile.R
import com.windscribe.mobile.base.BaseActivity
import com.windscribe.mobile.di.ActivityModule
import org.slf4j.LoggerFactory
import javax.inject.Inject

class AboutActivity : BaseActivity(), AboutView {

    @BindView(R.id.nav_button)
    lateinit var backButton: ImageView

    @Inject
    lateinit var aboutPresenter: AboutPresenter

    @BindView(R.id.nav_title)
    lateinit var activityTitleView: TextView

    private val logger = LoggerFactory.getLogger("about_a")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setActivityModule(ActivityModule(this, this)).inject(this)
        setContentLayout(R.layout.activity_about, true)
        aboutPresenter.init()
    }

    @OnClick(R.id.cl_about)
    fun onAboutClick() {
        logger.debug("User clicked about button")
        aboutPresenter.onAboutClick()
    }

    @OnClick(R.id.nav_button)
    fun onBackButtonClicked() {
        performHapticFeedback(backButton)
        logger.info("User clicked on back arrow...")
        onBackPressed()
    }

    @OnClick(R.id.cl_blog)
    fun onBlogClick() {
        logger.debug("User clicked blog button")
        aboutPresenter.onBlogClick()
    }

    @OnClick(R.id.cl_job)
    fun onJobClick() {
        logger.debug("User clicked job button")
        aboutPresenter.onJobsClick()
    }

    @OnClick(R.id.cl_privacy)
    fun onPrivacyClick() {
        logger.debug("User clicked privacy button")
        aboutPresenter.onPrivacyClick()
    }

    @OnClick(R.id.cl_status)
    fun onStatusClick() {
        logger.debug("User clicked status button")
        aboutPresenter.onStatusClick()
    }

    @OnClick(R.id.cl_term)
    fun onTermClick() {
        logger.debug("User clicked term button")
        aboutPresenter.onTermsClick()
    }

    @OnClick(R.id.cl_licence)
    fun onViewLicenceClick() {
        logger.debug("User clicked Licence button")
        aboutPresenter.onViewLicenceClick()
    }

    override fun openUrl(url: String) {
        logger.debug("Opening url in browser.")
        openURLInBrowser(url)
    }

    override fun setTitle(title: String) {
        logger.debug("Setting Activity title")
        activityTitleView.text = title
    }

    private fun performHapticFeedback(view: View) {
        if (aboutPresenter.isHapticFeedbackEnabled) {
            view.isHapticFeedbackEnabled = true
            view.performHapticFeedback(
                HapticFeedbackConstants.VIRTUAL_KEY,
                HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING
            )
        }
    }

    companion object {
        fun getStartIntent(context: Context?): Intent {
            return Intent(context, AboutActivity::class.java)
        }
    }
}