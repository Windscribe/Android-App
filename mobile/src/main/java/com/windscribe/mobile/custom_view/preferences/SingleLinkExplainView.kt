package com.windscribe.mobile.custom_view.preferences

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.TypedArray
import android.util.AttributeSet
import android.util.TypedValue
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.updateLayoutParams
import com.windscribe.mobile.R
import com.windscribe.mobile.utils.UiUtil


@SuppressLint("ClickableViewAccessibility")
class SingleLinkExplainView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {

    private val attributes: TypedArray = context.obtainStyledAttributes(attrs, R.styleable.SingleLinkExplainView)
    private val view: View = View.inflate(context, R.layout.lable_link_explain_view, this)

    init {
        view.findViewById<TextView>(R.id.title).text =
            attributes.getString(R.styleable.SingleLinkExplainView_Title)
        view.findViewById<TextView>(R.id.description).text =
            attributes.getString(R.styleable.SingleLinkExplainView_Description)
        val leftIcon = attributes.getResourceId(R.styleable.SingleLinkExplainView_LeftIcon, -1)
        if (leftIcon == -1) {
            view.findViewById<ImageView>(R.id.left_icon).visibility = GONE
        } else {
            view.findViewById<ImageView>(R.id.left_icon).setImageResource(leftIcon)
        }
        val rightIcon = view.findViewById<ImageView>(R.id.right_icon)
        rightIcon.tag = R.drawable.ic_forward_arrow_settings
        UiUtil.setupOnTouchListener(
            container = view.findViewById(R.id.container),
            textView = view.findViewById(R.id.title),
            iconView = view.findViewById(R.id.right_icon)
        )
        UiUtil.setupOnTouchListener(
            imageViewContainer = view.findViewById(R.id.clip_corner_background),
            textView = view.findViewById(R.id.title)
        )
        val rightMargin = attributes.getFloat(R.styleable.SingleLinkExplainView_RightMargin, 11F)
        val rightMarginPx = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            rightMargin,
            resources.displayMetrics
        ).toInt()
        rightIcon.updateLayoutParams {
            val params = this as ConstraintLayout.LayoutParams
            params.marginEnd = rightMarginPx
            params.rightMargin = rightMarginPx
        }
    }

    fun onClick(click: OnClickListener){
        view.findViewById<ConstraintLayout>(R.id.container).setOnClickListener(click)
        view.findViewById<ImageView>(R.id.clip_corner_background).setOnClickListener(click)
    }
}