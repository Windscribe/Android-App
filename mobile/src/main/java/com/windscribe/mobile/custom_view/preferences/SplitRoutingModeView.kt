package com.windscribe.mobile.custom_view.preferences

import android.view.View
import android.widget.*
import com.windscribe.mobile.R

class SplitRoutingModeView(private val childView: View) : BaseView(childView) , AdapterView.OnItemSelectedListener{
    private var spinner: Spinner? = null
    private var current: TextView? = null

    interface Delegate {
        fun onModeSelect(mode: String)
    }
    var delegate: Delegate? = null
    var values: Array<String>? = null

    init {
        spinner = view.findViewById(R.id.spinner)
        current = view.findViewById(R.id.current)
        view.findViewById<ImageView>(R.id.clickable_area).setOnClickListener { spinner?.performClick() }
        spinner?.onItemSelectedListener = this
    }

    override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
        view?.findViewById<TextView>(R.id.tv_drop_down)?.text = ""
        spinner?.selectedItem.toString().let {
            delegate?.onModeSelect(values?.get(position) ?: "")
            current?.text = it
        }
    }

    override fun onNothingSelected(parent: AdapterView<*>?) {}

    fun setAdapter(selectedValues: String, values: Array<String>, localizeValues: Array<String>) {
        this.values = values
        val modesAdapter: ArrayAdapter<String> = ArrayAdapter<String>(
            childView.context, R.layout.drop_down_layout, R.id.tv_drop_down, localizeValues
        )
        spinner?.adapter = modesAdapter
        spinner?.isSelected = false
        spinner?.setSelection(values.indexOf(selectedValues))
        current?.text = selectedValues
    }
}