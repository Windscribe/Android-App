package com.windscribe.mobile.custom_view.preferences

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.TypedArray
import android.util.AttributeSet
import android.view.Gravity
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.widget.TextViewCompat
import com.windscribe.mobile.R
import com.windscribe.mobile.utils.UiUtil


@SuppressLint("ClickableViewAccessibility")
class IconLinkView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {

    private val attributes: TypedArray =
        context.obtainStyledAttributes(attrs, R.styleable.ItemLinkView)
    private val view: View = View.inflate(context, R.layout.icon_link_item_view, this)
    var text: String
        get() {
            return view.findViewById<TextView>(R.id.title).text.toString()
        }
        set(value) {
            view.findViewById<TextView>(R.id.title).text = value
        }

    init {
        val titleTextView = view.findViewById<TextView>(R.id.title)
        titleTextView.text =
            attributes.getString(R.styleable.ItemLinkView_ItemLinkViewTitle)
        val leftIcon = attributes.getResourceId(R.styleable.ItemLinkView_ItemLinkViewLeftIcon, -1)
        if (leftIcon == -1) {
            view.findViewById<ImageView>(R.id.left_icon).visibility = GONE
        } else {
            view.findViewById<ImageView>(R.id.left_icon).setImageResource(leftIcon)
        }
        val rightIcon = attributes.getResourceId(R.styleable.ItemLinkView_ItemLinkViewRightIcon, -1)
        if (rightIcon != -1) {
            view.findViewById<ImageView>(R.id.right_icon).setImageResource(rightIcon)
            view.findViewById<ImageView>(R.id.right_icon).tag = rightIcon
        }
        val isCommunityLink = attributes.getBoolean(R.styleable.ItemLinkView_CommunityLinkLabel, false)
        if (isCommunityLink) {
            titleTextView.apply {
                TextViewCompat.setTextAppearance(this, R.style.CommunityLinkLabel)
                gravity = Gravity.START or Gravity.CENTER_VERTICAL
            }
        }
        if (!isCommunityLink) {
            UiUtil.setupOnTouchListener(
                container = view.findViewById(R.id.container),
                textView = view.findViewById(R.id.title),
                iconView = view.findViewById(R.id.right_icon)
            )
        }
    }

    fun onClick(click: OnClickListener) {
        view.findViewById<ConstraintLayout>(R.id.container).setOnClickListener(click)
    }
}