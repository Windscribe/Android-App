package com.windscribe.mobile.custom_view.preferences

import android.content.Context
import android.content.res.TypedArray
import android.util.AttributeSet
import android.view.View
import android.widget.*
import androidx.core.content.res.getResourceIdOrThrow
import com.windscribe.mobile.R
import java.util.concurrent.atomic.AtomicBoolean

class DropDownView @JvmOverloads constructor(
        context: Context,
        attrs: AttributeSet? = null,
        defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr), AdapterView.OnItemSelectedListener {

    interface Delegate {
        fun onItemSelect(value: String)
        fun onExplainClick()
    }
    var delegate: Delegate? = null
    private var spinner: Spinner? = null
    private var current: TextView? = null
    private val attributes: TypedArray = context.obtainStyledAttributes(attrs, R.styleable.DropDownView)
    private val view: View = View.inflate(context, R.layout.drop_down_view, this)
    private var keys: Array<String>? = null
    private var ignoreInitialEvent = AtomicBoolean(false)
    init {
        attributes.getString(R.styleable.DropDownView_DropDownDescription)?.let {
            view.findViewById<TextView>(R.id.description).text = it
        }
        view.findViewById<TextView>(R.id.label).text = attributes.getString(R.styleable.DropDownView_DropDownTitle)
        val leftIcon = attributes.getResourceIdOrThrow(R.styleable.DropDownView_DropDownLeftIcon)
        view.findViewById<ImageView>(R.id.left_icon).setImageResource(leftIcon)
        spinner = view.findViewById(R.id.spinner)
        current = view.findViewById(R.id.current)
        view.findViewById<ImageView>(R.id.right_icon).setOnClickListener { delegate?.onExplainClick() }
        view.findViewById<ImageView>(R.id.clickable_area).setOnClickListener { spinner?.performClick() }
        if(attributes.getBoolean(R.styleable.DropDownView_DropDownShowRightIcon, true).not()){
            view.findViewById<ImageView>(R.id.right_icon).visibility = GONE
        }
        spinner?.onItemSelectedListener = this
    }

    override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
        if (ignoreInitialEvent.getAndSet(true)) {
            view?.findViewById<TextView>(R.id.tv_drop_down)?. text = ""
            spinner?.selectedItem.toString().let {
                current?.text = it
                delegate?.onItemSelect(keys?.get(position) ?: "")
            }
        }
    }

    override fun onNothingSelected(parent: AdapterView<*>?) {}

    fun setCurrentValue(value: String) {
        view.findViewById<TextView>(R.id.current).text = value
    }

    fun setTitle(value: String) {
        view.findViewById<TextView>(R.id.label).text = value
    }

    fun setAdapter(localiseValues: Array<String>, selectedKey: String, keys: Array<String>) {
        this.keys = keys
        val selectionAdapter: ArrayAdapter<String> = ArrayAdapter<String>(
            context, R.layout.drop_down_layout, R.id.tv_drop_down, localiseValues
        )
        spinner?.adapter = selectionAdapter
        spinner?.isSelected = false
        spinner?.setSelection(keys.indexOf(selectedKey))
        if (keys.indexOf(selectedKey) != -1) {
            current?.text = localiseValues[keys.indexOf(selectedKey)]
        }
    }
}