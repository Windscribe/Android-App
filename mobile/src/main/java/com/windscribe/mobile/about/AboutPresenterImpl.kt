/*
 * Copyright (c) 2021 Windscribe Limited.
 */
package com.windscribe.mobile.about

import com.windscribe.mobile.R
import com.windscribe.vpn.ActivityInteractor
import com.windscribe.vpn.constants.NetworkKeyConstants
import com.windscribe.vpn.constants.NetworkKeyConstants.getWebsiteLink
import javax.inject.Inject

class AboutPresenterImpl @Inject constructor(
    private val aboutView: AboutView,
    private val interactor: ActivityInteractor
) : AboutPresenter {
    override fun init() {
        aboutView.setTitle(interactor.getResourceString(R.string.about))
    }

    override val isHapticFeedbackEnabled: Boolean
        get() = interactor.getAppPreferenceInterface().isHapticFeedbackEnabled

    override fun onAboutClick() {
        aboutView.openUrl(getWebsiteLink(NetworkKeyConstants.URL_ABOUT))
    }

    override fun onBlogClick() {
        aboutView.openUrl(NetworkKeyConstants.URL_BLOG)
    }

    override fun onJobsClick() {
        aboutView.openUrl(getWebsiteLink(NetworkKeyConstants.URL_JOB))
    }

    override fun onPrivacyClick() {
        aboutView.openUrl(getWebsiteLink(NetworkKeyConstants.URL_PRIVACY))
    }

    override fun onStatusClick() {
        aboutView.openUrl(getWebsiteLink(NetworkKeyConstants.URL_STATUS))
    }

    override fun onTermsClick() {
        aboutView.openUrl(getWebsiteLink(NetworkKeyConstants.URL_TERMS))
    }

    override fun onViewLicenceClick() {
        aboutView.openUrl(getWebsiteLink(NetworkKeyConstants.URL_VIEW_LICENCE))
    }

    override fun onChangelogClick() {
        aboutView.openUrl(getWebsiteLink(NetworkKeyConstants.URL_CHANGELOG))
    }
}