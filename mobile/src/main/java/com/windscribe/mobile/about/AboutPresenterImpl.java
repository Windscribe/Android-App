/*
 * Copyright (c) 2021 Windscribe Limited.
 */

package com.windscribe.mobile.about;

import com.windscribe.mobile.R;
import com.windscribe.vpn.ActivityInteractor;
import com.windscribe.vpn.constants.NetworkKeyConstants;

import javax.inject.Inject;

public class AboutPresenterImpl implements AboutPresenter {

    final ActivityInteractor mAboutInteractor;

    final AboutView mAboutView;

    @Inject
    public AboutPresenterImpl(AboutView aboutView, ActivityInteractor aboutInteractor) {
        this.mAboutView = aboutView;
        this.mAboutInteractor = aboutInteractor;
    }

    @Override
    public void init() {
        mAboutView.setTitle(mAboutInteractor.getResourceString(R.string.about));
    }

    @Override
    public boolean isHapticFeedbackEnabled() {
        return mAboutInteractor.getAppPreferenceInterface().isHapticFeedbackEnabled();
    }

    @Override
    public void onAboutClick() {
        mAboutView.openUrl(NetworkKeyConstants.getWebsiteLink(NetworkKeyConstants.URL_ABOUT));
    }

    @Override
    public void onBlogClick() {
        mAboutView.openUrl(NetworkKeyConstants.URL_BLOG);
    }

    @Override
    public void onJobsClick() {
        mAboutView.openUrl(NetworkKeyConstants.getWebsiteLink(NetworkKeyConstants.URL_JOB));
    }

    @Override
    public void onPrivacyClick() {
        mAboutView.openUrl(NetworkKeyConstants.getWebsiteLink(NetworkKeyConstants.URL_PRIVACY));
    }

    @Override
    public void onStatusClick() {
        mAboutView.openUrl(NetworkKeyConstants.getWebsiteLink(NetworkKeyConstants.URL_STATUS));
    }

    @Override
    public void onTermsClick() {
        mAboutView.openUrl(NetworkKeyConstants.getWebsiteLink(NetworkKeyConstants.URL_TERMS));
    }

    @Override
    public void onViewLicenceClick() {
        mAboutView.openUrl(NetworkKeyConstants.getWebsiteLink(NetworkKeyConstants.URL_VIEW_LICENCE));
    }
}
