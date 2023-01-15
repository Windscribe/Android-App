/*
 * Copyright (c) 2021 Windscribe Limited.
 */

package com.windscribe.mobile.adapter;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.Lifecycle;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import com.windscribe.mobile.gpsspoofing.fragments.GpsSpoofingDeveloperSettings;
import com.windscribe.mobile.gpsspoofing.fragments.GpsSpoofingError;
import com.windscribe.mobile.gpsspoofing.fragments.GpsSpoofingMockSettings;
import com.windscribe.mobile.gpsspoofing.fragments.GpsSpoofingStart;
import com.windscribe.mobile.gpsspoofing.fragments.GpsSpoofingSuccess;

public class GpsSpoofingPagerAdapter extends FragmentStateAdapter {

    public GpsSpoofingPagerAdapter(@NonNull FragmentManager fm, Lifecycle lifecycle) {
        super(fm, lifecycle);
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        switch (position) {
            case 0:
                return new GpsSpoofingStart();
            case 1:
                return new GpsSpoofingDeveloperSettings();
            case 2:
                return new GpsSpoofingMockSettings();
            case 3:
                return new GpsSpoofingSuccess();
            case 4:
                return new GpsSpoofingError();
        }
        return new GpsSpoofingStart();
    }

    @Override
    public int getItemCount() {
        return 5;
    }
}
