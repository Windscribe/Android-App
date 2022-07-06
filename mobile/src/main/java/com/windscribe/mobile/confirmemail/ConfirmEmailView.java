/*
 * Copyright (c) 2021 Windscribe Limited.
 */

package com.windscribe.mobile.confirmemail;

public interface ConfirmEmailView {

    void finishActivity();

    void setReasonToConfirmEmail(String reasonForConfirmEmail);

    void showEmailConfirmProgress(boolean show);

    void showToast(String toast);
}
