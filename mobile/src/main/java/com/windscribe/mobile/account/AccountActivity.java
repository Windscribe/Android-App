/*
 * Copyright (c) 2021 Windscribe Limited.
 */

package com.windscribe.mobile.account;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputFilter;
import android.text.InputFilter.LengthFilter;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.widget.AppCompatEditText;
import androidx.constraintlayout.widget.ConstraintLayout;

import com.windscribe.mobile.R;
import com.windscribe.mobile.listeners.AccountFragmentCallback;
import com.windscribe.mobile.fragments.GhostMostAccountFragment;
import com.windscribe.mobile.base.BaseActivity;
import com.windscribe.mobile.confirmemail.ConfirmActivity;
import com.windscribe.mobile.custom_view.CustomDialog;
import com.windscribe.mobile.custom_view.ErrorFragment;
import com.windscribe.mobile.custom_view.SuccessFragment;
import com.windscribe.mobile.di.ActivityModule;
import com.windscribe.mobile.di.DaggerActivityComponent;
import com.windscribe.mobile.email.AddEmailActivity;
import com.windscribe.mobile.upgradeactivity.UpgradeActivity;
import com.windscribe.mobile.welcome.WelcomeActivity;
import com.windscribe.vpn.Windscribe;
import com.windscribe.vpn.commonutils.ThemeUtils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;

import javax.inject.Inject;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;


public class AccountActivity extends BaseActivity implements AccountView, AccountFragmentCallback{

    AlertDialog alertDialog = null;

    @BindView(R.id.confirmContainer)
    ConstraintLayout confirmContainer;

    @BindView(R.id.edit_arrow)
    ImageView editAccountArrow;

    @BindView(R.id.edit_progress)
    ProgressBar editAccountProgressView;

    @BindView(R.id.img_progress)
    ProgressBar emailProgressCircle;

    @BindView(R.id.nav_button)
    ImageView imgAccountBackBtn;

    @Inject
    AccountPresenter mAccountPresenter;

    @BindView(R.id.nav_title)
    TextView mActivityTitleView;

    @Inject
    CustomDialog mCustomProgressDialog;

    @BindView(R.id.tv_send_btn)
    TextView resendButton;

    @BindView(R.id.tv_account_email)
    TextView tvAccountEmail;

    @BindView(R.id.tv_account_username)
    TextView tvAccountUserName;

    @BindView(R.id.tv_edit_account)
    TextView tvEditAccount;

    @BindView(R.id.tv_plan_data)
    TextView tvPlanData;

    @BindView(R.id.tv_reset_date)
    TextView tvResetDate;

    @BindView(R.id.tv_reset_date_label)
    TextView tvResetDateLabel;

    @BindView(R.id.tv_upgrade_info)
    TextView tvUpgradeInfo;

    private final String TAG = "account_a";

    private final Logger mActivityLogger = LoggerFactory.getLogger(TAG);

