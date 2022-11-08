/*
 * Copyright (c) 2021 Windscribe Limited.
 */

package com.windscribe.mobile.gpsspoofing;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings;
import android.widget.Toast;

import androidx.viewpager2.widget.ViewPager2;

import com.windscribe.mobile.R;
import com.windscribe.mobile.adapter.GpsSpoofingPagerAdapter;
import com.windscribe.mobile.base.BaseActivity;
import com.windscribe.mobile.di.ActivityModule;
import com.windscribe.vpn.mocklocation.MockLocationManager;

import javax.inject.Inject;

import butterknife.BindView;
import butterknife.ButterKnife;

public class GpsSpoofingSettingsActivity extends BaseActivity
        implements GpsSpoofingSettingView, GpsSpoofingFragmentListener {

    @Inject
    GpsSpoofingPresenter mPresenter;

    @BindView(R.id.view_pager)
    ViewPager2 viewPager;

    public static Intent getStartIntent(Context context) {
        return new Intent(context, GpsSpoofingSettingsActivity.class);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setActivityModule(new ActivityModule(this, this)).inject(this);
        setContentLayout(R.layout.activity_gps_spoofing_settings, true);
        ButterKnife.bind(this);
        setViewSetViewPager();
    }

    @Override
    public void checkSuccess() {
        if (MockLocationManager.isAppSelectedInMockLocationList(getApplicationContext()) && MockLocationManager
                .isDevModeOn(getApplicationContext())) {
            mPresenter.onSuccess();
        } else {
            mPresenter.onError();
        }
    }

    @Override
    public void exit() {
        finish();
    }

    @Override
    public void onError() {
        setFragment(4);
    }

    @Override
    public void onSuccess() {
        setFragment(3);
    }

    @Override
    public void openDeveloperSettings() {
        Intent intent = new Intent(Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        if (intent.resolveActivity(getPackageManager()) != null) {
            startActivity(intent);
        } else {
            Toast.makeText(this, "Developer settings not found.", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void openSettings() {
        Intent intent = new Intent(Settings.ACTION_SETTINGS);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        if (intent.resolveActivity(getPackageManager()) != null) {
            startActivity(intent);
        } else {
            Toast.makeText(this, "Settings App not found.", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void setFragment(int index) {
        viewPager.setCurrentItem(index);
    }

    private void setViewSetViewPager() {
        GpsSpoofingPagerAdapter adapter = new GpsSpoofingPagerAdapter(getSupportFragmentManager(), getLifecycle());
        viewPager.setAdapter(adapter);
        viewPager.setCurrentItem(0);
        viewPager.setUserInputEnabled(false);
    }
}
