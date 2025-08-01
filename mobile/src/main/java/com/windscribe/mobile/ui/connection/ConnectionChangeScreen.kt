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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.windscribe.mobile.R
import com.windscribe.mobile.ui.nav.LocalNavController
import com.windscribe.mobile.ui.helper.MultiDevicePreview
import com.windscribe.mobile.ui.helper.PreviewWithNav
import com.windscribe.mobile.ui.theme.AppColors
import com.windscribe.mobile.ui.theme.font16
import com.windscribe.mobile.ui.theme.font24
import com.windscribe.mobile.ui.common.ProtocolItemView
import com.windscribe.mobile.ui.AppStartActivityViewModel
import com.windscribe.vpn.autoconnection.ProtocolConnectionStatus
import com.windscribe.vpn.autoconnection.ProtocolInformation
import com.windscribe.vpn.constants.PreferencesKeyConstants
import kotlinx.coroutines.delay

@Composable
fun ConnectionChangeScreen(appStartActivityViewModel: AppStartActivityViewModel? = null, protocolFailed: Boolean) {
    val navController = LocalNavController.current
    val lazyListState = rememberLazyListState()
    val icon = if (protocolFailed) {
        R.drawable.ic_attention_icon
    } else {
        R.drawable.ic_change_protocol
    }
    val title = if (protocolFailed) {
        stringResource(com.windscribe.vpn.R.string.connection_failure)
    } else {
        stringResource(com.windscribe.vpn.R.string.protocol_change)
    }
    val description = if (protocolFailed) {
        stringResource(com.windscribe.vpn.R.string.the_protocol_you_ve_chosen_has_failed_to_connect_windscribe_will_attempt_to_reconnect_using_the_first_protocol_below)
    } else {
        stringResource(com.windscribe.vpn.R.string.protocol_change_description)
    }
    var countdown by remember { mutableIntStateOf(10) }
    LaunchedEffect(protocolFailed) {
        if (protocolFailed) {
            while (countdown > 0) {
                delay(1000)
                countdown--
            }
            val nextUpProtocol = appStartActivityViewModel?.protocolInformationList?.firstOrNull()
            if (nextUpProtocol != null) {
                appStartActivityViewModel.autoConnectionModeCallback?.onProtocolSelect(nextUpProtocol)
                navController.popBackStack()
            }
        }
    }
    val scrollState: ScrollState = rememberScrollState()
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(color = AppColors.midnight)
            .clickable(enabled = false) {}) {
        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .padding(horizontal = 32.dp)
                .width(560.dp)
                .statusBarsPadding()
                .verticalScroll(state = scrollState)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Spacer(modifier = Modifier.height(24.dp))
            Image(
                painter = painterResource(icon),
                contentDescription = null,
                modifier = Modifier
                    .size(100.dp)
                    .padding(top = 8.dp),
                colorFilter = ColorFilter.tint(AppColors.white)
            )
            Text(
                text = title,
                style = font24,
                color = AppColors.white,
                textAlign = TextAlign.Center
            )
            Text(
                text = description,
                style = font16,
                color = AppColors.white.copy(alpha = 0.5f),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 8.dp)
            )
            Spacer(modifier = Modifier.padding(top = 8.dp))
            for (i in appStartActivityViewModel?.protocolInformationList ?: emptyList()) {
                ProtocolItemView(timeleft = countdown, i, onSelected = {
                    appStartActivityViewModel?.autoConnectionModeCallback?.onProtocolSelect(i)
                    navController.popBackStack()
                })
            }

            TextButton(onClick = {
                appStartActivityViewModel?.autoConnectionModeCallback?.onCancel()
                navController.popBackStack()
            }) {
                Text(
                    stringResource(com.windscribe.vpn.R.string.cancel),
                    style = font16,
                    color = AppColors.white.copy(alpha = 0.5f)
                )
            }
        }
        
        // Close button
        Image(
            painter = painterResource(R.drawable.close),
            contentDescription = "Close",
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(24.dp)
                .statusBarsPadding()
                .size(32.dp)
                .clickable {
                    appStartActivityViewModel?.autoConnectionModeCallback?.onCancel()
                    navController.popBackStack()
                },
            colorFilter = ColorFilter.tint(AppColors.white)
        )
    }
}

@MultiDevicePreview
@Composable
fun ConnectionChangeScreenPreview() {
    PreviewWithNav {
        val protocol1 = ProtocolInformation(
            PreferencesKeyConstants.PROTO_IKev2,
            PreferencesKeyConstants.DEFAULT_IKEV2_PORT,
            stringResource(com.windscribe.vpn.R.string.iKEV2_description),
            ProtocolConnectionStatus.Connected
        )
        val protocol2 = ProtocolInformation(
            PreferencesKeyConstants.PROTO_UDP,
            PreferencesKeyConstants.DEFAULT_UDP_LEGACY_PORT,
            stringResource(com.windscribe.vpn.R.string.Udp_description),
            ProtocolConnectionStatus.NextUp
        )
        val protocol3 = ProtocolInformation(
            PreferencesKeyConstants.PROTO_TCP,
            PreferencesKeyConstants.DEFAULT_TCP_LEGACY_PORT,
            stringResource(com.windscribe.vpn.R.string.Tcp_description),
            ProtocolConnectionStatus.Failed
        )
        val protocol4 = ProtocolInformation(
            PreferencesKeyConstants.PROTO_WS_TUNNEL,
            PreferencesKeyConstants.DEFAULT_TCP_LEGACY_PORT,
            stringResource(com.windscribe.vpn.R.string.WSTunnel_description),
            ProtocolConnectionStatus.Disconnected
        )
        val protocols = listOf(protocol1, protocol2, protocol3, protocol4)
        ConnectionChangeScreen(protocolFailed = false)
    }
}