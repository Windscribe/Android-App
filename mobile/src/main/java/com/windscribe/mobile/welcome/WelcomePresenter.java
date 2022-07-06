/*
 * Copyright (c) 2021 Windscribe Limited.
 */

package com.windscribe.mobile.welcome;

import android.net.Uri;

public interface WelcomePresenter {

    void exportLog();

    boolean isUserPro();

    void onBackPressed();

    void onDestroy();

    void printStartUri(Uri uri);

    void startAccountClaim(String username, String password, String email, boolean ignoreEmptyEmail);

    void startGhostAccountSetup();

    void startLoginProcess(String username, String password, String twoFa);

    void startSignUpProcess(String username, String password, String email, boolean ignoreEmptyEmail);

}
