package com.windscribe.mobile.custom_view.preferences

import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.*
import com.windscribe.mobile.R

class SplitRoutingModeView(private val childView: View) : BaseView(childView) , AdapterView.OnItemSelectedListener{
    private var spinner: Spinner? = null
    private var current: TextView? = null

    interface Delegate {
        fun onModeSelect(mode: String)
    }
    var delegate: Delegate? = null

    init {
        spinner = view.findViewById(R.id.spinner)
        current = view.findViewById(R.id.current)
        view.findViewById<ImageView>(R.id.clickable_area).setOnClickListener { spinner?.performClick() }
        spinner?.onItemSelectedListener = this
    }

    override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
        view?.findViewById<TextView>(R.id.tv_drop_down)?. text = ""
        spinner?.selectedItem.toString().let {
            delegate?.onModeSelect(it)
            current?.text = it
        }
    }

    override fun onNothingSelected(parent: AdapterView<*>?) {}

    fun setAdapter(savedMode: String, modes: Array<String>) {
        val modesAdapter: ArrayAdapter<String> = ArrayAdapter<String>(childView.context, R.layout.drop_down_layout,
                R.id.tv_drop_down, modes)
        spinner?.adapter = modesAdapter
        spinner?.isSelected = false
        spinner?.setSelection(modesAdapter.getPosition(savedMode))
        current?.text = savedMode
    }
}