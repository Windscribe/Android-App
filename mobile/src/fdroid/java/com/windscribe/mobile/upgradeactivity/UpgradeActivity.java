/*
 * Copyright (c) 2021 Windscribe Limited.
 */

package com.windscribe.mobile.upgradeactivity;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;


public class UpgradeActivity extends AppCompatActivity {

    public static Intent getStartIntent(Context context) {
        return new Intent(context, UpgradeActivity.class);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Toast.makeText(this, getString(com.windscribe.vpn.R.string.billing_unavailable), Toast.LENGTH_SHORT).show();
        finish();
    }
}
