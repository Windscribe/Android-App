package com.windscribe.mobile.ui.popup

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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
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
import com.windscribe.mobile.ui.common.TextButton
import com.windscribe.mobile.ui.common.theme
import com.windscribe.mobile.ui.helper.MultiDevicePreview
import com.windscribe.mobile.ui.helper.PreviewWithNav
import com.windscribe.mobile.ui.theme.font16
import com.windscribe.mobile.ui.theme.preferencesBackgroundColor
import com.windscribe.vpn.R

@Composable
fun FullScreenDialog(text: String, error: Boolean = false, onDismiss: () -> Unit) {
    Dialog(
        onDismissRequest = {
            onDismiss()
        },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(color = MaterialTheme.colorScheme.preferencesBackgroundColor),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp)
                    .navigationBarsPadding()
                    .statusBarsPadding(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Top-close button
                Row(modifier = Modifier.fillMaxWidth()) {
                    Spacer(modifier = Modifier.weight(1f))
                    Image(
                        painter = painterResource(id = com.windscribe.mobile.R.drawable.ic_close),
                        contentDescription = "Close",
                        colorFilter = ColorFilter.tint(theme(com.windscribe.mobile.R.attr.wdPrimaryColor)),
                        modifier = Modifier
                            .size(24.dp)
                            .clickable {
                                onDismiss()
                            }
                    )
                }

                // Middle content
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(top = 32.dp, bottom = 32.dp)
                ) {
                    Image(
                        painter = painterResource(id = if (error) com.windscribe.mobile.R.drawable.ic_attention_icon else com.windscribe.mobile.R.drawable.ic_green_check_with_background),
                        contentDescription = "Success",
                        modifier = Modifier.padding(vertical = 16.dp)
                    )
                    Text(
                        text = text,
                        style = font16,
                        color = theme(com.windscribe.mobile.R.attr.wdPrimaryColor),
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .widthIn(max = 400.dp)
                            .padding(bottom = 16.dp)
                    )
                }

                // Bottom button
                TextButton(
                    modifier = Modifier
                        .width(400.dp)
                        .align(Alignment.CenterHorizontally),
                    text = stringResource(id = R.string.close),
                    onClick = {
                        onDismiss()
                    }
                )
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