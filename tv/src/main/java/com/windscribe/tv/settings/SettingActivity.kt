/*
 * Copyright (c) 2021 Windscribe Limited.
 */
package com.windscribe.tv.settings

import android.annotation.SuppressLint
import android.app.TaskStackBuilder
import android.content.Intent
import android.os.Bundle
import android.transition.Slide
import android.transition.TransitionManager
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.widget.NestedScrollView
import androidx.fragment.app.Fragment
import butterknife.BindView
import butterknife.OnClick
import com.windscribe.tv.R
import com.windscribe.tv.adapter.InstalledAppsAdapter
import com.windscribe.tv.base.BaseActivity
import com.windscribe.tv.confirmemail.ConfirmActivity
import com.windscribe.tv.customview.CustomDialog
import com.windscribe.tv.di.ActivityModule
import com.windscribe.tv.email.AddEmailActivity
import com.windscribe.tv.listeners.SettingsFragmentListener
import com.windscribe.tv.moredata.GetMoreDataActivity
import com.windscribe.tv.serverlist.customviews.PreferenceHeaderItemMain
import com.windscribe.tv.serverlist.customviews.SettingFocusAware
import com.windscribe.tv.serverlist.customviews.State
import com.windscribe.tv.settings.fragment.*
import com.windscribe.tv.upgrade.UpgradeActivity
import com.windscribe.tv.welcome.WelcomeActivity
import com.windscribe.tv.windscribe.WindscribeActivity
import com.windscribe.vpn.backend.utils.WindVpnController
import com.windscribe.vpn.commonutils.WindUtilities
import com.windscribe.vpn.constants.PreferencesKeyConstants
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
    @BindView(R.id.account)
    var btnAccount: PreferenceHeaderItemMain? = null

    @JvmField
    @BindView(R.id.connection)
    var btnConnection: PreferenceHeaderItemMain? = null

    @JvmField
    @BindView(R.id.debug_view)
    var btnDebug: PreferenceHeaderItemMain? = null

    @JvmField
    @BindView(R.id.general)
    var btnGeneral: PreferenceHeaderItemMain? = null

    @JvmField
    @BindView(R.id.get_more_data)
    var btnGetMoreData: PreferenceHeaderItemMain? = null

    @JvmField
    @BindView(R.id.login)
    var btnLoginAndClaim: PreferenceHeaderItemMain? = null

    @JvmField
    @BindView(R.id.logout)
    var btnLogout: PreferenceHeaderItemMain? = null

    @JvmField
    @BindView(R.id.container)
    var container: ConstraintLayout? = null

    @JvmField
    @BindView(R.id.debug_send)
    var debugSend: PreferenceHeaderItemMain? = null

    @JvmField
    @Inject
    var sendDebugDialog: CustomDialog? = null

    @JvmField
    @BindView(R.id.parent)
    var mainLayout: SettingFocusAware? = null

    @JvmField
    @BindView(R.id.scrollView)
    var scrollView: NestedScrollView? = null

    @JvmField
    @BindView(R.id.title)
    var titleTextView: TextView? = null

    @JvmField
    @BindView(R.id.gradient)
    var topGradient: TextView? = null

    @JvmField
    @BindView(R.id.version)
    var versionView: TextView? = null
    private var currentSelectedScreen = 1
    private var fragment: Fragment? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setActivityModule(ActivityModule(this, this)).inject(this)
        setContentLayout(R.layout.activity_setting)
        initFragment()
        scrollView?.let {
            it.setOnScrollChangeListener { _: NestedScrollView?, _: Int, _: Int, _: Int, _: Int ->
                if (fragment is GeneralFragment) {
                    (fragment as GeneralFragment).checkViewVisibility(it)
                }
            }
        }
        presenter.setUpTabMenu()
        versionView?.text = String.format("%s", WindUtilities.getVersionName())
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

    override fun onContainerHidden(hidden: Boolean) {
        if (hidden) {
            if (topGradient?.visibility == View.INVISIBLE) {
                topGradient?.visibility = View.VISIBLE
            }
        } else {
            if (topGradient?.visibility == View.VISIBLE) {
                topGradient?.visibility = View.INVISIBLE
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
        TaskStackBuilder.create(this)
                .addNextIntent(WindscribeActivity.getStartIntent(this))
                .addNextIntent(SettingActivity.getStartIntent(this))
                .addNextIntentWithParentStack(intent)
                .startActivities()

    }

    override fun setBootStartMode(mode: String) {
        if (fragment is ConnectionFragment) {
            (fragment as ConnectionFragment).setBootOnStart(mode)
        }
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
        debugSend?.visibility = View.GONE
        btnLogout?.visibility = View.GONE
        btnGetMoreData?.visibility = View.VISIBLE
        btnLoginAndClaim?.visibility = View.VISIBLE
        btnLoginAndClaim?.text = getString(R.string.text_login)
        btnLoginAndClaim?.setTextColor(resources.getColor(R.color.colorWhite50))
    }

    override fun setUpTabLayoutForGhostModePro() {
        debugSend?.visibility = View.GONE
        btnLogout?.visibility = View.GONE
        btnGetMoreData?.visibility = View.GONE
        btnLoginAndClaim?.visibility = View.VISIBLE
        btnLoginAndClaim?.text = getString(R.string.claim_account)
        btnLoginAndClaim?.setTextColor(resources.getColor(R.color.colorYellow))
    }

    override fun setUpTabLayoutForLoggedInUser() {
        debugSend?.visibility = View.VISIBLE
        btnLogout?.visibility = View.VISIBLE
        btnGetMoreData?.visibility = View.GONE
        btnLoginAndClaim?.visibility = View.GONE
        btnLoginAndClaim?.text = ""
        btnLoginAndClaim?.setTextColor(resources.getColor(R.color.colorWhite50))
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

    override fun setupSortAdapter(savedSort: String, sortList: Array<String>) {
        if (fragment is GeneralFragment) {
            (fragment as GeneralFragment).setSortAdapter(savedSort, sortList)
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

    @OnClick(R.id.account)
    fun onAccountClick() {
        currentSelectedScreen = if (presenter.isUserInGhostMode) {
            5
        } else {
            2
        }
        initFragment()
    }

    @OnClick(R.id.connection)
    fun onConnectionClick() {
        currentSelectedScreen = 3
        initFragment()
    }

    @OnClick(R.id.debug_view)
    fun onDebugClick() {
        currentSelectedScreen = 4
        initFragment()
    }

    @OnClick(R.id.general)
    fun onGeneralAccount() {
        currentSelectedScreen = 1
        initFragment()
    }

    @SuppressLint("NonConstantResourceId")
    @OnClick(R.id.get_more_data)
    fun onGetMoreDataClick() {
        startActivity(GetMoreDataActivity.getStartIntent(this))
    }

    @OnClick(R.id.login)
    fun onLoginAndClaimClick() {
        presenter.onLoginAndClaimClick()
    }

    @OnClick(R.id.logout)
    fun onLogoutClick() {
        presenter.onSignOutClicked()
    }

    @OnClick(R.id.debug_send)
    fun onSendLogClick() {
        presenter.onSendDebugClicked()
    }

    private fun initFragment() {
        fragment = null
        val currentFragment = supportFragmentManager.findFragmentById(R.id.container)
        when (currentSelectedScreen) {
            1 -> if (currentFragment !is GeneralFragment) {
                onContainerHidden(false)
                TransitionManager.beginDelayedTransition(mainLayout)
                btnGeneral?.setState(State.TwoState.SELECTED)
                btnAccount?.setState(State.TwoState.NOT_SELECTED)
                btnConnection?.setState(State.TwoState.NOT_SELECTED)
                btnDebug?.setState(State.TwoState.NOT_SELECTED)
                fragment = GeneralFragment()
                mainLayout?.setCurrentFragment(0)
            }
            2 -> if (currentFragment !is AccountFragment) {
                onContainerHidden(false)
                TransitionManager.beginDelayedTransition(mainLayout)
                btnAccount?.setState(State.TwoState.SELECTED)
                btnGeneral?.setState(State.TwoState.NOT_SELECTED)
                btnConnection?.setState(State.TwoState.NOT_SELECTED)
                btnDebug?.setState(State.TwoState.NOT_SELECTED)
                fragment = AccountFragment()
                mainLayout?.setCurrentFragment(1)
            }
            3 -> if (currentFragment !is ConnectionFragment) {
                onContainerHidden(false)
                TransitionManager.beginDelayedTransition(mainLayout)
                btnConnection?.setState(State.TwoState.SELECTED)
                btnGeneral?.setState(State.TwoState.NOT_SELECTED)
                btnAccount?.setState(State.TwoState.NOT_SELECTED)
                btnDebug?.setState(State.TwoState.NOT_SELECTED)
                fragment = ConnectionFragment()
                mainLayout?.setCurrentFragment(2)
            }
            4 -> if (currentFragment !is DebugFragment) {
                onContainerHidden(false)
                TransitionManager.beginDelayedTransition(mainLayout)
                btnDebug?.setState(State.TwoState.SELECTED)
                btnGeneral?.setState(State.TwoState.NOT_SELECTED)
                btnConnection?.setState(State.TwoState.NOT_SELECTED)
                btnAccount?.setState(State.TwoState.NOT_SELECTED)
                fragment = DebugFragment()
                mainLayout?.setCurrentFragment(3)
            }
            5 -> if (currentFragment !is GhostAccountFragment) {
                onContainerHidden(false)
                TransitionManager.beginDelayedTransition(mainLayout)
                btnAccount?.setState(State.TwoState.SELECTED)
                btnGeneral?.setState(State.TwoState.NOT_SELECTED)
                btnConnection?.setState(State.TwoState.NOT_SELECTED)
                btnDebug?.setState(State.TwoState.NOT_SELECTED)
                fragment = GhostAccountFragment(presenter.isUserPro)
                mainLayout?.setCurrentFragment(5)
            }
        }
        if (supportFragmentManager.isStateSaved) {
            return
        }
        fragment?.let {
            val slide = Slide()
            slide.duration = 400
            TransitionManager.beginDelayedTransition(mainLayout, slide)
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
