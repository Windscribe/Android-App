/*
 * Copyright (c) 2021 Windscribe Limited.
 */
package com.windscribe.tv.settings

import android.app.TaskStackBuilder
import android.content.Intent
import android.os.Bundle
import android.transition.Slide
import android.transition.TransitionManager
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.NestedScrollView
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import com.windscribe.tv.R
import com.windscribe.tv.adapter.InstalledAppsAdapter
import com.windscribe.tv.base.BaseActivity
import com.windscribe.tv.confirmemail.ConfirmActivity
import com.windscribe.tv.customview.CustomDialog
import com.windscribe.tv.databinding.ActivitySettingBinding
import com.windscribe.tv.di.ActivityModule
import com.windscribe.tv.email.AddEmailActivity
import com.windscribe.tv.listeners.SettingsFragmentListener
import com.windscribe.tv.moredata.GetMoreDataActivity
import com.windscribe.tv.serverlist.customviews.State
import com.windscribe.tv.settings.fragment.*
import com.windscribe.tv.upgrade.UpgradeActivity
import com.windscribe.tv.welcome.WelcomeActivity
import com.windscribe.tv.windscribe.WindscribeActivity
import com.windscribe.vpn.backend.utils.WindVpnController
import com.windscribe.vpn.commonutils.WindUtilities
import com.windscribe.vpn.apppreference.PreferencesKeyConstants
import javax.inject.Inject

