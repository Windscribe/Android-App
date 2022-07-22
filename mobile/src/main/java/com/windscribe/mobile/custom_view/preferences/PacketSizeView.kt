package com.windscribe.mobile.custom_view.preferences

import android.view.View
import android.widget.EditText
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import com.windscribe.mobile.R
import com.windscribe.vpn.commonutils.InputFilterMinMax

class PacketSizeView(childView: View) : BaseView(childView) {
    private val edPackageSize: EditText = childView.findViewById(R.id.edit_packet_size)
    private val makePacketSizeEditable: ImageView = childView.findViewById(R.id.make_packet_size_editable)
    private val autoFillBtn: ImageView = childView.findViewById(R.id.img_auto_fill_packet_size)
    private val progressBar: ProgressBar = childView.findViewById(R.id.progress_packet_size)
    private val autoFillProgress: TextView = childView.findViewById(R.id.edit_packet_progress)
    interface Delegate {
        fun onAutoFillButtonClick()
        fun onPacketSizeChanged(packetSize: String)
    }
    var delegate: Delegate? = null

    init {
        setEditTextListener()
        makePacketSizeEditable.setOnClickListener {
            edPackageSize.isEnabled = true
            edPackageSize.requestFocus()
            edPackageSize.setSelection(edPackageSize.text.length)
            showKeyboard(edPackageSize)
        }
        autoFillBtn.setOnClickListener { delegate?.onAutoFillButtonClick() }
    }

    private fun setEditTextListener() {
        edPackageSize.filters = arrayOf<android.text.InputFilter?>(InputFilterMinMax("0", "2000"))
        edPackageSize.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == android.view.inputmethod.EditorInfo.IME_ACTION_DONE) {
                if (edPackageSize.text.toString().trim().isNotEmpty()) {
                    delegate?.onPacketSizeChanged(edPackageSize.text.toString().trim())
                }
                edPackageSize.clearFocus()
                edPackageSize.isEnabled = false
                return@setOnEditorActionListener false
            }
            false
        }
    }

    fun setPacketSize(packetSize: String) {
        edPackageSize.setText(packetSize)
    }

    fun packetSizeDetectionProgress(progress: Boolean) {
        if (progress) {
            autoFillBtn.visibility = View.INVISIBLE
            progressBar.visibility = View.VISIBLE
            edPackageSize.visibility = View.INVISIBLE
            autoFillProgress.visibility = View.VISIBLE
        } else {
            progressBar.visibility = View.INVISIBLE
            autoFillBtn.visibility = View.VISIBLE
            autoFillProgress.visibility = View.INVISIBLE
            edPackageSize.visibility = View.VISIBLE
        }
    }
}