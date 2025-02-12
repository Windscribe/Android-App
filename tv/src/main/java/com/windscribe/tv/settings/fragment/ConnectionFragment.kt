/*
 * Copyright (c) 2021 Windscribe Limited.
 */
package com.windscribe.tv.settings.fragment

import android.content.Context
import android.graphics.Rect
import android.os.Bundle
import android.transition.TransitionManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.CompoundButton
import android.widget.TextView
import androidx.appcompat.widget.AppCompatEditText
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.widget.NestedScrollView
import androidx.fragment.app.Fragment
import androidx.leanback.widget.HorizontalGridView
import butterknife.BindView
import butterknife.ButterKnife
import butterknife.OnClick
import com.windscribe.tv.R
import com.windscribe.tv.adapter.InstalledAppsAdapter
import com.windscribe.tv.adapter.MenuAdapter
import com.windscribe.tv.adapter.MenuAdapter.MenuItemSelectListener
import com.windscribe.tv.listeners.SettingsFragmentListener
import com.windscribe.tv.serverlist.customviews.PreferenceItem
import com.windscribe.tv.serverlist.customviews.State
import com.windscribe.tv.settings.SettingActivity
import com.windscribe.vpn.constants.PreferencesKeyConstants.BOOT_ALLOW
import com.windscribe.vpn.constants.PreferencesKeyConstants.DISABLED_MODE
import com.windscribe.vpn.constants.PreferencesKeyConstants.EXCLUSIVE_MODE
import com.windscribe.vpn.constants.PreferencesKeyConstants.INCLUSIVE_MODE
import com.windscribe.vpn.constants.PreferencesKeyConstants.LAN_ALLOW

class ConnectionFragment : Fragment() {
    // Boot start
    @JvmField
    @BindView(R.id.allow_boot_start)
    var allowBootStart: PreferenceItem? = null

    // Lan traffic
    @JvmField
    @BindView(R.id.allow_lan)
    var allowLanView: PreferenceItem? = null

    @JvmField
    @BindView(R.id.appsList)
    var appsView: HorizontalGridView? = null

    @JvmField
    @BindView(R.id.block_boot_start)
    var blockBootStart: PreferenceItem? = null

    @JvmField
    @BindView(R.id.allow_anti_censorship)
    var allowAntiCensorship: PreferenceItem? = null

    @JvmField
    @BindView(R.id.block_anti_censorship)
    var blockAntiCensorship: PreferenceItem? = null

    @JvmField
    @BindView(R.id.block)
    var blockLanView: PreferenceItem? = null

    // Connection
    @JvmField
    @BindView(R.id.auto_connection)
    var btnAuto: PreferenceItem? = null

    @JvmField
    @BindView(R.id.manual_connection)
    var btnManual: PreferenceItem? = null

    @JvmField
    @BindView(R.id.title)
    var connectionModeTextView: TextView? = null

    @JvmField
    @BindView(R.id.disabledMode)
    var disabledModeView: PreferenceItem? = null

    // Split routing
    @JvmField
    @BindView(R.id.exclusiveMode)
    var exclusiveModeView: PreferenceItem? = null

    @JvmField
    @BindView(R.id.expandedView)
    var expandedView: ConstraintLayout? = null

    @JvmField
    @BindView(R.id.inclusiveMode)
    var inclusiveModeView: PreferenceItem? = null

    @JvmField
    @BindView(R.id.connected_dns_robert)
    var connectedDNSRobert: PreferenceItem? = null

    @JvmField
    @BindView(R.id.connected_dns_custom)
    var connectedDNSCustom: PreferenceItem? = null

    @JvmField
    @BindView(R.id.connect_dns_custom_address)
    var connectedDNSAddress: AppCompatEditText? = null

    @JvmField
    @BindView(R.id.connectionParent)
    var mainLayout: ConstraintLayout? = null

    @JvmField
    @BindView(R.id.portList)
    var portView: HorizontalGridView? = null

    @JvmField
    @BindView(R.id.protocolList)
    var protocolView: HorizontalGridView? = null

    @JvmField
    @BindView(R.id.showSystemApps)
    var showSystemApps: CheckBox? = null

    @JvmField
    @BindView(R.id.titlePort)
    var titlePortTextView: TextView? = null

    @JvmField
    @BindView(R.id.titleProtocol)
    var titleProtocolTextView: TextView? = null

