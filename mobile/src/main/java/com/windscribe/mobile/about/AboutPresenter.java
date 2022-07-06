/*
 * Copyright (c) 2021 Windscribe Limited.
 */

package com.windscribe.mobile.about;

import android.content.Context;

public interface AboutPresenter {

    void init();

    boolean isHapticFeedbackEnabled();

    void onAboutClick();

    void onBlogClick();

    void onJobsClick();

    void onPrivacyClick();

    void onStatusClick();

    void onTermsClick();

    void onViewLicenceClick();
}
