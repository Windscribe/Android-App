/*
 * Copyright (c) 2021 Windscribe Limited.
 */

package com.windscribe.mobile.alert;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import com.windscribe.mobile.R;

import org.jetbrains.annotations.NotNull;

import java.util.Objects;


public class AllProtocolFailedNetworkFragment extends DialogFragment {

    private AlertListener mAllProtocolFailedNetworkCallback;

    public static AllProtocolFailedNetworkFragment newInstance() {
        return new AllProtocolFailedNetworkFragment();
    }

    public AllProtocolFailedNetworkFragment() {
    }

    @Override
    public void onAttach(@NotNull Context context) {
        super.onAttach(context);
        try {
            mAllProtocolFailedNetworkCallback = (AlertListener) context;
        } catch (ClassCastException ignored) {
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setStyle(DialogFragment.STYLE_NORMAL, R.style.FullScreenDialog);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {

        if (getActivity() != null) {
            View dialogView;
            dialogView = inflater.inflate(R.layout.all_protocol_failed_alert, container, false);
            dialogView.findViewById(R.id.tv_send_log).setOnClickListener(v -> {
                mAllProtocolFailedNetworkCallback.sendLog();
                Objects.requireNonNull(getDialog()).cancel();
            });
            dialogView.findViewById(R.id.tv_contact_support).setOnClickListener(v -> {
                mAllProtocolFailedNetworkCallback.contactSupport();
                Objects.requireNonNull(getDialog()).cancel();
            });
            dialogView.findViewById(R.id.tv_cancel).setOnClickListener(
                    v -> Objects.requireNonNull(getDialog()).cancel());

            return dialogView;
        } else {
            return super.getView();
        }

    }

    @Override
    public void onStart() {
        super.onStart();
        Dialog d = getDialog();
        if (d != null) {
            int width = ViewGroup.LayoutParams.MATCH_PARENT;
            int height = ViewGroup.LayoutParams.MATCH_PARENT;
            Objects.requireNonNull(d.getWindow()).setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            Objects.requireNonNull(d.getWindow()).setLayout(width, height);
        }
    }
}