/*
 * Copyright (c) 2021 Windscribe Limited.
 */

package com.windscribe.mobile.splash;

public interface SplashView {

    boolean isConnectedToNetwork();

    void navigateToAccountSetUp();

    void navigateToHome();

    void navigateToLogin();
}
