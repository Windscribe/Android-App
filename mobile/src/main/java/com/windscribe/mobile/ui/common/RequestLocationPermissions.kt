package com.windscribe.mobile.ui.common

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.windscribe.mobile.R
import com.windscribe.mobile.ui.AppStartActivity
import com.windscribe.mobile.ui.DialogCallback
import com.windscribe.mobile.ui.DialogData
import com.windscribe.mobile.ui.nav.LocalNavController
import com.windscribe.mobile.ui.nav.Screen

@Composable
fun RequestLocationPermissions(
    onGranted: () -> Unit
) {
    val activity = LocalContext.current as AppStartActivity
    val navController = LocalNavController.current
    val permissionDialogType = remember { mutableStateOf(PermissionDialogType.None) }
    val permissionHelper = activity.permissionHelper

    LaunchedEffect(Unit) {
        permissionHelper.backgroundCallback = { granted ->
            if (granted) {
                onGranted()
            } else {
                permissionDialogType.value = PermissionDialogType.OpenSettings
            }
        }

        permissionHelper.foregroundCallback = { granted ->
            if (granted) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    permissionHelper.backgroundLocationPermissionLauncher.launch(
                        Manifest.permission.ACCESS_BACKGROUND_LOCATION
                    )
                } else {
                    onGranted()
                }
            } else {
                permissionDialogType.value = PermissionDialogType.OpenSettings
            }
        }

        when {
            !permissionHelper.isGranted(Manifest.permission.ACCESS_FINE_LOCATION) -> {
                if (permissionHelper.shouldShowRationale(Manifest.permission.ACCESS_FINE_LOCATION)) {
                    permissionDialogType.value = PermissionDialogType.ForegroundLocation
                } else {
                    permissionHelper.foregroundLocationPermissionLauncher.launch(
                        Manifest.permission.ACCESS_FINE_LOCATION
                    )
                }
            }

            Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q &&
                    !permissionHelper.isGranted(Manifest.permission.ACCESS_BACKGROUND_LOCATION) -> {
                if (permissionHelper.shouldShowRationale(Manifest.permission.ACCESS_BACKGROUND_LOCATION)) {
                    permissionDialogType.value = PermissionDialogType.BackgroundLocation
                } else {
                    permissionHelper.backgroundLocationPermissionLauncher.launch(
                        Manifest.permission.ACCESS_BACKGROUND_LOCATION
                    )
                }
            }

            else -> onGranted()
        }
    }

    val showDialog = { data: DialogData, onConfirm: () -> Unit ->
        val callback = object : DialogCallback() {
            override fun onDismiss() {
                permissionDialogType.value = PermissionDialogType.None
                navController.popBackStack()
            }

            override fun onConfirm() {
                permissionDialogType.value = PermissionDialogType.None
                navController.popBackStack()
                onConfirm()
            }
        }
        activity.viewmodel.setDialogCallback(data, callback)
        navController.navigate(Screen.OverlayDialog.route)
    }

    when (permissionDialogType.value) {
        PermissionDialogType.ForegroundLocation -> showDialog(
            DialogData(
                R.drawable.ic_attention_icon,
                stringResource(R.string.location_permission_required),
                stringResource(R.string.foreground_location_permission_reason),
                stringResource(R.string.grant_permission)
            )
        ) {
            permissionHelper.foregroundLocationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }

        PermissionDialogType.BackgroundLocation -> showDialog(
            DialogData(
                R.drawable.location_instruction_icon,
                stringResource(R.string.allow_all_the_time_location_access_required),
                stringResource(R.string.app_requires_background_location_permission),
                stringResource(R.string.grant_permission)
            )
        ) {
            permissionHelper.backgroundLocationPermissionLauncher.launch(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
        }

        PermissionDialogType.OpenSettings -> showDialog(
            DialogData(
                R.drawable.ic_attention_icon,
                stringResource(R.string.missing_location_permission),
                stringResource(R.string.location_permission_is_required_to_use_this_feature_go_to_app_settings_permissions_location_and_select_allow_all_the_time),
                stringResource(R.string.open_settings)
            )
        ) {
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.fromParts("package", activity.packageName, null)
            }
            intent.resolveActivity(activity.packageManager)?.let {
                activity.startActivity(intent)
            }
        }

        else -> {}
    }
}