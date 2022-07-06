/*
 * Copyright (c) 2021 Windscribe Limited.
 */

package com.windscribe.mobile.welcome.fragment;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.windscribe.mobile.R;

import butterknife.ButterKnife;
import butterknife.OnClick;


public class NoEmailAttentionFragment extends Fragment {


    private boolean accountClaim = false;

    private boolean isPro = false;

    private FragmentCallback mFragmentCallBack;

    private String password = "";

    private String username = "";

    public NoEmailAttentionFragment(boolean accountClaim, String username, String password, boolean isPro) {
        this.accountClaim = accountClaim;
        this.username = username;
        this.password = password;
        this.isPro = isPro;
    }

    public NoEmailAttentionFragment() {

    }

    @Override
    public void onAttach(@NonNull Context context) {
        if (getActivity() instanceof FragmentCallback) {
            mFragmentCallBack = (FragmentCallback) getActivity();
        }
        super.onAttach(context);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_no_email_attention, container, false);
        ButterKnife.bind(this, view);
        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        TextView warningText = view.findViewById(R.id.warningText);
        if (isPro) {
            warningText.setText(R.string.warning_no_email_pro_account);
        }

    }

    @OnClick(R.id.backButton)
    void onBackButtonClicked() {
        mFragmentCallBack.onBackButtonPressed();
    }

    @OnClick(R.id.continue_without_email)
    void onContinueWithoutEmailButtonClicked() {
        if (accountClaim) {
            mFragmentCallBack.onAccountClaimButtonClick(username, password, "", true);
        } else {
            mFragmentCallBack.onSignUpButtonClick(username, password, "", true);
        }
    }
}