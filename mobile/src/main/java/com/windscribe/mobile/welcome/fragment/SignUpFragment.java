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
import android.widget.ImageButton;
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


public class SignUpFragment extends Fragment implements TextWatcher, WelcomeActivityCallback {

    @BindView(R.id.email_sub_description)
    TextView addEmailLabel;

    @BindView(R.id.nav_button)
    ImageButton backButton;

    @BindView(R.id.email_description)
    TextView mEmailDescriptionView;

    @BindView(R.id.email)
    EditText mEmailEditText;

    @BindView(R.id.email_error)
    ImageView mEmailErrorView;

    @BindView(R.id.page_description)
    TextView mPageDescriptionView;

    @BindView(R.id.password)
    EditText mPasswordEditText;

    @BindView(R.id.password_error)
    ImageView mPasswordErrorView;

    @BindView(R.id.password_visibility_toggle)
    AppCompatCheckBox mPasswordVisibilityToggle;

    @BindView(R.id.set_up_later_button)
    Button mSetUpButton;

    @BindView(R.id.loginButton)
    Button mSignUpButton;

    @BindView(R.id.nav_title)
    TextView mTitleView;

    @BindView(R.id.username)
    EditText mUsernameEditText;

    @BindView(R.id.username_error)
    ImageView mUsernameErrorView;

    private boolean isAccountSetUpLayout = false;

    private FragmentCallback mFragmentCallBack;

    private boolean skipToHome = false;

    private boolean userPro = false;

    private final AtomicBoolean ignoreEditTextChange = new AtomicBoolean(false);

    public SignUpFragment(boolean userPro) {
        this.userPro = userPro;
    }

    public SignUpFragment() {

    }

    @Override
    public void onAttach(@NonNull Context context) {
        if (getActivity() instanceof FragmentCallback) {
            mFragmentCallBack = (FragmentCallback) getActivity();
        }
        super.onAttach(context);
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            isAccountSetUpLayout = getArguments().getString("startFragmentName", "SignUp").equals("AccountSetUp");
            skipToHome = getArguments().getBoolean("skipToHome", false);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_sign_up, container, false);
        ButterKnife.bind(this, view);
        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        if (isAccountSetUpLayout) {
            mPageDescriptionView.setText(getString(R.string.account_set_up_description));
            mTitleView.setText(getString(R.string.account_set_up));
            mSetUpButton.setVisibility(VISIBLE);
            if (userPro) {
                addEmailLabel.setVisibility(View.GONE);
            }
            if (skipToHome) {
                backButton.setVisibility(View.INVISIBLE);
            }
        } else {
            mPageDescriptionView.setText(getString(R.string.sign_up_description));
            mTitleView.setText(getString(R.string.sign_up));
            mSetUpButton.setVisibility(View.GONE);
        }
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
        mEmailDescriptionView.setTextColor(getResources().getColor(R.color.colorWhite50));
        mEmailDescriptionView.setText(getString(R.string.email_description));
        mEmailErrorView.setVisibility(View.INVISIBLE);
        mUsernameErrorView.setVisibility(View.INVISIBLE);
        mPasswordErrorView.setVisibility(View.INVISIBLE);
        mUsernameEditText.setTextColor(getResources().getColor(R.color.colorWhite));
        mPasswordEditText.setTextColor(getResources().getColor(R.color.colorWhite));
    }

    @OnClick({R.id.nav_button, R.id.set_up_later_button})
    public void onNavButtonClick() {
        if (skipToHome) {
            mFragmentCallBack.onSkipToHomeClick();
        } else {
            requireActivity().onBackPressed();
        }
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

    @OnClick({R.id.loginButton})
    public void onSignUpButtonClick() {
        if (isAccountSetUpLayout) {
            mFragmentCallBack.onAccountClaimButtonClick(mUsernameEditText.getText().toString().trim(),
                    mPasswordEditText.getText().toString().trim(), mEmailEditText.getText().toString().trim(), false);
        } else {
            mFragmentCallBack.onSignUpButtonClick(mUsernameEditText.getText().toString().trim(),
                    mPasswordEditText.getText().toString().trim(), mEmailEditText.getText().toString().trim(), false);
        }
    }

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {

    }

    public void setEmailError(String errorMessage) {
        mEmailDescriptionView.setTextColor(getResources().getColor(R.color.colorRed));
        mEmailDescriptionView.setText(errorMessage);
        mEmailErrorView.setVisibility(VISIBLE);
    }

    @Override
    public void setLoginError(String error) {
        mEmailDescriptionView.setTextColor(getResources().getColor(R.color.colorRed));
        mEmailDescriptionView.setText(error);
        mUsernameErrorView.setVisibility(VISIBLE);
        mPasswordErrorView.setVisibility(VISIBLE);
        mEmailErrorView.setVisibility(VISIBLE);
        mUsernameEditText.setTextColor(getResources().getColor(R.color.colorRed));
        mPasswordEditText.setTextColor(getResources().getColor(R.color.colorRed));
    }

    @Override
    public void setPasswordError(String error) {
        mEmailDescriptionView.setTextColor(getResources().getColor(R.color.colorRed));
        mEmailDescriptionView.setText(error);
        mPasswordErrorView.setVisibility(VISIBLE);
        mPasswordEditText.setTextColor(getResources().getColor(R.color.colorRed));
    }

    @Override
    public void setUsernameError(String error) {
        mEmailDescriptionView.setTextColor(getResources().getColor(R.color.colorRed));
        mEmailDescriptionView.setText(error);
        mUsernameErrorView.setVisibility(VISIBLE);
        mUsernameEditText.setTextColor(getResources().getColor(R.color.colorRed));
    }

    private void addEditTextChangeListener() {
        mUsernameEditText.addTextChangedListener(this);
        mPasswordEditText.addTextChangedListener(this);
        mEmailEditText.addTextChangedListener(this);
    }

    private void resetNextButtonView() {
        boolean enable = mUsernameEditText.getText().length() > 2 && mPasswordEditText.getText().length() > 7;
        mSignUpButton.setEnabled(enable);
    }
}