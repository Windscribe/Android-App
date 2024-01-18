/*
 * Copyright (c) 2021 Windscribe Limited.
 */
package com.windscribe.mobile.help

import android.annotation.SuppressLint
import android.app.ActivityOptions
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.constraintlayout.widget.ConstraintLayout
import butterknife.BindView
import butterknife.OnClick
import com.windscribe.mobile.R
import com.windscribe.mobile.base.BaseActivity
import com.windscribe.mobile.custom_view.preferences.MultipleLinkExplainView
import com.windscribe.mobile.custom_view.preferences.SingleLinkExplainView
import com.windscribe.mobile.debug.DebugViewActivity.Companion.getStartIntent
import com.windscribe.mobile.di.ActivityModule
import com.windscribe.mobile.ticket.SendTicketActivity
import com.windscribe.mobile.utils.UiUtil
import javax.inject.Inject

class HelpActivity : BaseActivity(), HelpView {
    @Inject
    lateinit var presenter: HelpPresenter

    @BindView(R.id.debug_progress)
    lateinit var imgProgress: ProgressBar

    @BindView(R.id.tv_send_label)
    lateinit var sendDebugLogLabel: TextView

    @BindView(R.id.cl_debug_send)
    lateinit var sendDebugView: ConstraintLayout

    @BindView(R.id.tv_debug_progress_label)
    lateinit var labelProgress: TextView

    @BindView(R.id.nav_title)
    lateinit var tvActivityTitle: TextView

    @BindView(R.id.tv_view_label)
    lateinit var debugViewLabel: TextView

    @BindView(R.id.cl_debug_view)
    lateinit var debugView: ConstraintLayout

    @BindView(R.id.sendTicket)
    lateinit var sendTicketView: SingleLinkExplainView

    private var logSent = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setActivityModule(ActivityModule(this, this)).inject(this)
        setContentLayout(R.layout.activity_help, true)
        presenter.init()
        addClickListeners()
        activityScope {
            presenter.observeUserStatus()
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun addClickListeners() {
        val knowledgeBtn = findViewById<SingleLinkExplainView>(R.id.knowledge)
        knowledgeBtn.onClick { presenter.onKnowledgeBaseClick() }
        val garryBtn = findViewById<SingleLinkExplainView>(R.id.garry)
        garryBtn.onClick { presenter.onGarryClick() }
        val ticketBtn = findViewById<SingleLinkExplainView>(R.id.sendTicket)
        ticketBtn.onClick { presenter.onSendTicketClick() }
        val communityBtn = findViewById<MultipleLinkExplainView>(R.id.community)
        communityBtn.onFirstItemClick { presenter.onRedditClick() }
        communityBtn.onSecondItemClick { presenter.onDiscordClick() }
        UiUtil.setupOnTouchListener(container = debugView, textView = debugViewLabel)
        UiUtil.setupOnTouchListener(container = sendDebugView, textView = sendDebugLogLabel)
    }

    override fun goToSendTicket() {
        startActivity(SendTicketActivity.getStartIntent(this))
    }

    @OnClick(R.id.nav_button)
    fun onBackButtonPressed() {
        super.onBackPressed()
    }

    @OnClick(R.id.cl_debug_send, R.id.tv_send_label)
    fun onSendDebugClicked() {
        if (logSent.not()) {
            presenter.onSendDebugClicked()
        }
    }

    @OnClick(R.id.cl_debug_view)
    fun onViewLogClicked() {
        val intent = getStartIntent(this, false)
        val options = ActivityOptions.makeSceneTransitionAnimation(this)
        startActivity(intent, options.toBundle())
    }

    override fun openInBrowser(url: String) {
        openURLInBrowser(url)
    }

    override fun setActivityTitle(title: String) {
        tvActivityTitle.text = title
    }

    override fun showProgress(inProgress: Boolean, success: Boolean) {
        if (inProgress) {
            imgProgress.visibility = View.VISIBLE
            labelProgress.visibility = View.INVISIBLE
            sendDebugLogLabel.text = getString(R.string.sending_log)
        } else {
            labelProgress.visibility = View.VISIBLE
            val msg =
                if (success) resources.getString(R.string.sent_thanks) else getString(R.string.error_try_again)
            labelProgress.text = msg
            imgProgress.visibility = View.INVISIBLE
            sendDebugLogLabel.text = getString(R.string.send_log)
            logSent = true
        }
    }

    override fun setSendTicketVisibility(visible: Boolean) {
        if (visible) {
            sendTicketView.visibility = View.VISIBLE
        } else {
            sendTicketView.visibility = View.GONE
        }
    }

    override fun showToast(message: String) {
        runOnUiThread { Toast.makeText(this@HelpActivity, message, Toast.LENGTH_SHORT).show() }
    }

    companion object {
        fun getStartIntent(context: Context?): Intent {
            return Intent(context, HelpActivity::class.java)
        }
    }
}