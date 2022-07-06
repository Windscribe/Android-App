/*
 * Copyright (c) 2021 Windscribe Limited.
 */

package com.windscribe.mobile.help;

import android.app.ActivityOptions;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.windscribe.mobile.R;
import com.windscribe.mobile.base.BaseActivity;
import com.windscribe.mobile.debug.DebugViewActivity;
import com.windscribe.mobile.di.ActivityModule;
import com.windscribe.mobile.ticket.SendTicketActivity;

import javax.inject.Inject;

import butterknife.BindView;
import butterknife.OnClick;

public class HelpActivity extends BaseActivity implements HelpView {

    @Inject
    HelpPresenter helpPresenter;

    @BindView(R.id.debug_progress)
    ProgressBar imgProgress;

    @BindView(R.id.tv_send_label)
    TextView labelLog;

    @BindView(R.id.tv_debug_progress_label)
    TextView labelProgress;

    @BindView(R.id.nav_title)
    TextView tvActivityTitle;

    private boolean logSent = false;

    public static Intent getStartIntent(Context context) {
        return new Intent(context, HelpActivity.class);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setActivityModule(new ActivityModule(this, this)).inject(this);
        setContentLayout(R.layout.activity_help,true);
        helpPresenter.init();
    }

    @Override
    public void goToSendTicket() {
        startActivity(SendTicketActivity.getStartIntent(this));
    }

    @OnClick(R.id.nav_button)
    public void onBackButtonPressed() {
        super.onBackPressed();
    }

    @OnClick(R.id.discord_container)
    public void onDiscordClick() {
        helpPresenter.onDiscordClick();
    }

    @OnClick(R.id.garry_container)
    public void onGarryClick() {
        helpPresenter.onGarryClick();
    }

    @OnClick(R.id.knowledge_base_container)
    public void onKnowledgeBaseClick() {
        helpPresenter.onKnowledgeBaseClick();
    }

    @OnClick(R.id.reddit_container)
    public void onRedditClick() {
        helpPresenter.onRedditClick();
    }

    @OnClick({R.id.cl_debug_send, R.id.tv_send_label})
    public void onSendDebugClicked() {
        if (!logSent) {
            helpPresenter.onSendDebugClicked();
        }
    }

    @OnClick(R.id.ticket_container)
    public void onSendTicketClick() {
        helpPresenter.onSendTicketClick();
    }

    @OnClick({R.id.cl_debug_view})
    public void onViewLogClicked() {
        Intent intent = DebugViewActivity.getStartIntent(this, false);
        ActivityOptions options = ActivityOptions.makeSceneTransitionAnimation(this);
        startActivity(intent, options.toBundle());
    }

    @Override
    public void openInBrowser(String url) {
        openURLInBrowser(url);
    }

    @Override
    public void setActivityTitle(String title) {
        tvActivityTitle.setText(title);
    }

    @Override
    public void showProgress(boolean inProgress, boolean success) {
        if (inProgress) {
            imgProgress.setVisibility(View.VISIBLE);
            labelProgress.setVisibility(View.INVISIBLE);
            labelLog.setText(getString(R.string.sending_log));
        } else {
            labelProgress.setVisibility(View.VISIBLE);
            String msg = success ? getResources().getString(R.string.sent_thanks)
                    : getString(R.string.error_try_again);
            labelProgress.setText(msg);
            imgProgress.setVisibility(View.INVISIBLE);
            labelLog.setText(getString(R.string.send_log));
            logSent = true;
        }
    }

    @Override
    public void showToast(String message) {
        runOnUiThread(() -> Toast.makeText(HelpActivity.this, message, Toast.LENGTH_SHORT).show());
    }
}