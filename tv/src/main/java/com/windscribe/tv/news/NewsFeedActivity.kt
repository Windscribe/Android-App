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
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.LinearLayoutManager
import com.windscribe.tv.R
import com.windscribe.tv.adapter.NewsFeedAdapter
import com.windscribe.tv.base.BaseActivity
import com.windscribe.tv.customview.CustomDialog
import com.windscribe.tv.databinding.ActivityNewsFeedBinding
import com.windscribe.tv.di.ActivityModule
import com.windscribe.tv.upgrade.UpgradeActivity
import com.windscribe.vpn.api.response.PushNotificationAction
import com.windscribe.vpn.constants.ExtraConstants.PROMO_EXTRA
import com.windscribe.vpn.localdatabase.tables.NewsfeedAction
import org.slf4j.LoggerFactory
import javax.inject.Inject

class NewsFeedActivity : BaseActivity(), NewsFeedView {
    @JvmField
    @Inject
    var customProgressDialog: CustomDialog? = null

    @Inject
    lateinit var presenter: NewsFeedPresenter
    private var newsFeedAdapter: NewsFeedAdapter? = null

    private lateinit var binding: ActivityNewsFeedBinding
    private val logger = LoggerFactory.getLogger("basic")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setActivityModule(ActivityModule(this, this)).inject(this)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_news_feed)
        setupUI()
    }

    private fun setupUI() {
        binding.newsFeedRecycleView.itemAnimator = DefaultItemAnimator()
        binding.newsFeedRecycleView.layoutManager = LinearLayoutManager(this)
        presenter.init(
            intent.getBooleanExtra("showPopUp", false),
            intent.getIntExtra("popUp", -1)
        )
        binding.actionLabel.setOnClickListener {
            val action = binding.actionLabel.getTag(R.id.action_label)
            if (action is NewsfeedAction) {
                presenter.onActionClick(action)
            }
        }
    }

    override fun onDestroy() {
        presenter.onDestroy()
        super.onDestroy()
    }

    override fun setActionLabel(action: NewsfeedAction) {
        binding.actionLabel.text = action.label
        binding.actionLabel.setTag(R.id.action_label, action)
        binding.actionLabel.visibility = View.VISIBLE
    }

    override fun hideActionLabel() {
        binding.actionLabel.visibility = View.GONE
    }

    override fun setItemSelected(notificationId: Int) {
        newsFeedAdapter?.setItemSelected(notificationId)
    }

    override fun setNewsFeedAdapter(mAdapter: NewsFeedAdapter) {
        logger.info("Setting news feed adapter.")
        newsFeedAdapter = mAdapter
        binding.newsFeedRecycleView.adapter = mAdapter
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
        binding.newsFeedContentTextView.text = spannable
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
