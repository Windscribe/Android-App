package com.windscribe.mobile.connectionmode

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.DialogFragment
import butterknife.BindView
import butterknife.ButterKnife
import butterknife.OnClick
import com.windscribe.mobile.R
import com.windscribe.vpn.autoconnection.AutoConnectionModeCallback
import com.windscribe.vpn.autoconnection.ProtocolInformation
import com.windscribe.vpn.backend.Util

class SetupPreferredProtocolFragment(
    private val protocolInformation: ProtocolInformation?,
    private val autoConnectionModeCallback: AutoConnectionModeCallback
) : DialogFragment() {

    @BindView(R.id.title)
    lateinit var titleView: TextView

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.setup_preferred_protocol, container, false)
        ButterKnife.bind(this, view)
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        protocolInformation?.let {
            titleView.text = getString(
                R.string.set_this_protocol_as_preferred,
                Util.getProtocolLabel(protocolInformation.protocol)
            )
        } ?: kotlin.run {
            dismiss()
        }
    }

    @OnClick(R.id.cancel, R.id.img_close_btn)
    fun onCancelClick() {
        dismiss()
        autoConnectionModeCallback.onCancel()
    }

    @OnClick(R.id.set_as_preferred)
    fun onSetAsPreferredProtocolClick() {
        dismiss()
        autoConnectionModeCallback.onSetAsPreferredClicked()
    }
}