package com.windscribe.mobile.connectionmode

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import butterknife.BindView
import butterknife.ButterKnife
import butterknife.OnClick
import com.windscribe.mobile.R
import com.windscribe.mobile.adapter.ItemSelectListener
import com.windscribe.mobile.adapter.ProtocolInformationAdapter
import com.windscribe.vpn.autoconnection.AutoConnectionModeCallback
import com.windscribe.vpn.autoconnection.ProtocolInformation

class ConnectionChangeFragment : DialogFragment(), ItemSelectListener {

    @BindView(R.id.protocol_list)
    lateinit var protocolListView: RecyclerView
    private var protocolInformation: List<ProtocolInformation>? = null
    private var autoConnectionModeCallback: AutoConnectionModeCallback? = null


    var adapter: ProtocolInformationAdapter? = null
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.connection_change, container, false)
        ButterKnife.bind(this, view)
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        adapter = ProtocolInformationAdapter(mutableListOf(), this)
        protocolListView.layoutManager = LinearLayoutManager(context)
        protocolListView.adapter = adapter
        protocolInformation?.let {
            adapter?.update(it)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NORMAL, R.style.FullScreenDialog)
    }

    @OnClick(R.id.cancel, R.id.img_close_btn)
    fun onCancelClick() {
        dismissAllowingStateLoss()
        autoConnectionModeCallback?.onCancel()
    }

    override fun onItemSelect(protocolInformation: ProtocolInformation) {
        dismissAllowingStateLoss()
        autoConnectionModeCallback?.onProtocolSelect(protocolInformation)
    }

    companion object {
        fun newInstance(
            protocolInformation: List<ProtocolInformation>,
            autoConnectionModeCallback: AutoConnectionModeCallback
        ): ConnectionChangeFragment {
            val fragment = ConnectionChangeFragment()
            fragment.protocolInformation = protocolInformation
            fragment.autoConnectionModeCallback = autoConnectionModeCallback
            return fragment
        }
    }
}