package com.windscribe.mobile.ui.connection

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import com.windscribe.mobile.R
import com.windscribe.mobile.ui.AppStartActivityViewModel
import com.windscribe.mobile.ui.common.NextButton
import com.windscribe.mobile.ui.helper.MultiDevicePreview
import com.windscribe.mobile.ui.helper.PreviewWithNav
import com.windscribe.mobile.ui.nav.LocalNavController
import com.windscribe.mobile.ui.theme.AppColors
import com.windscribe.mobile.ui.theme.font16
import com.windscribe.mobile.ui.theme.font24
import com.windscribe.vpn.apppreference.PreferencesKeyConstants
import com.windscribe.vpn.autoconnection.ProtocolConnectionStatus
import com.windscribe.vpn.autoconnection.ProtocolInformation
import com.windscribe.vpn.backend.Util

/**
 * Stateful entry point. The [AppStartActivityViewModel] is activity-scoped (it carries the
 * connection callbacks and the selected protocol set by the hosting activity), so it is passed in
 * rather than resolved via `hiltViewModel()`. The navigation guard and side effects are wired
 * here and rendering is delegated to [SetupPreferredProtocolContent].
 */
@Composable
fun SetupPreferredProtocolScreen(appStartActivityViewModel: AppStartActivityViewModel) {
    val navController = LocalNavController.current
    val proto = appStartActivityViewModel.protocolInformation
    var isNavigating by remember { mutableStateOf(false) }

    if (proto == null) {
        if (!isNavigating) {
            navController.popBackStack()
        }
        return
    }

    SetupPreferredProtocolContent(
        protocol = proto,
        cancelEnabled = !isNavigating,
        onSetAsPreferredClick = {
            if (!isNavigating) {
                isNavigating = true
                appStartActivityViewModel.autoConnectionModeCallback?.onSetAsPreferredClicked()
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
fun SetupPreferredProtocolContent(
    protocol: ProtocolInformation,
    cancelEnabled: Boolean,
    onSetAsPreferredClick: () -> Unit,
    onCancelClick: () -> Unit,
) {
    Box(
        modifier =
            Modifier
                .fillMaxSize()
                .testTag("preferred_protocol_screen")
                .background(color = AppColors.deepBlue)
                .clickable(enabled = false) {},
    ) {
        Column(
            modifier =
                Modifier
                    .align(Alignment.Center)
                    .padding(horizontal = 32.dp)
                    .width(560.dp)
                    .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Image(
                painter = painterResource(id = R.drawable.ic_attention_icon),
                contentDescription = null,
                modifier =
                    Modifier
                        .size(100.dp)
                        .padding(top = 8.dp),
                colorFilter = ColorFilter.tint(AppColors.white),
            )
            Text(
                text = stringResource(com.windscribe.vpn.R.string.set_this_protocol_as_preferred, Util.getProtocolLabel(protocol.protocol)),
                style = font24,
                color = AppColors.white,
                textAlign = TextAlign.Center,
            )
            Text(
                text =
                    stringResource(
                        com.windscribe.vpn.R.string
                            .windscribe_will_always_use_this_protocol_to_connect_on_this_network_in_the_future_to_avoid_any_interruptions,
                    ),
                style = font16,
                color = AppColors.white,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 8.dp),
            )
            Spacer(modifier = Modifier.padding(top = 24.dp))
            NextButton(
                Modifier.testTag("preferred_protocol_set"),
                text = stringResource(com.windscribe.vpn.R.string.set_as_preferred),
                enabled = true,
            ) {
                onSetAsPreferredClick()
            }
            TextButton(
                onClick = onCancelClick,
                enabled = cancelEnabled,
                modifier = Modifier.testTag("preferred_protocol_cancel"),
            ) {
                Text(
                    stringResource(com.windscribe.vpn.R.string.cancel),
                    style = font16,
                    color = AppColors.white,
                )
            }
        }
    }
}

private class SetupPreferredProtocolProvider : PreviewParameterProvider<ProtocolInformation> {
    override val values =
        sequenceOf(
            ProtocolInformation(
                PreferencesKeyConstants.PROTO_IKev2,
                PreferencesKeyConstants.DEFAULT_IKEV2_PORT,
                "IKEv2",
                ProtocolConnectionStatus.Connected,
            ),
        )
}

@MultiDevicePreview
@Composable
fun SetupPreferredProtocolScreenPreview(
    @PreviewParameter(SetupPreferredProtocolProvider::class) protocol: ProtocolInformation,
) {
    PreviewWithNav {
        SetupPreferredProtocolContent(
            protocol = protocol,
            cancelEnabled = true,
            onSetAsPreferredClick = {},
            onCancelClick = {},
        )
    }
}
