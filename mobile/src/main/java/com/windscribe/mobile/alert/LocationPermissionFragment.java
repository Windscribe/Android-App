/*
 * Copyright (c) 2021 Windscribe Limited.
 */

package com.windscribe.mobile.alert;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.windscribe.mobile.R;

import org.jetbrains.annotations.NotNull;

import butterknife.ButterKnife;
import butterknife.OnClick;

public class LocationPermissionFragment extends Fragment {

    private DisclaimerAlertListener disclaimerAlertListener;

    private int requestCode;

    public static LocationPermissionFragment newInstance(int requestCode) {
        return new LocationPermissionFragment(requestCode);
    }

    public LocationPermissionFragment(int requestCode) {
        this.requestCode = requestCode;
    }

    public LocationPermissionFragment() {

    }

    @Override
    public void onAttach(@NotNull Context context) {
        if (getActivity() instanceof DisclaimerAlertListener) {
            disclaimerAlertListener = (DisclaimerAlertListener) getActivity();
        }
        super.onAttach(context);
    }

    @Nullable
    @Override
    public View onCreateView(@NotNull LayoutInflater inflater, @Nullable ViewGroup container,
            Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.location_permission_alert, container, false);
        ButterKnife.bind(this, view);
        return view;
    }

    @SuppressLint("NonConstantResourceId")
    @OnClick(R.id.tv_cancel)
    public void onCancelClick() {
        disclaimerAlertListener.onRequestCancel();
    }

    @SuppressLint("NonConstantResourceId")
    @OnClick(R.id.tv_ok)
    public void onGrantPermissionClick() {
        disclaimerAlertListener.onRequestPermission(requestCode);
    }
}
