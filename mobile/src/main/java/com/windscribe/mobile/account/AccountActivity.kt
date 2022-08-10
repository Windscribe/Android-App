/*
 * Copyright (c) 2021 Windscribe Limited.
 */
package com.windscribe.mobile.account

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.InputFilter
import android.text.InputType
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.MotionEvent.ACTION_DOWN
import android.view.View
import android.view.WindowManager
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.widget.AppCompatEditText
import androidx.constraintlayout.widget.ConstraintLayout
import butterknife.BindView
import butterknife.ButterKnife
import butterknife.OnClick
import com.windscribe.mobile.R
import com.windscribe.mobile.base.BaseActivity
import com.windscribe.mobile.confirmemail.ConfirmActivity
import com.windscribe.mobile.custom_view.CustomDialog
import com.windscribe.mobile.custom_view.ErrorFragment
import com.windscribe.mobile.custom_view.SuccessFragment
import com.windscribe.mobile.custom_view.preferences.SingleLinkExplainView
import com.windscribe.mobile.di.ActivityModule
import com.windscribe.mobile.di.DaggerActivityComponent
import com.windscribe.mobile.email.AddEmailActivity
import com.windscribe.mobile.fragments.GhostMostAccountFragment
import com.windscribe.mobile.listeners.AccountFragmentCallback
import com.windscribe.mobile.upgradeactivity.UpgradeActivity
import com.windscribe.mobile.welcome.WelcomeActivity
import com.windscribe.vpn.Windscribe.Companion.appContext
import com.windscribe.vpn.commonutils.ThemeUtils.getColor
import org.slf4j.LoggerFactory
import java.util.*
import javax.inject.Inject

class AccountActivity : BaseActivity(), AccountView, AccountFragmentCallback {
    @BindView(R.id.warningContainer)
    lateinit var warningContainer: ConstraintLayout

    @BindView(R.id.edit_arrow)
    lateinit var editAccountArrow: ImageView

    @BindView(R.id.edit_progress)
    lateinit var editAccountProgressView: ProgressBar

    @BindView(R.id.img_progress)
    lateinit var emailProgressCircle: ProgressBar

    @BindView(R.id.nav_button)
    lateinit var imgAccountBackBtn: ImageView

    @BindView(R.id.cl_account_lazy_login)
    lateinit var lazyLoginButton: SingleLinkExplainView

    @BindView(R.id.nav_title)
    lateinit var mActivityTitleView: TextView

    @BindView(R.id.resend_email_btn)
    lateinit var resendButton: TextView

    @BindView(R.id.tv_account_email)
    lateinit var tvAccountEmail: TextView

    @BindView(R.id.tv_email_label)
    lateinit var tvAccountEmailLabel: TextView

    @BindView(R.id.tv_warning)
    lateinit var tvEmailWarning: TextView

    @BindView(R.id.tv_account_username)
    lateinit var tvAccountUserName: TextView

    @BindView(R.id.tv_edit_account)
    lateinit var tvEditAccount: TextView

    @BindView(R.id.tv_plan_data)
    lateinit var tvPlanData: TextView

    @BindView(R.id.confirm_email_icon)
    lateinit var confirmEmailIcon: ImageView

    @BindView(R.id.tv_reset_date)
    lateinit var tvResetDate: TextView

    @BindView(R.id.tv_reset_date_label)
    lateinit var tvResetDateLabel: TextView

    @BindView(R.id.tv_upgrade_info)
    lateinit var tvUpgradeInfo: TextView

    @BindView(R.id.data_left)
    lateinit var tvDataLeft: TextView

    @BindView(R.id.data_left_label)
    lateinit var dataLeftLabel: TextView

    @BindView(R.id.data_left_divider)
    lateinit var dataLeftDivider: ImageView

    @BindView(R.id.cl_edit_account)
    lateinit var clEditAccount: ConstraintLayout

    @Inject
    lateinit var mCustomProgressDialog: CustomDialog

    @Inject
    lateinit var presenter: AccountPresenter

