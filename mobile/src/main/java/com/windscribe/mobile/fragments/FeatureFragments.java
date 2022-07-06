/*
 * Copyright (c) 2021 Windscribe Limited.
 */

package com.windscribe.mobile.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.windscribe.mobile.R;

public class FeatureFragments extends Fragment {

    private Integer pageNumber;

    public static FeatureFragments newInstance(Integer featurePageNumber) {
        FeatureFragments featureFragment = new FeatureFragments();
        Bundle bundle = new Bundle();
        bundle.putInt("feature_page_number", featurePageNumber);
        featureFragment.setArguments(bundle);
        return featureFragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        pageNumber = getArguments() != null ? getArguments().getInt("feature_page_number") : 0;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        View view;
        if (pageNumber == 0) {
            view = inflater.inflate(R.layout.feature_page_1, container, false);
        } else if (pageNumber == 1) {
            view = inflater.inflate(R.layout.feature_page_2, container, false);
        } else if (pageNumber == 2) {
            view = inflater.inflate(R.layout.feature_page_3, container, false);
        } else {
            view = inflater.inflate(R.layout.feature_page_4, container, false);
        }
        return view;

    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
    }
}
