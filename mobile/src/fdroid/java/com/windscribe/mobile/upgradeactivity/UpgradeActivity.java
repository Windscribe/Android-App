/*
 * Copyright (c) 2021 Windscribe Limited.
 */

package com.windscribe.mobile.upgradeactivity;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import com.windscribe.mobile.R;
import com.windscribe.mobile.base.BaseActivity;
import com.windscribe.mobile.dialogs.ErrorDialog;


public class UpgradeActivity extends BaseActivity {

    public static Intent getStartIntent(Context context) {
        return new Intent(context, UpgradeActivity.class);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ErrorDialog.show(this, getString(R.string.billing_unavailable), null, true);
    }
}
