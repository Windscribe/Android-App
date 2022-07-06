/*
 * Copyright (c) 2021 Windscribe Limited.
 */

package com.windscribe.mobile.email;

public interface AddEmailPresenter {


    void onAddEmailClicked(String emailAddress);

    void onDestroy();

    void setUpLayout();
}
