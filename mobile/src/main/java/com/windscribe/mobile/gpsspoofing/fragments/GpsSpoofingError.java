/*
 * Copyright (c) 2021 Windscribe Limited.
 */

package com.windscribe.mobile.gpsspoofing.fragments;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.fragment.app.Fragment;

import com.windscribe.mobile.R;
import com.windscribe.mobile.gpsspoofing.GpsSpoofingFragmentListener;

import org.jetbrains.annotations.NotNull;

import butterknife.ButterKnife;
import butterknife.OnClick;


public class GpsSpoofingError extends Fragment {

    private GpsSpoofingFragmentListener mListener;

    public GpsSpoofingError() {

    }

    @Override
    public void onAttach(@NotNull Context context) {
        super.onAttach(context);
        if (context instanceof GpsSpoofingFragmentListener) {
            mListener = (GpsSpoofingFragmentListener) context;
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.gps_spoofing_error, container, false);
        ButterKnife.bind(this, view);
        return view;
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    @OnClick(R.id.close)
    public void onBackClick() {
        if (mListener != null) {
            mListener.exit();
        }
    }

    @OnClick(R.id.start)
    public void onStartClick() {
        if (mListener != null) {
            mListener.setFragment(1);
        }
    }
}
