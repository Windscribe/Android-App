/*
 * Copyright (c) 2021 Windscribe Limited.
 */
package com.windscribe.mobile.networksecurity

import android.app.ActivityOptions
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import butterknife.BindView
import butterknife.OnClick
import com.windscribe.mobile.R
import com.windscribe.mobile.adapter.NetworkListAdapter
import com.windscribe.mobile.base.BaseActivity
import com.windscribe.mobile.custom_view.CustomDialog
import com.windscribe.mobile.custom_view.preferences.ToggleView
import com.windscribe.mobile.di.ActivityModule
import com.windscribe.mobile.networksecurity.networkdetails.NetworkDetailsActivity.Companion.getStartIntent
import com.windscribe.mobile.networksecurity.viewholder.NetworkAdapterActionListener
import com.windscribe.vpn.Windscribe.Companion.appContext
import com.windscribe.vpn.backend.utils.WindVpnController
import com.windscribe.vpn.localdatabase.tables.NetworkInfo
import org.slf4j.LoggerFactory
import javax.inject.Inject

class NetworkSecurityActivity : BaseActivity(), NetworkSecurityView, NetworkAdapterActionListener {

    @BindView(R.id.nav_title)
    lateinit var activityTitleView: TextView

    @BindView(R.id.recycler_view_network_list)
    lateinit var networkListRecyclerView: RecyclerView

    @BindView(R.id.auto_secure_new_networks)
    lateinit var autoSecureNewNetworksToggleView: ToggleView

    @BindView(R.id.network_name)
    lateinit var currentNetworkName: TextView

    @BindView(R.id.tv_current_protection)
    lateinit var currentNetworkProtection: TextView

    @BindView(R.id.cl_current_network)
    lateinit var clCurrentNetwork: ConstraintLayout

    @BindView(R.id.tv_no_network_list)
    lateinit var tvNoNetworkFound: TextView

    @Inject
    lateinit var customProgress: CustomDialog

    @Inject
    lateinit var vpnController: WindVpnController

    @Inject
    lateinit var presenter: NetworkSecurityPresenter

    private val logger = LoggerFactory.getLogger("net_security_a")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setActivityModule(ActivityModule(this, this)).inject(this)
        setContentLayout(R.layout.activity_network_security, true)
        networkListRecyclerView.layoutManager =
            GridLayoutManager(this, 1)
        activityTitleView.text = getString(R.string.network_options)
        presenter.init()
        setCustomLayoutDelegates()
    }

    private fun setCustomLayoutDelegates() {
        autoSecureNewNetworksToggleView.delegate = object : ToggleView.Delegate {
            override fun onToggleClick() {
                presenter.onAutoSecureToggleClick()
            }

            override fun onExplainClick() {}
        }
    }

    override fun setAutoSecureToggle(resourceId: Int) {
        autoSecureNewNetworksToggleView.setToggleImage(resourceId)
    }

    override fun onResume() {
        super.onResume()
        presenter.setupNetworkListAdapter()
    }

    override fun onDestroy() {
        presenter.onDestroy()
        super.onDestroy()
    }

    override fun hideProgress() {
        logger.info("Dismissing progress dialog...")
        customProgress.dismiss()
    }

    @OnClick(R.id.cl_current_network)
    fun onCurrentNetworkClick() {
        presenter.onCurrentNetworkClick()
    }

    override fun setupCurrentNetwork(networkInfo: NetworkInfo) {
        clCurrentNetwork.visibility = View.VISIBLE
        currentNetworkName.text = networkInfo.networkName
        val protectionStatus = if (networkInfo.isAutoSecureOn) appContext
            .getText(R.string.network_secured)
            .toString() else appContext.getText(R.string.network_unsecured).toString()
        currentNetworkProtection.text = protectionStatus
    }

    override fun onAdapterLoadFailed(showUpdate: String) {
        tvNoNetworkFound.visibility = View.VISIBLE
        tvNoNetworkFound.text = showUpdate
    }

    @OnClick(R.id.nav_button)
    fun onBackArrowClicked() {
        logger.info("User clicked back arrow...")
        onBackPressed()
    }

    override fun onItemSelected(networkInfo: NetworkInfo) {
        logger.info("User selected " + networkInfo.networkName)
        presenter.onNetworkSecuritySelected(networkInfo)
    }

    override fun openNetworkSecurityDetails(networkName: String) {
        val intent = getStartIntent(this, networkName)
        val options = ActivityOptions.makeSceneTransitionAnimation(this)
        startActivity(intent, options.toBundle())
    }

    override fun setAdapter(mNetworkList: List<NetworkInfo>?) {
        val mAdapter = NetworkListAdapter(mNetworkList)
        networkListRecyclerView.adapter = mAdapter
        networkListRecyclerView.itemAnimator = DefaultItemAnimator()
        mAdapter.setAdapterActionListener(this)
        presenter.onAdapterSet()
    }

    override fun showProgress(progressTitle: String) {
        logger.info("Showing loading dialog...")
        customProgress.show()
        (customProgress.findViewById<View>(R.id.tv_dialog_header) as TextView).text =
            progressTitle
    }

    companion object {
        fun getStartIntent(context: Context?): Intent {
            return Intent(context, NetworkSecurityActivity::class.java)
        }
    }
}