/*
 * Copyright (c) 2021 Windscribe Limited.
 */

package com.windscribe.mobile.account;

import android.content.Context;

public interface AccountPresenter {

    void onAddEmailClicked(String tvEmailText);

    void observeUserData(AccountActivity accountActivity);

    void onCodeEntered(String code);

    void onDestroy();

    void onEditAccountClicked();

    void onResendEmail();

    void onUpgradeClicked(String textViewText);

    void onLazyloginClicked();

    void setLayoutFromApiSession();

    void setTheme(Context context);
}
