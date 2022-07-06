/*
 * Copyright (c) 2021 Windscribe Limited.
 */

package com.windscribe.mobile.confirmemail;

import static com.windscribe.vpn.Windscribe.appContext;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.work.Data;

import com.windscribe.mobile.R;
import com.windscribe.mobile.base.BaseActivity;
import com.windscribe.mobile.di.ActivityModule;
import com.windscribe.mobile.email.AddEmailActivity;

import javax.inject.Inject;

import butterknife.BindView;
import butterknife.OnClick;

public class ConfirmActivity extends BaseActivity implements ConfirmEmailView {

    @Inject
    ConfirmEmailPresenter confirmEmailPresenter;

    @BindView(R.id.description)
    TextView descriptionView;

    @BindView(R.id.progress_view)
    FrameLayout progressView;

    public static Intent getStartIntent(Context context) {
        return new Intent(context, ConfirmActivity.class);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setActivityModule(new ActivityModule(this, this)).inject(this);
        setContentLayout(R.layout.activity_confirm,true);
        confirmEmailPresenter.init();
    }

    @Override
    protected void onDestroy() {
        confirmEmailPresenter.onDestroy();
        super.onDestroy();
    }

    @Override
    public void finishActivity() {
        appContext.workManager.updateSession(Data.EMPTY);
        finish();
    }

    @OnClick(R.id.change_email)
    public void onChangeEmailClicked() {
        startActivity(new Intent(this, AddEmailActivity.class));
        finish();
    }

    @OnClick(R.id.close)
    public void onCloseClicked() {
        finishActivity();
    }

    @OnClick(R.id.resend_email)
    public void onResendEmailClicked() {
        confirmEmailPresenter.resendVerificationEmail();
    }

    @Override
    public void setReasonToConfirmEmail(String reasonForConfirmEmail) {
        descriptionView.setText(reasonForConfirmEmail);
    }

    @Override
    public void showEmailConfirmProgress(boolean show) {
        runOnUiThread(() -> progressView.setVisibility(show ? View.VISIBLE : View.GONE));
    }

    @Override
    public void showToast(String toast) {
        runOnUiThread(() -> Toast.makeText(ConfirmActivity.this, toast, Toast.LENGTH_SHORT).show());
    }
}