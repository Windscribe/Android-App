package com.windscribe.common

import android.annotation.SuppressLint
import android.app.Notification
import android.app.Service
import android.content.pm.ServiceInfo
import android.os.Build

@SuppressLint("WrongConstant")
fun Service.startSafeForeground(
    id: Int,
    notification: Notification,
    serviceType: Int = ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE,
) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
        startForeground(id, notification, serviceType)
    } else {
        startForeground(id, notification)
    }
}
