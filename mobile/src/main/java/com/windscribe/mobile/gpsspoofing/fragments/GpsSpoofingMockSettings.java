/*
 * Copyright (c) 2021 Windscribe Limited.
 */

package com.windscribe.mobile.gpsspoofing.fragments;

import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.windscribe.mobile.R;
import com.windscribe.mobile.gpsspoofing.GpsSpoofingFragmentListener;

import org.jetbrains.annotations.NotNull;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;


public class GpsSpoofingMockSettings extends Fragment {

    private GpsSpoofingFragmentListener mListener;

    @BindView(R.id.feature_explain)
    TextView explainerText;

    public GpsSpoofingMockSettings() {

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

        View view = inflater.inflate(R.layout.gps_spoofing_mock_settings, container, false);
        ButterKnife.bind(this, view);
        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            explainerText.setText(getString(R.string.add_to_mock_location_explain_android_12));
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    @OnClick(R.id.close)
    public void onBackPressed() {
        if (mListener != null) {
            mListener.exit();
        }
    }

    @OnClick(R.id.next)
    public void onNextPressed() {
        if (mListener != null) {
            mListener.checkSuccess();
        }
    }

    @OnClick(R.id.open_setting)
    public void onOpenSettingsClick() {
        if (mListener != null) {
            mListener.openDeveloperSettings();
        }
    }

    @OnClick(R.id.previous)
    public void onPreviousPressed() {
        if (mListener != null) {
            mListener.setFragment(1);
        }
    }
}
