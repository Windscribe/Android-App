package com.windscribe.mobile.custom_view.preferences

import android.content.Context
import android.transition.Fade
import android.transition.Transition
import android.transition.TransitionManager
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
import android.widget.*
import com.windscribe.mobile.R


class AppBackgroundView @JvmOverloads constructor(
        context: Context,
        attrs: AttributeSet? = null,
        defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr), AdapterView.OnItemSelectedListener {

    interface Delegate {
        fun onItemSelect(value: String)
        fun onFirstRightIconClick()
        fun onSecondRightIconClick()
    }
    var delegate: Delegate? = null
    private var spinner: Spinner? = null
    private var current: TextView? = null
    private val view: View = View.inflate(context, R.layout.app_background_view, this)
    private var keys: Array<String>? = null

    init {
        spinner = view.findViewById(R.id.spinner)
        current = view.findViewById(R.id.current)
        view.findViewById<ImageView>(R.id.first_item_right_icon).setOnClickListener { delegate?.onFirstRightIconClick() }
        view.findViewById<ImageView>(R.id.second_item_right_icon).setOnClickListener { delegate?.onSecondRightIconClick() }
        view.findViewById<ImageView>(R.id.clickable_area).setOnClickListener { spinner?.performClick() }
        spinner?.onItemSelectedListener = this
    }

    override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
        view?.findViewById<TextView>(R.id.tv_drop_down)?.text = ""
        spinner?.selectedItem.toString().let {
            current?.text = it
            delegate?.onItemSelect(keys?.get(position) ?: "")
            animateVisibilityChange(position)
        }
    }

    private fun animateVisibilityChange(position: Int) {
        val visibility = if (position == 1) {
            VISIBLE
        } else {
            GONE
        }
        val transition: Transition = Fade()
        transition.duration = 300
        TransitionManager.beginDelayedTransition(parent as ViewGroup?, transition)
        view.findViewById<ImageView>(R.id.divider1).visibility = visibility
        view.findViewById<TextView>(R.id.first_item_title).visibility = visibility
        view.findViewById<TextView>(R.id.first_item_description).visibility = visibility
        view.findViewById<ImageView>(R.id.first_item_right_icon).visibility = visibility
        view.findViewById<ImageView>(R.id.divider2).visibility = visibility
        view.findViewById<TextView>(R.id.second_item_title).visibility = visibility
        view.findViewById<TextView>(R.id.second_item_description).visibility = visibility
        view.findViewById<ImageView>(R.id.second_item_right_icon).visibility = visibility
        view.findViewById<TextView>(R.id.label1).visibility = visibility
        view.findViewById<TextView>(R.id.label2).visibility = visibility
    }

    override fun onNothingSelected(parent: AdapterView<*>?) {}

    fun setTitle(value: String){
        view.findViewById<TextView>(R.id.label).text = value
    }

    fun setFirstItemTitle(value: String){
        view.findViewById<TextView>(R.id.first_item_title).text = value
    }

    fun setSecondItemTitle(value: String){
        view.findViewById<TextView>(R.id.second_item_title).text = value
    }

    fun setFirstItemDescription(value: String) {
        if (value.isNotEmpty()) view.findViewById<TextView>(R.id.first_item_description).text =
            value
    }

    fun setSecondItemDescription(value: String) {
        if (value.isNotEmpty()) view.findViewById<TextView>(R.id.second_item_description).text =
            value
    }

    fun setAdapter(localiseValues: Array<String>, selectedKey: String, keys: Array<String>) {
        this.keys = keys
        val selectionAdapter: ArrayAdapter<String> = ArrayAdapter<String>(
            context, R.layout.drop_down_layout, R.id.tv_drop_down, localiseValues
        )
        spinner?.adapter = selectionAdapter
        spinner?.isSelected = false
        spinner?.setSelection(keys.indexOf(selectedKey))
        current?.text = localiseValues[keys.indexOf(selectedKey)]
    }
}