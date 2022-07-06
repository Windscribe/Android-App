/*
 * Copyright (c) 2021 Windscribe Limited.
 */

package com.windscribe.mobile.fragments;

import android.view.View;

import androidx.annotation.NonNull;
import androidx.viewpager.widget.ViewPager;

public class FeaturePageTransformer implements ViewPager.PageTransformer {

    @Override
    public void transformPage(@NonNull View view, float position) {
        if (!(position >= -1) || !(position <= 1)) {
            view.setAlpha(1);
        }
    }
}
