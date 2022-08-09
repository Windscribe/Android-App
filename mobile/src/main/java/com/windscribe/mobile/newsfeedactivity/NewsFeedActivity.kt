/*
 * Copyright (c) 2021 Windscribe Limited.
 */
package com.windscribe.mobile.newsfeedactivity

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import butterknife.BindView
import butterknife.OnClick
import com.windscribe.mobile.R
import com.windscribe.mobile.adapter.NewsFeedAdapter
import com.windscribe.mobile.base.BaseActivity
import com.windscribe.mobile.custom_view.CustomDialog
import com.windscribe.mobile.di.ActivityModule
import com.windscribe.mobile.upgradeactivity.UpgradeActivity
import com.windscribe.vpn.api.response.PushNotificationAction
import com.windscribe.vpn.constants.ExtraConstants.PROMO_EXTRA
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import javax.inject.Inject

class NewsFeedActivity : BaseActivity(), NewsFeedView {
    @Inject
    lateinit var customProgressDialog: CustomDialog

    @Inject
    lateinit var presenter: NewsFeedPresenter

    @BindView(R.id.recycler_view_news_feed)
    lateinit var newsFeedRecyclerView: RecyclerView

    @BindView(R.id.tv_error)
    lateinit var tvError: TextView

    val logger: Logger = LoggerFactory.getLogger("news_feed_a")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setActivityModule(ActivityModule(this, this)).inject(this)
        setContentLayout(R.layout.activity_news_feed, false)
        newsFeedRecyclerView.itemAnimator = DefaultItemAnimator()
        newsFeedRecyclerView.layoutManager = LinearLayoutManager(this)
        presenter.init(
            intent.getBooleanExtra("showPopUp", false),
            intent.getIntExtra("popUp", -1)
        )
    }

    override fun onDestroy() {
        presenter.onDestroy()
        super.onDestroy()
    }

    override fun hideProgress() {
        logger.info("Hiding progress dialog.")
        customProgressDialog.dismiss()
    }

    @OnClick(R.id.img_news_feed_close_btn)
    fun onCloseButtonClicked() {
        logger.info("User clicked on close button.")
        onBackPressed()
    }

    @OnClick(R.id.tv_error)
    fun onErrorButtonClicked() {
        logger.info("User clicked on error button.")
        presenter.init(false, -1)
    }

    override fun setNewsFeedAdapter(mAdapter: NewsFeedAdapter) {
        logger.info("Setting news feed adapter.")
        newsFeedRecyclerView.adapter = mAdapter
    }

    override fun showLoadingError(errorMessage: String) {
        logger.info("Showing loading error. Error message: $errorMessage")
        tvError.visibility = View.VISIBLE
        tvError.text = errorMessage
    }

    override fun showProgress(progressTitle: String) {
        logger.info("User clicked on error button.")
        customProgressDialog.show()
        (customProgressDialog.findViewById<View>(R.id.tv_dialog_header) as TextView).text =
            progressTitle
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