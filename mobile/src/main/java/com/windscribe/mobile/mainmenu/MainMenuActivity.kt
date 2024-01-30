/*
 * Copyright (c) 2021 Windscribe Limited.
 */
package com.windscribe.mobile.mainmenu

import android.app.ActivityOptions
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.HapticFeedbackConstants
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import butterknife.BindView
import butterknife.OnClick
import com.windscribe.mobile.BuildConfig
import com.windscribe.mobile.R
import com.windscribe.mobile.about.AboutActivity
import com.windscribe.mobile.account.AccountActivity
import com.windscribe.mobile.advance.AdvanceParamsActivity
import com.windscribe.mobile.base.BaseActivity
import com.windscribe.mobile.confirmemail.ConfirmActivity
import com.windscribe.mobile.connectionsettings.ConnectionSettingsActivity
import com.windscribe.mobile.custom_view.preferences.IconLinkView
import com.windscribe.mobile.di.ActivityModule
import com.windscribe.mobile.dialogs.ShareAppLinkDialog
import com.windscribe.mobile.email.AddEmailActivity
import com.windscribe.mobile.generalsettings.GeneralSettingsActivity
import com.windscribe.mobile.help.HelpActivity
import com.windscribe.mobile.robert.RobertSettingsActivity
import com.windscribe.mobile.upgradeactivity.UpgradeActivity
import com.windscribe.mobile.utils.UiUtil
import com.windscribe.mobile.welcome.WelcomeActivity
import com.windscribe.vpn.BuildConfig.BUILD_TYPE
import com.windscribe.vpn.BuildConfig.DEV
import com.windscribe.vpn.alert.showAlertDialog
import com.windscribe.vpn.backend.utils.WindVpnController
import com.windscribe.vpn.state.PreferenceChangeObserver
import org.slf4j.LoggerFactory
import javax.inject.Inject

class MainMenuActivity : BaseActivity(), MainMenuView {
    @JvmField
    @BindView(R.id.addEmail)
    var addEmail: Button? = null

    @JvmField
    @BindView(R.id.nav_button)
    var backButton: ImageView? = null

    @BindView(R.id.cl_data_status)
    lateinit var clDataStatus: ConstraintLayout

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
    @BindView(R.id.nav_title)
    var tvActivityTitle: TextView? = null

    @BindView(R.id.data_left)
    lateinit var tvDataLeft: TextView

    @BindView(R.id.data_upgrade_label)
    lateinit var tvDataUpgrade: TextView

    @BindView(R.id.cl_general)
    lateinit var generalView: IconLinkView

    @BindView(R.id.cl_account)
    lateinit var accountView: IconLinkView

    @BindView(R.id.cl_connection)
    lateinit var connectionView: IconLinkView

    @BindView(R.id.cl_robert)
    lateinit var robertView: IconLinkView

    @BindView(R.id.cl_help)
    lateinit var helpView: IconLinkView

    @BindView(R.id.cl_about)
    lateinit var aboutView: IconLinkView

    @BindView(R.id.cl_sign)
    lateinit var logoutView: IconLinkView

    @BindView(R.id.cl_refer_for_data)
    lateinit var referForDataView: IconLinkView

    @BindView(R.id.cl_advance)
    lateinit var advanceView: IconLinkView

    @BindView(R.id.divider_advance)
    lateinit var advanceParamDivider: ImageView

    @BindView(R.id.divider_refer_for_data)
    lateinit var referForDataDivider: ImageView

    private val logger = LoggerFactory.getLogger(TAG)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setActivityModule(ActivityModule(this, this)).inject(this)
        setContentLayout(R.layout.activity_main_menu)
        presenter.observeUserChange(this)
        preferenceChangeObserver.addLanguageChangeObserver(this) { presenter.onLanguageChanged() }
        setupCustomLayoutDelegates()
    }

    private fun setupCustomLayoutDelegates() {
        generalView.onClick {
            performHapticFeedback(it)
            presenter.onGeneralSettingsClicked()
        }
        accountView.onClick {
            performHapticFeedback(it)
            presenter.onMyAccountClicked()
        }
        connectionView.onClick {
            performHapticFeedback(it)
            presenter.onConnectionSettingsClicked()
        }
        robertView.onClick {
            performHapticFeedback(it)
            presenter.onRobertSettingsClicked()
        }
        helpView.onClick {
            performHapticFeedback(it)
            presenter.onHelpMeClicked()
        }
        aboutView.onClick {
            performHapticFeedback(it)
            presenter.onAboutClicked()
        }
        logoutView.onClick {
            performHapticFeedback(it)
            presenter.onSignOutClicked()
        }
        referForDataView.onClick {
            performHapticFeedback(it)
            presenter.onReferForDataClick()
        }
        advanceView.onClick {
            presenter.advanceViewClick()
        }
        UiUtil.setupOnTouchListener(textViewContainer = tvDataUpgrade, textView = tvDataUpgrade)
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

        logger.info("User clicked on connection settings...")
        presenter.onConnectionSettingsClicked()
    }

    @OnClick(R.id.login)
    fun onLoginClicked() {
        presenter.onLoginClicked()
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
        generalView.text = general
        accountView.text = account
        connectionView.text = connection
        helpView.text = helpMe
        logoutView.text = signOut
        aboutView.text = about
        robertView.text = robert
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
        logoutView.visibility = visibility
    }

    override fun setupLayoutForFreeUser(dataLeft: String, upgradeLabel: String, color: Int) {
        clDataStatus.visibility = View.VISIBLE
        tvDataLeft.text = dataLeft
        tvDataLeft.setTextColor(color)
        tvDataUpgrade.text = upgradeLabel
    }

    override fun setupLayoutForPremiumUser() {
        clDataStatus.visibility = View.GONE
    }

    override fun showLogoutAlert() {
        showAlertDialog(
            getString(R.string.logout),
            getString(R.string.logout_alert_description),
            getString(R.string.logout),
            getString(R.string.cancel)
        ) {
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

    override fun showShareLinkDialog() {
        ShareAppLinkDialog.show(this)
    }

    override fun showShareLinkOption() {
        referForDataView.visibility = View.VISIBLE
        referForDataDivider.visibility = View.VISIBLE
    }

    override fun showAdvanceParamsActivity() {
        val startIntent = Intent(this, AdvanceParamsActivity::class.java)
        startActivity(startIntent)
    }

    companion object {
        private const val TAG = "main_menu_a"

        @JvmStatic
        fun getStartIntent(context: Context?): Intent {
            return Intent(context, MainMenuActivity::class.java)
        }
    }
}