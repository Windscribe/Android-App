/*
 * Copyright (c) 2021 Windscribe Limited.
 */

package com.windscribe.mobile.about;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.HapticFeedbackConstants;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.windscribe.mobile.R;
import com.windscribe.mobile.base.BaseActivity;
import com.windscribe.mobile.di.ActivityModule;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;

import butterknife.BindView;
import butterknife.OnClick;

public class AboutActivity extends BaseActivity implements AboutView {

    @BindView(R.id.nav_button)
    ImageView backButton;

    @Inject
    AboutPresenter mAboutPresenter;

    @BindView(R.id.nav_title)
    TextView mActivityTitleView;

    private final Logger logger = LoggerFactory.getLogger("about_a");

    public static Intent getStartIntent(Context context) {
        return new Intent(context, AboutActivity.class);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setActivityModule(new ActivityModule(this, this)).inject(this);
        setContentLayout(R.layout.activity_about,true);
        mAboutPresenter.init();
    }

    @OnClick(R.id.cl_about)
    public void onAboutClick() {
        logger.debug("User clicked about button");
        mAboutPresenter.onAboutClick();
    }

    @OnClick(R.id.nav_button)
    public void onBackButtonClicked() {
        performHapticFeedback(backButton);
        logger.info("User clicked on back arrow...");
        onBackPressed();
    }

    @OnClick(R.id.cl_blog)
    public void onBlogClick() {
        logger.debug("User clicked blog button");
        mAboutPresenter.onBlogClick();
    }

    @OnClick(R.id.cl_job)
    public void onJobClick() {
        logger.debug("User clicked job button");
        mAboutPresenter.onJobsClick();
    }

    @OnClick(R.id.cl_privacy)
    public void onPrivacyClick() {
        logger.debug("User clicked privacy button");
        mAboutPresenter.onPrivacyClick();
    }

    @OnClick(R.id.cl_status)
    public void onStatusClick() {
        logger.debug("User clicked status button");
        mAboutPresenter.onStatusClick();
    }

    @OnClick(R.id.cl_term)
    public void onTermClick() {
        logger.debug("User clicked term button");
        mAboutPresenter.onTermsClick();
    }

    @OnClick(R.id.cl_licence)
    public void onViewLicenceClick(){
        logger.debug("User clicked Licence button");
        mAboutPresenter.onViewLicenceClick();
    }

    @Override
    public void openUrl(final String url) {
        logger.debug("Opening url in browser.");
        openURLInBrowser(url);
    }

    @Override
    public void setTitle(final String title) {
        logger.debug("Setting Activity title");
        mActivityTitleView.setText(title);
    }

    private void performHapticFeedback(View view) {
        if (mAboutPresenter.isHapticFeedbackEnabled()) {
            view.setHapticFeedbackEnabled(true);
            view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY,
                    HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING);
        }
    }
}