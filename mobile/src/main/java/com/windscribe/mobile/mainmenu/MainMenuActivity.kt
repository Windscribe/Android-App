/*
 * Copyright (c) 2021 Windscribe Limited.
 */
package com.windscribe.mobile.mainmenu

import android.app.ActivityOptions
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Pair
import android.view.HapticFeedbackConstants
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import butterknife.BindView
import butterknife.OnClick
import com.windscribe.mobile.R
import com.windscribe.mobile.about.AboutActivity
import com.windscribe.mobile.account.AccountActivity
import com.windscribe.mobile.base.BaseActivity
import com.windscribe.mobile.confirmemail.ConfirmActivity
import com.windscribe.mobile.connectionsettings.ConnectionSettingsActivity
import com.windscribe.mobile.di.ActivityModule
import com.windscribe.mobile.email.AddEmailActivity
import com.windscribe.mobile.generalsettings.GeneralSettingsActivity
import com.windscribe.mobile.help.HelpActivity
import com.windscribe.mobile.robert.RobertSettingsActivity
import com.windscribe.mobile.upgradeactivity.UpgradeActivity
import com.windscribe.mobile.welcome.WelcomeActivity
import com.windscribe.vpn.alert.showAlertDialog
import com.windscribe.vpn.backend.utils.WindVpnController
import com.windscribe.vpn.state.PreferenceChangeObserver
import javax.inject.Inject
import org.slf4j.LoggerFactory

class MainMenuActivity : BaseActivity(), MainMenuView {
    @JvmField
    @BindView(R.id.addEmail)
    var addEmail: Button? = null

    @JvmField
    @BindView(R.id.nav_button)
    var backButton: ImageView? = null

    @JvmField
    @BindView(R.id.cl_data_status)
    var clDataStatus: ConstraintLayout? = null

    @JvmField
    @BindView(R.id.cl_sign)
    var clSign: ConstraintLayout? = null

    @JvmField
    @BindView(R.id.confirmEmail)
    var confirmEmail: Button? = null

    @JvmField
    @BindView(R.id.login)
    var loginButton: Button? = null

    @Inject
    lateinit var presenter: MainMenuPresenter

    @Inject
    lateinit var preferenceChangeObserver: PreferenceChangeObserver

    @Inject
    lateinit var vpnController: WindVpnController

    @JvmField
    @BindView(R.id.setupAccount)
    var setupAccountButton: Button? = null

    @JvmField
    @BindView(R.id.tv_about_label)
    var tvAboutLabel: TextView? = null

    @JvmField
    @BindView(R.id.nav_title)
    var tvActivityTitle: TextView? = null

    @JvmField
    @BindView(R.id.tv_connection_label)
    var tvConnection: TextView? = null

    @JvmField
    @BindView(R.id.data_left)
    var tvDataLeft: TextView? = null

    @JvmField
    @BindView(R.id.data_upgrade_label)
    var tvDataUpgrade: TextView? = null

    @JvmField
    @BindView(R.id.tv_account_label)
    var tvMenuItemAccount: TextView? = null

    @JvmField
    @BindView(R.id.tv_preference_label)
    var tvMenuItemGeneral: TextView? = null

    @JvmField
    @BindView(R.id.tv_help_label)
    var tvMenuItemHelpMe: TextView? = null

    @JvmField
    @BindView(R.id.tv_robert_label)
    var tvRobert: TextView? = null

