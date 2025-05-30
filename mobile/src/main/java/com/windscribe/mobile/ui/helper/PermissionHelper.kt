package com.windscribe.mobile.ui.helper

import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat.startActivity
import com.windscribe.mobile.networksecurity.networkdetails.NetworkDetailsActivity
import com.windscribe.mobile.ui.AppStartActivity

class PermissionHelper(val activity: AppStartActivity) {
    var backgroundLocationPermissionLauncher: ActivityResultLauncher<String>
    var foregroundLocationPermissionLauncher: ActivityResultLauncher<String>
    var foregroundCallback: (granted: Boolean) -> Unit = { granted -> }
    var backgroundCallback: (granted: Boolean) -> Unit = { granted -> }

    init {
        backgroundLocationPermissionLauncher =
            activity.registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
                backgroundCallback(granted)
            }
        foregroundLocationPermissionLauncher =
            activity.registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
                foregroundCallback(granted)
            }
    }

    fun isGranted(permission: String): Boolean {
        return ActivityCompat.checkSelfPermission(
            activity,
            permission
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED
    }

    fun shouldShowRationale(permission: String): Boolean {
        return ActivityCompat.shouldShowRequestPermissionRationale(activity, permission)
    }

    fun launchNetworkDetailsActivity(networkName: String?) {
        if (networkName != null) {
            activity.startActivity(NetworkDetailsActivity.getStartIntent(activity, networkName))
        } else {
            Toast.makeText(activity, "Network SSID is not available", Toast.LENGTH_SHORT).show()
        }
    }
}