class SettingActivity :
    BaseActivity(),
    SettingView,
    SettingsFragmentListener {

    @Inject
    lateinit var vpnController: WindVpnController

    @Inject
    lateinit var presenter: SettingsPresenter

    @JvmField
    @Inject
    var sendDebugDialog: CustomDialog? = null

    private lateinit var binding: ActivitySettingBinding
    private var currentSelectedScreen = 1
    private var fragment: Fragment? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setActivityModule(ActivityModule(this, this)).inject(this)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_setting)
        onActivityLaunch()
        initFragment()
        binding.scrollView.setOnScrollChangeListener { _: NestedScrollView?, _: Int, _: Int, _: Int, _: Int ->
            if (fragment is GeneralFragment) {
                (fragment as GeneralFragment).checkViewVisibility(binding.scrollView)
            }
        }
        presenter.setUpTabMenu()
        binding.version.text = String.format("%s", WindUtilities.getVersionName())
        addClickListeners()
    }

    override fun onResume() {
        super.onResume()
        presenter.updateUserDataFromApi()
    }

    override fun onDestroy() {
        presenter.onDestroy()
        super.onDestroy()
    }

    override fun getResourceString(id: Int): String {
        return resources.getString(id)
    }

    override fun goToClaimAccount() {
        val startIntent = WelcomeActivity.getStartIntent(this)
        startIntent.putExtra("startFragmentName", "AccountSetUp")
        startActivity(startIntent)
    }

    override fun goToLogin() {
        val startIntent = WelcomeActivity.getStartIntent(this)
        startIntent.putExtra("startFragmentName", "Login")
        startActivity(startIntent)
    }

    override fun gotoLoginRegistrationActivity() {
        val intent = WelcomeActivity.getStartIntent(this)
        intent.addFlags(
            Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_NO_HISTORY or Intent.FLAG_ACTIVITY_CLEAR_TASK
        )
        startActivity(intent)
        overridePendingTransition(R.anim.slide_up, R.anim.slide_down)
        finish()
    }

    override fun setCustomDNS(isCustom: Boolean) {
        if (fragment is ConnectionFragment) {
            (fragment as ConnectionFragment).setCustomDNS(isCustom)
        }
    }

    override fun setCustomDNSAddress(url: String) {
        if (fragment is ConnectionFragment) {
            (fragment as ConnectionFragment).setCustomDNSAddress(url)
        }
    }

    override fun hideProgress() {
        runOnUiThread { sendDebugDialog?.dismiss() }
    }

    override fun onAllowBootStartClick() {
        presenter.onAllowBootStartClick()
    }

    override fun onAllowLanClicked() {
        presenter.onAllowLanClicked()
    }

    override fun onAutoClicked() {
        presenter.onConnectionModeAutoClicked()
    }

    override fun onBlockBootStartClick() {
        presenter.onBlockBootStartClick()
    }

    override fun onBlockLanClicked() {
        presenter.onBlockLanClicked()
    }

    override fun onAllowAntiCensorshipClicked() {
        presenter.onAllowAntiCensorshipClicked()
    }

    override fun onBlockAntiCensorshipClicked() {
        presenter.onBlockAntiCensorshipClicked()
    }

    override fun onRobertDNSClicked() {
        presenter.onRobertDNSClicked()
    }

    override fun onCustomDNSClicked() {
        presenter.onCustomDNSClicked()
    }

    override fun saveCustomDNSAddress(url: String) {
        presenter.saveCustomDNSAddress(url)
    }

    override fun setCustomDNSAddressVisibility(show: Boolean) {
        if (fragment is ConnectionFragment) {
            (fragment as ConnectionFragment).setCustomDNSAddressVisibility(show)
        }
    }

    override fun onContainerHidden(hidden: Boolean) {
        if (hidden) {
            if (binding.gradient.visibility == View.INVISIBLE) {
                binding.gradient.visibility = View.VISIBLE
            }
        } else {
            if (binding.gradient.visibility == View.VISIBLE) {
                binding.gradient.visibility = View.INVISIBLE
            }
        }
    }

    override fun onDisabledModeClick() {
        presenter.onDisabledModeClick()
    }

    override fun onEmailClick() {
        presenter.onAddEmailClicked()
    }

    override fun onEmailResend() {
        presenter.onEmailResend()
    }

    override fun onExclusiveModeClick() {
        presenter.onExclusiveModeClick()
    }

    override fun onFragmentReady(fragment: Fragment) {
        if (fragment is GeneralFragment) {
            presenter.setupLayoutForGeneralTab()
        }
        if (fragment is ConnectionFragment) {
            presenter.setupLayoutBasedOnConnectionMode()
        }
        if (fragment is AccountFragment) {
            presenter.showLayoutBasedOnUserType()
            presenter.observeUserData(this)
            presenter.updateUserDataFromApi()
        }
        if (fragment is DebugFragment) {
            presenter.setupLayoutForDebugTab()
        }
    }

    override fun onInclusiveModeClick() {
        presenter.onInclusiveModeClick()
    }

    override fun onLanguageSelect(language: String?) {
        language?.let { presenter.onLanguageSelected(language) }
    }

    override fun onLoginClick() {
        goToLogin()
    }

    override fun onManualClicked() {
        presenter.onConnectionModeManualClicked()
    }

    override fun onPortSelected(protocol: String, port: String) {
        presenter.onPortSelected(protocol, port)
    }

    override fun onProtocolSelected(protocol: String) {
        presenter.onProtocolSelected(protocol)
    }

    override fun onSignUpClick() {
        goToClaimAccount()
    }

    override fun onSortSelect(newSort: String) {
        presenter.onSortSelected(newSort)
    }

    override fun onUpgradeClick(planText: String) {
        presenter.onUpgradeClicked(planText)
    }

    override fun openConfirmEmailActivity() {
        val intent = ConfirmActivity.getStartIntent(this)
        startActivity(intent)
    }

    override fun openEmailActivity(email: String?) {
        val intent = AddEmailActivity.getStartIntent(this)
        intent.putExtra("pro_user", presenter.isUserPro)
        intent.action = PreferencesKeyConstants.ACTION_ADD_EMAIL_FROM_ACCOUNT
        intent.putExtra(AddEmailActivity.Email_Tag, email)
        startActivity(intent)
    }

    override fun openUpgradeActivity() {
        startActivity(UpgradeActivity.getStartIntent(this))
    }

    override fun reloadApp() {
        val windscribeIntent = WindscribeActivity.getStartIntent(this).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val settingsIntent = getStartIntent(this).apply {
            flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
        }
        startActivities(arrayOf(windscribeIntent, settingsIntent))
    }

    override fun setBootStartMode(mode: String) {
        if (fragment is ConnectionFragment) {
            (fragment as ConnectionFragment).setBootOnStart(mode)
        }
    }

    override fun setAntiCensorshipMode(enabled: Boolean) {
        (fragment as? ConnectionFragment)?.setAntiCensorshipMode(enabled)
    }

    override fun setDebugLog(log: List<String>) {
        if (fragment is DebugFragment) {
            runOnUiThread { (fragment as DebugFragment).showLogs(log) }
        }
    }

    override fun setDebugLogProgress(progressText: String, error: String) {
        if (fragment is DebugFragment) {
            (fragment as DebugFragment).showLoading(progressText, error)
        }
    }

    override fun setEmail(email: String) {
        if (fragment is AccountFragment) {
            (fragment as AccountFragment).setEmail(email)
        }
    }

    override fun setEmailState(state: AccountFragment.Status?, email: String?) {
        if (fragment is AccountFragment) {
            (fragment as AccountFragment).setEmailState(state, email)
        }
    }

    override fun setLanTrafficMode(lanTrafficMode: String) {
        if (fragment is ConnectionFragment) {
            (fragment as ConnectionFragment).setLanTrafficMode(lanTrafficMode)
        }
    }

    override fun setPlanName(planName: String) {
        if (fragment is AccountFragment) {
            (fragment as AccountFragment).setPlanName(planName)
        }
    }

    override fun setResetDate(resetDateLabel: String, resetDate: String) {
        if (fragment is AccountFragment) {
            (fragment as AccountFragment).setResetDate(resetDateLabel, resetDate)
        }
    }

    override fun setSplitRouteMode(mode: String) {
        if (fragment is ConnectionFragment) {
            (fragment as ConnectionFragment).setSplitRouteMode(mode)
        }
    }

    override fun setUpTabLayoutForGhostMode() {
        binding.debugSend.visibility = View.GONE
        binding.logout.visibility = View.GONE
        binding.getMoreData.visibility = View.VISIBLE
        binding.login.visibility = View.VISIBLE
        binding.login.text = getString(com.windscribe.vpn.R.string.text_login)
        binding.login.setTextColor(resources.getColor(R.color.colorWhite50))
    }

    override fun setUpTabLayoutForGhostModePro() {
        binding.debugSend.visibility = View.GONE
        binding.logout.visibility = View.GONE
        binding.getMoreData.visibility = View.GONE
        binding.login.visibility = View.VISIBLE
        binding.login.text = getString(com.windscribe.vpn.R.string.claim_account)
        binding.login.setTextColor(resources.getColor(R.color.colorYellow))
    }

    override fun setUpTabLayoutForLoggedInUser() {
        binding.debugSend.visibility = View.VISIBLE
        binding.logout.visibility = View.VISIBLE
        binding.getMoreData.visibility = View.GONE
        binding.login.visibility = View.GONE
        binding.login.text = ""
        binding.login.setTextColor(resources.getColor(R.color.colorWhite50))
    }

    override fun setUsername(username: String) {
        if (fragment is AccountFragment) {
            (fragment as AccountFragment).setUsername(username)
        }
    }

    override fun setupAppsAdapter(adapter: InstalledAppsAdapter) {
        if (fragment is ConnectionFragment) {
            (fragment as ConnectionFragment).setSplitAppsAdapter(adapter)
        }
    }

    override fun setupLanguageAdapter(savedLanguage: String, languageList: Array<String>) {
        if (fragment is GeneralFragment) {
            (fragment as GeneralFragment).setLanguageAdapter(savedLanguage, languageList)
        }
    }

    override fun setupLayoutForAutoMode() {
        if (fragment is ConnectionFragment) {
            (fragment as ConnectionFragment).setConnectionMode(true)
        }
    }

    override fun setupLayoutForFreeUser(upgradeText: String) {
        if (fragment is AccountFragment) {
            (fragment as AccountFragment).setupLayoutForFreeUser(upgradeText)
        }
    }

    override fun setupLayoutForManualMode() {
        if (fragment is ConnectionFragment) {
            (fragment as ConnectionFragment).setConnectionMode(false)
        }
    }

    override fun setupLayoutForPremiumUser(upgradeText: String) {
        if (fragment is AccountFragment) {
            (fragment as AccountFragment).setupLayoutForPremiumUser(upgradeText)
        }
    }

    override fun setupPortMapAdapter(savedPort: String, ports: List<String>) {
        if (fragment is ConnectionFragment) {
            (fragment as ConnectionFragment).setPortAdapter(savedPort, ports)
        }
    }

    override fun setupProtocolAdapter(protocol: String, protocols: List<String>) {
        if (fragment is ConnectionFragment) {
            (fragment as ConnectionFragment).setProtocolAdapter(protocol, protocols)
        }
    }

    override fun setupSortAdapter(
        localiseValues: Array<String>,
        selectedItem: String,
        values: Array<String>
    ) {
        if (fragment is GeneralFragment) {
            (fragment as GeneralFragment).setSortAdapter(localiseValues, selectedItem, values)
        }
    }

    override fun showProgress(progressText: String) {
        runOnUiThread {
            sendDebugDialog?.show()
            (sendDebugDialog?.findViewById<View>(R.id.tv_dialog_header) as TextView).text =
                progressText
        }
    }

    override fun showToast(toastString: String) {
        Toast.makeText(this, toastString, Toast.LENGTH_SHORT).show()
    }

    override fun startSplitTunnelingHelpActivity() {
        startActivity(SplitTunnelingHelpActivity.getStartIntent(this))
    }

    override fun updateLocale() {
        updateLanguage()
    }

    private fun addClickListeners() {
        binding.general.setOnClickListener {
            currentSelectedScreen = 1
            initFragment()
        }
        binding.account.setOnClickListener {
            currentSelectedScreen = if (presenter.isUserInGhostMode) {
                5
            } else {
                2
            }
            initFragment()
        }
        binding.connection.setOnClickListener {
            currentSelectedScreen = 3
            initFragment()
        }
        binding.debugView.setOnClickListener {
            currentSelectedScreen = 4
            initFragment()
        }
        binding.getMoreData.setOnClickListener {
            startActivity(GetMoreDataActivity.getStartIntent(this))
        }
        binding.login.setOnClickListener {
            presenter.onLoginAndClaimClick()
        }
        binding.logout.setOnClickListener {
            presenter.onSignOutClicked()
        }
        binding.debugSend.setOnClickListener {
            presenter.onSendDebugClicked()
        }
    }

    private fun initFragment() {
        fragment = null
        val currentFragment = supportFragmentManager.findFragmentById(R.id.container)
        when (currentSelectedScreen) {
            1 -> if (currentFragment !is GeneralFragment) {
                onContainerHidden(false)
                TransitionManager.beginDelayedTransition(binding.parent)
                binding.general.setState(State.TwoState.SELECTED)
                binding.account.setState(State.TwoState.NOT_SELECTED)
                binding.connection.setState(State.TwoState.NOT_SELECTED)
                binding.debugView.setState(State.TwoState.NOT_SELECTED)
                fragment = GeneralFragment()
                binding.parent.setCurrentFragment(0)
            }

            2 -> if (currentFragment !is AccountFragment) {
                onContainerHidden(false)
                TransitionManager.beginDelayedTransition(binding.parent)
                binding.account.setState(State.TwoState.SELECTED)
                binding.general.setState(State.TwoState.NOT_SELECTED)
                binding.connection.setState(State.TwoState.NOT_SELECTED)
                binding.debugView.setState(State.TwoState.NOT_SELECTED)
                fragment = AccountFragment()
                binding.parent.setCurrentFragment(1)
            }

            3 -> if (currentFragment !is ConnectionFragment) {
                onContainerHidden(false)
                TransitionManager.beginDelayedTransition(binding.parent)
                binding.connection.setState(State.TwoState.SELECTED)
                binding.general.setState(State.TwoState.NOT_SELECTED)
                binding.account.setState(State.TwoState.NOT_SELECTED)
                binding.debugView.setState(State.TwoState.NOT_SELECTED)
                fragment = ConnectionFragment()
                binding.parent.setCurrentFragment(2)
            }

            4 -> if (currentFragment !is DebugFragment) {
                onContainerHidden(false)
                TransitionManager.beginDelayedTransition(binding.parent)
                binding.debugView.setState(State.TwoState.SELECTED)
                binding.general.setState(State.TwoState.NOT_SELECTED)
                binding.connection.setState(State.TwoState.NOT_SELECTED)
                binding.account.setState(State.TwoState.NOT_SELECTED)
                fragment = DebugFragment()
                binding.parent.setCurrentFragment(3)
            }

            5 -> if (currentFragment !is GhostAccountFragment) {
                onContainerHidden(false)
                TransitionManager.beginDelayedTransition(binding.parent)
                binding.account.setState(State.TwoState.SELECTED)
                binding.general.setState(State.TwoState.NOT_SELECTED)
                binding.connection.setState(State.TwoState.NOT_SELECTED)
                binding.debugView.setState(State.TwoState.NOT_SELECTED)
                fragment = GhostAccountFragment(presenter.isUserPro)
                binding.parent.setCurrentFragment(5)
            }
        }
        if (supportFragmentManager.isStateSaved) {
            return
        }
        fragment?.let {
            val slide = Slide()
            slide.duration = 400
            TransitionManager.beginDelayedTransition(binding.parent, slide)
            supportFragmentManager.beginTransaction().replace(R.id.container, it)
                .commitNow()
        }
    }

    companion object {
        fun getStartIntent(windscribeActivity: AppCompatActivity?): Intent {
            return Intent(windscribeActivity, SettingActivity::class.java)
        }
    }
}
