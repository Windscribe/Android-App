/*
 * Copyright (c) 2021 Windscribe Limited.
 */

package com.windscribe.mobile.adapter;

import android.os.Bundle;
import android.os.Parcelable;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentStatePagerAdapter;

import com.windscribe.mobile.fragments.ServerListFragment;

import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.List;

public class ServerListFragmentPager extends FragmentStatePagerAdapter {

    private final List<ServerListFragment> mFragmentList;

    public ServerListFragmentPager(FragmentManager fm, List<ServerListFragment> mFragmentList) {
        super(fm, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT);
        this.mFragmentList = mFragmentList;
    }

    @Override
    public int getCount() {
        return mFragmentList != null ? mFragmentList.size() : 0;
    }

    @NotNull
    @Override
    public Fragment getItem(int i) {
        return mFragmentList.get(i);
    }

    @Override
    public Parcelable saveState() {
        Bundle bundle = (Bundle) super.saveState();
        if (bundle != null) {
            Parcelable[] states = bundle.getParcelableArray("states");
            if (states != null) {
                states = Arrays.copyOfRange(states, states.length > 3 ? states.length - 3 : 0, states.length - 1);
            }
            bundle.putParcelableArray("states", states);
        } else {
            bundle = new Bundle();
        }
        return bundle;
    }
}
