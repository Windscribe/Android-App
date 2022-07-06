/*
 * Copyright (c) 2021 Windscribe Limited.
 */

package com.windscribe.mobile.alert;


import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatEditText;
import androidx.fragment.app.Fragment;

import com.windscribe.mobile.R;
import com.windscribe.vpn.serverlist.entity.ConfigFile;

import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public class UsernameAndPasswordRequestDialog extends Fragment {

    private final ConfigFile configFile;

    private AlertListener requestDialogCallback;

    public static UsernameAndPasswordRequestDialog newInstance(ConfigFile configFile) {
        return new UsernameAndPasswordRequestDialog(configFile);
    }

    private UsernameAndPasswordRequestDialog(ConfigFile configFile) {
        this.configFile = configFile;
    }

    @Override
    public void onAttach(@NotNull Context context) {
        super.onAttach(context);
        try {
            requestDialogCallback = (AlertListener) context;
        } catch (ClassCastException ignored) {
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.username_and_password_request_layout, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        AppCompatEditText mUsername = view.findViewById(R.id.username);
        AppCompatEditText mPassword = view.findViewById(R.id.password);
        if (configFile.getUsername() != null) {
            mUsername.setText(configFile.getUsername());
        }
        if (configFile.getPassword() != null) {
            mPassword.setText(configFile.getPassword());
        }
        ImageView mRememberCheck = view.findViewById(R.id.remember_check);
        mRememberCheck
                .setImageResource(configFile.isRemember() ? R.drawable.ic_checkmark_on : R.drawable.ic_checkmark_off);
        mRememberCheck.setOnClickListener(v -> {
            configFile.setRemember(!configFile.isRemember());
            mRememberCheck.setImageResource(
                    configFile.isRemember() ? R.drawable.ic_checkmark_on : R.drawable.ic_checkmark_off);
        });
        view.findViewById(R.id.request_alert_ok).setOnClickListener(view1 -> {
            String username = Objects.requireNonNull(mUsername.getText()).toString();
            String password = Objects.requireNonNull(mPassword.getText()).toString();
            configFile.setUsername(username);
            configFile.setPassword(password);
            requestDialogCallback.onSubmitUsernameAndPassword(configFile);
        });

        view.findViewById(R.id.request_alert_cancel).setOnClickListener(
                view12 -> Objects.requireNonNull(getActivity()).onBackPressed());

    }

}