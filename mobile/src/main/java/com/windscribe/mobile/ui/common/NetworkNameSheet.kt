import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.BlurEffect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.windscribe.mobile.R
import com.windscribe.mobile.ui.AppStartActivity
import com.windscribe.mobile.ui.DialogCallback
import com.windscribe.mobile.ui.DialogData
import com.windscribe.mobile.ui.connection.ConnectionViewmodel
import com.windscribe.mobile.ui.connection.NetworkInfoState
import com.windscribe.mobile.ui.helper.hapticClickable
import com.windscribe.mobile.ui.home.HomeViewmodel
import com.windscribe.mobile.ui.nav.LocalNavController
import com.windscribe.mobile.ui.nav.Screen
import com.windscribe.mobile.ui.theme.AppColors
import com.windscribe.mobile.ui.theme.font16

private enum class PermissionDialogType {
    ForegroundLocation, BackgroundLocation, OpenSettings, None
}

@Composable
fun NetworkNameSheet(connectionViewmodel: ConnectionViewmodel, homeViewmodel: HomeViewmodel) {
    val activity = LocalContext.current as AppStartActivity
    val navController = LocalNavController.current
    val networkInfo by connectionViewmodel.networkInfoState.collectAsState()
    val permissionDialogType = remember { mutableStateOf(PermissionDialogType.None) }
    val hapticEnabled by homeViewmodel.hapticFeedbackEnabled.collectAsState()
    val permissionHelper = activity.permissionHelper

    permissionHelper.backgroundCallback = { granted ->
        if (granted) {
            permissionHelper.launchNetworkDetailsActivity(networkInfo.name)
        } else {
            permissionDialogType.value = PermissionDialogType.OpenSettings
        }
    }

    permissionHelper.foregroundCallback = { granted ->
        if (granted) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                permissionHelper.backgroundLocationPermissionLauncher.launch(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
            } else {
                permissionHelper.launchNetworkDetailsActivity(networkInfo.name)
            }
        } else {
            permissionDialogType.value = PermissionDialogType.OpenSettings
        }
    }

    val requestPermissions = {
        when {
            !permissionHelper.isGranted(Manifest.permission.ACCESS_FINE_LOCATION) -> {
                if (permissionHelper.shouldShowRationale(Manifest.permission.ACCESS_FINE_LOCATION)) {
                    permissionDialogType.value = PermissionDialogType.ForegroundLocation
                } else {
                    permissionHelper.foregroundLocationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                }
            }

            Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q &&
                    !permissionHelper.isGranted(Manifest.permission.ACCESS_BACKGROUND_LOCATION) -> {
                if (permissionHelper.shouldShowRationale(Manifest.permission.ACCESS_BACKGROUND_LOCATION)) {
                    permissionDialogType.value = PermissionDialogType.BackgroundLocation
                } else {
                    permissionHelper.backgroundLocationPermissionLauncher.launch(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
                }
            }

            else -> permissionHelper.launchNetworkDetailsActivity(networkInfo.name)
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

        else -> Unit
    }

    Row(verticalAlignment = Alignment.CenterVertically) {
        Image(
            painter = painterResource(
                if (networkInfo is NetworkInfoState.Unsecured)
                    R.drawable.ic_wifi_unsecure
                else
                    R.drawable.ic_wifi
            ),
            contentDescription = null,
            modifier = Modifier.padding(start = 8.dp)
        )

        val hideIp = remember { mutableStateOf(false) }

        Text(
            text = networkInfo.name ?: stringResource(R.string.unknown),
            style = font16.copy(fontWeight = FontWeight.Medium),
            color = AppColors.white,
            modifier = Modifier
                .alpha(0.7f)
                .padding(start = 4.dp)
                .graphicsLayer {
                    renderEffect = if (hideIp.value) BlurEffect(15f, 15f) else null
                }
                .clickable { hideIp.value = !hideIp.value }
        )

        Image(
            painter = painterResource(R.drawable.arrow_right),
            contentDescription = null,
            modifier = Modifier
                .size(24.dp)
                .hapticClickable(hapticEnabled = hapticEnabled) { requestPermissions() },
            contentScale = ContentScale.None
        )
    }
}