/*
 * Copyright (c) 2021 Windscribe Limited.
 */

package com.windscribe.mobile.generalsettings;

public interface GeneralSettingsView {

    String[] getOrderList();

    String[] getThemeList();

    void openFileChooser(int requestCode);

    void registerLocaleChangeListener();

    void resetTextResources(String title, String sortBy, String latencyDisplay, String language, String appearance,
            String notificationState, String hapticFeedback, String version, String connected, String disconnected,
            String appBackground);

    void setActivityTitle(String activityTitle);

    void setAppVersionText(String versionText);

    void setConnectedFlagPath(String path);

    void setDisconnectedFlagPath(String path);

    void setFlagSizeLabel(String label);

    void setLanguageTextView(String language);

    void setLatencyType(String latencyType);

    void setSelectionTextView(String selection);

    void setThemeTextView(String theme);

    void setupCustomFlagAdapter(String saved, String[] options);

    void setupHapticToggleImage(int ic_toggle_button_off);

    void setupLanguageAdapter(String savedLanguage, String[] language);

    void setupLatencyAdapter(String savedLatency, String[] latencyTypes);

    void setupLocationHealthToggleImage(int image);

    void setupNotificationToggleImage(int ic_toggle_button_off);

    void setupSelectionAdapter(String savedSelection, String[] selections);

    void setupThemeAdapter(String savedTheme, String[] themeList);
}
