/*
 * Copyright (c) 2021 Windscribe Limited.
 */
package com.windscribe.mobile.welcome

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.transition.Slide
import android.view.Gravity
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.core.app.ShareCompat
import androidx.core.content.FileProvider
import androidx.core.view.GravityCompat
import androidx.fragment.app.Fragment
import butterknife.ButterKnife
import com.windscribe.mobile.R
import com.windscribe.mobile.alert.UnknownErrorAlert
import com.windscribe.mobile.alert.UnknownErrorAlert.LoginAttemptFailedAlertInterface
import com.windscribe.mobile.base.BaseActivity
import com.windscribe.mobile.custom_view.ErrorFragment
import com.windscribe.mobile.custom_view.ProgressFragment
import com.windscribe.mobile.di.ActivityModule
import com.windscribe.mobile.di.DaggerActivityComponent
import com.windscribe.mobile.welcome.fragment.*
import com.windscribe.mobile.windscribe.WindscribeActivity
import com.windscribe.vpn.Windscribe.Companion.appContext
import com.windscribe.vpn.constants.NetworkKeyConstants
import com.windscribe.vpn.constants.NetworkKeyConstants.getWebsiteLink
import java.io.File
import javax.inject.Inject

class WelcomeActivity : BaseActivity(), FragmentCallback, WelcomeView,
    LoginAttemptFailedAlertInterface {

    @Inject
    lateinit var presenter: WelcomePresenter

    private val requestLocationPermissionCode = 201
    private var softInputAssist: SoftInputAssist? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_welcome)
        DaggerActivityComponent.builder().activityModule(ActivityModule(this, this))
            .applicationComponent(
                appContext
                    .applicationComponent
            ).build().inject(this)
        ButterKnife.bind(this)
        addStartFragment()
    }

    override fun onResume() {
        super.onResume()
        softInputAssist?.onResume()
    }

    override fun onPause() {
        super.onPause()
        softInputAssist?.onPause()
    }

    override fun onDestroy() {
        softInputAssist?.onDestroy()
        presenter.onDestroy()
        super.onDestroy()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<String>,
        grantResults: IntArray
    ) {
        if (requestCode == requestLocationPermissionCode) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                presenter.exportLog()
            } else {
                showToast("Please provide storage permission")
            }
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    override fun clearInputErrors() {
        val fragment = supportFragmentManager.findFragmentById(R.id.fragment_container)
        if (fragment is WelcomeActivityCallback) {
            (fragment as WelcomeActivityCallback).clearInputErrors()
        }
    }

    override fun contactSupport() {
        openURLInBrowser(getWebsiteLink(NetworkKeyConstants.URL_HELP_ME))
    }

    override fun exportLog() {
        presenter.exportLog()
    }

    override fun goToSignUp() {
        val signUpFragment = SignUpFragment(false)
        val direction = GravityCompat
            .getAbsoluteGravity(GravityCompat.END, resources.configuration.layoutDirection)
        signUpFragment.enterTransition =
            Slide(direction).addTarget(R.id.sign_up_container)
        replaceFragment(signUpFragment, true)
    }

    override fun gotoHomeActivity(clearTop: Boolean) {
        val startIntent = Intent(this, WindscribeActivity::class.java)
        if (clearTop) {
            startIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }
        startActivity(startIntent)
        finish()
    }

    override fun hideSoftKeyboard() {
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(
            window.decorView.windowToken,
            InputMethodManager.HIDE_NOT_ALWAYS
        )
    }

    override fun launchShareIntent(file: File) {
        val fileUri = FileProvider.getUriForFile(
            this,
            "com.windscribe.vpn.provider",
            file
        )
        ShareCompat.IntentBuilder.from(this)
            .setType("*/*")
            .setStream(fileUri).startChooser()
    }

    override fun onAccountClaimButtonClick(
        username: String,
        password: String,
        email: String,
        ignoreEmptyEmail: Boolean
    ) {
        presenter.startAccountClaim(username, password, email, ignoreEmptyEmail)
    }

    override fun onBackButtonPressed() {
        onBackPressed()
    }

    override fun onBackPressed() {
        presenter.onBackPressed()
        super.onBackPressed()
    }

    override fun onContinueWithOutAccountClick() {
        presenter.startGhostAccountSetup()
    }

    override fun onForgotPasswordClick() {
        openURLInBrowser(getWebsiteLink(NetworkKeyConstants.URL_FORGOT_PASSWORD))
    }

    override fun onLoginButtonClick(username: String, password: String, twoFa: String) {
        presenter.startLoginProcess(username, password, twoFa)
    }

    override fun onLoginClick() {
        val loginFragment = LoginFragment()
        val direction = GravityCompat
            .getAbsoluteGravity(GravityCompat.END, resources.configuration.layoutDirection)
        loginFragment.enterTransition =
            Slide(direction).addTarget(R.id.login_container)
        replaceFragment(loginFragment, true)
    }

    override fun onSignUpButtonClick(
        username: String,
        password: String,
        email: String,
        referralUsername: String,
        ignoreEmptyEmail: Boolean
    ) {
        if (ignoreEmptyEmail) {
            supportFragmentManager.popBackStack()
        }
        presenter.startSignUpProcess(
            username,
            password,
            email,
            referralUsername,
            ignoreEmptyEmail
        )
    }

    override fun onSkipToHomeClick() {
        val startIntent = Intent(this, WindscribeActivity::class.java)
        startActivity(startIntent)
        finish()
    }

    override fun prepareUiForApiCallFinished() {
        val progressFragment = supportFragmentManager.findFragmentById(R.id.progress_container)
        if (progressFragment is ProgressFragment || progressFragment is NoEmailAttentionFragment) {
            supportFragmentManager.popBackStack()
        }
        val mainFragment = supportFragmentManager.findFragmentById(R.id.fragment_container)
        if (mainFragment is NoEmailAttentionFragment) {
            supportFragmentManager.popBackStack()
        }
    }

    override fun prepareUiForApiCallStart() {
        supportFragmentManager.findFragmentById(R.id.progress_container)?.let {
            if (it !is ProgressFragment) {
                ProgressFragment.getInstance().add(this, R.id.progress_container, true)
            }
        }
    }

    private fun replaceFragment(fragment: Fragment, addToBackStack: Boolean) {
        val transaction = supportFragmentManager
            .beginTransaction()
            .replace(R.id.fragment_container, fragment)
        if (addToBackStack) {
            transaction.addToBackStack(fragment.javaClass.name)
        }
        transaction.commit()
    }

    override fun setEmailError(errorMessage: String) {
        val fragment = supportFragmentManager.findFragmentById(R.id.fragment_container)
        if (fragment is SignUpFragment) {
            fragment.setEmailError(errorMessage)
        }
    }

    override fun setFaFieldsVisibility(visible: Int) {
        val fragment = supportFragmentManager.findFragmentById(R.id.fragment_container)
        if (fragment is LoginFragment) {
            fragment.setTwoFaVisibility(visible)
        }
    }

    override fun setLoginRegistrationError(error: String) {
        val fragment = supportFragmentManager.findFragmentById(R.id.fragment_container)
        if (fragment is WelcomeActivityCallback) {
            (fragment as WelcomeActivityCallback).setLoginError(error)
        }
    }

    override fun setPasswordError(error: String) {
        val fragment = supportFragmentManager.findFragmentById(R.id.fragment_container)
        if (fragment is WelcomeActivityCallback) {
            (fragment as WelcomeActivityCallback).setPasswordError(error)
        }
    }

    override fun setTwoFaError(errorMessage: String) {
        val fragment = supportFragmentManager.findFragmentById(R.id.fragment_container)
        if (fragment is LoginFragment) {
            fragment.setTwoFaError(errorMessage)
        }
    }

    override fun setUsernameError(error: String) {
        val fragment = supportFragmentManager.findFragmentById(R.id.fragment_container)
        if (fragment is WelcomeActivityCallback) {
            (fragment as WelcomeActivityCallback).setUsernameError(error)
        }
    }

    override fun setWindow() {
        val statusBarColor = resources.getColor(android.R.color.transparent)
        window.decorView.systemUiVisibility =
            View.SYSTEM_UI_FLAG_LAYOUT_STABLE or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
        window.statusBarColor = statusBarColor
    }

    override fun showError(error: String) {
        ErrorFragment.getInstance().add(error, this, R.id.fragment_container, true)
    }

    override fun showFailedAlert(error: String) {
        runOnUiThread {
            val unknownErrorAlert = UnknownErrorAlert.newInstance(error)
            unknownErrorAlert.show(supportFragmentManager, "failed_login")
        }
    }

    override fun showNoEmailAttentionFragment(
        username: String, password: String, accountClaim: Boolean,
        pro: Boolean
    ) {
        val noEmailAttentionFragment =
            NoEmailAttentionFragment(accountClaim, username, password, pro)
        noEmailAttentionFragment.enterTransition = Slide(Gravity.BOTTOM)
            .addTarget(R.id.email_fragment_container)
        supportFragmentManager.beginTransaction()
            .add(R.id.fragment_container, noEmailAttentionFragment)
            .addToBackStack(noEmailAttentionFragment.javaClass.name)
            .commit()
    }

    override fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    override fun updateCurrentProcess(mCurrentCall: String) {
        runOnUiThread {
            val fragment = supportFragmentManager.findFragmentById(R.id.progress_container)
            if (fragment is ProgressFragment) {
                fragment.updateProgressStatus(mCurrentCall)
            }
        }
    }

    private fun addStartFragment() {
        val startFragmentName = intent.getStringExtra("startFragmentName")
        val skipToHome = intent.getBooleanExtra("skipToHome", false)
        val fragment: Fragment
        if (startFragmentName != null && startFragmentName == "Login") {
            fragment = LoginFragment()
            softInputAssist = SoftInputAssist(this, intArrayOf(R.id.forgot_password))
        } else if (startFragmentName != null && startFragmentName == "SignUp") {
            softInputAssist = SoftInputAssist(this, intArrayOf(R.id.forgot_password))
            fragment = SignUpFragment(false)
        } else if (startFragmentName != null && startFragmentName == "AccountSetUp") {
            softInputAssist =
                SoftInputAssist(this, intArrayOf(R.id.forgot_password, R.id.set_up_later_button))
            val proAccount = presenter.isUserPro
            fragment = SignUpFragment(proAccount)
        } else {
            softInputAssist = SoftInputAssist(this, intArrayOf(R.id.forgot_password))
            fragment = WelcomeFragment()
        }
        val bundle = Bundle()
        bundle.putString("startFragmentName", startFragmentName)
        bundle.putBoolean("skipToHome", skipToHome)
        fragment.arguments = bundle
        val direction = GravityCompat
            .getAbsoluteGravity(GravityCompat.END, resources.configuration.layoutDirection)
        fragment.enterTransition =
            Slide(direction).addTarget(R.id.welcome_container)
        replaceFragment(fragment, false)
    }

    companion object {
        @JvmStatic
        fun getStartIntent(context: Context?): Intent {
            return Intent(context, WelcomeActivity::class.java)
        }
    }
}