    public static Intent getStartIntent(Context context) {
        return new Intent(context, AccountActivity.class);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        DaggerActivityComponent.builder().activityModule(new ActivityModule(this, this))
                .applicationComponent(Windscribe.getAppContext().getApplicationComponent())
                .build().inject(this);
        mAccountPresenter.setTheme(this);
        setContentView(R.layout.activity_account);
        ButterKnife.bind(this);
        mAccountPresenter.observeUserData(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        mAccountPresenter.setLayoutFromApiSession();
    }

    @Override
    protected void onDestroy() {
        mAccountPresenter.onDestroy();
        super.onDestroy();
    }

    @Override
    public void goToConfirmEmailActivity() {
        startActivity(ConfirmActivity.getStartIntent(this));
    }

    @Override
    public void goToEmailActivity() {
        Intent startIntent = new Intent(this, AddEmailActivity.class);
        startActivity(startIntent);
    }

    @Override
    public void hideProgress() {
        mCustomProgressDialog.dismiss();
    }

    @OnClick(R.id.tv_account_email)
    public void onAddEmailClick() {
        mActivityLogger.info("User clicked on " + tvAccountEmail.getText().toString()
                + " email text view...");
        mAccountPresenter.onAddEmailClicked(tvAccountEmail.getText().toString());
    }

    @OnClick(R.id.nav_button)
    public void onBackButtonClicked() {
        mActivityLogger.info("User clicked on back arrow...");
        onBackPressed();
    }

    @OnClick({R.id.tv_edit_account, R.id.edit_arrow})
    public void onEditAccountClick() {
        mActivityLogger.info("User clicked on edit account button...");
        mAccountPresenter.onEditAccountClicked();
    }

    @Override
    public void onLoginClicked() {
        Intent startIntent = WelcomeActivity.getStartIntent(this);
        startIntent.putExtra("startFragmentName", "Login");
        startActivity(startIntent);
    }

    @Override
    public void onSignUpClicked() {
        Intent startIntent = WelcomeActivity.getStartIntent(this);
        startIntent.putExtra("startFragmentName", "AccountSetUp");
        startActivity(startIntent);
    }

    @OnClick(R.id.tv_upgrade_info)
    public void onUpgradeClick() {
        mActivityLogger.info("User clicked on " + tvUpgradeInfo.getText().toString()
                + " upgrade button...");
        mAccountPresenter.onUpgradeClicked(tvUpgradeInfo.getText().toString());
    }

    @Override
    public void openEditAccountInBrowser(String url) {
        openURLInBrowser(url);
    }

    @Override
    public void openUpgradeActivity() {
        startActivity(UpgradeActivity.getStartIntent(this));
    }

    @OnClick(R.id.confirmContainer)
    public void resendEmailClicked() {
        mAccountPresenter.onResendEmail();
    }

    @Override
    public void setActivityTitle(String title) {
        mActivityTitleView.setText(title);
    }

    @Override
    public void setEmail(String email, int textColor) {
        mActivityLogger.info("Displaying account email ...");
        tvAccountEmail.setText(email);
        tvAccountEmail.setTextColor(textColor);
        confirmContainer.setVisibility(View.GONE);
    }

    @Override
    public void setEmailConfirm(String emailConfirm) {
        mActivityLogger.info("Displaying account email ...");
        tvAccountEmail.setText(emailConfirm);
        confirmContainer.setVisibility(View.VISIBLE);
    }

    @Override
    public void setPlanName(String planName) {
        mActivityLogger.info("Displaying user plan name ...");
        tvPlanData.setText(planName);
    }

    @Override
    public void setResetDate(String resetDateLabel, String resetDate) {
        mActivityLogger.info("Displaying user next reset date ...");
        tvResetDateLabel.setText(resetDateLabel);
        tvResetDate.setText(resetDate);
    }

    @Override
    public void setUsername(String username) {
        mActivityLogger.info("Displaying account username ...");
        tvAccountUserName.setText(username);
    }

    @Override
    public void setWebSessionLoading(final boolean show) {
        editAccountArrow.setVisibility(show ? View.GONE : View.VISIBLE);
        editAccountProgressView.setVisibility(show ? View.VISIBLE : View.GONE);
        tvEditAccount.setEnabled(!show);
    }

    @Override
    public void setupLayoutForFreeUser(String upgradeText) {
        mActivityLogger.info("Setting up layout for free user...");
        tvUpgradeInfo.setText(upgradeText);
    }

    @Override
    public void setupLayoutForGhostMode(boolean proUser) {
        GhostMostAccountFragment.getInstance().add(this, R.id.fragment_container, false, proUser);
    }

    @Override
    public void setupLayoutForPremiumUser(String upgradeText) {
        mActivityLogger.info("Setting up layout for premium user...");
        tvUpgradeInfo.setText(upgradeText);
    }

    @Override
    public void showEnterCodeDialog() {
        AlertDialog.Builder alert = new AlertDialog.Builder(this, R.style.OverlayAlert);
        final View view = LayoutInflater.from(this).inflate(R.layout.alert_input_layout, null);
        AppCompatEditText editText = view.findViewById(R.id.alert_edit_view);
        editText.setFilters(new InputFilter[]{new InputFilter.AllCaps(), new LengthFilter(9)});
        editText.setInputType(InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
        editText.addTextChangedListener(new TextWatcher() {
            @Override
            public void afterTextChanged(final Editable editable) {
                if (editable.length() == 9 && alertDialog != null) {
                    alertDialog.getButton(DialogInterface.BUTTON_POSITIVE).performClick();
                }
            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (editText.getText() != null) {
                    int replaceIndex = editText.getSelectionEnd();
                    if (replaceIndex == 4 && start == 3) {
                        editText.setText(String.format("%s-", editText.getText().toString()));
                        editText.setSelection(editText.getText().length());
                    }
                    if (replaceIndex == 5 && !(s.charAt(s.length() - 1) == '-')) {
                        editText.getText().insert(4, "-");
                        editText.setSelection(editText.getText().length());
                    }
                }
            }
        });
        alert.setTitle(R.string.enter_code);
        alert.setView(view);
        alert.setPositiveButton(R.string.enter, (dialog, whichButton) -> {
            String code = Objects.requireNonNull(editText.getText()).toString();
            mAccountPresenter.onCodeEntered(code);
        });
        alert.setNegativeButton(R.string.cancel, (dialog, whichButton) -> dialog.dismiss());
        alertDialog = alert.create();
        editText.requestFocus();
        alertDialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
        alertDialog.show();
    }

    @Override
    public void showErrorDialog(final String error) {
        ErrorFragment.getInstance().add(error, this, R.id.fragment_container, true,
                ThemeUtils.getColor(this, R.attr.overlayDialogBackgroundColor, R.color.colorDeepBlue90));
    }

    @Override
    public void showErrorMessage(String errorMessage) {
        Toast.makeText(this, errorMessage, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void showProgress(String progressTitle) {
        mCustomProgressDialog.show();
        ((TextView) mCustomProgressDialog.findViewById(R.id.tv_dialog_header)).setText(progressTitle);
    }

    @Override
    public void showSuccessDialog(final String message) {
        SuccessFragment.getInstance().add(message, this, R.id.fragment_container, true,
                ThemeUtils.getColor(this, R.attr.overlayDialogBackgroundColor, R.color.colorDeepBlue90));
    }

    @OnClick({R.id.x_press_login_arrow, R.id.tv_x_press_login})
    public void xPressLoginClick() {
        mActivityLogger.info("User clicked XPress login button.");
        mAccountPresenter.onXPressLoginClicked();
    }
}
