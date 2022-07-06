/*
 * Copyright (c) 2021 Windscribe Limited.
 */
package com.windscribe.mobile.networksecurity.networkdetails

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.res.ResourcesCompat
import butterknife.BindView
import butterknife.OnClick
import butterknife.OnItemSelected
import com.windscribe.mobile.R
import com.windscribe.mobile.base.BaseActivity
import com.windscribe.mobile.di.ActivityModule
import com.windscribe.vpn.localdatabase.tables.NetworkInfo
import javax.inject.Inject

class NetworkDetailsActivity : BaseActivity(), NetworkDetailView {
    @JvmField
    @BindView(R.id.auto_secure_toggle)
    var autoSecureToggle: ImageView? = null

    @JvmField
    @BindView(R.id.cl_error)
    var clError: ConstraintLayout? = null

    @JvmField
    @BindView(R.id.cl_network_detail)
    var clNetworkDetails: ConstraintLayout? = null

    @JvmField
    @BindView(R.id.cl_port)
    var clPort: ConstraintLayout? = null

    @JvmField
    @BindView(R.id.cl_protocol)
    var clProtocol: ConstraintLayout? = null

    @JvmField
    @BindView(R.id.tv_current_port)
    var currentPort: TextView? = null

    @JvmField
    @BindView(R.id.tv_current_protocol)
    var currentProtocol: TextView? = null

    @JvmField
    @BindView(R.id.forgetNetworkLabel)
    var forgetNetworkView: TextView? = null

    @JvmField
    @BindView(R.id.error)
    var networkErrorView: TextView? = null

    var networkName: String? = null

    @JvmField
    @BindView(R.id.nav_title)
    var networkNameView: TextView? = null

    @JvmField
    @BindView(R.id.port_divider)
    var portDivider: ImageView? = null

    @JvmField
    @BindView(R.id.spinner_port)
    var portSpinner: Spinner? = null

    @JvmField
    @BindView(R.id.preferred_protocol_divider)
    var preferredProtocolDivider: ImageView? = null

    @JvmField
    @BindView(R.id.preferredProtocolLabel)
    var preferredProtocolLabel: TextView? = null

    @JvmField
    @BindView(R.id.preferred_protocol_toggle)
    var preferredProtocolToggle: ImageView? = null

    @JvmField
    @BindView(R.id.protocol_divider)
    var protocolDivider: ImageView? = null

    @JvmField
    @BindView(R.id.spinner_protocol)
    var protocolSpinner: Spinner? = null

    override var networkInfo: NetworkInfo? = null

