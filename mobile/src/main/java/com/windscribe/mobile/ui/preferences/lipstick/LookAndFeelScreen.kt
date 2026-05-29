package com.windscribe.mobile.ui.preferences.lipstick

import AppTheme
import AppThemeContent
import PreferencesNavBar
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.windscribe.mobile.ui.common.PreferenceBackground
import com.windscribe.mobile.ui.connection.ToastMessage
import com.windscribe.mobile.ui.helper.MultiDevicePreview
import com.windscribe.mobile.ui.helper.PreviewWithNav
import com.windscribe.mobile.ui.helper.hapticClickable
import com.windscribe.mobile.ui.nav.LocalNavController
import com.windscribe.mobile.ui.nav.Screen
import com.windscribe.mobile.ui.theme.font14
import com.windscribe.mobile.ui.theme.font16
import com.windscribe.mobile.ui.theme.preferencesSubtitleColor
import com.windscribe.mobile.ui.theme.primaryTextColor
import com.windscribe.vpn.R

@Composable
fun LookAndFeelScreen(viewmodel: LipstickViewmodel = hiltViewModel<LipstickViewmodelImpl>()) {
    val navController = LocalNavController.current
    val scrollState = rememberScrollState()
    val toastMessage by viewmodel.toastMessage.collectAsState()
    val selectedAppIcon by viewmodel.selectedAppIcon.collectAsState()

    val backgroundState =
        AppCustomBackgroundState(
            aspectRatioItem = viewmodel.aspectRatioItem.collectAsState().value,
            whenDisconnectedItem = viewmodel.whenDisconnectedBackgroundItem.collectAsState().value,
            bundledDisconnectedItem = viewmodel.bundledDisconnectedBackgroundItem.collectAsState().value,
            customDisconnectedItem = viewmodel.customDisconnectedBackgroundItem.collectAsState().value,
            whenConnectedItem = viewmodel.whenConnectedBackgroundItem.collectAsState().value,
            bundledConnectedItem = viewmodel.bundledConnectedBackgroundItem.collectAsState().value,
            customConnectedItem = viewmodel.customConnectedBackgroundItem.collectAsState().value,
        )
    val backgroundActions =
        AppCustomBackgroundActions(
            onAspectRatioSelected = viewmodel::onAspectRatioItemSelected,
            onWhenDisconnectedSelected = viewmodel::onWhenDisconnectedBackgroundItemSelected,
            onDisconnectedBundledSelected = viewmodel::onDisconnectedBundledBackgroundItemSelected,
            onLoadDisconnectedCustomBackground = viewmodel::loadDisconnectedCustomBackground,
            onWhenConnectedSelected = viewmodel::onWhenConnectedBackgroundItemSelected,
            onConnectedBundledSelected = viewmodel::onConnectedBundledBackgroundItemSelected,
            onLoadConnectedCustomBackground = viewmodel::loadConnectedCustomBackground,
        )

    val soundState =
        AppCustomSoundState(
            whenDisconnectedItem = viewmodel.whenDisconnectedSoundItem.collectAsState().value,
            bundledDisconnectedItem = viewmodel.bundledDisconnectedSoundItem.collectAsState().value,
            customDisconnectedItem = viewmodel.customDisconnectedSoundItem.collectAsState().value,
            whenConnectedItem = viewmodel.whenConnectedSoundItem.collectAsState().value,
            bundledConnectedItem = viewmodel.bundledConnectedSoundItem.collectAsState().value,
            customConnectedItem = viewmodel.customConnectedSoundItem.collectAsState().value,
        )
    val soundActions =
        AppCustomSoundActions(
            onWhenDisconnectedSelected = viewmodel::onWhenDisconnectedSoundItemSelected,
            onDisconnectedBundledSelected = viewmodel::onDisconnectedBundledSoundItemSelected,
            onLoadDisconnectedCustomSound = viewmodel::loadDisconnectedCustomSound,
            onWhenConnectedSelected = viewmodel::onWhenConnectedSoundItemSelected,
            onConnectedBundledSelected = viewmodel::onConnectedBundledSoundItemSelected,
            onLoadConnectedCustomSound = viewmodel::loadConnectedCustomSound,
        )

    val renameActions =
        RenameLocationsActions(
            onImportServerList = viewmodel::loadServerListFile,
            onExportServerList = viewmodel::exportServerListFile,
            onResetClick = viewmodel::onResetClick,
        )

    HandleToast(toastMessage = toastMessage, onClearToast = viewmodel::clearToast)

    PreferenceBackground {
        Column(
            modifier =
                Modifier
                    .padding(vertical = 16.dp, horizontal = 16.dp)
                    .navigationBarsPadding(),
        ) {
            PreferencesNavBar(stringResource(R.string.look_and_feel)) {
                navController.popBackStack()
            }
            Spacer(modifier = Modifier.height(20.dp))
            Column(modifier = Modifier.verticalScroll(scrollState)) {
                AppTheme(viewmodel)
                Spacer(modifier = Modifier.height(16.dp))
                AppCustomBackground(state = backgroundState, actions = backgroundActions)
                Spacer(modifier = Modifier.height(16.dp))
                AppCustomSound(state = soundState, actions = soundActions)
                Spacer(modifier = Modifier.height(16.dp))
                CustomIcon(selectedAppIcon = selectedAppIcon)
                Spacer(modifier = Modifier.height(16.dp))
                RenameLocations(actions = renameActions)
            }
            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

@Composable
private fun CustomIcon(selectedAppIcon: Int) {
    val navController = LocalNavController.current
    val context = LocalContext.current
    val bitmap =
        remember(selectedAppIcon) {
            ContextCompat.getDrawable(context, selectedAppIcon)?.toBitmap()?.asImageBitmap()
        }
    Column(
        modifier =
            Modifier
                .background(
                    color =
                        MaterialTheme.colorScheme.primaryTextColor.copy(
                            alpha = 0.05f,
                        ),
                    shape = RoundedCornerShape(size = 12.dp),
                ).hapticClickable {
                    navController.navigate(Screen.CustomIcon.route)
                }.padding(vertical = 14.dp, horizontal = 14.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            bitmap?.let { imageBitmap ->
                Image(
                    bitmap = imageBitmap,
                    contentDescription = "App icon",
                    modifier = Modifier.size(16.dp),
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                stringResource(R.string.app_icon),
                style =
                    font16.copy(
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.primaryTextColor,
                    ),
            )
            Spacer(modifier = Modifier.weight(1f))
            Icon(
                modifier = Modifier.size(16.dp),
                painter = painterResource(com.windscribe.mobile.R.drawable.arrow_right),
                contentDescription = "",
                tint = MaterialTheme.colorScheme.primaryTextColor,
            )
        }
        Spacer(modifier = Modifier.height(13.5.dp))
        Text(
            text = stringResource(R.string.app_icon_description),
            style = font14.copy(fontWeight = FontWeight.Normal),
            color = MaterialTheme.colorScheme.preferencesSubtitleColor,
            textAlign = TextAlign.Start,
        )
    }
}

@Composable
private fun HandleToast(
    toastMessage: ToastMessage,
    onClearToast: () -> Unit,
) {
    val context = LocalContext.current
    LaunchedEffect(toastMessage) {
        when (toastMessage) {
            is ToastMessage.Localized -> {
                Toast
                    .makeText(
                        context,
                        toastMessage.message,
                        Toast.LENGTH_SHORT,
                    ).show()
                onClearToast()
            }

            is ToastMessage.Raw -> {
                Toast
                    .makeText(
                        context,
                        toastMessage.message,
                        Toast.LENGTH_SHORT,
                    ).show()
                onClearToast()
            }

            else -> {}
        }
    }
}

@Composable
@MultiDevicePreview
private fun LookAndFeelScreenPreview() {
    val backgroundState =
        AppCustomBackgroundState(
            aspectRatioItem = LookAndFeelHelper.getAspectRatioOptions().first(),
            whenDisconnectedItem = LookAndFeelHelper.getBackgroundOptions().first(),
            bundledDisconnectedItem = LookAndFeelHelper.getBundledBackgroundOptions().first(),
            customDisconnectedItem = null,
            whenConnectedItem = LookAndFeelHelper.getBackgroundOptions().first(),
            bundledConnectedItem = LookAndFeelHelper.getBundledBackgroundOptions().first(),
            customConnectedItem = null,
        )
    val soundState =
        AppCustomSoundState(
            whenDisconnectedItem = LookAndFeelHelper.getSoundOptions().first(),
            bundledDisconnectedItem = LookAndFeelHelper.getBundledSoundOptions().first(),
            customDisconnectedItem = null,
            whenConnectedItem = LookAndFeelHelper.getSoundOptions().first(),
            bundledConnectedItem = LookAndFeelHelper.getBundledSoundOptions().first(),
            customConnectedItem = null,
        )
    PreviewWithNav {
        PreferenceBackground {
            Column(
                modifier =
                    Modifier
                        .padding(vertical = 16.dp, horizontal = 16.dp)
                        .navigationBarsPadding(),
            ) {
                PreferencesNavBar(stringResource(R.string.look_and_feel)) {}
                Spacer(modifier = Modifier.height(20.dp))
                Column {
                    AppThemeContent(
                        themeItem = LookAndFeelHelper.getThemeOptions().first(),
                        onThemeSelected = {},
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    AppCustomBackground(state = backgroundState)
                    Spacer(modifier = Modifier.height(16.dp))
                    AppCustomSound(state = soundState)
                    Spacer(modifier = Modifier.height(16.dp))
                    CustomIcon(selectedAppIcon = R.mipmap.windscribe)
                    Spacer(modifier = Modifier.height(16.dp))
                    RenameLocations()
                }
            }
        }
    }
}
