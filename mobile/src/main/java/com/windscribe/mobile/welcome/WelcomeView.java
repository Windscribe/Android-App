/*
 * Copyright (c) 2021 Windscribe Limited.
 */

package com.windscribe.mobile.welcome;

import java.io.File;

public interface WelcomeView {

    void clearInputErrors();

    void goToSignUp();

    void gotoHomeActivity(boolean clearTop);

    void hideSoftKeyboard();

    void launchShareIntent(File file);

    void prepareUiForApiCallFinished();

    void prepareUiForApiCallStart();

    void setEmailError(String errorMessage);

    void setFaFieldsVisibility(int visible);

    void setLoginRegistrationError(String error);

    void setPasswordError(String error);

    void setTwoFaError(String errorMessage);

    void setUsernameError(String error);

    void showError(String error);

    void showFailedAlert(String error);

    void showNoEmailAttentionFragment(String username, String password, boolean accountClaim, boolean pro);

    void showToast(String message);

    void updateCurrentProcess(String mCurrentCall);
}
