package com.windscribe.mobile.connectionmode

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ProgressBar
import androidx.fragment.app.DialogFragment
import butterknife.BindView
import butterknife.ButterKnife
import butterknife.OnClick
import com.windscribe.mobile.R
import com.windscribe.vpn.autoconnection.AutoConnectionModeCallback

class AllProtocolFailedFragment : DialogFragment() {

    @BindView(R.id.progressBar)
    lateinit var progressView: ProgressBar

    @BindView(R.id.send_debug_log)
    lateinit var sendDebugLog: Button

    private var autoConnectionModeCallback: AutoConnectionModeCallback? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.all_protocol_failed, container, false)
        ButterKnife.bind(this, view)
        return view
    }

    @OnClick(R.id.cancel, R.id.img_close_btn)
    fun onCancelClick() {
        dismiss()
        autoConnectionModeCallback?.onCancel()
    }

    @OnClick(R.id.send_debug_log)
    fun onSendLogClick() {
        sendDebugLog.visibility = View.INVISIBLE
        progressView.visibility = View.VISIBLE
        autoConnectionModeCallback?.onSendLogClicked()
    }

    companion object {
        fun newInstance(autoConnectionModeCallback: AutoConnectionModeCallback): AllProtocolFailedFragment {
            val fragment = AllProtocolFailedFragment()
            fragment.autoConnectionModeCallback = autoConnectionModeCallback
            return fragment
        }
    }
}