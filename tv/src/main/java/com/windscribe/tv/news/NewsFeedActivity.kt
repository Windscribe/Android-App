/*
 * Copyright (c) 2021 Windscribe Limited.
 */
package com.windscribe.tv.news

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.text.Html
import android.text.Spannable
import android.text.SpannableString
import android.text.Spanned
import android.text.style.URLSpan
import android.view.View
import android.widget.TextView
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import butterknife.BindView
import butterknife.OnClick
import com.windscribe.tv.R
import com.windscribe.tv.adapter.NewsFeedAdapter
import com.windscribe.tv.base.BaseActivity
import com.windscribe.tv.customview.CustomDialog
import com.windscribe.tv.di.ActivityModule
import com.windscribe.tv.upgrade.UpgradeActivity
import com.windscribe.vpn.api.response.PushNotificationAction
import com.windscribe.vpn.constants.ExtraConstants.PROMO_EXTRA
import com.windscribe.vpn.localdatabase.tables.NewsfeedAction
import org.slf4j.LoggerFactory
import javax.inject.Inject

class NewsFeedActivity : BaseActivity(), NewsFeedView {
    @JvmField
    @BindView(R.id.action_label)
    var actionLabel: TextView? = null

    @JvmField
    @Inject
    var customProgressDialog: CustomDialog? = null

    @JvmField
    @BindView(R.id.newsFeedContentTextView)
    var newsFeedContentTextView: TextView? = null

    @JvmField
    @BindView(R.id.newsFeedRecycleView)
    var newsFeedRecyclerView: RecyclerView? = null

    @Inject
    lateinit var presenter: NewsFeedPresenter
    private var newsFeedAdapter: NewsFeedAdapter? = null

    private val logger = LoggerFactory.getLogger("news_feed_a")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setActivityModule(ActivityModule(this, this)).inject(this)
        setContentLayout(R.layout.activity_news_feed)
        newsFeedRecyclerView?.itemAnimator = DefaultItemAnimator()
        newsFeedRecyclerView?.layoutManager = LinearLayoutManager(this)
        presenter.init(
            intent.getBooleanExtra("showPopUp", false),
            intent.getIntExtra("popUp", -1)
        )
    }

    override fun onDestroy() {
        presenter.onDestroy()
        super.onDestroy()
    }

    @OnClick(R.id.action_label)
    fun onActionClick() {
        val action = actionLabel?.getTag(R.id.action_label)
        if (action is NewsfeedAction) {
            presenter.onActionClick(action)
        }
    }

    override fun setActionLabel(action: NewsfeedAction) {
        actionLabel?.text = action.label
        actionLabel?.setTag(R.id.action_label, action)
        actionLabel?.visibility = View.VISIBLE
    }

    override fun setItemSelected(notificationId: Int) {
        newsFeedAdapter?.setItemSelected(notificationId)
    }

    override fun setNewsFeedAdapter(mAdapter: NewsFeedAdapter) {
        logger.info("Setting news feed adapter.")
        newsFeedAdapter = mAdapter
        newsFeedRecyclerView?.adapter = mAdapter
    }

    override fun setNewsFeedContentText(contentText: String) {
        val spanned: Spanned = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            Html.fromHtml(contentText, Html.FROM_HTML_MODE_LEGACY)
        } else {
            Html.fromHtml(contentText)
        }
        val spannable: Spannable = SpannableString(spanned)
        val spans = spannable.getSpans(0, spannable.length, URLSpan::class.java)
        for (span in spans) {
            spannable.removeSpan(span)
        }
        newsFeedContentTextView?.text = spannable
    }

    override fun showLoadingError(errorMessage: String) {
        logger.info("Showing loading error. Error message: $errorMessage")
    }

    override fun startUpgradeActivity(pushNotificationAction: PushNotificationAction) {
        logger.info("Promo action notification , Launching upgrade Activity.")
        val launchIntent = UpgradeActivity.getStartIntent(this)
        launchIntent.putExtra(PROMO_EXTRA, pushNotificationAction)
        startActivity(launchIntent)
    }

    companion object {
        fun getStartIntent(context: Context?, showPopUp: Boolean, popUp: Int): Intent {
            val startIntent = Intent(context, NewsFeedActivity::class.java)
            startIntent.putExtra("showPopUp", showPopUp)
            startIntent.putExtra("popUp", popUp)
            return startIntent
        }
    }
}
