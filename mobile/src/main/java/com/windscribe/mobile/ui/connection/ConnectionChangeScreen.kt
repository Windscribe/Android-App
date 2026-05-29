package com.windscribe.mobile.ui.connection

import androidx.compose.foundation.Image
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import com.windscribe.mobile.R
import com.windscribe.mobile.ui.AppStartActivityViewModel
import com.windscribe.mobile.ui.common.ProtocolItemView
import com.windscribe.mobile.ui.helper.MultiDevicePreview
import com.windscribe.mobile.ui.helper.PreviewWithNav
import com.windscribe.mobile.ui.nav.LocalNavController
import com.windscribe.mobile.ui.theme.AppColors
import com.windscribe.mobile.ui.theme.font16
import com.windscribe.mobile.ui.theme.font24
import com.windscribe.vpn.apppreference.PreferencesKeyConstants
import com.windscribe.vpn.autoconnection.ProtocolConnectionStatus
import com.windscribe.vpn.autoconnection.ProtocolInformation
import kotlinx.coroutines.delay

/**
 * Stateful entry point. The [AppStartActivityViewModel] is activity-scoped (it carries the
 * connection callbacks and protocol list set by the hosting activity), so it is passed in rather
 * than resolved via `hiltViewModel()`. The countdown, navigation guard and side effects are
 * wired here and rendering is delegated to [ConnectionChangeContent].
 */
@Composable
fun ConnectionChangeScreen(
    appStartActivityViewModel: AppStartActivityViewModel,
    protocolFailed: Boolean,
) {
    val navController = LocalNavController.current
    var isNavigating by remember { mutableStateOf(false) }
    var countdown by remember { mutableIntStateOf(10) }
    val protocols = appStartActivityViewModel.protocolInformationList ?: emptyList()

    LaunchedEffect(protocolFailed) {
        if (protocolFailed) {
            while (countdown > 0 && !isNavigating) {
                delay(1000)
                countdown--
            }
            val nextUpProtocol = appStartActivityViewModel.protocolInformationList?.firstOrNull()
            if (nextUpProtocol != null && !isNavigating) {
                isNavigating = true
                appStartActivityViewModel.autoConnectionModeCallback?.onProtocolSelect(nextUpProtocol)
                navController.popBackStack()
            }
        }
    }

    ConnectionChangeContent(
        protocolFailed = protocolFailed,
        protocols = protocols,
        countdown = countdown,
        cancelEnabled = !isNavigating,
        onProtocolSelected = { protocol ->
            if (!isNavigating) {
                isNavigating = true
                appStartActivityViewModel.autoConnectionModeCallback?.onProtocolSelect(protocol)
                navController.popBackStack()
            }
        },
        onCancelClick = {
            if (!isNavigating) {
                isNavigating = true
                appStartActivityViewModel.autoConnectionModeCallback?.onCancel()
                navController.popBackStack()
            }
        },
    )
}

/**
 * Stateless UI. Everything it needs is passed in, so it renders identically in the app and in
 * `@Preview`. This is the composable previews target.
 */
@Composable
fun ConnectionChangeContent(
    protocolFailed: Boolean,
    protocols: List<ProtocolInformation>,
    countdown: Int,
    cancelEnabled: Boolean,
    onProtocolSelected: (ProtocolInformation) -> Unit,
    onCancelClick: () -> Unit,
) {
    val icon =
        if (protocolFailed) {
            R.drawable.ic_attention_icon
        } else {
            R.drawable.ic_change_protocol
        }
    val title =
        if (protocolFailed) {
            stringResource(com.windscribe.vpn.R.string.connection_failure)
        } else {
            stringResource(com.windscribe.vpn.R.string.protocol_change)
        }
    val description =
        if (protocolFailed) {
            stringResource(
                com.windscribe.vpn.R.string
                    .the_protocol_you_ve_chosen_has_failed_to_connect_windscribe_will_attempt_to_reconnect_using_the_first_protocol_below,
            )
        } else {
            stringResource(com.windscribe.vpn.R.string.protocol_change_description)
        }
    val scrollState: ScrollState = rememberScrollState()

    Box(
        modifier =
            Modifier
                .fillMaxSize()
                .background(color = AppColors.midnight)
                .clickable(enabled = false) {},
    ) {
        Column(
            modifier =
                Modifier
                    .align(Alignment.Center)
                    .padding(horizontal = 32.dp)
                    .width(560.dp)
                    .statusBarsPadding()
                    .verticalScroll(state = scrollState)
                    .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Spacer(modifier = Modifier.height(24.dp))
            Image(
                painter = painterResource(icon),
                contentDescription = null,
                modifier =
                    Modifier
                        .size(100.dp)
                        .padding(top = 8.dp),
                colorFilter = ColorFilter.tint(AppColors.white),
            )
            Text(
                text = title,
                style = font24,
                color = AppColors.white,
                textAlign = TextAlign.Center,
            )
            Text(
                text = description,
                style = font16,
                color = AppColors.white.copy(alpha = 0.5f),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 8.dp),
            )
            Spacer(modifier = Modifier.padding(top = 8.dp))
            for (i in protocols) {
                ProtocolItemView(timeleft = countdown, i, onSelected = {
                    onProtocolSelected(i)
                })
            }

            TextButton(
                onClick = onCancelClick,
                enabled = cancelEnabled,
            ) {
                Text(
                    stringResource(com.windscribe.vpn.R.string.cancel),
                    style = font16,
                    color = AppColors.white.copy(alpha = 0.5f),
                )
            }
        }

        // Close button
        Image(
            painter = painterResource(R.drawable.close),
            contentDescription = "Close",
            modifier =
                Modifier
                    .align(Alignment.TopEnd)
                    .padding(24.dp)
                    .statusBarsPadding()
                    .size(32.dp)
                    .clickable {
                        onCancelClick()
                    },
            colorFilter = ColorFilter.tint(AppColors.white),
        )
    }
}

private class ConnectionChangeProvider : PreviewParameterProvider<List<ProtocolInformation>> {
    override val values =
        sequenceOf(
            listOf(
                ProtocolInformation(
                    PreferencesKeyConstants.PROTO_IKev2,
                    PreferencesKeyConstants.DEFAULT_IKEV2_PORT,
                    "IKEv2",
                    ProtocolConnectionStatus.Connected,
                ),
                ProtocolInformation(
                    PreferencesKeyConstants.PROTO_UDP,
                    PreferencesKeyConstants.DEFAULT_UDP_LEGACY_PORT,
                    "UDP",
                    ProtocolConnectionStatus.NextUp,
                ),
                ProtocolInformation(
                    PreferencesKeyConstants.PROTO_TCP,
                    PreferencesKeyConstants.DEFAULT_TCP_LEGACY_PORT,
                    "TCP",
                    ProtocolConnectionStatus.Failed,
                ),
                ProtocolInformation(
                    PreferencesKeyConstants.PROTO_WS_TUNNEL,
                    PreferencesKeyConstants.DEFAULT_TCP_LEGACY_PORT,
                    "WSTunnel",
                    ProtocolConnectionStatus.Disconnected,
                ),
            ),
        )
}

@MultiDevicePreview
@Composable
fun ConnectionChangeScreenPreview(
    @PreviewParameter(ConnectionChangeProvider::class) protocols: List<ProtocolInformation>,
) {
    PreviewWithNav {
        ConnectionChangeContent(
            protocolFailed = false,
            protocols = protocols,
            countdown = 10,
            cancelEnabled = true,
            onProtocolSelected = {},
            onCancelClick = {},
        )
    }
}
