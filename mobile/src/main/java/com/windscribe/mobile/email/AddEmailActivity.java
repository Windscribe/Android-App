/*
 * Copyright (c) 2021 Windscribe Limited.
 */

package com.windscribe.mobile.email;


import static com.windscribe.vpn.Windscribe.appContext;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Patterns;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.fragment.app.Fragment;
import androidx.work.Data;

import com.windscribe.mobile.R;
import com.windscribe.mobile.base.BaseActivity;
import com.windscribe.mobile.confirmemail.ConfirmActivity;
import com.windscribe.mobile.custom_view.ProgressFragment;
import com.windscribe.mobile.di.ActivityModule;
import com.windscribe.mobile.welcome.SoftInputAssist;
import com.windscribe.mobile.windscribe.WindscribeActivity;
import com.windscribe.vpn.commonutils.ThemeUtils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;

import butterknife.BindView;
import butterknife.OnClick;

public class AddEmailActivity extends BaseActivity implements AddEmailView {

    @BindView(R.id.cl_add_email)
    ConstraintLayout mConstraintLayoutMain;

    @BindView(R.id.email_description)
    TextView mEmailDescription;

    @BindView(R.id.email)
    EditText mEmailEditView;

    @BindView(R.id.email_error)
    ImageView mEmailErrorView;

    @BindView(R.id.next)
    TextView mNextButton;

    @Inject
    AddEmailPresenter mPresenter;

    @BindView(R.id.nav_title)
    TextView mTitleView;

    private final String TAG = "[add_email_a]";

    private boolean goToHomeAfterFinish = false;

    private final TextWatcher mGeneralTextWatcher = new TextWatcher() {
        @Override
        public void afterTextChanged(Editable s) {
            if (s.length() > 0) {
                if (s.hashCode() == mEmailEditView.getText().hashCode()) {
                    mEmailDescription.setText(getString(R.string.email_description));
                    mEmailDescription.setTextColor(ThemeUtils.getColor(AddEmailActivity.this,R.attr.wdSecondaryColor,R.color.colorWhite50));
                    mEmailErrorView.setVisibility(View.GONE);
                    mEmailEditView.setTextColor(ThemeUtils.getColor(AddEmailActivity.this,R.attr.wdPrimaryColor,R.color.colorWhite50));
                }
            }
        }

        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {

        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
            if (start == 0 && before == 1 && count == 0) {
                mNextButton.setPressed(false);
            } else {
                mNextButton.setEnabled(
                        s.hashCode() == mEmailEditView.getText().hashCode() && Patterns.EMAIL_ADDRESS.matcher(s)
                                .matches());
            }
        }
    };

    private final Logger mMainLogger = LoggerFactory.getLogger(TAG);

    private SoftInputAssist softInputAssist;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setActivityModule(new ActivityModule(this, this)).inject(this);
        setContentLayout(R.layout.activity_add_email_address,true);

        goToHomeAfterFinish = getIntent().getBooleanExtra("goToHomeAfterFinish", false);
        mPresenter.setUpLayout();
    }

    @Override
    protected void onResume() {
        softInputAssist.onResume();
        super.onResume();
    }

    @Override
    protected void onPause() {
        softInputAssist.onPause();
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        softInputAssist.onDestroy();
        mPresenter.onDestroy();
        super.onDestroy();
    }

    @Override
    public void gotoWindscribeActivity() {
        appContext.workManager.updateSession(Data.EMPTY);
        if (goToHomeAfterFinish) {
            Intent startIntent = WindscribeActivity.getStartIntent(this);
            startIntent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY | Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(startIntent);
        } else {
            startActivity(ConfirmActivity.getStartIntent(this));
            finish();
        }
    }

    @Override
    public void hideSoftKeyboard() {
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) {
            imm.hideSoftInputFromWindow(getWindow().getDecorView().getWindowToken(),
                    InputMethodManager.HIDE_NOT_ALWAYS);
        }
    }

    @OnClick(R.id.next)
    public void onAddEmailClick() {
        mPresenter.onAddEmailClicked(mEmailEditView.getText() != null ? mEmailEditView.getText().toString() : null);
    }

    @OnClick(R.id.nav_button)
    public void onBackButtonPressed() {
        onBackPressed();
    }

    @Override
    public void prepareUiForApiCallFinished() {
        mMainLogger.info("Preparing ui for api call finished...");
        Fragment fragment = getSupportFragmentManager().findFragmentById(R.id.cl_add_email);
        if (fragment instanceof ProgressFragment) {
            ((ProgressFragment) fragment).finishProgress();
        }
    }

    @Override
    public void prepareUiForApiCallStart() {
        mMainLogger.info("Preparing ui for api call start...");
        ProgressFragment.getInstance().add(this, R.id.cl_add_email, true);
    }

    @Override
    public void setUpLayout(String title) {
        mEmailEditView.addTextChangedListener(mGeneralTextWatcher);
        softInputAssist = new SoftInputAssist(this, new int[]{});
        mTitleView.setText(title);
    }

    @Override
    public void showInputError(String errorText) {
        mEmailDescription.setTextColor(getResources().getColor(R.color.colorRed));
        mEmailDescription.setText(errorText);
        mEmailErrorView.setVisibility(View.VISIBLE);
        mEmailEditView.setTextColor(getResources().getColor(R.color.colorRed));
    }

    @Override
    public void showToast(String toastString) {
        Toast.makeText(this, toastString, Toast.LENGTH_SHORT).show();
    }
}
