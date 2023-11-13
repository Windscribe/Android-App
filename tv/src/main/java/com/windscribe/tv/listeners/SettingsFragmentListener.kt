/*
 * Copyright (c) 2021 Windscribe Limited.
 */
package com.windscribe.tv.listeners

import androidx.fragment.app.Fragment

interface SettingsFragmentListener {
    fun onAllowBootStartClick()
    fun onAllowLanClicked()
    fun onAutoClicked()
    fun onBlockBootStartClick()
    fun onBlockLanClicked()
    fun onContainerHidden(hidden: Boolean)
    fun onDisabledModeClick()
    fun onEmailClick()
    fun onEmailResend()
    fun onExclusiveModeClick()
    fun onFragmentReady(fragment: Fragment)
    fun onInclusiveModeClick()
    fun onLanguageSelect(language: String?)
    fun onLoginClick()
    fun onManualClicked()
    fun onPortSelected(protocol: String, port: String)
    fun onProtocolSelected(protocol: String)
    fun onSignUpClick()
    fun onSortSelect(newSort: String)
    fun onUpgradeClick(planName: String)
    fun startSplitTunnelingHelpActivity()
    fun onAllowAntiCensorshipClicked()
    fun onBlockAntiCensorshipClicked()
}