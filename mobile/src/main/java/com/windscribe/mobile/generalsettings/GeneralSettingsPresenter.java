/*
 * Copyright (c) 2021 Windscribe Limited.
 */

package com.windscribe.mobile.generalsettings;

import android.content.Context;

import java.io.InputStream;
import java.io.OutputStream;

public interface GeneralSettingsPresenter {

    String getSavedLocale();

    void onConnectedFlagEditClicked(int requestCode);

    void onConnectedFlagPathPicked(String path);

    void onCustomFlagToggleButtonClicked(String value);

    void onDestroy();

    void onDisConnectedFlagPathPicked(String path);

    void onDisconnectedFlagEditClicked(int requestCode);

    void onHapticToggleButtonClicked();

    void onLanguageChanged();

    void onLanguageSelected(String selectedLanguage);

    void onLatencyTypeSelected(String latencyType);

    void onNotificationToggleButtonClicked();

    void onSelectionSelected(String selection);

    void onShowHealthToggleClicked();

    void onThemeSelected(String theme);

    void resizeAndSaveBitmap(final InputStream inputStream, final OutputStream outputStream);

    void setTheme(Context context);

    void setupInitialLayout();
}
