/*
 * Copyright (c) 2021 Windscribe Limited.
 */

package com.windscribe.mobile.welcome.fragment;

public interface FragmentCallback {

    void onAccountClaimButtonClick(String username, String password, String email, boolean ignoreEmptyEmail);

    void onBackButtonPressed();

    void onContinueWithOutAccountClick();

    void onForgotPasswordClick();

    void onLoginButtonClick(String username, String password, String twoFa);

    void onLoginClick();

    void onSignUpButtonClick(String username, String password, String email, boolean ignoreEmptyEmail);

    void onSkipToHomeClick();
}
