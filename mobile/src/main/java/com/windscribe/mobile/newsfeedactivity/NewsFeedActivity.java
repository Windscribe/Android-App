/*
 * Copyright (c) 2021 Windscribe Limited.
 */

package com.windscribe.mobile.newsfeedactivity;


import static com.windscribe.vpn.constants.ExtraConstants.PROMO_EXTRA;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.windscribe.mobile.R;
import com.windscribe.mobile.adapter.NewsFeedAdapter;
import com.windscribe.mobile.base.BaseActivity;
import com.windscribe.mobile.custom_view.CustomDialog;
import com.windscribe.mobile.di.ActivityModule;
import com.windscribe.mobile.upgradeactivity.UpgradeActivity;
import com.windscribe.vpn.api.response.PushNotificationAction;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;

import butterknife.BindView;
import butterknife.OnClick;

public class NewsFeedActivity extends BaseActivity implements NewsFeedView {

    @Inject
    CustomDialog mCustomProgressDialog;

    @Inject
    NewsFeedPresenter mFeedPresenter;

    @BindView(R.id.recycler_view_news_feed)
    RecyclerView mNewsFeedRecyclerView;

    @BindView(R.id.tv_error)
    TextView tvError;

    private final String TAG = "news_feed_a";

    private final Logger mActivityLogger = LoggerFactory.getLogger(TAG);

    public static Intent getStartIntent(Context context, boolean showPopUp, int popUp) {
        Intent startIntent = new Intent(context, NewsFeedActivity.class);
        startIntent.putExtra("showPopUp", showPopUp);
        startIntent.putExtra("popUp", popUp);
        return startIntent;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setActivityModule(new ActivityModule(this, this)).inject(this);
        setContentLayout(R.layout.activity_news_feed,false);
        mNewsFeedRecyclerView.setItemAnimator(new DefaultItemAnimator());
        mNewsFeedRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        mFeedPresenter.init(getIntent().getBooleanExtra("showPopUp", false), getIntent().getIntExtra("popUp", -1));

    }

    @Override
    protected void onDestroy() {
        mFeedPresenter.onDestroy();
        super.onDestroy();
    }

    @Override
    public void hideProgress() {
        mActivityLogger.info("Hiding progress dialog.");
        mCustomProgressDialog.dismiss();
    }

    @OnClick(R.id.img_news_feed_close_btn)
    public void onCloseButtonClicked() {
        mActivityLogger.info("User clicked on close button.");
        onBackPressed();
    }

    @OnClick(R.id.tv_error)
    public void onErrorButtonClicked() {
        mActivityLogger.info("User clicked on error button.");
        mFeedPresenter.init(false, -1);
    }

    @Override
    public void setNewsFeedAdapter(NewsFeedAdapter mAdapter) {
        mActivityLogger.info("Setting news feed adapter.");
        mNewsFeedRecyclerView.setAdapter(mAdapter);
    }

    @Override
    public void showLoadingError(String errorMessage) {
        mActivityLogger.info("Showing loading error. Error message: " + errorMessage);
        tvError.setVisibility(View.VISIBLE);
        tvError.setText(errorMessage);
    }

    @Override
    public void showProgress(String progressTitle) {
        mActivityLogger.info("User clicked on error button.");
        mCustomProgressDialog.show();
        ((TextView) mCustomProgressDialog.findViewById(R.id.tv_dialog_header)).setText(progressTitle);
    }

    @Override
    public void startUpgradeActivity(final PushNotificationAction pushNotificationAction) {
        mActivityLogger.info("Promo action notification , Launching upgrade Activity.");
        Intent launchIntent = UpgradeActivity.getStartIntent(this);
        launchIntent.putExtra(PROMO_EXTRA, pushNotificationAction);
        startActivity(launchIntent);
    }
}