    @JvmField
    @BindView(R.id.titleSplitRoutingApps)
    var titleSplitRoutingApps: TextView? = null
    private var listener: SettingsFragmentListener? = null
    private var selectedProtocol: String? = null
    override fun onAttach(context: Context) {
        super.onAttach(context)
        val activity: SettingActivity
        if (context is SettingActivity) {
            activity = context
            try {
                listener = activity
            } catch (e: ClassCastException) {
                e.printStackTrace()
            }
        }
    }

    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_connection, container, false)
        ButterKnife.bind(this, view)
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        listener?.onFragmentReady(this)
        showSystemApps?.setOnCheckedChangeListener { _: CompoundButton?, isChecked: Boolean ->
            if (appsView != null && appsView?.adapter != null) {
                val appsAdapter = appsView?.adapter as InstalledAppsAdapter?
                appsAdapter?.setFilterType(isChecked)
            }
        }
        makeSpaceForKeyboard()
    }

    override fun onDestroyView() {
        val scrollView = activity?.findViewById<NestedScrollView>(R.id.scrollView)
        val viewTreeObserver = scrollView?.viewTreeObserver
        viewTreeObserver?.removeOnGlobalLayoutListener {  }
        listener?.saveCustomDNSAddress(connectedDNSAddress?.text.toString())
        super.onDestroyView()
    }

    private fun makeSpaceForKeyboard() {
        val scrollView = activity?.findViewById<NestedScrollView>(R.id.scrollView)
        val viewTreeObserver = scrollView?.viewTreeObserver
        viewTreeObserver?.addOnGlobalLayoutListener {
            val rect = Rect()
            scrollView.getWindowVisibleDisplayFrame(rect)
            val screenHeight = scrollView.rootView.height
            val keypadHeight = screenHeight - rect.bottom
            if (keypadHeight > screenHeight * 0.15) {
                connectionModeTextView?.visibility = View.GONE
                btnAuto?.visibility = View.GONE
                btnManual?.visibility = View.GONE
            } else {
                connectionModeTextView?.visibility = View.VISIBLE
                btnAuto?.visibility = View.VISIBLE
                btnManual?.visibility = View.VISIBLE
            }
        }
    }

    fun setBootOnStart(mode: String) {
        if (mode == BOOT_ALLOW) {
            allowBootStart?.setState(State.MenuButtonState.Selected)
            blockBootStart?.setState(State.MenuButtonState.NotSelected)
        } else {
            blockBootStart?.setState(State.MenuButtonState.Selected)
            allowBootStart?.setState(State.MenuButtonState.NotSelected)
        }
    }

    fun setConnectionMode(isAuto: Boolean) {
        if (isAuto) {
            btnAuto?.setState(State.MenuButtonState.Selected)
            btnManual?.setState(State.MenuButtonState.NotSelected)
            expandedView?.visibility = View.GONE
        } else {
            btnManual?.setState(State.MenuButtonState.Selected)
            btnAuto?.setState(State.MenuButtonState.NotSelected)
            TransitionManager.beginDelayedTransition(mainLayout)
            expandedView?.visibility = View.VISIBLE
        }
    }

    fun setLanTrafficMode(mode: String) {
        if (mode == LAN_ALLOW) {
            allowLanView?.setState(State.MenuButtonState.Selected)
            blockLanView?.setState(State.MenuButtonState.NotSelected)
        } else {
            blockLanView?.setState(State.MenuButtonState.Selected)
            allowLanView?.setState(State.MenuButtonState.NotSelected)
        }
    }

    fun setCustomDNS(isCustom: Boolean) {
        if (isCustom) {
            connectedDNSCustom?.setState(State.MenuButtonState.Selected)
            connectedDNSRobert?.setState(State.MenuButtonState.NotSelected)
        } else {
            connectedDNSRobert?.setState(State.MenuButtonState.Selected)
            connectedDNSCustom?.setState(State.MenuButtonState.NotSelected)
        }
    }

    fun setCustomDNSAddress(url: String) {
        connectedDNSAddress?.setText(url)
    }

    fun setCustomDNSAddressVisibility(show: Boolean) {
        if (show) {
            connectedDNSAddress?.visibility = View.VISIBLE
        } else {
            connectedDNSAddress?.visibility = View.GONE
        }
    }

    fun setPortAdapter(savedPort: String, ports: List<String>) {
        val portAdapter = MenuAdapter(ports, savedPort)
        portAdapter.setListener(object : MenuItemSelectListener {
            override fun onItemSelected(selectedItemKey: String?) {
                selectedProtocol?.let { protocol ->
                    selectedItemKey?.let {
                        listener?.onPortSelected(
                                protocol, selectedItemKey
                        )
                    }
                }
            }
        })
        portView?.setNumRows(1)
        portView?.setRowHeight(ViewGroup.LayoutParams.WRAP_CONTENT)
        portView?.adapter = portAdapter
    }

    fun setProtocolAdapter(savedProtocol: String, protocols: List<String>) {
        selectedProtocol = savedProtocol
        val protocolAdapter = MenuAdapter(protocols, savedProtocol)
        protocolAdapter.setListener(object : MenuItemSelectListener {
            override fun onItemSelected(selectedItemKey: String?) {
                selectedProtocol = selectedItemKey
                selectedItemKey?.let {
                    listener?.onProtocolSelected(selectedItemKey)
                }
            }
        })
        protocolView?.setNumRows(1)
        protocolView?.setRowHeight(ViewGroup.LayoutParams.WRAP_CONTENT)
        protocolView?.adapter = protocolAdapter
    }

    fun setSplitAppsAdapter(adapter: InstalledAppsAdapter) {
        showSystemApps?.let { adapter.setFilterType(it.isChecked) }
        appsView?.setNumRows(1)
        appsView?.setRowHeight(ViewGroup.LayoutParams.WRAP_CONTENT)
        appsView?.adapter = adapter
    }

    fun setSplitRouteMode(mode: String?) {
        when (mode) {
            DISABLED_MODE -> {
                exclusiveModeView?.setState(State.MenuButtonState.NotSelected)
                inclusiveModeView?.setState(State.MenuButtonState.NotSelected)
                disabledModeView?.setState(State.MenuButtonState.Selected)
                titleSplitRoutingApps?.text = getString(R.string.apps)
                showAppsView(false)
            }

            EXCLUSIVE_MODE -> {
                inclusiveModeView?.setState(State.MenuButtonState.NotSelected)
                disabledModeView?.setState(State.MenuButtonState.NotSelected)
                exclusiveModeView?.setState(State.MenuButtonState.Selected)
                titleSplitRoutingApps?.text = getString(R.string.apps_to_exclude)
                showAppsView(true)
            }

            INCLUSIVE_MODE -> {
                exclusiveModeView?.setState(State.MenuButtonState.NotSelected)
                disabledModeView?.setState(State.MenuButtonState.NotSelected)
                inclusiveModeView?.setState(State.MenuButtonState.Selected)
                titleSplitRoutingApps?.text = getString(R.string.apps_to_include)
                showAppsView(true)
            }
        }
    }

    @OnClick(R.id.allow_boot_start)
    fun onAllowBootClick() {
        listener?.onAllowBootStartClick()
    }

    @OnClick(R.id.allow_lan)
    fun onAllowLanClick() {
        listener?.onAllowLanClicked()
    }

    @OnClick(R.id.auto_connection)
    fun onAutoClick() {
        listener?.onAutoClicked()
    }

    @OnClick(R.id.block_boot_start)
    fun onBlockBootStart() {
        listener?.onBlockBootStartClick()
    }

    @OnClick(R.id.block)
    fun onBlockLanClick() {
        listener?.onBlockLanClicked()
    }

    @OnClick(R.id.disabledMode)
    fun onDisabledModeClick() {
        listener?.onDisabledModeClick()
    }

    @OnClick(R.id.exclusiveMode)
    fun onExclusiveModeClick() {
        listener?.onExclusiveModeClick()
    }

    @OnClick(R.id.inclusiveMode)
    fun onInclusiveModeClick() {
        listener?.onInclusiveModeClick()
    }

    @OnClick(R.id.manual_connection)
    fun onManualClick() {
        listener?.onManualClicked()
    }

    @OnClick(R.id.splitRoutingIcon)
    fun onSplitRoutingHelpClick() {
        listener?.startSplitTunnelingHelpActivity()
    }

    @OnClick(R.id.allow_anti_censorship)
    fun onAllowAntiCensorshipClick() {
        listener?.onAllowAntiCensorshipClicked()
    }

    @OnClick(R.id.block_anti_censorship)
    fun onBlockAntiCensorshipClicked() {
        listener?.onBlockAntiCensorshipClicked()
    }

    @OnClick(R.id.connected_dns_robert)
    fun onRobertClicked() {
        listener?.onRobertDNSClicked()
    }

    @OnClick(R.id.connected_dns_custom)
    fun onCustomDNSClicked() {
        listener?.onCustomDNSClicked()
    }

    private fun showAppsView(show: Boolean) {
        if (show) {
            TransitionManager.beginDelayedTransition(mainLayout)
            showSystemApps?.visibility = View.VISIBLE
            titleSplitRoutingApps?.visibility = View.VISIBLE
            appsView?.visibility = View.VISIBLE
        } else {
            titleSplitRoutingApps?.visibility = View.GONE
            showSystemApps?.visibility = View.GONE
            appsView?.visibility = View.GONE
        }
    }

    fun setAntiCensorshipMode(enabled: Boolean) {
        if (enabled) {
            allowAntiCensorship?.setState(State.MenuButtonState.Selected)
            blockAntiCensorship?.setState(State.MenuButtonState.NotSelected)
        } else {
            blockAntiCensorship?.setState(State.MenuButtonState.Selected)
            allowAntiCensorship?.setState(State.MenuButtonState.NotSelected)
        }
    }
}
