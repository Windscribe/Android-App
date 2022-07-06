/*
 * Copyright (c) 2021 Windscribe Limited.
 */

package com.windscribe.mobile.splash;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;

import com.windscribe.mobile.R;
import com.windscribe.mobile.base.BaseActivity;
import com.windscribe.mobile.di.ActivityModule;
import com.windscribe.mobile.di.DaggerActivityComponent;
import com.windscribe.mobile.welcome.WelcomeActivity;
import com.windscribe.mobile.windscribe.WindscribeActivity;
import com.windscribe.vpn.Windscribe;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;

import butterknife.ButterKnife;


@SuppressLint("CustomSplashScreen")
public class SplashActivity extends BaseActivity implements SplashView {

    private static final String TAG = "splash_a";

    @Inject
    SplashPresenter mPresenter;

    private final Logger mActivityLogger = LoggerFactory.getLogger(TAG);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        DaggerActivityComponent.builder().activityModule(new ActivityModule(this, this))
                .applicationComponent(Windscribe.getAppContext()
                        .getApplicationComponent()).build().inject(this);
        setContentView(R.layout.activity_splash);

        mActivityLogger.info("OnCreate: Splash Activity");
        mPresenter.checkNewMigration();

    }


    @Override
    protected void onDestroy() {
        mPresenter.onDestroy();
        super.onDestroy();
    }

    @Override
    public boolean isConnectedToNetwork() {
        return isNetworkPresent();
    }

    @Override
    public void navigateToAccountSetUp() {
        mActivityLogger.info("Navigating to account set up activity...");
        Intent intent = new Intent(this, WelcomeActivity.class);
        intent.putExtra("startFragmentName", "AccountSetUp");
        intent.putExtra("skipToHome", true);
        startActivity(intent);
        finish();
    }

    @Override
    public void navigateToHome() {
        mActivityLogger.info("Navigating to home activity...");
        Intent homeIntent = new Intent(this, WindscribeActivity.class);
        if (getIntent().getExtras() != null) {
            mActivityLogger.debug("Forwarding intent extras home activity.");
            homeIntent.putExtras(getIntent().getExtras());
        }
        startActivity(homeIntent);
        finish();
    }

    @Override
    public void navigateToLogin() {
        mActivityLogger.info("Navigating to login activity...");
        Intent loginIntent = new Intent(this, WelcomeActivity.class);
        startActivity(loginIntent);
        finish();
    }
}
