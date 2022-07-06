/*
 * Copyright (c) 2021 Windscribe Limited.
 */

package com.windscribe.mobile.ticket;

import android.content.Context;

import com.windscribe.vpn.api.response.QueryType;

public interface SendTicketPresenter {

    void init();

    void onInputChanged(String email, String subject, String message);

    void onQueryTypeSelected(QueryType queryType);

    void onSendTicketClicked(String email, String subject, String message);

    void setTheme(Context context);
}
