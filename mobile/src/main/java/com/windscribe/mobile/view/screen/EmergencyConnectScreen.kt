package com.windscribe.mobile.view.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewScreenSizes
import com.windscribe.mobile.R
import com.windscribe.mobile.view.LocalNavController
import com.windscribe.mobile.view.NavigationStack
import com.windscribe.mobile.view.ui.AppBackground
import com.windscribe.mobile.view.ui.NextButton
import com.windscribe.mobile.viewmodel.EmergencyConnectViewModal
import com.windscribe.mobile.welcome.state.EmergencyConnectUIState
import com.windscribe.mobile.view.theme.AppColors
import com.windscribe.mobile.view.theme.Dimen
import com.windscribe.mobile.view.theme.font16
import com.windscribe.mobile.view.theme.font24

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
        )
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(Dimen.dp16),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Spacer(modifier = Modifier.height(Dimen.dp16))
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                EmergencyConnectHeroIcon()
                Spacer(modifier = Modifier.height(Dimen.dp16))
                EmergencyConnectTitle()
                Spacer(modifier = Modifier.height(Dimen.dp8))
                EmergencyConnectDescription(uiState)
                Spacer(modifier = Modifier.height(Dimen.dp40))
                EmergencyConnectProgressBar(uiState, connectionProgressText)
            }
            Spacer(modifier = Modifier.height(Dimen.dp24))
            EmergencyConnectButton(uiState) { viewModel?.connectButtonClick() }
            Spacer(modifier = Modifier.height(Dimen.dp16))
            EmergencyConnectCancelButton()
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
            painter = painterResource(id = R.drawable.ic_close_white),
            contentDescription = "Close",
            tint = AppColors.white
        )
    }
}

@Composable
fun EmergencyConnectHeroIcon() {
    Icon(
        painter = painterResource(id = R.drawable.emergency_icon_white),
        contentDescription = null,
        modifier = Modifier.size(Dimen.dp60),
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
        color = AppColors.white50,
        modifier = Modifier.padding(horizontal = Dimen.dp16)
    )
}

@Composable
fun EmergencyConnectProgressBar(uiState: EmergencyConnectUIState, connectionProgressText: String) {
    if (uiState == EmergencyConnectUIState.Connecting) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = connectionProgressText,
                style = font16,
                color = AppColors.white50
            )
            Spacer(modifier = Modifier.height(Dimen.dp16))
            CircularProgressIndicator(
                modifier = Modifier.size(Dimen.dp48),
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
            color = AppColors.white50
        )
    }
}

@Composable
@Preview(showSystemUi = true)
@PreviewScreenSizes
fun EmergencyConnectScreenPreview() {
    NavigationStack(Screen.EmergencyConnect)
}