    @JvmField
    @BindView(R.id.tv_sign_label)
    var tvSign: TextView? = null
    private val logger = LoggerFactory.getLogger(TAG)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setActivityModule(ActivityModule(this, this)).inject(this)
        setContentLayout(R.layout.activity_main_menu)
        presenter.observeUserChange(this)
        preferenceChangeObserver.addLanguageChangeObserver(this) { presenter.onLanguageChanged() }
    }

    override fun onResume() {
        super.onResume()
        presenter.setLayoutFromApiSession()
    }

    override fun onDestroy() {
        presenter.onDestroy()
        super.onDestroy()
    }

    override fun goRobertSettingsActivity() {
        val intent = RobertSettingsActivity.getStartIntent(this)
        val options = ActivityOptions.makeSceneTransitionAnimation(this)
        startActivity(intent, options.toBundle())
    }

    override fun gotToHelp() {
        val intent = HelpActivity.getStartIntent(this)
        val options = ActivityOptions.makeSceneTransitionAnimation(this)
        startActivity(intent, options.toBundle())
    }

    override fun gotoAboutActivity() {
        val intent = AboutActivity.getStartIntent(this)
        val options = ActivityOptions.makeSceneTransitionAnimation(this)
        startActivity(intent, options.toBundle())
    }

    override fun gotoAccountActivity() {
        val intent = AccountActivity.getStartIntent(this)
        val options = ActivityOptions.makeSceneTransitionAnimation(this)
        startActivity(intent, options.toBundle())
    }

    override fun gotoConnectionSettingsActivity() {
        val intent = ConnectionSettingsActivity.getStartIntent(this)
        val options = ActivityOptions.makeSceneTransitionAnimation(this)
        startActivity(intent, options.toBundle())
    }

    override fun gotoGeneralSettingsActivity() {
        val intent = GeneralSettingsActivity.getStartIntent(this)
        val options = ActivityOptions.makeSceneTransitionAnimation(this)
        startActivity(intent, options.toBundle())
    }

    override fun gotoLoginRegistrationActivity() {
        val intent = WelcomeActivity.getStartIntent(this)
        intent.addFlags(
            Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_NO_HISTORY or Intent.FLAG_ACTIVITY_CLEAR_TASK
        )
        startActivity(intent)
        finish()
    }

    @OnClick(R.id.cl_about)
    fun onAboutClick() {
        performHapticFeedback(tvMenuItemHelpMe)
        logger.info("User clicked on about...")
        presenter.onAboutClicked()
    }

    @OnClick(R.id.cl_account)
    fun onAccountClick() {
        performHapticFeedback(tvMenuItemAccount)
        logger.info("User clicked on my account...")
        presenter.onMyAccountClicked()
    }

    @OnClick(R.id.setupAccount)
    fun onAccountSetUpClicked() {
        presenter.onAccountSetUpClicked()
    }

    @OnClick(R.id.addEmail)
    fun onAddEmailClicked() {
        presenter.onAddEmailClicked()
    }

    @OnClick(R.id.nav_button)
    fun onBackButtonClicked() {
        performHapticFeedback(backButton)
        logger.info("User clicked on back arrow...")
        onBackPressed()
    }

    @OnClick(R.id.confirmEmail)
    fun onConfirmEmailClicked() {
        presenter.onConfirmEmailClicked()
    }

    @OnClick(R.id.cl_connection)
    fun onConnectionSettingsClick() {
        performHapticFeedback(tvConnection)
        logger.info("User clicked on connection settings...")
        presenter.onConnectionSettingsClicked()
    }

    @OnClick(R.id.cl_general)
    fun onGeneralClick() {
        performHapticFeedback(tvMenuItemGeneral)
        logger.info("User clicked on general settings...")
        presenter.onGeneralSettingsClicked()
    }

    @OnClick(R.id.cl_help)
    fun onHelpMeClick() {
        performHapticFeedback(tvMenuItemHelpMe)
        logger.info("User clicked on network security...")
        presenter.onHelpMeClicked()
    }

    @OnClick(R.id.login)
    fun onLoginClicked() {
        presenter.onLoginClicked()
    }

    @OnClick(R.id.cl_robert)
    fun onRobertSettingsClick() {
        performHapticFeedback(tvRobert)
        logger.info("User clicked on robert settings...")
        presenter.onRobertSettingsClicked()
    }

    @OnClick(R.id.cl_sign, R.id.tv_sign_label)
    fun onSignOutClicked() {
        performHapticFeedback(tvSign)
        logger.info("User clicked on sign out button...")
        presenter.onSignOutClicked()
    }

    @OnClick(R.id.data_upgrade_label)
    fun onUpgradeClicked() {
        presenter.onUpgradeClicked()
    }

    override fun openConfirmEmailActivity() {
        startActivity(ConfirmActivity.getStartIntent(this))
    }

    override fun openUpgradeActivity() {
        startActivity(UpgradeActivity.getStartIntent(this))
    }

    override fun resetAllTextResources(
        activityTitle: String,
        general: String,
        account: String,
        connection: String,
        helpMe: String,
        signOut: String,
        about: String,
        robert: String
    ) {
        tvActivityTitle?.text = activityTitle
        tvMenuItemGeneral?.text = general
        tvMenuItemAccount?.text = account
        tvConnection?.text = connection
        tvMenuItemHelpMe?.text = helpMe
        tvSign?.text = signOut
        tvAboutLabel?.text = about
        tvRobert?.text = robert
    }

    override fun setActionButtonVisibility(
        loginButtonVisibility: Int,
        addEmailButtonVisibility: Int,
        setUpAccountButtonVisibility: Int,
        confirmEmailButtonVisibility: Int
    ) {
        loginButton?.visibility = loginButtonVisibility
        addEmail?.visibility = addEmailButtonVisibility
        setupAccountButton?.visibility = setUpAccountButtonVisibility
        confirmEmail?.visibility = confirmEmailButtonVisibility
    }

    override fun setActivityTitle(title: String) {
        tvActivityTitle?.text = title
    }

    override fun setLoginButtonVisibility(visibility: Int) {
        clSign?.visibility = visibility
    }

    override fun setupLayoutForFreeUser(dataLeft: String, upgradeLabel: String, color: Int) {
        clDataStatus?.visibility = View.VISIBLE
        tvDataLeft?.text = dataLeft
        tvDataLeft?.setTextColor(color)
        tvDataUpgrade?.text = upgradeLabel
    }

    override fun setupLayoutForPremiumUser() {
        clDataStatus?.visibility = View.GONE
    }

    override fun showLogoutAlert() {
        showAlertDialog(getString(R.string.logout), getString(R.string.logout_alert_description), getString(R.string.logout), getString(R.string.cancel)) {
            presenter.continueWithLogoutClicked()
        }
    }

    override fun startAccountSetUpActivity() {
        val startIntent = WelcomeActivity.getStartIntent(this)
        startIntent.putExtra("startFragmentName", "AccountSetUp")
        startActivity(startIntent)
    }

    override fun startAddEmailActivity() {
        val startIntent = Intent(this, AddEmailActivity::class.java)
        startActivity(startIntent)
    }

    override fun startLoginActivity() {
        val startIntent = WelcomeActivity.getStartIntent(this)
        startIntent.putExtra("startFragmentName", "Login")
        startActivity(startIntent)
    }

    private fun performHapticFeedback(view: View?) {
        if (presenter.isHapticFeedbackEnabled()) {
            view?.isHapticFeedbackEnabled = true
            view?.performHapticFeedback(
                HapticFeedbackConstants.VIRTUAL_KEY,
                HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING
            )
        }
    }

    companion object {
        private const val TAG = "main_menu_a"

        @JvmStatic
        fun getStartIntent(context: Context?): Intent {
            return Intent(context, MainMenuActivity::class.java)
        }
    }
}