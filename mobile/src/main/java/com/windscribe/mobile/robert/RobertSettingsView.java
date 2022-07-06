/*
 * Copyright (c) 2021 Windscribe Limited.
 */

package com.windscribe.mobile.robert;

import com.windscribe.mobile.adapter.RobertSettingsAdapter;

public interface RobertSettingsView {

    void hideProgress();

    void openUrl(String url);

    void setAdapter(RobertSettingsAdapter robertSettingsAdapter);

    void setTitle(String title);

    void setWebSessionLoading(boolean loading);

    void showError(String error);

    void showErrorDialog(String error);

    void showProgress();

    void showToast(String message);
}
