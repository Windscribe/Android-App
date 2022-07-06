/*
 * Copyright (c) 2021 Windscribe Limited.
 */

package com.windscribe.mobile.ratemyapp;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;

import com.windscribe.mobile.R;

import org.jetbrains.annotations.NotNull;

public class RateDialogFragment extends DialogFragment {

    public interface RateDialogResponse {

        void neverAskAgainClicked();

        void rateLaterClicked();

        void rateNowClicked();
    }

    private AlertDialog alertDialog;

    private RateDialogResponse rateDialogListener;

    public static void createDialog(AppCompatActivity appCompatActivity) {
        RateDialogFragment fragment = new RateDialogFragment();
        Fragment lastFragment = appCompatActivity.getSupportFragmentManager()
                .findFragmentById(R.id.cl_windscribe_main);
        if (!(lastFragment instanceof RateDialogFragment)) {
            appCompatActivity.getSupportFragmentManager().beginTransaction()
                    .add(R.id.cl_windscribe_main, fragment)
                    .commit();
        }

    }

    @Override
    public void onAttach(@NotNull Context context) {
        super.onAttach(context);
        rateDialogListener = (RateDialogResponse) getActivity();
        onCreateDialog(null);
    }

    @SuppressLint("InflateParams")
    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        if (getActivity() != null) {
            AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
            View dialogView;
            LayoutInflater inflater = getActivity().getLayoutInflater();
            dialogView = inflater.inflate(R.layout.rate_my_dialog, null);
            builder.setView(dialogView);
            dialogView.findViewById(R.id.rateMeNow).setOnClickListener(v -> {
                alertDialog.dismiss();
                rateDialogListener.rateNowClicked();
            });
            dialogView.findViewById(R.id.rateMeLater).setOnClickListener(v -> {
                alertDialog.dismiss();
                rateDialogListener.rateLaterClicked();
            });
            dialogView.findViewById(R.id.neverAskAgain).setOnClickListener(v -> {
                alertDialog.dismiss();
                rateDialogListener.neverAskAgainClicked();
            });

            alertDialog = builder.create();

            if (alertDialog.getWindow() != null) {
                alertDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            }

            alertDialog.show();
            return alertDialog;

        } else {
            return super.onCreateDialog(savedInstanceState);
        }


    }

    @Override
    public void onDismiss(@NotNull DialogInterface dialog) {
        super.onDismiss(dialog);
    }


}
