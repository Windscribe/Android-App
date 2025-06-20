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
import android.widget.CompoundButton
import androidx.core.widget.NestedScrollView
import androidx.fragment.app.Fragment
import com.windscribe.tv.R
import com.windscribe.tv.adapter.InstalledAppsAdapter
import com.windscribe.tv.adapter.MenuAdapter
import com.windscribe.tv.adapter.MenuAdapter.MenuItemSelectListener
import com.windscribe.tv.databinding.FragmentConnectionBinding
import com.windscribe.tv.listeners.SettingsFragmentListener
import com.windscribe.tv.serverlist.customviews.State
import com.windscribe.tv.settings.SettingActivity
import com.windscribe.vpn.constants.PreferencesKeyConstants.BOOT_ALLOW
import com.windscribe.vpn.constants.PreferencesKeyConstants.DISABLED_MODE
import com.windscribe.vpn.constants.PreferencesKeyConstants.EXCLUSIVE_MODE
import com.windscribe.vpn.constants.PreferencesKeyConstants.INCLUSIVE_MODE
import com.windscribe.vpn.constants.PreferencesKeyConstants.LAN_ALLOW

class ConnectionFragment : Fragment() {
    private lateinit var binding: FragmentConnectionBinding
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
    ): View {
        binding = FragmentConnectionBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        listener?.onFragmentReady(this)
        binding.showSystemApps.setOnCheckedChangeListener { _: CompoundButton?, isChecked: Boolean ->
            if (binding.appsList.adapter != null) {
                val appsAdapter = binding.appsList.adapter as InstalledAppsAdapter?
                appsAdapter?.setFilterType(isChecked)
            }
        }
        makeSpaceForKeyboard()
        addClickListeners()
    }

    override fun onDestroyView() {
        val scrollView = activity?.findViewById<NestedScrollView>(R.id.scrollView)
        val viewTreeObserver = scrollView?.viewTreeObserver
        viewTreeObserver?.removeOnGlobalLayoutListener { }
        listener?.saveCustomDNSAddress(binding.connectDnsCustomAddress.text.toString())
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
                binding.title.visibility = View.GONE
                binding.autoConnection.visibility = View.GONE
                binding.manualConnection.visibility = View.GONE
            } else {
                binding.title.visibility = View.VISIBLE
                binding.autoConnection.visibility = View.VISIBLE
                binding.manualConnection.visibility = View.VISIBLE
            }
        }
    }

    fun setBootOnStart(mode: String) {
        if (mode == BOOT_ALLOW) {
            binding.allowBootStart.setState(State.MenuButtonState.Selected)
            binding.blockBootStart.setState(State.MenuButtonState.NotSelected)
        } else {
            binding.blockBootStart.setState(State.MenuButtonState.Selected)
            binding.allowBootStart.setState(State.MenuButtonState.NotSelected)
        }
    }

    fun setConnectionMode(isAuto: Boolean) {
        if (isAuto) {
            binding.autoConnection.setState(State.MenuButtonState.Selected)
            binding.manualConnection.setState(State.MenuButtonState.NotSelected)
            binding.expandedView.visibility = View.GONE
        } else {
            binding.manualConnection.setState(State.MenuButtonState.Selected)
            binding.autoConnection.setState(State.MenuButtonState.NotSelected)
            TransitionManager.beginDelayedTransition(binding.connectionParent)
            binding.expandedView.visibility = View.VISIBLE
        }
    }

    fun setLanTrafficMode(mode: String) {
        if (mode == LAN_ALLOW) {
            binding.allowLan.setState(State.MenuButtonState.Selected)
            binding.block.setState(State.MenuButtonState.NotSelected)
        } else {
            binding.block.setState(State.MenuButtonState.Selected)
            binding.allowLan.setState(State.MenuButtonState.NotSelected)
        }
    }

    fun setCustomDNS(isCustom: Boolean) {
        if (isCustom) {
            binding.connectedDnsCustom.setState(State.MenuButtonState.Selected)
            binding.connectedDnsRobert.setState(State.MenuButtonState.NotSelected)
        } else {
            binding.connectedDnsRobert.setState(State.MenuButtonState.Selected)
            binding.connectedDnsCustom.setState(State.MenuButtonState.NotSelected)
        }
    }

    fun setCustomDNSAddress(url: String) {
        binding.connectDnsCustomAddress.setText(url)
    }

    fun setCustomDNSAddressVisibility(show: Boolean) {
        if (show) {
            binding.connectDnsCustomAddress.visibility = View.VISIBLE
        } else {
            binding.connectDnsCustomAddress.visibility = View.GONE
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
        binding.portList.setNumRows(1)
        binding.portList.setRowHeight(ViewGroup.LayoutParams.WRAP_CONTENT)
        binding.portList.adapter = portAdapter
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
        binding.protocolList.setNumRows(1)
        binding.protocolList.setRowHeight(ViewGroup.LayoutParams.WRAP_CONTENT)
        binding.protocolList.adapter = protocolAdapter
    }

    fun setSplitAppsAdapter(adapter: InstalledAppsAdapter) {
        adapter.setFilterType(binding.showSystemApps.isChecked)
        binding.appsList.setNumRows(1)
        binding.appsList.setRowHeight(ViewGroup.LayoutParams.WRAP_CONTENT)
        binding.appsList.adapter = adapter
    }

    fun setSplitRouteMode(mode: String?) {
        when (mode) {
            DISABLED_MODE -> {
                binding.exclusiveMode.setState(State.MenuButtonState.NotSelected)
                binding.inclusiveMode.setState(State.MenuButtonState.NotSelected)
                binding.disabledMode.setState(State.MenuButtonState.Selected)
                binding.titleSplitRoutingApps.text = getString(com.windscribe.vpn.R.string.apps)
                showAppsView(false)
            }

            EXCLUSIVE_MODE -> {
                binding.inclusiveMode.setState(State.MenuButtonState.NotSelected)
                binding.disabledMode.setState(State.MenuButtonState.NotSelected)
                binding.exclusiveMode.setState(State.MenuButtonState.Selected)
                binding.titleSplitRoutingApps.text = getString(com.windscribe.vpn.R.string.apps_to_exclude)
                showAppsView(true)
            }

            INCLUSIVE_MODE -> {
                binding.exclusiveMode.setState(State.MenuButtonState.NotSelected)
                binding.disabledMode.setState(State.MenuButtonState.NotSelected)
                binding.inclusiveMode.setState(State.MenuButtonState.Selected)
                binding.titleSplitRoutingApps.text = getString(com.windscribe.vpn.R.string.apps_to_include)
                showAppsView(true)
            }
        }
    }

    private fun addClickListeners() {
        binding.allowLan.setOnClickListener {
            listener?.onAllowLanClicked()
        }
        binding.block.setOnClickListener {
            listener?.onBlockLanClicked()
        }

        binding.allowBootStart.setOnClickListener {
            listener?.onAllowBootStartClick()
        }

        binding.blockBootStart.setOnClickListener {
            listener?.onBlockBootStartClick()
        }

        binding.allowAntiCensorship.setOnClickListener {
            listener?.onAllowAntiCensorshipClicked()
        }
        binding.blockAntiCensorship.setOnClickListener {
            listener?.onBlockAntiCensorshipClicked()
        }
        binding.autoConnection.setOnClickListener {
            listener?.onAutoClicked()
        }
        binding.manualConnection.setOnClickListener {
            listener?.onManualClicked()
        }
        binding.disabledMode.setOnClickListener {
            listener?.onDisabledModeClick()
        }
        binding.exclusiveMode.setOnClickListener {
            listener?.onExclusiveModeClick()
        }
        binding.inclusiveMode.setOnClickListener {
            listener?.onInclusiveModeClick()
        }
        binding.splitRoutingIcon.setOnClickListener {
            listener?.startSplitTunnelingHelpActivity()
        }
        binding.titleSplitRouting.setOnClickListener {
            listener?.startSplitTunnelingHelpActivity()
        }
        binding.connectedDnsRobert.setOnClickListener {
            listener?.onRobertDNSClicked()
        }
        binding.connectedDnsCustom.setOnClickListener {
            listener?.onCustomDNSClicked()
        }
    }

    private fun showAppsView(show: Boolean) {
        if (show) {
            TransitionManager.beginDelayedTransition(binding.connectionParent)
            binding.showSystemApps.visibility = View.VISIBLE
            binding.titleSplitRoutingApps.visibility = View.VISIBLE
            binding.appsList.visibility = View.VISIBLE
        } else {
            binding.titleSplitRoutingApps.visibility = View.GONE
            binding.showSystemApps.visibility = View.GONE
            binding.appsList.visibility = View.GONE
        }
    }

    fun setAntiCensorshipMode(enabled: Boolean) {
        if (enabled) {
            binding.allowAntiCensorship.setState(State.MenuButtonState.Selected)
            binding.blockAntiCensorship.setState(State.MenuButtonState.NotSelected)
        } else {
            binding.blockAntiCensorship.setState(State.MenuButtonState.Selected)
            binding.allowAntiCensorship.setState(State.MenuButtonState.NotSelected)
        }
    }
}
