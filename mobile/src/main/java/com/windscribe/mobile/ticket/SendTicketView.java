/*
 * Copyright (c) 2021 Windscribe Limited.
 */

package com.windscribe.mobile.ticket;

public interface SendTicketView {

    void addTextChangeListener();

    void setActivityTitle(String title);

    void setEmail(String email);

    void setErrorLayout(String message);

    void setProgressView(boolean show);

    void setQueryTypeSpinner();

    void setSendButtonState(boolean enabled);

    void setSuccessLayout(String message);
}
