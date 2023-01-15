/*
 * Copyright (c) 2021 Windscribe Limited.
 */
package com.windscribe.tv.serverlist.detail

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import com.windscribe.vpn.Windscribe.Companion.appContext
import com.windscribe.tv.disconnectalert.DisconnectActivity.Companion.getIntent
import butterknife.BindView
import javax.inject.Inject
import androidx.leanback.widget.VerticalGridView
import com.bumptech.glide.Glide
import com.windscribe.tv.R
import com.windscribe.tv.base.BaseActivity
import com.windscribe.tv.di.ActivityModule
import com.windscribe.tv.serverlist.adapters.DetailViewAdapter
import com.windscribe.tv.serverlist.overlay.LoadState
import com.windscribe.tv.windscribe.WindscribeActivity
import org.slf4j.LoggerFactory

class DetailActivity : BaseActivity(), DetailView {
    @JvmField
    @BindView(R.id.detailCount)
    var detailCount: TextView? = null

    @JvmField
    @BindView(R.id.detailTitle)
    var detailTitle: TextView? = null

    @JvmField
    @BindView(R.id.detailRecycleView)
    var detailViewRecycleView: VerticalGridView? = null

    @JvmField
    @BindView(R.id.imageBackground)
    var imageBackground: ImageView? = null

    @JvmField
    @BindView(R.id.state_layout)
    var stateLayout: TextView? = null

    @Inject
    lateinit var presenter: DetailPresenter
    private val logger = LoggerFactory.getLogger("detail:a")
    private var fragmentTag = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setActivityModule(ActivityModule(this, this)).inject(this)
        setContentLayout(R.layout.activity_detail)
        detailViewRecycleView?.setNumColumns(1)
        if (intent != null) {
            fragmentTag = intent.getStringExtra("fragment_tag")?.toInt() ?: 1
            presenter.init(intent.getIntExtra(SELECTED_ID, -1))
        }
    }

    override fun onDestroy() {
        presenter.onDestroy()
        super.onDestroy()
    }

    override fun onBackPressed() {
        logger.debug("Closing detail view from back")
        setResult(RESULT_CANCELED)
        finish()
    }

    override fun onDisabledNodeClick() {
        appContext.startActivity(
            getIntent(
                appContext,
                getString(R.string.node_under_construction_text),
                "Alert"
            )
        )
    }

    override fun onNodeSelected(cityID: Int) {
        logger.debug("Closing detail view from connect.")
        val startIntent = Intent(this, WindscribeActivity::class.java)
        startIntent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        startIntent.putExtra("city", cityID)
        startIntent.putExtra("connectingToStaticIP", false)
        startActivity(startIntent)
    }

    override fun setCount(count: String) {
        detailCount?.text = count
    }

    override fun setCountryFlagBackground(flagIconResource: Int) {
        Glide.with(this@DetailActivity)
            .load(ContextCompat.getDrawable(this, flagIconResource))
            .dontAnimate()
            .into(imageBackground!!)
    }

    override fun setDetailAdapter(detailAdapter: DetailViewAdapter) {
        detailViewRecycleView?.adapter = detailAdapter
    }

    override fun setState(state: LoadState, stateDrawable: Int, stateText: Int) {
        val selectedStateListDrawable: Int = if (fragmentTag == 1) {
            R.drawable.ic_all_icon
        } else {
            R.drawable.ic_flix_icon
        }
        when (state) {
            LoadState.Loaded -> stateLayout?.visibility = View.GONE
            LoadState.NoResult, LoadState.Error, LoadState.Loading -> {
                stateLayout?.visibility = View.VISIBLE
                stateLayout?.text = getString(stateText)
                stateLayout?.setCompoundDrawablesWithIntrinsicBounds(
                    null,
                    ResourcesCompat.getDrawable(resources, selectedStateListDrawable, theme), null, null
                )
            }
        }
    }

    override fun setTitle(text: String) {
        detailTitle?.text = text
    }

    override fun showToast(text: String) {
        Toast.makeText(this, text, Toast.LENGTH_SHORT).show()
    }

    companion object {
        const val SELECTED_ID = "selected_id"
    }
}