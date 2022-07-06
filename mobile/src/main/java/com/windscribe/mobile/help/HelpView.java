/*
 * Copyright (c) 2021 Windscribe Limited.
 */

package com.windscribe.mobile.help;

public interface HelpView {

    void goToSendTicket();

    void openInBrowser(String url);

    void setActivityTitle(String title);

    void showProgress(boolean inProgress, boolean success);

    void showToast(String message);
}
