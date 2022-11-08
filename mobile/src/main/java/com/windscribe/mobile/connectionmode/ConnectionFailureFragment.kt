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
import com.windscribe.vpn.commonutils.Ext.launchPeriodicAsync
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.cancel

class ConnectionFailureFragment(
    private val protocolInformation: List<ProtocolInformation>,
    private val autoConnectionModeCallback: AutoConnectionModeCallback
) : DialogFragment(), ItemSelectListener {

    @BindView(R.id.protocol_list)
    lateinit var protocolListView: RecyclerView
    private var scope = CoroutineScope(Main)

    var adapter: ProtocolInformationAdapter? = null
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.connection_failure, container, false)
        ButterKnife.bind(this, view)
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        adapter = ProtocolInformationAdapter(mutableListOf(), this)
        protocolListView.layoutManager = LinearLayoutManager(context)
        protocolListView.adapter = adapter
        adapter?.update(protocolInformation)
        startAutoSelectTimer()
    }

    private fun startAutoSelectTimer() {
        scope.launchPeriodicAsync(1000) {
            adapter?.data?.let {
                if (it[0].autoConnectTimeLeft > 0) {
                    it[0].autoConnectTimeLeft = it[0].autoConnectTimeLeft - 1
                }
                adapter?.notifyItemChanged(0)
                if (it[0].autoConnectTimeLeft <= 0) {
                    onItemSelect(it[0])
                }
            }
        }
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NORMAL, R.style.FullScreenDialog)
    }

    @OnClick(R.id.cancel, R.id.img_close_btn)
    fun onCancelClick() {
        scope.cancel()
        dismissAllowingStateLoss()
        autoConnectionModeCallback.onCancel()
    }

    override fun onItemSelect(protocolInformation: ProtocolInformation) {
        dismissAllowingStateLoss()
        scope.cancel()
        autoConnectionModeCallback.onProtocolSelect(protocolInformation)
    }
}