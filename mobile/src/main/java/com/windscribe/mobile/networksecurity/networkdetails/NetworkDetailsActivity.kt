/*
 * Copyright (c) 2021 Windscribe Limited.
 */
package com.windscribe.mobile.networksecurity.networkdetails

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.constraintlayout.widget.ConstraintLayout
import butterknife.BindView
import butterknife.OnClick
import com.windscribe.mobile.R
import com.windscribe.mobile.base.BaseActivity
import com.windscribe.mobile.custom_view.preferences.ConnectionModeView
import com.windscribe.mobile.custom_view.preferences.ExpandableToggleView
import com.windscribe.mobile.custom_view.preferences.ToggleView
import com.windscribe.mobile.di.ActivityModule
import com.windscribe.vpn.localdatabase.tables.NetworkInfo
import javax.inject.Inject

class NetworkDetailsActivity : BaseActivity(), NetworkDetailView {

    @JvmField
    @BindView(R.id.cl_error)
    var clError: ConstraintLayout? = null

    @JvmField
    @BindView(R.id.error)
    var networkErrorView: TextView? = null

    private var networkName: String? = null

    @JvmField
    @BindView(R.id.cl_forget_network)
    var forgetNetworkView: TextView? = null

    @JvmField
    @BindView(R.id.cl_preferred_protocol)
    var preferredProtocolToggleView: ExpandableToggleView? = null

    @JvmField
    @BindView(R.id.cl_auto_secure)
    var autoSecureToggleView: ToggleView? = null

    @JvmField
    @BindView(R.id.nav_title)
    var activityTitle: TextView? = null

    override var networkInfo: NetworkInfo? = null

    @Inject
    lateinit var presenter: NetworkDetailPresenter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setActivityModule(ActivityModule(this, this)).inject(this)
        setContentLayout(R.layout.activity_network_details, true)
        presenter.init()
        networkName = intent.getStringExtra("network_name")
        networkName?.let {
            presenter.setNetworkDetails(it)
        }
        setupCustomLayoutDelegates()
    }

    override fun setActivityTitle(title: String) {
        activityTitle?.text = title
    }

    override fun onDestroy() {
        presenter.onDestroy()
        super.onDestroy()
    }

    @OnClick(R.id.nav_button)
    fun onBackButtonClick() {
        onBackPressed()
    }

    override fun onNetworkDeleted() {
        finish()
    }

    private fun setupCustomLayoutDelegates() {
        forgetNetworkView?.setOnClickListener {
            networkName?.let {
                presenter.removeNetwork(it)
            }
        }
        autoSecureToggleView?.delegate = object : ToggleView.Delegate {
            override fun onToggleClick() {
                presenter.toggleAutoSecure()
            }

            override fun onExplainClick() {

            }
        }
        preferredProtocolToggleView?.delegate = object : ExpandableToggleView.Delegate {
            override fun onToggleClick() {
                presenter.togglePreferredProtocol()
            }

            override fun onExplainClick() {

            }
        }
        val connectionModeView = preferredProtocolToggleView?.childView as? ConnectionModeView
        connectionModeView?.delegate = object : ConnectionModeView.Delegate {
            override fun onProtocolSelected(protocol: String) {
                presenter.onProtocolSelected(protocol)
            }

            override fun onPortSelected(protocol: String, port: String) {
                presenter.onPortSelected(port)
            }
        }
    }

    override fun onNetworkDetailAvailable(networkInfo: NetworkInfo) {
        this.networkInfo = networkInfo
        setAutoSecureToggle(networkInfo.isAutoSecureOn)
        setPreferredProtocolToggle(networkInfo.isPreferredOn)
        presenter.setProtocols()
    }

    override fun setAutoSecureToggle(autoSecure: Boolean) {
        if (autoSecure) {
            autoSecureToggleView?.setToggleImage(R.drawable.ic_toggle_button_on)
        } else {
            autoSecureToggleView?.setToggleImage(R.drawable.ic_toggle_button_off)
        }
    }

    override fun setNetworkDetailError(show: Boolean, error: String?) {
        if (show) {
            preferredProtocolToggleView?.visibility = View.GONE
            autoSecureToggleView?.visibility = View.GONE
            forgetNetworkView?.visibility = View.GONE
            clError?.visibility = View.VISIBLE
            networkErrorView?.text = error
        } else {
            preferredProtocolToggleView?.visibility = View.VISIBLE
            autoSecureToggleView?.visibility = View.VISIBLE
            forgetNetworkView?.visibility = View.VISIBLE
            clError?.visibility = View.GONE
            networkErrorView?.text = ""
        }
    }

    override fun setPreferredProtocolToggle(preferredProtocol: Boolean) {
        networkInfo?.let {
            if (preferredProtocol && it.isAutoSecureOn) {
                preferredProtocolToggleView?.setToggleImage(R.drawable.ic_toggle_button_on)
            } else {
                preferredProtocolToggleView?.setToggleImage(R.drawable.ic_toggle_button_off)
            }
        }
    }

    override fun setupPortMapAdapter(port: String, portMap: List<String>) {
        val connectionModeView = preferredProtocolToggleView?.childView as? ConnectionModeView
        connectionModeView?.sePortAdapter(port, portMap)
    }

    override fun setupProtocolAdapter(protocol: String, protocols: Array<String>) {
        val connectionModeView = preferredProtocolToggleView?.childView as? ConnectionModeView
        connectionModeView?.seProtocolAdapter(protocol, protocols)
    }

    override fun showToast(message: String) {
        runOnUiThread {
            Toast.makeText(this@NetworkDetailsActivity, message, Toast.LENGTH_SHORT).show()
        }
    }

    companion object {
        @JvmStatic
        fun getStartIntent(context: Context, networkName: String): Intent {
            val intent = Intent(context, NetworkDetailsActivity::class.java)
            intent.putExtra("network_name", networkName)
            return intent
        }
    }
}