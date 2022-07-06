/*
 * Copyright (c) 2021 Windscribe Limited.
 */

package com.windscribe.mobile.welcome;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.transition.Slide;
import android.view.Gravity;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.app.ShareCompat;
import androidx.core.content.FileProvider;
import androidx.core.view.GravityCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import com.windscribe.mobile.R;
import com.windscribe.mobile.alert.UnknownErrorAlert;
import com.windscribe.mobile.alert.UnknownErrorAlert.LoginAttemptFailedAlertInterface;
import com.windscribe.mobile.base.BaseActivity;
import com.windscribe.mobile.custom_view.ErrorFragment;
import com.windscribe.mobile.custom_view.ProgressFragment;
import com.windscribe.mobile.di.ActivityModule;
import com.windscribe.mobile.di.DaggerActivityComponent;
import com.windscribe.mobile.welcome.fragment.FragmentCallback;
import com.windscribe.mobile.welcome.fragment.LoginFragment;
import com.windscribe.mobile.welcome.fragment.NoEmailAttentionFragment;
import com.windscribe.mobile.welcome.fragment.SignUpFragment;
import com.windscribe.mobile.welcome.fragment.WelcomeActivityCallback;
import com.windscribe.mobile.welcome.fragment.WelcomeFragment;
import com.windscribe.mobile.windscribe.WindscribeActivity;
import com.windscribe.vpn.Windscribe;
import com.windscribe.vpn.constants.NetworkKeyConstants;

import java.io.File;

import javax.inject.Inject;

import butterknife.ButterKnife;


