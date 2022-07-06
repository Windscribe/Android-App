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
import com.windscribe.tv.serverlist.customviews.State.FavouriteState

class FavouriteButtonView : AppCompatImageView {
    var state = FavouriteState.NotFavourite.stateValue
    private var stroke = 0

    constructor(context: Context) : super(context) {
        setDimens()
        setState(state)
    }

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
        setDimens()
        setState(state)
    }

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    ) {
        setDimens()
        setState(state)
    }

    fun getState(): FavouriteState {
        if (state == 1) {
            return FavouriteState.NotFavourite
        }
        return if (state == 2) {
            FavouriteState.Favourite
        } else {
            FavouriteState.NotFavourite
        }
    }

    @JvmName("setState1")
    fun setState(currentState: Int) {
        state = currentState
        if (currentState == FavouriteState.Favourite.stateValue) {
            setFavouriteState()
        } else {
            setNotFavouriteState()
        }
    }

    private fun setFavouriteState() {
        if (hasFocus()) {
            val gradientDrawable = GradientDrawable()
            gradientDrawable.shape = GradientDrawable.OVAL
            gradientDrawable.setStroke(stroke, resources.getColor(R.color.colorWhite20))
            gradientDrawable.setSize(
                context.resources.getDimension(R.dimen.reg_56).toInt(),
                context.resources.getDimension(R.dimen.reg_56).toInt()
            )
            setImageDrawable(
                ResourcesCompat.getDrawable(
                    resources, R.drawable.ic_remove_fav_icon_focused,
                    context.theme
                )
            )
            scaleType = ScaleType.FIT_CENTER
            setPadding(0, 0, 0, 0)
            background = gradientDrawable
        } else {
            val gradientDrawable = GradientDrawable()
            gradientDrawable.shape = GradientDrawable.OVAL
            gradientDrawable.setStroke(stroke, resources.getColor(R.color.colorWhite20))
            gradientDrawable.color = null
            setImageDrawable(
                ResourcesCompat.getDrawable(
                    resources, R.drawable.ic_remove_fav_icon,
                    context.theme
                )
            )
            scaleType = ScaleType.FIT_CENTER
            setPadding(0, 0, 0, 0)
            background = gradientDrawable
        }
    }

    private fun setNotFavouriteState() {
        if (hasFocus()) {
            val gradientDrawable = GradientDrawable()
            gradientDrawable.shape = GradientDrawable.OVAL
            gradientDrawable.setStroke(stroke, resources.getColor(R.color.colorWhite20))
            gradientDrawable.setSize(
                context.resources.getDimension(R.dimen.reg_56).toInt(),
                context.resources.getDimension(R.dimen.reg_56).toInt()
            )
            setImageDrawable(
                ResourcesCompat.getDrawable(
                    resources, R.drawable.ic_add_fav_icon_focused,
                    context.theme
                )
            )
            scaleType = ScaleType.FIT_CENTER
            background = gradientDrawable
            setPadding(0, 0, 0, 0)
        } else {
            val gradientDrawable = GradientDrawable()
            gradientDrawable.shape = GradientDrawable.OVAL
            gradientDrawable.setStroke(stroke, resources.getColor(R.color.colorWhite20))
            gradientDrawable.color = null
            setImageDrawable(
                ResourcesCompat.getDrawable(resources, R.drawable.ic_add_fav_icon, context.theme)
            )
            scaleType = ScaleType.FIT_CENTER
            setPadding(0, 0, 0, 0)
            background = gradientDrawable
        }
    }

    override fun onFocusChanged(
        gainFocus: Boolean,
        direction: Int,
        previouslyFocusedRect: Rect?
    ) {
        super.onFocusChanged(gainFocus, direction, previouslyFocusedRect)
        setState(state)
    }

    private fun setDimens() {
        stroke = context.resources.getDimension(R.dimen.reg_2dp).toInt()
    }
}