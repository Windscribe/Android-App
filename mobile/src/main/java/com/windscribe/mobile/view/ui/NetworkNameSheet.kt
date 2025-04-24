
import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.BlurEffect
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.windscribe.mobile.R
import com.windscribe.mobile.view.AppStartActivity
import com.windscribe.mobile.view.theme.AppColors
import com.windscribe.mobile.view.theme.Dimen
import com.windscribe.mobile.view.theme.font16
import com.windscribe.mobile.view.theme.font24
import com.windscribe.mobile.view.ui.NextButton
import com.windscribe.mobile.view.ui.theme
import com.windscribe.mobile.viewmodel.ConnectionViewmodel
import com.windscribe.mobile.viewmodel.NetworkInfoState

private enum class PermissionDialogType {
    ForegroundLocation,
    BackgroundLocation,
    OpenSettings,
    None
}

@Composable
fun NetworkNameSheet(connectionViewmodel: ConnectionViewmodel) {
    val activity = LocalContext.current as AppStartActivity
    val networkInfo by connectionViewmodel.networkInfoState.collectAsState()

    var permissionDialogType by remember { mutableStateOf(PermissionDialogType.None) }

    val isForegroundLocationPermissionGranted =
        remember { activity.isGranted(Manifest.permission.ACCESS_FINE_LOCATION) }

    val isBackgroundLocationPermissionGranted =
        remember {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                activity.isGranted(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
            } else true
        }

    val backgroundLocationPermissionLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { wasGranted ->
            if (wasGranted) {
                activity.launchNetworkDetailsActivity(networkInfo.name)
            } else {
                permissionDialogType = PermissionDialogType.OpenSettings
            }
        }

    val foregroundLocationPermissionLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { wasGranted ->
            if (wasGranted) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    backgroundLocationPermissionLauncher.launch(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
                } else {
                    activity.launchNetworkDetailsActivity(networkInfo.name)
                }
            } else {
                permissionDialogType = PermissionDialogType.OpenSettings
            }
        }

    // Helper function to handle permission requests
    fun requestPermissions() {
        when {
            !isForegroundLocationPermissionGranted -> {
                if (activity.shouldShowRationale(Manifest.permission.ACCESS_FINE_LOCATION)) {
                    permissionDialogType = PermissionDialogType.ForegroundLocation
                } else {
                    foregroundLocationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                }
            }

            !isBackgroundLocationPermissionGranted && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q -> {
                if (activity.shouldShowRationale(Manifest.permission.ACCESS_BACKGROUND_LOCATION)) {
                    permissionDialogType = PermissionDialogType.BackgroundLocation
                } else {
                    backgroundLocationPermissionLauncher.launch(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
                }
            }

            else -> activity.launchNetworkDetailsActivity(networkInfo.name)
        }
    }

    if (permissionDialogType == PermissionDialogType.ForegroundLocation) {
        activity.presentDialog {
            ShowRationaleDialog(
                stringResource(R.string.location_permission_required),
                stringResource(R.string.foreground_location_permission_reason),
                stringResource(R.string.grant_permission),
                R.drawable.ic_attention_icon,
                onConfirm = {
                    permissionDialogType = PermissionDialogType.None
                    activity.cancelDialog()
                    foregroundLocationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                },
                onDismiss = {
                    permissionDialogType = PermissionDialogType.None
                    activity.cancelDialog()
                }
            )
        }
    }

    if (permissionDialogType == PermissionDialogType.BackgroundLocation) {
        activity.presentDialog {
            ShowRationaleDialog(
                stringResource(R.string.allow_all_the_time_location_access_required),
                stringResource(R.string.app_requires_background_location_permission),
                stringResource(R.string.grant_permission),
                R.drawable.location_instruction_icon,
                onConfirm = {
                    permissionDialogType = PermissionDialogType.None
                    activity.cancelDialog()
                    backgroundLocationPermissionLauncher.launch(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
                },
                onDismiss = {
                    permissionDialogType = PermissionDialogType.None
                    activity.cancelDialog()
                }
            )
        }
    }
    if (permissionDialogType == PermissionDialogType.OpenSettings) {
        activity.presentDialog {
            ShowRationaleDialog(
                stringResource(R.string.missing_location_permission),
                stringResource(R.string.location_permission_is_required_to_use_this_feature_go_to_app_settings_permissions_location_and_select_allow_all_the_time),
                stringResource(R.string.open_settings),
                R.drawable.ic_attention_icon,
                onConfirm = {
                    permissionDialogType = PermissionDialogType.None
                    activity.cancelDialog()
                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                    val uri = Uri.fromParts("package", activity.packageName, null)
                    intent.data = uri
                    if (intent.resolveActivity(activity.packageManager) != null) {
                        activity.startActivity(intent)
                    }
                },
                onDismiss = {
                    permissionDialogType = PermissionDialogType.None
                    activity.cancelDialog()
                }
            )
        }
    }

    Row(verticalAlignment = Alignment.CenterVertically) {
        Image(
            painter = painterResource(if (networkInfo is NetworkInfoState.Unsecured) R.drawable.ic_wifi_unsecure else R.drawable.ic_wifi),
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
                    renderEffect = if (hideIp.value) {
                        BlurEffect(15f, 15f)
                    } else {
                        null
                    }
                }
                .clickable {
                    hideIp.value = !hideIp.value
                }
        )
        Image(
            painter = painterResource(R.drawable.arrowright),
            contentDescription = null,
            modifier = Modifier
                .size(Dimen.dp24)
                .clickable { requestPermissions() },
            contentScale = ContentScale.None,
        )
    }
}

@Composable
fun ShowRationaleDialog(
    title: String,
    text: String,
    actionButtonText: String,
    @DrawableRes icon: Int,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(theme(R.attr.wdPrimaryInvertedColor)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(32.dp)
        ) {
            Row(modifier = Modifier.fillMaxWidth()) {
                Spacer(modifier = Modifier.weight(1f))
                Image(
                    painter = painterResource(R.drawable.ic_close),
                    contentDescription = null,
                    colorFilter = ColorFilter.tint(theme(R.attr.wdPrimaryColor)),
                    modifier = Modifier
                        .size(24.dp)
                        .clickable { onDismiss() }
                )
            }
            Spacer(modifier = Modifier.weight(1f))
            Image(
                painter = painterResource(icon),
                contentDescription = "Attention",
                colorFilter = ColorFilter.tint(theme(R.attr.wdPrimaryColor)),
                modifier = Modifier.padding(vertical = 16.dp)
            )

            Text(
                text = title,
                style = font24,
                color = theme(R.attr.wdPrimaryColor),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            Text(
                text = text,
                style = font16,
                color = theme(R.attr.wdPrimaryColor),
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .widthIn(max = 400.dp)
                    .padding(bottom = 16.dp)
            )
            Spacer(modifier = Modifier.weight(1f))
            NextButton(
                modifier = Modifier.width(400.dp),
                text = actionButtonText,
                enabled = true,
                onClick = { onConfirm() }
            )

            Spacer(modifier = Modifier.height(8.dp))

            TextButton(onClick = { onDismiss() }) {
                Text(
                    text = stringResource(R.string.cancel),
                    style = font16,
                    color = theme(R.attr.wdPrimaryColor)
                )
            }
        }
    }
}