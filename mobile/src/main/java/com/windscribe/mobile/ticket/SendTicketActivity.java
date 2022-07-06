/*
 * Copyright (c) 2021 Windscribe Limited.
 */

package com.windscribe.mobile.ticket;


import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.graphics.Rect;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.appcompat.widget.AppCompatEditText;
import androidx.fragment.app.Fragment;

import com.windscribe.mobile.R;
import com.windscribe.mobile.base.BaseActivity;
import com.windscribe.mobile.custom_view.ErrorFragment;
import com.windscribe.mobile.custom_view.ProgressFragment;
import com.windscribe.mobile.custom_view.SuccessFragment;
import com.windscribe.mobile.di.ActivityModule;
import com.windscribe.vpn.api.response.QueryType;

import java.util.Objects;

import javax.inject.Inject;

import butterknife.BindView;
import butterknife.OnClick;
import butterknife.OnItemSelected;

public class SendTicketActivity extends BaseActivity
        implements SendTicketView, TextWatcher, ViewTreeObserver.OnGlobalLayoutListener {

    @BindView(R.id.btn_send_ticket)
    Button btnSendButton;

    @BindView(R.id.tv_current_category)
    TextView currentQueryType;

    @BindView(R.id.email)
    AppCompatEditText emailView;

    @BindView(R.id.scroll_view)
    ScrollView mScrollView;

    @BindView(R.id.message)
    AppCompatEditText messageView;

    @Inject
    SendTicketPresenter presenter;

    @BindView(R.id.spinner_query)
    Spinner queryTypeSpinner;

    @BindView(R.id.subject)
    AppCompatEditText subjectView;

    @BindView(R.id.nav_title)
    TextView tvActivityTitle;

    public static Intent getStartIntent(Context context) {
        return new Intent(context, SendTicketActivity.class);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setActivityModule(new ActivityModule(this, this)).inject(this);
        setContentLayout(R.layout.activity_send_ticket,true);
        presenter.init();
    }

    @Override
    protected void onResume() {
        super.onResume();
        mScrollView.getViewTreeObserver().addOnGlobalLayoutListener(this);
    }

    @Override
    protected void onPause() {
        super.onPause();
        mScrollView.getViewTreeObserver().removeOnGlobalLayoutListener(this);
    }

    @Override
    public void addTextChangeListener() {
        messageView.addTextChangedListener(this);
        emailView.addTextChangedListener(this);
        subjectView.addTextChangedListener(this);
    }

    @Override
    public void afterTextChanged(Editable s) {
        String message = Objects.requireNonNull(messageView.getText()).toString();
        String email = Objects.requireNonNull(emailView.getText()).toString();
        String subject = Objects.requireNonNull(subjectView.getText()).toString();
        presenter.onInputChanged(email, subject, message);
    }

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {

    }

    @SuppressLint("NonConstantResourceId")
    @OnClick(R.id.nav_button)
    public void onBackButtonPressed() {
        super.onBackPressed();
    }

    @OnClick({R.id.tv_current_category, R.id.img_category_drop_down_btn})
    public void onCurrentQueryTypeClick() {
        queryTypeSpinner.performClick();
    }

    @Override
    public void onGlobalLayout() {
        Rect r = new Rect();
        mScrollView.getWindowVisibleDisplayFrame(r);
        int heightDiff = mScrollView.getRootView().getHeight() - (r.bottom - r.top);
        if (heightDiff > 150) {
            if (mScrollView.getVerticalScrollbarPosition() != mScrollView.getBottom()) {
                mScrollView.post(() -> mScrollView.smoothScrollTo(0, mScrollView.getBottom()));
            }
        }
    }

    @SuppressWarnings("unused")
    @OnItemSelected(R.id.spinner_query)
    public void onQueryTypeSelected(View view, int position) {
        String queryType = queryTypeSpinner.getSelectedItem().toString();
        currentQueryType.setText(queryType);
        presenter.onQueryTypeSelected(QueryType.valueOf(queryType));
    }

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {

    }

    @SuppressLint("NonConstantResourceId")
    @OnClick(R.id.btn_send_ticket)
    public void sendTicketClicked() {
        hideKeyBoard();
        messageView.clearFocus();
        presenter.onSendTicketClicked(Objects.requireNonNull(emailView.getText()).toString(), Objects
                        .requireNonNull(subjectView.getText()).toString(),
                Objects.requireNonNull(messageView.getText()).toString());
    }

    @Override
    public void setActivityTitle(String title) {
        tvActivityTitle.setText(title);
    }

    @Override
    public void setEmail(String email) {
        emailView.setText(email);
    }

    @Override
    public void setErrorLayout(String message) {
        ErrorFragment.getInstance().add(message, this, R.id.cl_overlay, true);
    }

    @Override
    public void setProgressView(boolean show) {
        runOnUiThread(() -> {
            if (show) {
                ProgressFragment.getInstance().add(SendTicketActivity.this, R.id.cl_settings_ticket, true);
            } else {
                Fragment fragment = getSupportFragmentManager().findFragmentById(R.id.cl_settings_ticket);
                if (fragment instanceof ProgressFragment) {
                    ((ProgressFragment) fragment).finishProgress();
                }
            }
        });
    }

    @Override
    public void setQueryTypeSpinner() {
        ArrayAdapter<QueryType> queryAdapter = new ArrayAdapter<>(this,
                R.layout.drop_down_layout,
                R.id.tv_drop_down, QueryType.values());
        queryTypeSpinner.setAdapter(queryAdapter);
    }

    @Override
    public void setSendButtonState(boolean enabled) {
        btnSendButton.setEnabled(enabled);
    }

    @Override
    public void setSuccessLayout(String message) {
        SuccessFragment.getInstance().add(message, this, R.id.cl_overlay, false);
    }

    private void hideKeyBoard() {
        try {
            InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(), 0);
        } catch (Exception ignored) {
        }
    }
}