public class WelcomeActivity extends BaseActivity
        implements FragmentCallback, WelcomeView, LoginAttemptFailedAlertInterface {

    @Inject
    WelcomePresenter presenter;

    private final int REQUEST_PERMISSION_CODE = 201;

    private SoftInputAssist softInputAssist;

    public static Intent getStartIntent(Context context) {
        return new Intent(context, WelcomeActivity.class);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_welcome);
        DaggerActivityComponent.builder().activityModule(new ActivityModule(this, this))
                .applicationComponent(Windscribe.getAppContext()
                        .getApplicationComponent()).build().inject(this);
        ButterKnife.bind(this);
        addStartFragment();

        Uri uri = ActivityCompat.getReferrer(this);
        presenter.printStartUri(uri);

    }

    @Override
    protected void onResume() {
        super.onResume();
        softInputAssist.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        softInputAssist.onPause();
    }

    @Override
    protected void onDestroy() {
        softInputAssist.onDestroy();
        presenter.onDestroy();
        super.onDestroy();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
            @NonNull int[] grantResults) {
        if (requestCode == REQUEST_PERMISSION_CODE) {
            if (permissionGranted()) {
                presenter.exportLog();
            } else {
                showToast("Please provide storage permission");
            }
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    @Override
    public void clearInputErrors() {
        Fragment fragment = getSupportFragmentManager().findFragmentById(R.id.fragment_container);
        if (fragment instanceof WelcomeActivityCallback) {
            ((WelcomeActivityCallback) fragment).clearInputErrors();
        }
    }

    @Override
    public void contactSupport() {
        openURLInBrowser(NetworkKeyConstants.getWebsiteLink(NetworkKeyConstants.URL_HELP_ME));
    }

    @Override
    public void exportLog() {
        if (permissionGranted()) {
            presenter.exportLog();
        } else {
            askForPermission();
        }
    }

    @Override
    public void goToSignUp() {
        SignUpFragment signUpFragment = new SignUpFragment(false);
        int direction = GravityCompat
                .getAbsoluteGravity(GravityCompat.END, getResources().getConfiguration().getLayoutDirection());
        signUpFragment.setEnterTransition(new Slide(direction).addTarget(R.id.sign_up_container));
        replaceFragment(signUpFragment, true);
    }

    @Override
    public void gotoHomeActivity(boolean clearTop) {
        Intent startIntent = new Intent(this, WindscribeActivity.class);
        if (clearTop) {
            startIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        }
        startActivity(startIntent);
        finish();
    }

    @Override
    public void hideSoftKeyboard() {
        InputMethodManager imm = (InputMethodManager)
                getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) {
            imm.hideSoftInputFromWindow(getWindow()
                    .getDecorView().getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
        }
    }

    @Override
    public void launchShareIntent(File file) {
        Uri fileUri = FileProvider.getUriForFile(
                this,
                "com.windscribe.vpn.provider",
                file);
        ShareCompat.IntentBuilder.from(this)
                .setType("*/*")
                .setStream(fileUri).startChooser();
    }

    @Override
    public void onAccountClaimButtonClick(String username, String password, String email, boolean ignoreEmptyEmail) {
        presenter.startAccountClaim(username, password, email, ignoreEmptyEmail);
    }

    @Override
    public void onBackButtonPressed() {
        onBackPressed();
    }

    @Override
    public void onBackPressed() {
        presenter.onBackPressed();
        super.onBackPressed();
    }

    @Override
    public void onContinueWithOutAccountClick() {
        presenter.startGhostAccountSetup();
    }

    @Override
    public void onForgotPasswordClick() {
        openURLInBrowser(NetworkKeyConstants.getWebsiteLink(NetworkKeyConstants.URL_FORGOT_PASSWORD));
    }

    @Override
    public void onLoginButtonClick(String username, String password, String twoFa) {
        presenter.startLoginProcess(username, password, twoFa);
    }

    @Override
    public void onLoginClick() {
        LoginFragment loginFragment = new LoginFragment();
        int direction = GravityCompat
                .getAbsoluteGravity(GravityCompat.END, getResources().getConfiguration().getLayoutDirection());
        loginFragment.setEnterTransition(new Slide(direction).addTarget(R.id.login_container));
        replaceFragment(loginFragment, true);
    }

    @Override
    public void onSignUpButtonClick(String username, String password, String email, boolean ignoreEmptyEmail) {
        if (ignoreEmptyEmail) {
            getSupportFragmentManager().popBackStack();
        }
        presenter.startSignUpProcess(username, password, email, ignoreEmptyEmail);
    }

    @Override
    public void onSkipToHomeClick() {
        Intent startIntent = new Intent(this, WindscribeActivity.class);
        startActivity(startIntent);
        finish();
    }

    @Override
    public void prepareUiForApiCallFinished() {
        Fragment progressFragment = getSupportFragmentManager().findFragmentById(R.id.progress_container);
        if (progressFragment instanceof ProgressFragment | progressFragment instanceof NoEmailAttentionFragment) {
            getSupportFragmentManager().popBackStack();
        }
        Fragment mainFragment = getSupportFragmentManager().findFragmentById(R.id.fragment_container);
        if (mainFragment instanceof NoEmailAttentionFragment) {
            getSupportFragmentManager().popBackStack();
        }
    }

    @Override
    public void prepareUiForApiCallStart() {
        ProgressFragment.getInstance().add(this, R.id.progress_container, true);
    }

    public void replaceFragment(Fragment fragment, boolean addToBackStack) {
        FragmentTransaction transaction = getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, fragment);
        if (addToBackStack) {
            transaction.addToBackStack(fragment.getClass().getName());
        }
        transaction.commit();
    }

    @Override
    public void setEmailError(String errorMessage) {
        Fragment fragment = getSupportFragmentManager().findFragmentById(R.id.fragment_container);
        if (fragment instanceof SignUpFragment) {
            ((SignUpFragment) fragment).setEmailError(errorMessage);
        }
    }

    @Override
    public void setFaFieldsVisibility(int visibility) {
        Fragment fragment = getSupportFragmentManager().findFragmentById(R.id.fragment_container);
        if (fragment instanceof LoginFragment) {
            ((LoginFragment) fragment).setTwoFaVisibility(visibility);
        }
    }

    @Override
    public void setLoginRegistrationError(String error) {
        Fragment fragment = getSupportFragmentManager().findFragmentById(R.id.fragment_container);
        if (fragment instanceof WelcomeActivityCallback) {
            ((WelcomeActivityCallback) fragment).setLoginError(error);
        }
    }

    @Override
    public void setPasswordError(String error) {
        Fragment fragment = getSupportFragmentManager().findFragmentById(R.id.fragment_container);
        if (fragment instanceof WelcomeActivityCallback) {
            ((WelcomeActivityCallback) fragment).setPasswordError(error);
        }
    }

    @Override
    public void setTwoFaError(String errorMessage) {
        Fragment fragment = getSupportFragmentManager().findFragmentById(R.id.fragment_container);
        if (fragment instanceof LoginFragment) {
            ((LoginFragment) fragment).setTwoFaError(errorMessage);
        }
    }

    @Override
    public void setUsernameError(String error) {
        Fragment fragment = getSupportFragmentManager().findFragmentById(R.id.fragment_container);
        if (fragment instanceof WelcomeActivityCallback) {
            ((WelcomeActivityCallback) fragment).setUsernameError(error);
        }
    }

    public void setWindow() {
        int statusBarColor = getResources().getColor(android.R.color.transparent);
        getWindow().getDecorView()
                .setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);
        getWindow().setStatusBarColor(statusBarColor);
    }

    @Override
    public void showError(String error) {
        ErrorFragment.getInstance().add(error, this, R.id.fragment_container, true);
    }

    @Override
    public void showFailedAlert(String error) {
        runOnUiThread(() -> {
            UnknownErrorAlert unknownErrorAlert = UnknownErrorAlert.newInstance(error);
            unknownErrorAlert.show(getSupportFragmentManager(), "failed_login");
        });
    }

    @Override
    public void showNoEmailAttentionFragment(String username, String password, boolean accountClaim,
            boolean isUserPro) {
        NoEmailAttentionFragment noEmailAttentionFragment = new NoEmailAttentionFragment(accountClaim, username,
                password, isUserPro);
        noEmailAttentionFragment
                .setEnterTransition(new Slide(Gravity.BOTTOM).addTarget(R.id.email_fragment_container));
        getSupportFragmentManager().beginTransaction()
                .add(R.id.fragment_container, noEmailAttentionFragment)
                .addToBackStack(noEmailAttentionFragment.getClass().getName())
                .commit();
    }

    public void showToast(String error) {
        Toast.makeText(this, error, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void updateCurrentProcess(String call) {
        runOnUiThread(() -> {
            Fragment fragment = getSupportFragmentManager().findFragmentById(R.id.progress_container);
            if (fragment instanceof ProgressFragment) {
                ((ProgressFragment) fragment).updateProgressStatus(call);
            }
        });

    }

    void addStartFragment() {
        String startFragmentName = getIntent().getStringExtra("startFragmentName");
        boolean skipToHome = getIntent().getBooleanExtra("skipToHome", false);
        Fragment fragment;
        if (startFragmentName != null && startFragmentName.equals("Login")) {
            fragment = new LoginFragment();
            softInputAssist = new SoftInputAssist(this, new int[]{R.id.forgot_password, R.id.page_description});
        } else if (startFragmentName != null && startFragmentName.equals("SignUp")) {
            softInputAssist = new SoftInputAssist(this, new int[]{R.id.forgot_password, R.id.page_description});
            fragment = new SignUpFragment(false);
        } else if (startFragmentName != null && startFragmentName.equals("AccountSetUp")) {
            softInputAssist = new SoftInputAssist(this,
                    new int[]{R.id.forgot_password, R.id.page_description, R.id.set_up_later_button});
            boolean proAccount = presenter.isUserPro();
            fragment = new SignUpFragment(proAccount);
        } else {
            softInputAssist = new SoftInputAssist(this, new int[]{R.id.forgot_password, R.id.page_description});
            fragment = new WelcomeFragment();
        }
        Bundle bundle = new Bundle();
        bundle.putString("startFragmentName", startFragmentName);
        bundle.putBoolean("skipToHome", skipToHome);
        fragment.setArguments(bundle);
        int direction = GravityCompat
                .getAbsoluteGravity(GravityCompat.END, getResources().getConfiguration().getLayoutDirection());
        fragment.setEnterTransition(new Slide(direction).addTarget(R.id.welcome_container));
        replaceFragment(fragment, false);
    }

    void askForPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_PERMISSION_CODE);
        }
    }

    boolean permissionGranted() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    == PackageManager.PERMISSION_GRANTED;
        } else {
            return true;
        }
    }
}