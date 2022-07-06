/*
 * Copyright (c) 2021 Windscribe Limited.
 */
package com.windscribe.tv.welcome

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.transition.Slide
import android.view.Gravity
import android.view.inputmethod.InputMethodManager
import android.widget.ImageView
import android.widget.Toast
import androidx.core.app.ShareCompat
import androidx.core.content.FileProvider
import androidx.core.view.GravityCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentTransaction
import butterknife.BindView
import com.windscribe.tv.R
import com.windscribe.tv.base.BaseActivity
import com.windscribe.tv.customview.ErrorFragment
import com.windscribe.tv.customview.ProgressFragment
import com.windscribe.tv.di.ActivityModule
import com.windscribe.tv.email.AddEmailActivity
import com.windscribe.tv.welcome.fragment.ForgotPasswordFragment
import com.windscribe.tv.welcome.fragment.FragmentCallback
import com.windscribe.tv.welcome.fragment.LoginFragment
import com.windscribe.tv.welcome.fragment.NoEmailAttentionFragment
import com.windscribe.tv.welcome.fragment.SignUpFragment
import com.windscribe.tv.welcome.fragment.TwoFactorFragment
import com.windscribe.tv.welcome.fragment.WelcomeActivityCallback
import com.windscribe.tv.welcome.fragment.WelcomeFragment
import com.windscribe.tv.windscribe.WindscribeActivity
import com.windscribe.vpn.constants.PreferencesKeyConstants
import java.io.File
import javax.inject.Inject

