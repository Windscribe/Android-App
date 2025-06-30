package com.windscribe.mobile.ui.helper

import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
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

    fun shouldShowRationale(): Boolean {
        return true
    }
}