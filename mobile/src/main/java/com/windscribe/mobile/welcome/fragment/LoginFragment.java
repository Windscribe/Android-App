/*
 * Copyright (c) 2021 Windscribe Limited.
 */

package com.windscribe.mobile.welcome.fragment;

import static android.view.View.VISIBLE;

import android.content.Context;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.text.method.PasswordTransformationMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatCheckBox;
import androidx.fragment.app.Fragment;

import com.windscribe.mobile.R;

import java.util.concurrent.atomic.AtomicBoolean;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnCheckedChanged;
import butterknife.OnClick;


public class LoginFragment extends Fragment implements TextWatcher, WelcomeActivityCallback {

    @BindView(R.id.loginButton)
    Button mLoginButton;

    @BindView(R.id.password)
    EditText mPasswordEditText;

    @BindView(R.id.password_error)
    ImageView mPasswordErrorView;

    @BindView(R.id.password_visibility_toggle)
    AppCompatCheckBox mPasswordVisibilityToggle;

    @BindView(R.id.nav_title)
    TextView mTitleView;

    @BindView(R.id.two_fa)
    EditText mTwoFaEditText;

    @BindView(R.id.two_fa_error)
    ImageView mTwoFaErrorView;

    @BindView(R.id.two_fa_hint)
    TextView mTwoFaHintView;

    @BindView(R.id.twoFaToggle)
    Button mTwoFaToggle;

    @BindView(R.id.username)
    EditText mUsernameEditText;

    @BindView(R.id.username_error)
    ImageView mUsernameErrorView;

    @BindView(R.id.two_fa_description)
    TextView twoFaDescriptionView;

    private final AtomicBoolean ignoreEditTextChange = new AtomicBoolean(false);

    private FragmentCallback mFragmentCallBack;

    public LoginFragment() {
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
        View view = inflater.inflate(R.layout.fragment_login, container, false);
        ButterKnife.bind(this, view);
        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mTitleView.setText(getString(R.string.login));
        addEditTextChangeListener();
    }

    @Override
    public void afterTextChanged(Editable s) {
        resetNextButtonView();
    }

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        if(!ignoreEditTextChange.getAndSet(false)){
            clearInputErrors();
        }
    }

    @Override
    public void clearInputErrors() {
        twoFaDescriptionView.setText(getString(R.string.two_fa_description));
        twoFaDescriptionView.setTextColor(getResources().getColor(R.color.colorWhite50));
        mTwoFaErrorView.setVisibility(View.INVISIBLE);
        mUsernameErrorView.setVisibility(View.INVISIBLE);
        mPasswordErrorView.setVisibility(View.INVISIBLE);
        mUsernameEditText.setTextColor(getResources().getColor(R.color.colorWhite));
        mPasswordEditText.setTextColor(getResources().getColor(R.color.colorWhite));
    }

    @OnClick({R.id.forgot_password})
    public void onForgotPasswordClick() {
        mFragmentCallBack.onForgotPasswordClick();
    }

    @OnClick({R.id.loginButton})
    public void onLoginButtonClick() {
        mFragmentCallBack.onLoginButtonClick(mUsernameEditText.getText().toString().trim(),
                mPasswordEditText.getText().toString().trim(), mTwoFaEditText.getText().toString().trim());
    }

    @OnClick({R.id.nav_button})
    public void onNavButtonClick() {
        requireActivity().onBackPressed();
    }

    @OnCheckedChanged(R.id.password_visibility_toggle)
    public void onPasswordVisibilityToggleChanged() {
        ignoreEditTextChange.set(true);
        if (mPasswordVisibilityToggle.isChecked()) {
            mPasswordEditText.setTransformationMethod(null);
        } else {
            mPasswordEditText.setTransformationMethod(new PasswordTransformationMethod());
        }
        mPasswordEditText.setSelection(mPasswordEditText.getText().length());
    }

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {

    }

    @OnClick(R.id.twoFaToggle)
    public void onTwoFaToggleClick() {
        if (mTwoFaToggle.getVisibility() == View.VISIBLE) {
            setTwoFaVisibility(View.VISIBLE);
        }
    }

    @Override
    public void setLoginError(String error) {
        twoFaDescriptionView.setVisibility(VISIBLE);
        twoFaDescriptionView.setText(error);
        twoFaDescriptionView.setTextColor(getResources().getColor(R.color.colorRed));
        mUsernameErrorView.setVisibility(VISIBLE);
        mPasswordErrorView.setVisibility(VISIBLE);
        mUsernameEditText.setTextColor(getResources().getColor(R.color.colorRed));
        mPasswordEditText.setTextColor(getResources().getColor(R.color.colorRed));
    }

    @Override
    public void setPasswordError(String error) {
        twoFaDescriptionView.setVisibility(VISIBLE);
        twoFaDescriptionView.setText(error);
        twoFaDescriptionView.setTextColor(getResources().getColor(R.color.colorRed));
        mPasswordErrorView.setVisibility(VISIBLE);
        mPasswordEditText.setTextColor(getResources().getColor(R.color.colorRed));
    }

    public void setTwoFaError(String errorMessage) {
        twoFaDescriptionView.setVisibility(VISIBLE);
        twoFaDescriptionView.setText(errorMessage);
        mTwoFaErrorView.setVisibility(VISIBLE);
    }

    public void setTwoFaVisibility(int visibility) {
        twoFaDescriptionView.setVisibility(visibility);
        mTwoFaEditText.setVisibility(visibility);
        mTwoFaHintView.setVisibility(visibility);
        mTwoFaToggle.setVisibility(visibility == VISIBLE ? View.GONE : VISIBLE);
    }

    @Override
    public void setUsernameError(String error) {
        twoFaDescriptionView.setVisibility(VISIBLE);
        twoFaDescriptionView.setText(error);
        twoFaDescriptionView.setTextColor(getResources().getColor(R.color.colorRed));
        mUsernameErrorView.setVisibility(VISIBLE);
        mUsernameEditText.setTextColor(getResources().getColor(R.color.colorRed));
    }

    private void addEditTextChangeListener() {
        mUsernameEditText.addTextChangedListener(this);
        mPasswordEditText.addTextChangedListener(this);
        mTwoFaEditText.addTextChangedListener(this);
    }

    private void resetNextButtonView() {
        boolean enable = mUsernameEditText.getText().length() > 2 && mPasswordEditText.getText().length() > 3;
        mLoginButton.setEnabled(enable);
    }
}