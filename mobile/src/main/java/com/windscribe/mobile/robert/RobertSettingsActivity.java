/*
 * Copyright (c) 2021 Windscribe Limited.
 */

package com.windscribe.mobile.robert;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.windscribe.mobile.R;
import com.windscribe.mobile.adapter.RobertSettingsAdapter;
import com.windscribe.mobile.base.BaseActivity;
import com.windscribe.mobile.custom_view.ErrorFragment;
import com.windscribe.mobile.custom_view.ProgressFragment;
import com.windscribe.mobile.di.ActivityModule;

import javax.inject.Inject;

import butterknife.BindView;
import butterknife.OnClick;

public class RobertSettingsActivity extends BaseActivity implements RobertSettingsView {

    @BindView(R.id.cl_custom_rules)
    ConstraintLayout clCustomRules;

    @BindView(R.id.custom_rules_divider)
    ImageView customRulesDivider;

    @BindView(R.id.tv_learn_more)
    TextView learnMoreView;

    @BindView(R.id.nav_title)
    TextView mActivityTitleView;

    @BindView(R.id.custom_rules_arrow)
    ImageView mCustomRulesArrow;

    @BindView(R.id.custom_rules_progress)
    ProgressBar mCustomRulesProgressView;

    @Inject
    RobertSettingsPresenter mPresenter;

    @BindView(R.id.recycle_settings_view)
    RecyclerView mRecyclerSettingsView;

    public static Intent getStartIntent(Context context) {
        return new Intent(context, RobertSettingsActivity.class);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setActivityModule(new ActivityModule(this, this)).inject(this);
        setContentLayout(R.layout.activity_robert_settings,true);
        mPresenter.init();
    }

    @Override
    protected void onDestroy() {
        mPresenter.onDestroy();
        super.onDestroy();
    }

    @Override
    public void hideProgress() {
        Fragment fragment = getSupportFragmentManager().findFragmentById(R.id.cl_robert);
        if (fragment instanceof ProgressFragment) {
            ((ProgressFragment) fragment).finishProgress();
        }
    }

    @Override
    public void openUrl(final String url) {
        openURLInBrowser(url);
    }

    @Override
    public void setAdapter(final RobertSettingsAdapter robertSettingsAdapter) {
        mRecyclerSettingsView.setLayoutManager(new LinearLayoutManager(this));
        mRecyclerSettingsView.setAdapter(robertSettingsAdapter);
    }

    @Override
    public void setTitle(final String title) {
        mActivityTitleView.setText(title);
    }

    @Override
    public void setWebSessionLoading(final boolean show) {
        mCustomRulesArrow.setVisibility(show ? View.GONE : View.VISIBLE);
        mCustomRulesProgressView.setVisibility(show ? View.VISIBLE : View.GONE);
        clCustomRules.setEnabled(!show);
    }

    @Override
    public void showError(final String error) {
        ErrorFragment.getInstance().add(error, this, R.id.cl_robert, false);
    }

    @Override
    public void showErrorDialog(final String error) {
        ErrorFragment.getInstance().add(error, this, R.id.cl_robert, true);
    }

    @Override
    public void showProgress() {
        ProgressFragment.getInstance().add(this, R.id.cl_robert, true);
    }

    @Override
    public void showToast(final String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    @OnClick(R.id.nav_button)
    protected void onBackButtonClick() {
        onBackPressed();
    }

    @OnClick(R.id.cl_custom_rules)
    protected void onCustomRulesClick() {
        mPresenter.onCustomRulesClick();
    }

    @OnClick(R.id.cl_learn_more)
    protected void onLearnMoreClick() {
        mPresenter.onLearnMoreClick();
    }
}