/*
 * Copyright (c) 2021 Windscribe Limited.
 */

package com.windscribe.mobile.confirmemail;

public interface ConfirmEmailPresenter {

    void init();

    void onDestroy();

    void resendVerificationEmail();
}
