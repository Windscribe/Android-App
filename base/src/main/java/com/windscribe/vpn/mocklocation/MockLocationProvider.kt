/*
 * Copyright (c) 2021 Windscribe Limited.
 */

package com.windscribe.vpn.mocklocation

import android.content.Context
import android.location.Location
import android.location.LocationManager
import android.os.Build.VERSION
import android.os.SystemClock
import java.lang.Exception
import java.lang.NullPointerException

class MockLocationProvider constructor(private val providerName: String, ctx: Context) {

    private val locationManager: LocationManager = ctx.getSystemService(
        Context.LOCATION_SERVICE
    ) as LocationManager

    @Throws(MockLocationPermissionException::class)
    fun pushLocation(lat: Double, lon: Double) {
        try {
            val location = Location(providerName)
            location.latitude = lat
            location.longitude = lon
            location.altitude = 3.0
            location.speed = 0.01f
            location.bearing = 1.0f
            location.accuracy = 3.0f
            location.time = System.currentTimeMillis()
            location.elapsedRealtimeNanos = SystemClock.elapsedRealtimeNanos()
            if (VERSION.SDK_INT >= 26) {
                location.bearingAccuracyDegrees = 0.1f
            }
            if (VERSION.SDK_INT >= 26) {
                location.verticalAccuracyMeters = 0.1f
            }
            if (VERSION.SDK_INT >= 26) {
                location.speedAccuracyMetersPerSecond = 0.01f
            }
            locationManager.setTestProviderLocation(providerName, location)
        } catch (unused: SecurityException) {
            throw MockLocationPermissionException()
        }
    }

    fun shutdown() {
        try {
            locationManager.removeTestProvider(providerName)
        } catch (ignored: Exception) {
        }
    }

    private fun removeTesProviders() {
        // Remove Network location
        try {
            locationManager.removeTestProvider(LocationManager.NETWORK_PROVIDER)
        } catch (ignored: Exception) {
        }
        // Remove Gps location
        try {
            locationManager.removeTestProvider(LocationManager.GPS_PROVIDER)
        } catch (ignored: Exception) {
        }
    }

    init {
        removeTesProviders()
        try {
            locationManager.addTestProvider(providerName, false, false, false, false, true, true, true, 1, 1)
            locationManager.setTestProviderStatus(providerName, 2, null, System.currentTimeMillis())
            locationManager.setTestProviderEnabled(providerName, true)
        } catch (exception: NullPointerException) {
            throw SecurityException("No location manager found")
        } catch (e: Exception) {
            throw SecurityException("Not allowed to perform MOCK_LOCATION")
        }
    }
}
class MockLocationPermissionException : Throwable()
