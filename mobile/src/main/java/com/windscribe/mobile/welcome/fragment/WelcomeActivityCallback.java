/*
 * Copyright (c) 2021 Windscribe Limited.
 */

package com.windscribe.mobile.welcome.fragment;


public interface WelcomeActivityCallback {

    void clearInputErrors();

    void setLoginError(String error);

    void setPasswordError(String error);

    void setUsernameError(String error);

}
