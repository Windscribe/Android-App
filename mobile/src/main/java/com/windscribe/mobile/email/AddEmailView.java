/*
 * Copyright (c) 2021 Windscribe Limited.
 */

package com.windscribe.mobile.email;

public interface AddEmailView {

    void gotoWindscribeActivity();

    void hideSoftKeyboard();

    void prepareUiForApiCallFinished();

    void prepareUiForApiCallStart();

    void setUpLayout(String title);

    void showInputError(String errorText);

    void showToast(String toastString);

}
