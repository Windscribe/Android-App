package com.windscribe.mobile.ui.auth

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.windscribe.mobile.R
import com.windscribe.mobile.ui.AppStartActivity
import com.windscribe.mobile.ui.common.AppBackground
import com.windscribe.mobile.ui.common.NextButton
import com.windscribe.mobile.ui.helper.MultiDevicePreview
import com.windscribe.mobile.ui.helper.PreviewWithNav
import com.windscribe.mobile.ui.nav.LocalNavController
import com.windscribe.mobile.ui.theme.AppColors
import com.windscribe.mobile.ui.theme.font16
import com.windscribe.mobile.ui.theme.font24

@Composable
fun EmergencyConnectScreen(viewModel: EmergencyConnectViewModal? = null) {
    val uiState by viewModel?.uiState?.collectAsState() ?: remember {
        mutableStateOf(
            EmergencyConnectUIState.Disconnected
        )
    }
    val connectionProgressText by viewModel?.connectionProgressText?.collectAsState()
        ?: remember { mutableStateOf("") }
    AppBackground {
        EmergencyConnectCloseIcon(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .statusBarsPadding()
                .padding(16.dp)
        )
        Column(
            modifier = Modifier
                .widthIn(min = 325.dp, max = 373.dp)
                .padding(16.dp)
                .align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Spacer(modifier = Modifier.height(16.dp))
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                EmergencyConnectHeroIcon()
                Spacer(modifier = Modifier.height(16.dp))
                EmergencyConnectTitle()
                Spacer(modifier = Modifier.height(8.dp))
                EmergencyConnectDescription(uiState)
                Spacer(modifier = Modifier.height(40.dp))
                EmergencyConnectProgressBar(uiState, connectionProgressText)
            }
            Spacer(modifier = Modifier.height(24.dp))
            EmergencyConnectButton(uiState) { viewModel?.connectButtonClick() }
            Spacer(modifier = Modifier.height(16.dp))
            EmergencyConnectCancelButton()
        }
        HandleToast(viewModel)
    }
}

@Composable
private fun HandleToast(viewModel: EmergencyConnectViewModal?) {
    if (viewModel == null) return
    val error by viewModel.error.collectAsState(initial = "")
    val activity = LocalNavController.current.context as AppStartActivity
    LaunchedEffect(error) {
        if (error.isNotEmpty()) {
            Toast.makeText(activity, error, Toast.LENGTH_LONG).show()
        }
    }
}

@Composable
fun EmergencyConnectButton(uiState: EmergencyConnectUIState, onClick: () -> Unit) {
    val buttonText = when (uiState) {
        EmergencyConnectUIState.Disconnected -> stringResource(id = R.string.connect)
        else -> stringResource(id = R.string.disconnect)
    }
    NextButton(modifier = Modifier.padding(), buttonText, enabled = true, onClick = onClick)
}

@Composable
fun EmergencyConnectCloseIcon(modifier: Modifier = Modifier) {
    val navController = LocalNavController.current
    IconButton(
        onClick = {
            navController.popBackStack()
        }, modifier = modifier
    ) {
        Icon(
            painter = painterResource(id = R.drawable.close),
            contentDescription = "Close",
            tint = AppColors.white,
            modifier = Modifier.size(24.dp)
        )
    }
}

@Composable
fun EmergencyConnectHeroIcon() {
    Icon(
        painter = painterResource(id = R.drawable.emergency_icon_white),
        contentDescription = null,
        modifier = Modifier.size(60.dp),
        tint = AppColors.white
    )
}

@Composable
fun EmergencyConnectTitle() {
    Text(
        text = stringResource(id = R.string.emergency_connect),
        style = font24,
        textAlign = TextAlign.Center,
        color = AppColors.white
    )
}

@Composable
fun EmergencyConnectDescription(uiState: EmergencyConnectUIState) {
    val descriptionText = when (uiState) {
        EmergencyConnectUIState.Disconnected -> stringResource(id = R.string.emergency_connect_description)
        EmergencyConnectUIState.Connected -> stringResource(id = R.string.emergency_connected_description)
        EmergencyConnectUIState.Connecting -> ""
    }
    if (descriptionText.isEmpty()) return
    Text(
        text = descriptionText,
        style = font16,
        textAlign = TextAlign.Center,
        color = AppColors.white.copy(alpha = 0.50f),
        modifier = Modifier.padding(horizontal = 16.dp)
    )
}

@Composable
fun EmergencyConnectProgressBar(uiState: EmergencyConnectUIState, connectionProgressText: String) {
    if (uiState == EmergencyConnectUIState.Connecting) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = connectionProgressText,
                style = font16,
                color = AppColors.white.copy(alpha = 0.50f)
            )
            Spacer(modifier = Modifier.height(16.dp))
            CircularProgressIndicator(
                modifier = Modifier.size(48.dp),
                color = AppColors.white
            )
        }
    }
}

@Composable
fun EmergencyConnectCancelButton() {
    val navController = LocalNavController.current
    TextButton(onClick = {
        navController.popBackStack()
    }) {
        Text(
            text = stringResource(id = R.string.cancel),
            style = font16,
            color = AppColors.white.copy(alpha = 0.50f)
        )
    }
}

@Composable
@MultiDevicePreview
fun EmergencyConnectScreenPreview() {
    PreviewWithNav {
        EmergencyConnectScreen()
    }
}