/*
 * Copyright (c) 2021 Windscribe Limited.
 */

package com.windscribe.mobile.alert;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.widget.TextViewCompat;
import androidx.fragment.app.DialogFragment;

import com.windscribe.mobile.R;

import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public class AlwaysOnFragment extends DialogFragment {


    public interface AlwaysOnDialogCallBack {

        void onGoToSettings();
    }

    private final boolean bootStartSetting;

    private AlwaysOnDialogCallBack callBack;

    public AlwaysOnFragment(boolean bootStartSetting) {
        this.bootStartSetting = bootStartSetting;
    }

    @Override
    public void onAttach(@NotNull Context context) {
        super.onAttach(context);
        try {
            callBack = (AlwaysOnDialogCallBack) context;
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
            dialogView = inflater.inflate(R.layout.fragment_always_on, container, false);
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N && !bootStartSetting) {
                TextView settingsBtn = dialogView.findViewById(R.id.tv_ok);
                TextViewCompat
                        .setAutoSizeTextTypeWithDefaults(settingsBtn, TextViewCompat.AUTO_SIZE_TEXT_TYPE_UNIFORM);
                settingsBtn.setText(getString(R.string.always_on_not_supported));
                settingsBtn.setEnabled(false);
            }
            if (bootStartSetting) {
                TextView title = dialogView.findViewById(R.id.tv_title);
                TextView description = dialogView.findViewById(R.id.tv_description);
                title.setText(getString(R.string.enable_start_on_boot));
                description.setText(getString(R.string.always_on_setting));
            }

            dialogView.findViewById(R.id.tv_ok).setOnClickListener(view -> {
                callBack.onGoToSettings();
                dismissAllowingStateLoss();
            });

            dialogView.findViewById(R.id.tv_cancel).setOnClickListener(view -> dismissAllowingStateLoss());

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
