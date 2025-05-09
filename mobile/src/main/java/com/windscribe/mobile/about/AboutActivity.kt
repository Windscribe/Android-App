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
import com.windscribe.mobile.custom_view.preferences.IconLinkView
import com.windscribe.mobile.di.ActivityModule
import org.slf4j.LoggerFactory
import javax.inject.Inject

class AboutActivity : BaseActivity(), AboutView {

    @BindView(R.id.nav_button)
    lateinit var backButton: ImageView

    @BindView(R.id.cl_status)
    lateinit var statusView: IconLinkView

    @BindView(R.id.cl_about)
    lateinit var aboutView: IconLinkView

    @BindView(R.id.cl_privacy)
    lateinit var privacyView: IconLinkView

    @BindView(R.id.cl_term)
    lateinit var termsView: IconLinkView

    @BindView(R.id.cl_blog)
    lateinit var blogView: IconLinkView

    @BindView(R.id.cl_job)
    lateinit var jobView: IconLinkView

    @BindView(R.id.cl_licence)
    lateinit var licenceView: IconLinkView

    @BindView(R.id.cl_changelog)
    lateinit var changelogView: IconLinkView

    @Inject
    lateinit var aboutPresenter: AboutPresenter

    @BindView(R.id.nav_title)
    lateinit var activityTitleView: TextView

    private val logger = LoggerFactory.getLogger("basic")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setActivityModule(ActivityModule(this, this)).inject(this)
        setContentLayout(R.layout.activity_about, true)
        aboutPresenter.init()
        setupCustomLayoutDelegates()
    }

    private fun setupCustomLayoutDelegates() {
        statusView.onClick {
            aboutPresenter.onStatusClick()
        }
        aboutView.onClick {
            aboutPresenter.onAboutClick()
        }
        privacyView.onClick {
            aboutPresenter.onPrivacyClick()
        }
        termsView.onClick {
            aboutPresenter.onTermsClick()
        }
        blogView.onClick {
            aboutPresenter.onBlogClick()
        }
        jobView.onClick {
            aboutPresenter.onJobsClick()
        }
        licenceView.onClick {
            aboutPresenter.onViewLicenceClick()
        }
        changelogView.onClick {
            aboutPresenter.onChangelogClick()
        }
    }

    @OnClick(R.id.nav_button)
    fun onBackButtonClicked() {
        performHapticFeedback(backButton)
        logger.info("User clicked on back arrow...")
        onBackPressed()
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