    @Inject
    lateinit var presenter: NetworkDetailPresenter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setActivityModule(ActivityModule(this, this)).inject(this)
        setContentLayout(R.layout.activity_network_details, true)
        networkName = intent.getStringExtra("network_name")
        networkNameView?.text = networkName
        networkName?.let {
            presenter.setNetworkDetails(it)
        }
    }

    override fun onDestroy() {
        presenter.onDestroy()
        super.onDestroy()
    }

    @OnClick(R.id.auto_secure_toggle)
    fun autoSecureToggleClick() {
        presenter.toggleAutoSecure()
    }

    override fun hideError() {
        clError?.visibility = View.GONE
        clNetworkDetails?.visibility = View.VISIBLE
    }

    @OnClick(R.id.nav_button)
    fun onBackButtonClick() {
        onBackPressed()
    }

    @OnClick(R.id.tv_current_port)
    fun onCurrentPortClick() {
        portSpinner?.performClick()
    }

    @OnClick(R.id.tv_current_protocol)
    fun onCurrentProtocolClick() {
        protocolSpinner?.performClick()
    }

    @OnClick(R.id.forgetNetworkLabel)
    fun onForgetNetworkClick() {
        networkName?.let {
            presenter.removeNetwork(it)
        }
    }

    override fun onNetworkDeleted() {
        finish()
    }

    override fun onNetworkDetailAvailable(networkInfo: NetworkInfo) {
        this.networkInfo = networkInfo
        setAutoSecureToggle(networkInfo.isAutoSecureOn)
        setPreferredProtocolToggle(networkInfo.isPreferredOn)
        presenter.setProtocols()
    }

    @OnItemSelected(R.id.spinner_port)
    fun onPortSelected() {
        currentPort?.text = portSpinner?.selectedItem.toString()
        presenter.onPortSelected(
            portSpinner?.selectedItem.toString()
        )
    }

    @OnItemSelected(R.id.spinner_protocol)
    fun onProtocolSelected() {
        currentProtocol?.text = protocolSpinner?.selectedItem.toString()
        presenter.onProtocolSelected(protocolSpinner?.selectedItem.toString())
    }

    @OnClick(R.id.preferred_protocol_toggle)
    fun preferredProtocolToggleClick() {
        presenter.togglePreferredProtocol()
    }

    override fun setAutoSecureToggle(autoSecure: Boolean) {
        if (autoSecure) {
            setPreferredProtocolContainerVisibility(View.VISIBLE)
            // setProtocolAndPortVisibility(VISIBLE);
            autoSecureToggle?.setImageDrawable(
                ResourcesCompat.getDrawable(resources, R.drawable.ic_toggle_button_on, theme)
            )
        } else {
            setPreferredProtocolContainerVisibility(View.GONE)
            setProtocolAndPortVisibility(View.GONE)
            autoSecureToggle?.setImageDrawable(
                ResourcesCompat.getDrawable(resources, R.drawable.ic_toggle_button_off, theme)
            )
        }
    }

    override fun setNetworkDetailError(error: String) {
        clNetworkDetails?.visibility = View.GONE
        clError?.visibility = View.VISIBLE
        networkErrorView?.text = error
    }

    override fun setPreferredProtocolToggle(preferredProtocol: Boolean) {
        networkInfo?.let {
            if (preferredProtocol && it.isAutoSecureOn) {
                setProtocolAndPortVisibility(View.VISIBLE)
                preferredProtocolToggle?.setImageDrawable(
                    ResourcesCompat.getDrawable(resources, R.drawable.ic_toggle_button_on, theme)
                )
            } else {
                setProtocolAndPortVisibility(View.GONE)
                preferredProtocolToggle?.setImageDrawable(
                    ResourcesCompat.getDrawable(resources, R.drawable.ic_toggle_button_off, theme)
                )
            }
        }
    }

    override fun setupPortMapAdapter(port: String, portMap: List<String>) {
        val portMapAdapter = ArrayAdapter(
            this, R.layout.drop_down_layout,
            R.id.tv_drop_down, portMap
        )
        portSpinner?.adapter = portMapAdapter
        portSpinner?.isSelected = false
        portSpinner?.setSelection(portMapAdapter.getPosition(port))
        currentPort?.text = port
    }

    override fun setupProtocolAdapter(protocol: String, mProtocols: Array<String>) {
        val spinnerAdapter = ArrayAdapter<CharSequence>(
            this, R.layout.drop_down_layout,
            R.id.tv_drop_down, mProtocols
        )
        protocolSpinner?.adapter = spinnerAdapter
        protocolSpinner?.isSelected = false
        protocolSpinner?.setSelection(spinnerAdapter.getPosition(protocol))
        currentProtocol?.text = protocol
    }

    override fun showToast(message: String) {
        runOnUiThread {
            Toast.makeText(this@NetworkDetailsActivity, message, Toast.LENGTH_SHORT).show()
        }
    }

    private fun setPreferredProtocolContainerVisibility(visibility: Int) {
        preferredProtocolToggle?.visibility = visibility
        preferredProtocolDivider?.visibility = visibility
        preferredProtocolLabel?.visibility = visibility
    }

    private fun setProtocolAndPortVisibility(visibility: Int) {
        clPort?.visibility = visibility
        clProtocol?.visibility = visibility
        portDivider?.visibility = visibility
        protocolDivider?.visibility = visibility
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