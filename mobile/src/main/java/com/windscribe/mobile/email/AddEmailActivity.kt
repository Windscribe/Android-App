/*
 * Copyright (c) 2021 Windscribe Limited.
 */
package com.windscribe.mobile.email

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Patterns
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.work.Data
import butterknife.BindView
import butterknife.OnClick
import com.windscribe.mobile.R
import com.windscribe.mobile.base.BaseActivity
import com.windscribe.mobile.confirmemail.ConfirmActivity
import com.windscribe.mobile.custom_view.ProgressFragment
import com.windscribe.mobile.di.ActivityModule
import com.windscribe.mobile.welcome.SoftInputAssist
import com.windscribe.mobile.windscribe.WindscribeActivity.Companion.getStartIntent
import com.windscribe.vpn.Windscribe
import com.windscribe.vpn.commonutils.ThemeUtils.getColor
import org.slf4j.LoggerFactory
import javax.inject.Inject

class AddEmailActivity : BaseActivity(), AddEmailView {

    @BindView(R.id.cl_add_email)
    lateinit var constraintLayoutMain: ConstraintLayout

    @BindView(R.id.email_description)
    lateinit var emailDescription: TextView

    @BindView(R.id.email)
    lateinit var emailEditView: EditText

    @BindView(R.id.email_error)
    lateinit var emailErrorView: ImageView

    @BindView(R.id.next)
    lateinit var nextButton: TextView

    @BindView(R.id.nav_title)
    lateinit var titleView: TextView

    @Inject
    lateinit var presenter: AddEmailPresenter

    private val logger = LoggerFactory.getLogger("[add_email_a]")
    private var softInputAssist: SoftInputAssist? = null

    private val generalTextWatcher: TextWatcher = object : TextWatcher {
        override fun afterTextChanged(s: Editable) {
            if (s.isNotEmpty()) {
                if (s.hashCode() == emailEditView.text.hashCode()) {
                    emailDescription.text = getString(R.string.email_description)
                    emailDescription.setTextColor(
                        getColor(
                            this@AddEmailActivity,
                            R.attr.wdSecondaryColor,
                            R.color.colorWhite50
                        )
                    )
                    emailErrorView.visibility = View.GONE
                    emailEditView.setTextColor(
                        getColor(
                            this@AddEmailActivity,
                            R.attr.wdPrimaryColor,
                            R.color.colorWhite50
                        )
                    )
                }
            }
        }

        override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}
        override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
            if (start == 0 && before == 1 && count == 0) {
                nextButton.isPressed = false
            } else {
                nextButton.isEnabled =
                    s.hashCode() == emailEditView.text.hashCode() && Patterns.EMAIL_ADDRESS.matcher(
                        s
                    )
                        .matches()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setActivityModule(ActivityModule(this, this)).inject(this)
        setContentLayout(R.layout.activity_add_email_address, true)
        presenter.setUpLayout()
    }

    override fun onResume() {
        softInputAssist?.onResume()
        super.onResume()
    }

    override fun onPause() {
        softInputAssist?.onPause()
        super.onPause()
    }

    override fun onDestroy() {
        softInputAssist?.onDestroy()
        presenter.onDestroy()
        super.onDestroy()
    }

    override fun gotoWindscribeActivity() {
        Windscribe.appContext.workManager.updateSession(Data.EMPTY)
        if (intent.getBooleanExtra(finishAfterAddEmail, false)) {
            finish()
        } else if (intent.getBooleanExtra(goToHomeAfterFinish, false)) {
            val startIntent = getStartIntent(this)
            startIntent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            startActivity(startIntent)
        } else {
            startActivity(ConfirmActivity.getStartIntent(this))
            finish()
        }
    }

    override fun hideSoftKeyboard() {
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(
            window.decorView.windowToken,
            InputMethodManager.HIDE_NOT_ALWAYS
        )
    }

    @OnClick(R.id.next)
    fun onAddEmailClick() {
        if (emailEditView.text != null) {
            presenter.onAddEmailClicked(emailEditView.text.toString())
        }
    }

    @OnClick(R.id.nav_button)
    fun onBackButtonPressed() {
        onBackPressed()
    }

    override fun prepareUiForApiCallFinished() {
        logger.info("Preparing ui for api call finished...")
        val fragment = supportFragmentManager.findFragmentById(R.id.cl_add_email)
        if (fragment is ProgressFragment) {
            fragment.finishProgress()
        }
    }

    override fun prepareUiForApiCallStart() {
        logger.info("Preparing ui for api call start...")
        ProgressFragment.getInstance().add(this, R.id.cl_add_email, true)
    }

    override fun setUpLayout(title: String) {
        emailEditView.addTextChangedListener(generalTextWatcher)
        softInputAssist = SoftInputAssist(this, intArrayOf())
        titleView.text = title
    }

    override fun showInputError(errorText: String) {
        emailDescription.setTextColor(resources.getColor(R.color.colorRed))
        emailDescription.text = errorText
        emailErrorView.visibility = View.VISIBLE
        emailEditView.setTextColor(resources.getColor(R.color.colorRed))
    }

    override fun showToast(toastString: String) {
        Toast.makeText(this, toastString, Toast.LENGTH_SHORT).show()
    }

    companion object {
        const val finishAfterAddEmail = "finishAfterAddEmail"
        const val goToHomeAfterFinish = "goToHomeAfterFinish"
    }
}