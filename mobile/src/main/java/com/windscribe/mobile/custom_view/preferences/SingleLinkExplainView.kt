package com.windscribe.mobile.custom_view.preferences

import android.content.Context
import android.content.res.TypedArray
import android.util.AttributeSet
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.res.getResourceIdOrThrow
import com.windscribe.mobile.R


class SingleLinkExplainView @JvmOverloads constructor(
        context: Context,
        attrs: AttributeSet? = null,
        defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {

    private val attributes: TypedArray = context.obtainStyledAttributes(attrs, R.styleable.SingleLinkExplainView)
    private val view: View = View.inflate(context, R.layout.lable_link_explain_view, this)

    init {
        view.findViewById<TextView>(R.id.title).text = attributes.getString(R.styleable.SingleLinkExplainView_Title)
        view.findViewById<TextView>(R.id.description).text = attributes.getString(R.styleable.SingleLinkExplainView_Description)
        val leftIcon = attributes.getResourceIdOrThrow(R.styleable.SingleLinkExplainView_LeftIcon)
        view.findViewById<ImageView>(R.id.left_icon).setImageResource(leftIcon)
    }

    fun onClick(click: OnClickListener){
        view.findViewById<ConstraintLayout>(R.id.container).setOnClickListener(click)
    }
}