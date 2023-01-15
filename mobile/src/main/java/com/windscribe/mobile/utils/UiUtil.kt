/*
 * Copyright (c) 2021 Windscribe Limited.
 */

package com.windscribe.mobile.utils

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.content.res.Resources
import android.os.Build
import android.view.ContextThemeWrapper
import android.view.MotionEvent
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.vectordrawable.graphics.drawable.VectorDrawableCompat
import com.windscribe.mobile.R
import com.windscribe.mobile.base.BaseActivity.Companion.REQUEST_BACKGROUND_PERMISSION
import com.windscribe.vpn.Windscribe.Companion.appContext
import com.windscribe.vpn.alert.showAlertDialog
import com.windscribe.vpn.commonutils.ThemeUtils
import com.windscribe.vpn.constants.BillingConstants
import com.windscribe.vpn.constants.UserStatusConstants


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

    fun isBackgroundLocationPermissionGranted(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            (ContextCompat
                .checkSelfPermission(context, Manifest.permission.ACCESS_BACKGROUND_LOCATION)
                    == PackageManager.PERMISSION_GRANTED)
        } else {
            true
        }
    }

    fun showBackgroundLocationPermissionAlert(context: AppCompatActivity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            showAlertDialog(
                "App requires background location permission to access WIFI SSID on Android 11+. If you wish to use this feature, press Okay and select \"Allow all the time\" from the permission dialog.",
            ) {
                context.requestPermissions(
                    arrayOf(Manifest.permission.ACCESS_BACKGROUND_LOCATION),
                    REQUEST_BACKGROUND_PERMISSION
                )
            }
        }
    }

    fun locationPermissionAvailable(): Boolean {
        return (ContextCompat
            .checkSelfPermission(appContext, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED
                ) && isBackgroundLocationPermissionGranted(appContext)
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