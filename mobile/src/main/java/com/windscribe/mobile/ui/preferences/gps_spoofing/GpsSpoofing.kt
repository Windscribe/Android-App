package com.windscribe.mobile.ui.preferences.gps_spoofing

import android.content.Intent
import android.provider.Settings
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle.Event
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.windscribe.mobile.R
import com.windscribe.mobile.ui.AppStartActivityViewModel
import com.windscribe.mobile.ui.common.NextButton
import com.windscribe.mobile.ui.common.PreferenceBackground
import com.windscribe.mobile.ui.helper.MultiDevicePreview
import com.windscribe.mobile.ui.helper.PreviewWithNav
import com.windscribe.mobile.ui.nav.LocalNavController
import com.windscribe.mobile.ui.theme.AppColors
import com.windscribe.mobile.ui.theme.font16
import com.windscribe.mobile.ui.theme.font24
import com.windscribe.mobile.ui.theme.preferencesSubtitleColor
import com.windscribe.mobile.ui.theme.primaryTextColor
import com.windscribe.vpn.mocklocation.MockLocationManager.Companion.isAppSelectedInMockLocationList
import com.windscribe.vpn.mocklocation.MockLocationManager.Companion.isDevModeOn

private enum class GpsStep {
    Start,
    Setup,
    Success,
    Error,
}

@Composable
fun GpsSpoofing(viewModel: AppStartActivityViewModel) {
    GpsSpoofingContent(onEnableGpsSpoofing = viewModel::enableGpsSpoofing)
}

@Composable
fun GpsSpoofingContent(onEnableGpsSpoofing: () -> Unit = {}) {
    val navController = LocalNavController.current
    var currentStep by rememberSaveable { mutableStateOf(GpsStep.Start) }
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var devModeOn by remember { mutableStateOf(false) }
    var mockAppSelected by remember { mutableStateOf(false) }
    var openedSettings by rememberSaveable { mutableStateOf(false) }
    DisposableEffect(lifecycleOwner) {
        val observer =
            LifecycleEventObserver { _, event ->
                if (event == Event.ON_RESUME) {
                    mockAppSelected = isAppSelectedInMockLocationList(context)
                    // Being the mock location app already proves dev mode is on, so only check the flag otherwise.
                    devModeOn = mockAppSelected || isDevModeOn(context)
                    if (currentStep == GpsStep.Setup) {
                        if (mockAppSelected) {
                            currentStep = GpsStep.Success
                        } else if (openedSettings && !devModeOn) {
                            currentStep = GpsStep.Error
                        }
                        openedSettings = false
                    }
                }
            }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }
    when (currentStep) {
        GpsStep.Start -> {
            Dialog(
                icon = R.drawable.ic_network_security_feature_icon,
                title = com.windscribe.vpn.R.string.gps_spoofing,
                description = com.windscribe.vpn.R.string.gps_spoofing_explain,
                action = com.windscribe.vpn.R.string.let_s_do_it,
                onAccept = { currentStep = GpsStep.Setup },
                onCancel = { navController.popBackStack() },
            )
        }

        GpsStep.Setup -> {
            Dialog(
                icon = R.drawable.ic_network_security_feature_icon,
                title = com.windscribe.vpn.R.string.gps_spoofing_setup,
                description = com.windscribe.vpn.R.string.gps_spoofing_setup_explain,
                onAccept = {
                    if (mockAppSelected) {
                        currentStep = GpsStep.Success
                    } else {
                        openedSettings = true
                        val intent =
                            Intent(
                                if (devModeOn) {
                                    Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS
                                } else {
                                    Settings.ACTION_DEVICE_INFO_SETTINGS
                                },
                            )
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        if (intent.resolveActivity(context.packageManager) != null) {
                            context.startActivity(intent)
                        } else {
                            Toast
                                .makeText(context, "Settings App not found.", Toast.LENGTH_SHORT)
                                .show()
                        }
                    }
                },
                action =
                    if (mockAppSelected) {
                        com.windscribe.vpn.R.string.next
                    } else {
                        com.windscribe.vpn.R.string.open_settings
                    },
                onCancel = { navController.popBackStack() },
            )
        }

        GpsStep.Success -> {
            Dialog(
                icon = R.drawable.ic_success_bg,
                title = com.windscribe.vpn.R.string.you_are_done,
                description = com.windscribe.vpn.R.string.gps_spoofing_success,
                action = com.windscribe.vpn.R.string.close,
                hideCancel = true,
                backgroundColor = AppColors.neonGreen.copy(alpha = 0.25f),
                onAccept = {
                    onEnableGpsSpoofing()
                    navController.popBackStack()
                },
                onCancel = {
                    navController.popBackStack()
                },
            )
        }

        GpsStep.Error -> {
            Dialog(
                icon = R.drawable.ic_exmark,
                title = com.windscribe.vpn.R.string.gps_spoofing_not_set,
                description = com.windscribe.vpn.R.string.gps_spoofing_error,
                action = com.windscribe.vpn.R.string.close,
                hideCancel = true,
                backgroundColor = AppColors.yellow.copy(alpha = 0.25f),
                onAccept = {
                    navController.popBackStack()
                },
                onCancel = {
                    navController.popBackStack()
                },
            )
        }
    }
}

@Composable
private fun Dialog(
    icon: Int = R.drawable.ic_network_security_feature_icon,
    title: Int = com.windscribe.vpn.R.string.gps_spoofing_setup,
    description: Int,
    action: Int = com.windscribe.vpn.R.string.open_settings,
    hideCancel: Boolean = false,
    backgroundColor: Color = Color.Transparent,
    onAccept: () -> Unit,
    onCancel: () -> Unit,
) {
    PreferenceBackground {
        Column(
            modifier =
                Modifier
                    .width(400.dp)
                    .padding(horizontal = 32.dp)
                    .align(Alignment.Center),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Image(
                painter = painterResource(id = icon),
                contentDescription = null,
                colorFilter =
                    if (backgroundColor == Color.Transparent) {
                        ColorFilter.tint(
                            MaterialTheme.colorScheme.primaryTextColor,
                        )
                    } else {
                        null
                    },
                modifier =
                    Modifier
                        .background(color = backgroundColor, shape = CircleShape)
                        .padding(24.dp),
            )
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = stringResource(id = title),
                style = font24,
                color = MaterialTheme.colorScheme.primaryTextColor,
                modifier =
                    Modifier
                        .padding(vertical = 16.dp)
                        .fillMaxWidth(),
            )
            Text(
                text = stringResource(id = description),
                style = font16,
                color = MaterialTheme.colorScheme.preferencesSubtitleColor,
                modifier =
                    Modifier
                        .fillMaxWidth(),
            )
            Spacer(modifier = Modifier.height(16.dp))
            NextButton(
                text = stringResource(action),
                enabled = true,
                onClick = {
                    onAccept()
                },
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(top = 32.dp),
            )
            if (!hideCancel) {
                Spacer(modifier = Modifier.height(16.dp))
                TextButton(onClick = {
                    onCancel()
                }) {
                    Text(
                        stringResource(id = com.windscribe.vpn.R.string.close),
                        style = font16,
                        color = MaterialTheme.colorScheme.preferencesSubtitleColor,
                    )
                }
            }
        }
    }
}

@MultiDevicePreview
@Composable
fun GpsSpoofingPreview() {
    PreviewWithNav {
        GpsSpoofingContent()
    }
}