    var alertDialog: AlertDialog? = null
    private val logger = LoggerFactory.getLogger("account_a")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        DaggerActivityComponent.builder().activityModule(ActivityModule(this, this))
            .applicationComponent(appContext.applicationComponent)
            .build().inject(this)
        presenter.setTheme(this)
        setContentView(R.layout.activity_account)
        ButterKnife.bind(this)
        presenter.observeUserData(this)
        setupCustomLayoutDelegates()
    }

    override fun onResume() {
        super.onResume()
        presenter.setLayoutFromApiSession()
    }

    override fun onDestroy() {
        presenter.onDestroy()
        super.onDestroy()
    }

    override fun goToConfirmEmailActivity() {
        startActivity(ConfirmActivity.getStartIntent(this))
    }

    override fun goToEmailActivity() {
        val startIntent = Intent(this, AddEmailActivity::class.java)
        startActivity(startIntent)
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setupCustomLayoutDelegates() {
        logger.info("User clicked Lazy login button.")
        lazyLoginButton.onClick { presenter.onLazyLoginClicked() }
        clEditAccount.setOnTouchListener { _, event ->
            if (event.action == ACTION_DOWN) {
                tvEditAccount.setTextColor(
                    getColor(
                        this,
                        R.attr.wdPrimaryColor,
                        R.color.colorWhite50
                    )
                )
            } else {
                tvEditAccount.setTextColor(
                    getColor(
                        this,
                        R.attr.wdSecondaryColor,
                        R.color.colorWhite50
                    )
                )
            }
            return@setOnTouchListener false
        }
    }

    override fun hideProgress() {
        mCustomProgressDialog.dismiss()
    }

    @OnClick(R.id.tv_account_email)
    fun onAddEmailClick() {
        logger.info(
            "User clicked on " + tvAccountEmail.text.toString()
                    + " email text view..."
        )
        presenter.onAddEmailClicked(tvAccountEmail.text.toString())
    }

    @OnClick(R.id.nav_button)
    fun onBackButtonClicked() {
        logger.info("User clicked on back arrow...")
        onBackPressed()
    }

    @OnClick(R.id.cl_edit_account)
    fun onEditAccountClick() {
        logger.info("User clicked on edit account button...")
        presenter.onEditAccountClicked()
    }

    override fun onLoginClicked() {
        val startIntent = WelcomeActivity.getStartIntent(this)
        startIntent.putExtra("startFragmentName", "Login")
        startActivity(startIntent)
    }

    override fun onSignUpClicked() {
        val startIntent = WelcomeActivity.getStartIntent(this)
        startIntent.putExtra("startFragmentName", "AccountSetUp")
        startActivity(startIntent)
    }

    @OnClick(R.id.tv_upgrade_info)
    fun onUpgradeClick() {
        logger.info(
            "User clicked on " + tvUpgradeInfo.text.toString()
                    + " upgrade button..."
        )
        presenter.onUpgradeClicked(tvUpgradeInfo.text.toString())
    }

    override fun openEditAccountInBrowser(url: String) {
        openURLInBrowser(url)
    }

    override fun openUpgradeActivity() {
        startActivity(UpgradeActivity.getStartIntent(this))
    }

    @OnClick(R.id.resend_email_btn)
    fun resendEmailClicked() {
        presenter.onResendEmail()
    }

    override fun setActivityTitle(title: String) {
        mActivityTitleView.text = title
    }

    override fun setEmail(
        email: String,
        warningText: String,
        warningColor: Int,
        emailColor: Int,
        labelColor: Int,
        infoIcon: Int,
        containerBackground: Int
    ) {
        logger.info("Setting up add email layout.")
        tvAccountEmail.text = email
        tvAccountEmail.setTextColor(emailColor)
        tvAccountEmailLabel.setTextColor(labelColor)
        warningContainer.visibility = View.VISIBLE
        confirmEmailIcon.visibility = View.VISIBLE
        confirmEmailIcon.setImageResource(infoIcon)
        warningContainer.setBackgroundResource(containerBackground)
        tvEmailWarning.text = warningText
        tvEmailWarning.setTextColor(warningColor)
        resendButton.visibility = View.GONE
    }

    override fun setEmailConfirm(
        emailConfirm: String,
        warningText: String,
        emailColor: Int,
        emailLabelColor: Int,
        infoIcon: Int,
        containerBackground: Int
    ) {
        logger.info("Setting up confirm email layout.")
        tvAccountEmail.text = emailConfirm
        tvAccountEmail.setTextColor(emailColor)
        warningContainer.visibility = View.VISIBLE
        confirmEmailIcon.visibility = View.VISIBLE
        tvAccountEmailLabel.setTextColor(emailLabelColor)
        resendButton.visibility = View.VISIBLE
        confirmEmailIcon.setImageResource(infoIcon)
        tvEmailWarning.text = warningText
        tvEmailWarning.setTextColor(emailLabelColor)
        warningContainer.setBackgroundResource(containerBackground)
    }

    override fun setEmailConfirmed(
        emailConfirm: String,
        warningText: String,
        emailColor: Int,
        emailLabelColor: Int,
        infoIcon: Int,
        containerBackground: Int
    ) {
        logger.info("Setting up confirmed email layout.")
        tvAccountEmail.text = emailConfirm
        tvAccountEmail.setTextColor(emailColor)
        warningContainer.visibility = View.VISIBLE
        confirmEmailIcon.visibility = View.VISIBLE
        confirmEmailIcon.setImageResource(infoIcon)
        tvAccountEmailLabel.setTextColor(emailLabelColor)
        resendButton.visibility = View.GONE
        tvEmailWarning.text = warningText
        tvEmailWarning.setTextColor(emailColor)
        warningContainer.setBackgroundResource(containerBackground)
    }

    override fun setPlanName(planName: String) {
        logger.info("Displaying user plan name ...")
        tvPlanData.text = planName
    }

    override fun setDataLeft(dataLeft: String) {
        if (dataLeft.isEmpty()) {
            dataLeftDivider.visibility = View.GONE
            dataLeftLabel.visibility = View.GONE
            tvDataLeft.visibility = View.GONE
        } else {
            dataLeftDivider.visibility = View.VISIBLE
            dataLeftLabel.visibility = View.VISIBLE
            tvDataLeft.visibility = View.VISIBLE
            tvDataLeft.text = dataLeft
        }
    }

    override fun setResetDate(resetDateLabel: String, resetDate: String) {
        logger.info("Displaying user next reset date ...")
        tvResetDateLabel.text = resetDateLabel
        tvResetDate.text = resetDate
    }

    override fun setUsername(username: String) {
        logger.info("Displaying account username ...")
        tvAccountUserName.text = username
    }

    override fun setWebSessionLoading(show: Boolean) {
        editAccountArrow.visibility = if (show) View.GONE else View.VISIBLE
        editAccountProgressView.visibility =
            if (show) View.VISIBLE else View.GONE
        tvEditAccount.isEnabled = !show
    }

    override fun setupLayoutForFreeUser(upgradeText: String, color: Int) {
        logger.info("Setting up layout for free user...")
        tvUpgradeInfo.text = upgradeText
        tvUpgradeInfo.setTextColor(color)
    }

    override fun setupLayoutForGhostMode(proUser: Boolean) {
        GhostMostAccountFragment.getInstance().add(this, R.id.fragment_container, false, proUser)
    }

    override fun setupLayoutForPremiumUser(upgradeText: String, color: Int) {
        logger.info("Setting up layout for premium user...")
        tvUpgradeInfo.text = upgradeText
        tvUpgradeInfo.setTextColor(color)
    }

    override fun showEnterCodeDialog() {
        val alert = AlertDialog.Builder(this, R.style.OverlayAlert)
        val view = LayoutInflater.from(this).inflate(R.layout.alert_input_layout, null)
        val editText = view.findViewById<AppCompatEditText>(R.id.alert_edit_view)
        editText.filters = arrayOf(InputFilter.AllCaps(), InputFilter.LengthFilter(9))
        editText.inputType = InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS
        editText.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(editable: Editable) {
                if (editable.length == 9 && alertDialog != null) {
                    alertDialog!!.getButton(DialogInterface.BUTTON_POSITIVE).performClick()
                }
            }

            override fun beforeTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {}
            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
                if (editText.text != null) {
                    val replaceIndex = editText.selectionEnd
                    if (replaceIndex == 4 && start == 3) {
                        editText.setText(String.format("%s-", editText.text.toString()))
                        editText.setSelection(editText.text!!.length)
                    }
                    if (replaceIndex == 5 && s[s.length - 1] != '-') {
                        editText.text!!.insert(4, "-")
                        editText.setSelection(editText.text!!.length)
                    }
                }
            }
        })
        alert.setTitle(R.string.enter_code)
        alert.setView(view)
        alert.setPositiveButton(R.string.enter) { _: DialogInterface, _: Int ->
            val code = Objects.requireNonNull(editText.text).toString()
            presenter.onCodeEntered(code)
        }
        alert.setNegativeButton(R.string.cancel) { dialog: DialogInterface, _: Int -> dialog.dismiss() }
        alertDialog = alert.create()
        editText.requestFocus()
        alertDialog?.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE)
        alertDialog?.show()
    }

    override fun showErrorDialog(error: String) {
        ErrorFragment.getInstance().add(
            error, this, R.id.fragment_container, true,
            getColor(this, R.attr.overlayDialogBackgroundColor, R.color.colorDeepBlue90)
        )
    }

    override fun showErrorMessage(errorMessage: String) {
        Toast.makeText(this, errorMessage, Toast.LENGTH_SHORT).show()
    }

    override fun showProgress(progressText: String) {
        mCustomProgressDialog.show()
        (mCustomProgressDialog.findViewById<View>(R.id.tv_dialog_header) as TextView).text =
            progressText
    }

    override fun showSuccessDialog(message: String) {
        SuccessFragment.getInstance().add(
            message, this, R.id.fragment_container, true,
            getColor(this, R.attr.overlayDialogBackgroundColor, R.color.colorDeepBlue90)
        )
    }

    companion object {
        fun getStartIntent(context: Context?): Intent {
            return Intent(context, AccountActivity::class.java)
        }
    }
}