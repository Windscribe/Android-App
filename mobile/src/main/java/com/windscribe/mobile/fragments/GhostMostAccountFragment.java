/*
 * Copyright (c) 2021 Windscribe Limited.
 */

package com.windscribe.mobile.fragments;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import com.windscribe.mobile.R;
import com.windscribe.mobile.listeners.AccountFragmentCallback;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;


public class GhostMostAccountFragment extends Fragment {

    @BindView(R.id.login)
    Button loginButton;

    @BindView(R.id.nav_title)
    TextView titleView;

    private AccountFragmentCallback callback;

    private boolean proUser = false;

    public static GhostMostAccountFragment getInstance() {
        return new GhostMostAccountFragment();
    }

    public GhostMostAccountFragment() {

    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        try {
            callback = (AccountFragmentCallback) context;
        } catch (ClassCastException ignored) {

        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_ghost_account, container, false);
        ButterKnife.bind(this, view);
        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        titleView.setText(getString(R.string.my_account));

        if (getArguments() != null) {
            proUser = getArguments().getBoolean("pro_user", false);
        }
        loginButton.setVisibility(proUser ? View.GONE : View.VISIBLE);
    }

    public void add(AppCompatActivity activity, int container, boolean addToBackStack, boolean proUser) {
        FragmentTransaction transaction = activity.getSupportFragmentManager()
                .beginTransaction()
                .replace(container, this);
        Bundle bundle = new Bundle();
        bundle.putBoolean("pro_user", proUser);
        setArguments(bundle);
        if (addToBackStack) {
            transaction.addToBackStack(this.getClass().getName());
        }
        transaction.commit();
    }

    @OnClick(R.id.nav_button)
    public void onBackPressed() {
        if (getActivity() != null) {
            getActivity().onBackPressed();
        }
    }

    @OnClick(R.id.login)
    public void onLoginClicked() {
        callback.onLoginClicked();
    }

    @OnClick(R.id.sign_up)
    public void onSignUpClicked() {
        callback.onSignUpClicked();
    }
}