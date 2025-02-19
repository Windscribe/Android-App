/*
 * Copyright (c) 2021 Windscribe Limited.
 */

package com.windscribe.mobile.utils

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.Resources
import android.view.ContextThemeWrapper
import android.view.MotionEvent
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.vectordrawable.graphics.drawable.VectorDrawableCompat
import com.windscribe.mobile.R
import com.windscribe.vpn.Windscribe.Companion.appContext
import com.windscribe.vpn.commonutils.ThemeUtils
import com.windscribe.vpn.constants.BillingConstants
import com.windscribe.vpn.constants.UserStatusConstants
import java.util.regex.Pattern


object UiUtil {
    fun getDataRemainingColor(dataRemaining: Float, maxData: Long): Int {
        return if (maxData != -1L) when {
            dataRemaining < BillingConstants.DATA_LOW_PERCENTAGE * (
                    maxData /
                            UserStatusConstants.GB_DATA.toFloat()
                    ) -> appContext.resources.getColor(R.color.colorRed)

            dataRemaining
                    < BillingConstants.DATA_WARNING_PERCENTAGE * (maxData / UserStatusConstants.GB_DATA.toFloat()) ->
                appContext.resources.getColor(R.color.colorYellow)

            else ->
                appContext.resources.getColor(R.color.colorWhite)
        } else appContext.resources.getColor(R.color.colorWhite)
    }

    fun getPriceWithCurrency(price: String?): Pair<String, Double>? {
        if (price == null){
            return null
        }
        val rawPrice = price.replace("\u00A0", " ").trim()
        val pattern = Pattern.compile("([A-Za-z]{3}|[^\\d,\\.]+)?\\s?([\\d,.]+)")
        val matcher = pattern.matcher(rawPrice)

        return if (matcher.find()) {
            val currency = matcher.group(1)?.trim().orEmpty()
            val priceString = matcher.group(2)?.replace(",", "")
            priceString?.toDoubleOrNull()?.let { priceValue ->
                Pair(currency, priceValue)
            }
        } else {
            null
        }
    }

    fun getStartCount(resources: Resources, width: Int, height: Int): Int {
        val density = resources.displayMetrics.density
        val referenceWidthPx = 163f * density
        val referenceHeightPx = 182f * density
        val starDensity = 60 / (referenceWidthPx * referenceHeightPx)
        return (starDensity * width * height).toInt()
    }

    @SuppressLint("ClickableViewAccessibility")
    fun setupOnTouchListener(
        container: ConstraintLayout? = null,
        imageViewContainer: ImageView? = null,
        textViewContainer: TextView? = null,
        iconView: ImageView? = null,
        textView: TextView
    ) {
        container?.setOnTouchListener { v: View, event: MotionEvent ->
            handleTouchEvent(v.context, event.action, iconView, textView)
            false
        }
        imageViewContainer?.setOnTouchListener { v: View, event: MotionEvent ->
            handleTouchEvent(v.context, event.action, iconView, textView)
            false
        }
        textViewContainer?.setOnTouchListener { v: View, event: MotionEvent ->
            handleTouchEvent(v.context, event.action, iconView, textView)
            false
        }
    }

    private fun handleTouchEvent(
        context: Context,
        event: Int,
        iconView: ImageView? = null,
        textView: TextView
    ) {
        val defaultColor =
            ThemeUtils.getColor(context, R.attr.wdSecondaryColor, R.color.colorWhite50)
        val selectedColor =
            ThemeUtils.getColor(context, R.attr.wdPrimaryColor, R.color.colorWhite50)
        val selectedTheme = ContextThemeWrapper(context, R.style.RightIconFullOpacity).theme
        val defaultTheme = ContextThemeWrapper(context, R.style.ForwardArrowIcon).theme
        if (event == MotionEvent.ACTION_DOWN) {
            setDrawable(context, iconView, theme = selectedTheme)
            textView.setTextColor(selectedColor)
        } else {
            setDrawable(context, iconView, theme = defaultTheme)
            textView.setTextColor(defaultColor)
        }
    }

    private fun setDrawable(context: Context, iconView: ImageView?, theme: Resources.Theme) {
        val tag = iconView?.tag as? Int
        tag?.let {
            val drawable = VectorDrawableCompat.create(context.resources, it, theme)
            iconView.setImageDrawable(drawable)
        }
    }
}