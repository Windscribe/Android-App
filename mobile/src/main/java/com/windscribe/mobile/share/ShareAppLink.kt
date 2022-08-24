package com.windscribe.mobile.share

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ShareCompat
import androidx.fragment.app.DialogFragment
import butterknife.BindView
import butterknife.ButterKnife
import butterknife.OnClick
import com.windscribe.mobile.R
import com.windscribe.mobile.confirmemail.ConfirmActivity
import com.windscribe.mobile.email.AddEmailActivity
import com.windscribe.mobile.email.AddEmailActivity.Companion.finishAfterAddEmail
import com.windscribe.vpn.ActivityInteractor
import com.windscribe.vpn.model.User
import javax.inject.Inject

class ShareAppLink @Inject constructor(private val activityInteractor: ActivityInteractor) :
    DialogFragment() {

    @BindView(R.id.continue_btn)
    lateinit var continueButton: Button

    @BindView(R.id.error)
    lateinit var errorView: TextView

    private var userEmailStatus: User.EmailStatus = User.EmailStatus.NoEmail

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_share_app_link, container, false)
        ButterKnife.bind(this, view)
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        activityInteractor.getUserRepository().user.observe(this) {
            setupLayout(it.emailStatus)
        }
    }

    private fun setupLayout(userEmailStatus: User.EmailStatus) {
        this.userEmailStatus = userEmailStatus
        when (userEmailStatus) {
            User.EmailStatus.NoEmail -> {
                errorView.visibility = View.VISIBLE
                continueButton.text = getString(R.string.add_email)
            }
            User.EmailStatus.EmailProvided -> {
                errorView.text = getString(R.string.please_confirm_email_first)
                errorView.visibility = View.VISIBLE
                continueButton.text = getString(R.string.confirm_your_email)
            }
            User.EmailStatus.Confirmed -> {
                errorView.visibility = View.GONE
                continueButton.text = getString(R.string.share_invite_link)
            }
        }
    }

    @OnClick(R.id.continue_btn)
    fun onContinueClick() {
        when (userEmailStatus) {
            User.EmailStatus.NoEmail -> {
                val intent = Intent(context, AddEmailActivity::class.java)
                intent.putExtra(finishAfterAddEmail, true)
                startActivity(intent)
            }
            User.EmailStatus.EmailProvided -> {
                val intent = Intent(context, ConfirmActivity::class.java)
                intent.putExtra(
                    ConfirmActivity.ReasonToConfirmEmail,
                    "Confirm to email to refer Windscribe to your friends."
                )
                startActivity(intent)
            }
            User.EmailStatus.Confirmed -> {
                val launchActivity = activity as AppCompatActivity
                ShareCompat.IntentBuilder(launchActivity)
                    .setType("text/plain")
                    .setChooserTitle("Share App")
                    .setText("http://play.google.com/store/apps/details?id=" + launchActivity.packageName)
                    .startChooser()
            }
        }
    }

    @OnClick(R.id.backButton)
    fun onBackButtonClick() {
        activity?.onBackPressed()
    }
}