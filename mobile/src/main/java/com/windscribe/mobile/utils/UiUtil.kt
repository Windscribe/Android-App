/*
 * Copyright (c) 2021 Windscribe Limited.
 */

package com.windscribe.mobile.utils

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.windscribe.mobile.R
import com.windscribe.mobile.base.BaseActivity.Companion.REQUEST_BACKGROUND_PERMISSION
import com.windscribe.vpn.Windscribe.Companion.appContext
import com.windscribe.vpn.alert.showAlertDialog
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
}