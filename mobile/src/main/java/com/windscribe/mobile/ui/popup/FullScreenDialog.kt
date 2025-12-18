package com.windscribe.mobile.ui.popup

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.windscribe.mobile.ui.common.PopupSecondaryActionButton
import com.windscribe.mobile.ui.helper.MultiDevicePreview
import com.windscribe.mobile.ui.helper.PreviewWithNav
import com.windscribe.mobile.ui.theme.font16
import com.windscribe.mobile.ui.theme.primaryTextColor
import com.windscribe.mobile.ui.theme.serverListBackgroundColor
import com.windscribe.vpn.R

sealed class FullScreenDialogState {
    object None : FullScreenDialogState()
    data class Success(val message: String) : FullScreenDialogState()
    data class Error(val message: String) : FullScreenDialogState()
}

@Composable
fun HandleFullScreenDialog(
    state: FullScreenDialogState,
    onDismiss: () -> Unit
) {
    when (state) {
        is FullScreenDialogState.Success -> {
            FullScreenDialog(
                text = state.message,
                error = false,
                onDismiss = onDismiss
            )
        }
        is FullScreenDialogState.Error -> {
            FullScreenDialog(
                text = state.message,
                error = true,
                onDismiss = onDismiss
            )
        }
        is FullScreenDialogState.None -> {
            // No dialog to show
        }
    }
}

@Composable
fun FullScreenDialog(text: String, error: Boolean = false, onDismiss: () -> Unit) {
    Dialog(
        onDismissRequest = {
            onDismiss()
        },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        val scrollState = rememberScrollState()
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(color = MaterialTheme.colorScheme.serverListBackgroundColor),
            verticalArrangement = Arrangement.Top,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .fillMaxHeight()
                    .width(274.dp)
                    .verticalScroll(scrollState)
            ) {
                Spacer(modifier = Modifier.weight(1f))

                // Icon
                Image(
                    painter = painterResource(id = if (error) com.windscribe.mobile.R.drawable.ic_attention_icon else com.windscribe.mobile.R.drawable.ic_green_check_with_background),
                    contentDescription = if (error) "Error" else "Success",
                    colorFilter = if (error) ColorFilter.tint(MaterialTheme.colorScheme.primaryTextColor) else null
                )

                Spacer(modifier = Modifier.height(25.dp))

                // Message
                Text(
                    text = text,
                    style = font16,
                    color = MaterialTheme.colorScheme.primaryTextColor.copy(alpha = 0.50f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                )

                Spacer(modifier = Modifier.height(32.dp))

                // Close button
                PopupSecondaryActionButton(
                    text = stringResource(id = R.string.close),
                    onClick = {
                        onDismiss()
                    }
                )

                Spacer(modifier = Modifier.weight(1f))
            }
        }
    }
}

@Composable
@MultiDevicePreview
private fun SuccessDialog() {
    PreviewWithNav {
        FullScreenDialog("Sweet, we’ll get back to you as soon as one of our agents is back from lunch.", true) {

        }
    }
}