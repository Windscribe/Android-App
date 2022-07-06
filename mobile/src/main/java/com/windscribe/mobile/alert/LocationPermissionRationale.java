/*
 * Copyright (c) 2021 Windscribe Limited.
 */

package com.windscribe.mobile.alert;


import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import com.windscribe.mobile.R;

import org.jetbrains.annotations.NotNull;

public class LocationPermissionRationale extends DialogFragment {


    private PermissionRationaleListener callBack;

    public LocationPermissionRationale() {
        // Required empty public constructor
    }

    @Override
    public void onAttach(@NotNull Context context) {
        super.onAttach(context);
        try {
            callBack = (PermissionRationaleListener) context;
        } catch (ClassCastException ignored) {
        }
    }

    @SuppressLint("InflateParams")
    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        if (getActivity() != null) {
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity(), R.style.FullScreenDialog);
            View dialogView;

            LayoutInflater inflater = getActivity().getLayoutInflater();
            dialogView = inflater.inflate(R.layout.fragment_location_permission_rationale, null);

            dialogView.findViewById(R.id.tv_ok).setOnClickListener(view -> {
                callBack.goToAppInfoSettings();
                dismissAllowingStateLoss();
            });

            dialogView.findViewById(R.id.tv_cancel).setOnClickListener(view -> dismissAllowingStateLoss());

            builder.setView(dialogView);
            Dialog dialog = builder.create();

            if (dialog.getWindow() != null) {
                dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            }

            return dialog;
        } else {
            return super.onCreateDialog(savedInstanceState);
        }

    }

}
