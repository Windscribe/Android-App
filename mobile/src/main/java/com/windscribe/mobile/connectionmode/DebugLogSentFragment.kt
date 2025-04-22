package com.windscribe.mobile.connectionmode

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import butterknife.ButterKnife
import butterknife.OnClick
import com.windscribe.mobile.R
import com.windscribe.mobile.base.BaseActivity
import com.windscribe.vpn.Windscribe.Companion.appContext
import com.windscribe.vpn.autoconnection.AutoConnectionModeCallback
import com.windscribe.vpn.constants.NetworkKeyConstants

class DebugLogSentFragment : DialogFragment() {

    private var autoConnectionModeCallback: AutoConnectionModeCallback? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.debug_log_sent, container, false)
        ButterKnife.bind(this, view)
        return view
    }

    @OnClick(R.id.cancel, R.id.img_close_btn)
    fun onCancelClick() {
        dismiss()
        autoConnectionModeCallback?.onCancel()
    }

    @OnClick(R.id.contact_support)
    fun onContactSupportClick() {
        dismiss()
        appContext.applicationInterface.cancelDialog()
        val activity = activity as? BaseActivity
        activity?.openURLInBrowser(NetworkKeyConstants.getWebsiteLink(NetworkKeyConstants.URL_HELP_ME))
    }

    companion object {
        fun newInstance(autoConnectionModeCallback: AutoConnectionModeCallback): DebugLogSentFragment {
            val fragment = DebugLogSentFragment()
            fragment.autoConnectionModeCallback = autoConnectionModeCallback
            return fragment
        }
    }
}