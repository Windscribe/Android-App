package com.windscribe.mobile.dialogs

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ShareCompat
import com.windscribe.mobile.R
import com.windscribe.mobile.databinding.FragmentShareAppLinkBinding
import com.windscribe.mobile.di.DaggerDialogComponent
import com.windscribe.mobile.di.DialogModule
import com.windscribe.vpn.Windscribe.Companion.appContext
import com.windscribe.vpn.apppreference.PreferencesHelper
import com.windscribe.vpn.repository.UserRepository
import javax.inject.Inject

class ShareAppLinkDialog : FullScreenDialog() {

    @Inject
    lateinit var userRepository: UserRepository

    @Inject
    lateinit var preferencesHelper: PreferencesHelper

    private var binding: FragmentShareAppLinkBinding? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        binding = FragmentShareAppLinkBinding.inflate(inflater, container, false)
        return binding?.root
    }

    override fun onDestroyView() {
        binding = null
        super.onDestroyView()
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        DaggerDialogComponent.builder().applicationComponent(appContext.applicationComponent)
            .dialogModule(DialogModule(this)).build().inject(this)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        preferencesHelper.alreadyShownShareAppLink = true
        binding?.shareAppNavButton?.setOnClickListener {
            dismiss()
        }
        binding?.shareAppLinkButton?.setOnClickListener {
            userRepository.user.value?.let {
                val launchActivity = activity as AppCompatActivity
                val launchUrl =
                    "https://play.google.com/store/apps/details?id=${launchActivity.packageName}"
                ShareCompat.IntentBuilder(launchActivity).setType("text/plain")
                    .setChooserTitle(getString(R.string.share_app))
                    .setText(getString(R.string.share_app_description, it.userName, launchUrl))
                    .startChooser()
            }
            dismiss()
        }
    }

    companion object {
        const val tag = "ShareAppLinkDialog"
        fun show(activity: AppCompatActivity) {
            if (activity.supportFragmentManager.findFragmentByTag(tag) != null) {
                return
            }
            activity.runOnUiThread {
                kotlin.runCatching {
                    ShareAppLinkDialog().showNow(activity.supportFragmentManager, tag)
                }
            }
        }
    }
}