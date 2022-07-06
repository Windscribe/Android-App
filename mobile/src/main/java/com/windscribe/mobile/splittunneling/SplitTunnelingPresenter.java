/*
 * Copyright (c) 2021 Windscribe Limited.
 */

package com.windscribe.mobile.splittunneling;

import android.content.Context;

public interface SplitTunnelingPresenter {

    void onBackPressed();

    void onDestroy();

    void onFilter(String query);

    void onNewRoutingModeSelected(String mode);

    void onToggleButtonClicked();

    void setTheme(Context context);

    void setupLayoutBasedOnPreviousSettings();
}
