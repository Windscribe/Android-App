package com.windscribe.mobile.custom_view.preferences

import android.content.Context
import android.content.res.TypedArray
import android.util.AttributeSet
import android.view.View
import android.widget.*
import androidx.core.content.res.getResourceIdOrThrow
import com.windscribe.mobile.R

class ToggleView @JvmOverloads constructor(
        context: Context,
        attrs: AttributeSet? = null,
        defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr){

    interface Delegate {
        fun onToggleClick()
        fun onExplainClick()
    }
    var delegate: Delegate? = null
    var toggle: ImageView? = null
    private val attributes: TypedArray = context.obtainStyledAttributes(attrs, R.styleable.ToggleView)
    private val view: View = View.inflate(context, R.layout.toggle_view, this)

    init {
        attributes.getString(R.styleable.ToggleView_ToggleDescription)?.let {
            view.findViewById<TextView>(R.id.description).text = it
        }
        view.findViewById<TextView>(R.id.label).text = attributes.getString(R.styleable.ToggleView_ToggleTitle)
        val leftIcon = attributes.getResourceIdOrThrow(R.styleable.ToggleView_ToggleLeftIcon)
        view.findViewById<ImageView>(R.id.left_icon).setImageResource(leftIcon)
        view.findViewById<ImageView>(R.id.right_icon).setOnClickListener { delegate?.onExplainClick() }
        view.findViewById<ImageView>(R.id.clickable_area).setOnClickListener {delegate?.onToggleClick() }
        if(attributes.getBoolean(R.styleable.ToggleView_ToggleShowRightIcon, true).not()){
            view.findViewById<ImageView>(R.id.right_icon).visibility = INVISIBLE
        }
    }

    fun setTitle(value: String){
        view.findViewById<TextView>(R.id.label).text = value
    }

    fun setToggleImage(resourceId: Int) {
        view.findViewById<ImageView>(R.id.toggle).setImageResource(resourceId)
    }
}