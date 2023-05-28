package com.windscribe.mobile.custom_view.preferences

import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.ImageView
import com.windscribe.mobile.R

class KeepAliveView(childView: View) : BaseView(childView) {
    private val keepAliveEditText: EditText = childView.findViewById(R.id.keep_alive_edit_view)
    private val keepAliveButton: ImageView = childView.findViewById(R.id.keep_alive_edit_button)
    interface Delegate {
        fun onKeepAliveTimeChanged(time: String)
    }
    var delegate: Delegate? = null

    init {
        setEditTextListener()
        keepAliveButton.setOnClickListener {
            keepAliveEditText.isEnabled = true
            keepAliveEditText.requestFocus()
            keepAliveEditText.setSelection(keepAliveEditText.text.length)
            showKeyboard(keepAliveEditText)
        }
    }

    private fun setEditTextListener() {
        keepAliveEditText.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                if (keepAliveEditText.text.toString().trim().isNotEmpty()) {
                    delegate?.onKeepAliveTimeChanged(keepAliveEditText.text.toString().trim())
                }
                keepAliveEditText.clearFocus()
                keepAliveEditText.isEnabled = false
                return@setOnEditorActionListener false
            }
            false
        }

        keepAliveEditText.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable) {
                if (keepAliveEditText.text.toString().trim().isNotEmpty()) {
                    delegate?.onKeepAliveTimeChanged(keepAliveEditText.text.toString().trim())
                }
            }

            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {}
        })
    }

    fun setKeepAlive(keepAliveTime: String) {
        keepAliveEditText.setText(keepAliveTime)
    }
}