class WelcomeActivity :
    BaseActivity(),
    FragmentCallback,
    WelcomeView,
    FragmentManager.OnBackStackChangedListener {
    @JvmField
    @BindView(R.id.image)
    var backgroundImageView: ImageView? = null

    @Inject
    lateinit var presenter: WelcomePresenter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setActivityModule(ActivityModule(this, this)).inject(this)
        setContentLayout(R.layout.activity_welcome)
        registerFragmentChangeListener()
        addStartFragment()
    }

    override fun onDestroy() {
        supportFragmentManager.removeOnBackStackChangedListener(this)
        presenter.onDestroy()
        super.onDestroy()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        if (requestCode == 201) {
            if (permissionGranted()) {
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

    override fun goToSignUp() {
        val signUpFragment = SignUpFragment()
        val direction = GravityCompat
            .getAbsoluteGravity(GravityCompat.END, resources.configuration.layoutDirection)
        signUpFragment.enterTransition = Slide(direction)
            .addTarget(R.id.login_sign_up_container)
        replaceFragment(signUpFragment, true)
    }

    override fun gotoAddEmailActivity(proUser: Boolean) {
        val intent = AddEmailActivity.getStartIntent(this)
        intent.putExtra("pro_user", proUser)
        intent.action = PreferencesKeyConstants.ACTION_ADD_EMAIL_FROM_LOGIN
        startActivity(intent)
    }

    override fun gotoHomeActivity() {
        val intent = Intent(this, WindscribeActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        startActivity(intent)
        finish()
    }

    override fun hideSoftKeyboard() {
        val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(
            window
                .decorView.windowToken,
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
        email: String?,
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

    override fun onBackStackChanged() {
        val fragment = supportFragmentManager.findFragmentById(R.id.fragment_container)
        if (fragment != null) {
            backgroundImageView?.alpha =
                if (fragment is LoginFragment) 0.5f else 1.0f
        }
    }

    override fun onContinueWithOutAccountClick() {
        presenter.startGhostAccountSetup()
    }

    override fun onForgotPasswordClick() {
        val forgotFragment = ForgotPasswordFragment()
        val direction = GravityCompat
            .getAbsoluteGravity(GravityCompat.END, resources.configuration.layoutDirection)
        forgotFragment.enterTransition = Slide(direction)
            .addTarget(R.id.forgot_password_container)
        replaceFragment(forgotFragment, true)
    }

    override fun onGenerateCodeClick() {
        presenter.onGenerateCodeClick()
    }

    override fun onLoginButtonClick(username: String, password: String, twoFa: String?) {
        presenter.startLoginProcess(username, password, twoFa)
    }

    override fun onLoginClick() {
        val loginFragment = LoginFragment()
        val direction = GravityCompat
            .getAbsoluteGravity(GravityCompat.END, resources.configuration.layoutDirection)
        loginFragment.enterTransition = Slide(direction)
            .addTarget(R.id.login_sign_up_container)
        replaceFragment(loginFragment, true)
    }

    override fun onSignUpButtonClick(
        username: String,
        password: String,
        email: String?,
        ignoreEmptyEmail: Boolean
    ) {
        presenter.startSignUpProcess(username, password, email, ignoreEmptyEmail)
    }

    override fun prepareUiForApiCallFinished() {
        val fragment = supportFragmentManager.findFragmentById(R.id.progress_container)
        if (fragment is ProgressFragment) {
            supportFragmentManager.popBackStack()
        }
    }

    override fun prepareUiForApiCallStart() {
        ProgressFragment.instance.add(this, R.id.progress_container, true)
    }

    private fun replaceFragment(fragment: Fragment, addToBackStack: Boolean) {
        val transaction = supportFragmentManager
            .beginTransaction()
            .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE)
            .replace(R.id.fragment_container, fragment)
        if (addToBackStack) {
            transaction.addToBackStack(fragment.javaClass.name)
        }
        transaction.commit()
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

    override fun setSecretCode(secretCode: String) {
        val fragment = supportFragmentManager.findFragmentById(R.id.fragment_container)
        if (fragment is WelcomeActivityCallback) {
            (fragment as WelcomeActivityCallback).setSecretCode(secretCode)
        }
    }

    override fun setTwoFaError(error: String) {
        val fragment = supportFragmentManager.findFragmentById(R.id.fragment_container)
        if (fragment is TwoFactorFragment) {
            fragment.setTwoFaError(error)
        }
    }

    override fun setTwoFaRequired(username: String, password: String) {
        val fragment = supportFragmentManager.findFragmentById(R.id.fragment_container)
        if (fragment is LoginFragment) {
            val twoFactorFragment = TwoFactorFragment()
            val bundle = Bundle()
            bundle.putString("username", username)
            bundle.putString("password", password)
            twoFactorFragment.arguments = bundle
            replaceFragment(twoFactorFragment, true)
        }
    }

    override fun setUsernameError(error: String) {
        val fragment = supportFragmentManager.findFragmentById(R.id.fragment_container)
        if (fragment is WelcomeActivityCallback) {
            (fragment as WelcomeActivityCallback).setUsernameError(error)
        }
    }

    override fun showError(error: String) {
        ErrorFragment.instance.add(error, this, R.id.fragment_container, true)
    }

    override fun showFailedAlert() {
        runOnUiThread { showToast("Check you network connection.") }
    }

    override fun showNoEmailAttentionFragment() {
        val noEmailAttentionFragment = NoEmailAttentionFragment()
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

    override fun updateCurrentProcess(currentCall: String) {
        runOnUiThread {
            val fragment = supportFragmentManager.findFragmentById(R.id.progress_container)
            if (fragment is ProgressFragment) {
                fragment.updateProgressStatus(currentCall)
            }
        }
    }

    private fun addStartFragment() {
        val startFragmentName = intent.getStringExtra("startFragmentName")
        val fragment: Fragment = if (startFragmentName != null && startFragmentName == "Login") {
            LoginFragment()
        } else if (startFragmentName != null && startFragmentName == "SignUp") {
            SignUpFragment()
        } else if (startFragmentName != null && startFragmentName == "AccountSetUp") {
            SignUpFragment()
        } else {
            WelcomeFragment()
        }
        val bundle = Bundle()
        bundle.putString("startFragmentName", startFragmentName)
        fragment.arguments = bundle
        val direction = GravityCompat
            .getAbsoluteGravity(GravityCompat.END, resources.configuration.layoutDirection)
        fragment.enterTransition =
            Slide(direction).addTarget(R.id.welcome_container)
        replaceFragment(fragment, false)
    }

    private fun permissionGranted(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            (
                checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    == PackageManager.PERMISSION_GRANTED
                )
        } else {
            true
        }
    }

    private fun registerFragmentChangeListener() {
        supportFragmentManager.addOnBackStackChangedListener(this)
    }

    companion object {
        fun getStartIntent(context: Context?): Intent {
            return Intent(context, WelcomeActivity::class.java)
        }
    }
}
