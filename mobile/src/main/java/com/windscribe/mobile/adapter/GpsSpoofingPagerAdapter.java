/*
 * Copyright (c) 2021 Windscribe Limited.
 */

package com.windscribe.mobile.adapter;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;

import com.windscribe.mobile.gpsspoofing.fragments.GpsSpoofingDeveloperSettings;
import com.windscribe.mobile.gpsspoofing.fragments.GpsSpoofingError;
import com.windscribe.mobile.gpsspoofing.fragments.GpsSpoofingMockSettings;
import com.windscribe.mobile.gpsspoofing.fragments.GpsSpoofingStart;
import com.windscribe.mobile.gpsspoofing.fragments.GpsSpoofingSuccess;

public class GpsSpoofingPagerAdapter extends FragmentPagerAdapter {

    public GpsSpoofingPagerAdapter(@NonNull FragmentManager fm) {
        super(fm, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT);
    }

    @Override
    public int getCount() {
        return 5;
    }

    @NonNull
    @Override
    public Fragment getItem(int position) {
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
}
