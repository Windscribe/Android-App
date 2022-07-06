/*
 * Copyright (c) 2021 Windscribe Limited.
 */
package com.windscribe.tv.serverlist.customviews

import android.content.Context
import android.graphics.Rect
import android.graphics.drawable.GradientDrawable
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatImageView
import androidx.core.content.res.ResourcesCompat
import com.windscribe.tv.R

class HomeMenuButton : AppCompatImageView {
    private var radius = 0
    private var stroke = 0

    constructor(context: Context) : super(context) {
        setDimens()
        setState()
    }

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
        setDimens()
        setState()
    }

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    ) {
        setDimens()
        setState()
    }

    fun setState() {
        if (hasFocus()) {
            setImageDrawable(null)
            when (id) {
                R.id.btn_settings -> background = ResourcesCompat.getDrawable(
                    resources, R.drawable.ic_settings_icon_focused,
                    context.theme
                )
                R.id.btn_notifications -> background = ResourcesCompat.getDrawable(
                    resources, R.drawable.ic_notification_icon_focused,
                    context.theme
                )
                R.id.btn_help -> background = ResourcesCompat.getDrawable(
                    resources, R.drawable.ic_help_icon_focused,
                    context.theme
                )
            }
        } else {
            val gradientDrawable = GradientDrawable()
            gradientDrawable.shape = GradientDrawable.OVAL
            gradientDrawable.cornerRadius = radius.toFloat()
            gradientDrawable.color = null
            gradientDrawable.setStroke(stroke, resources.getColor(R.color.colorWhite20))
            background = gradientDrawable
            when (id) {
                R.id.btn_settings -> setImageDrawable(
                    ResourcesCompat.getDrawable(
                        resources, R.drawable.ic_settings_icon,
                        context.theme
                    )
                )
                R.id.btn_notifications -> setImageDrawable(
                    ResourcesCompat.getDrawable(
                        resources, R.drawable.ic_notification_icon,
                        context.theme
                    )
                )
                R.id.btn_help -> setImageDrawable(
                    ResourcesCompat.getDrawable(
                        resources, R.drawable.ic_help_icon,
                        context.theme
                    )
                )
            }
        }
    }

    override fun onFocusChanged(
        gainFocus: Boolean,
        direction: Int,
        previouslyFocusedRect: Rect?
    ) {
        super.onFocusChanged(gainFocus, direction, previouslyFocusedRect)
        setState()
    }

    private fun setDimens() {
        radius = context.resources.getDimension(R.dimen.reg_56).toInt() / 2
        stroke = context.resources.getDimension(R.dimen.reg_2dp).toInt()
    }
}