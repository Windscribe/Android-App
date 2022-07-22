package com.windscribe.mobile.custom_view.preferences

import android.view.View
import android.widget.*
import com.windscribe.mobile.R

class ConnectionModeView(childView: View) : BaseView(childView) {
    interface Delegate {
        fun onProtocolSelected(protocol: String)
        fun onPortSelected(protocol: String, port: String)
    }

    var delegate: Delegate? = null
    private var protocolSpinner: Spinner? = null
    private var protocolCurrent: TextView? = null
    private var portSpinner: Spinner? = null
    private var portCurrent: TextView? = null

    init {
        protocolCurrent = childView.findViewById(R.id.tv_current_protocol)
        protocolSpinner = childView.findViewById(R.id.spinner_protocol)
        portCurrent = childView.findViewById(R.id.tv_current_port)
        portSpinner = childView.findViewById(R.id.spinner_port)
        portCurrent?.setOnClickListener { portSpinner?.performClick() }
        childView.findViewById<ImageView>(R.id.img_port_drop_down_btn).setOnClickListener { portSpinner?.performClick() }
        protocolCurrent?.setOnClickListener { protocolSpinner?.performClick() }
        childView.findViewById<ImageView>(R.id.img_protocol_drop_down_btn).setOnClickListener { protocolSpinner?.performClick() }
    }

    fun seProtocolAdapter(savedSelection: String, selections: Array<String>) {
        val selectionAdapter: ArrayAdapter<String> = ArrayAdapter<String>(view.context, R.layout.drop_down_layout,
                R.id.tv_drop_down, selections)
        protocolSpinner?.adapter = selectionAdapter
        protocolSpinner?.isSelected = false
        protocolSpinner?.setSelection(selectionAdapter.getPosition(savedSelection))
        protocolCurrent?.text = savedSelection
        protocolSpinner?.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                view?.findViewById<TextView>(R.id.tv_drop_down)?.text = ""
                protocolSpinner?.selectedItem.toString().let {
                    protocolCurrent?.text = it
                    delegate?.onProtocolSelected(it)
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    fun sePortAdapter(savedSelection: String, selections: List<String>) {
        val selectionAdapter: ArrayAdapter<String> = ArrayAdapter<String>(view.context, R.layout.drop_down_layout,
                R.id.tv_drop_down, selections)
        portSpinner?.adapter = selectionAdapter
        portSpinner?.isSelected = false
        portSpinner?.setSelection(selectionAdapter.getPosition(savedSelection))
        portCurrent?.text = savedSelection
        portSpinner?.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                view?.findViewById<TextView>(R.id.tv_drop_down)?.text = ""
                portSpinner?.selectedItem.toString().let {
                    portCurrent?.text = it
                    protocolCurrent?.text?.let { protocol ->
                        delegate?.onPortSelected(protocol.toString(), it)
                    }
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }
}