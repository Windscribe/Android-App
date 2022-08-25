package com.windscribe.mobile.share

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ShareCompat
import butterknife.BindView
import butterknife.ButterKnife
import butterknife.OnClick
import com.windscribe.mobile.R
import com.windscribe.mobile.base.BaseDialogFragment
import com.windscribe.vpn.ActivityInteractor
import javax.inject.Inject

class ShareAppLink @Inject constructor(private val activityInteractor: ActivityInteractor) :
    BaseDialogFragment() {

    @BindView(R.id.continue_btn)
    lateinit var continueButton: Button

    @BindView(R.id.nav_title)
    lateinit var navTitle: TextView

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_share_app_link, container, false)
        setViewWithCutout(view)
        ButterKnife.bind(this, view)
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        activityInteractor.getAppPreferenceInterface().alreadyShownShareAppLink = true
        navTitle.visibility = View.INVISIBLE
    }

    @OnClick(R.id.continue_btn)
    fun onContinueClick() {
        activityInteractor.getUserRepository().user.value?.let {
            val launchActivity = activity as AppCompatActivity
            ShareCompat.IntentBuilder(launchActivity)
                .setType("text/plain")
                .setChooserTitle("Share App")
                .setText("${it.userName} is inviting you to join Windscribe. Provide their username at signup and you’ll both get 1gb of free data added to your accounts. If you go pro, they’ll go pro too!\nhttps://play.google.com/store/apps/details?id=" + launchActivity.packageName)
                .startChooser()
        }
    }

    @OnClick(R.id.nav_button)
    fun onBackButtonClick() {
        activity?.onBackPressed()
    }
}