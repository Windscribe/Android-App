/*
 * Copyright (c) 2021 Windscribe Limited.
 */

package com.windscribe.mobile.robert;

import android.content.Context;

public interface RobertSettingsPresenter {

    String getSavedLocale();

    void init();

    void onCustomRulesClick();

    void onDestroy();

    void onLearnMoreClick();

    void setTheme(Context context);
}
