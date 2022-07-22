/*
 * Copyright (c) 2021 Windscribe Limited.
 */

package com.windscribe.mobile.networksecurity;

import android.content.Context;

import com.windscribe.vpn.localdatabase.tables.NetworkInfo;

public interface NetworkSecurityPresenter {

    String getSavedLocale();

    void onAdapterSet();

    void onDestroy();

    void onNetworkSecuritySelected(NetworkInfo networkInfo);

    void onCurrentNetworkClick();

    void setTheme(Context context);

    void setupNetworkListAdapter();

    void init();

    void onAutoSecureToggleClick();
}
