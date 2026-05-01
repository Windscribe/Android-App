package com.windscribe.mobile.ui.popup

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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
import com.windscribe.mobile.R
import com.windscribe.mobile.ui.common.PopupSecondaryActionButton
import com.windscribe.mobile.ui.theme.font16
import com.windscribe.mobile.ui.theme.primaryTextColor
import com.windscribe.mobile.ui.theme.serverListBackgroundColor
import com.windscribe.vpn.R as BaseR

@Composable
fun EncryptionWarningDialog(onAcknowledge: () -> Unit) {
    Dialog(
        onDismissRequest = { }, // Force user to acknowledge
        properties = DialogProperties(
            dismissOnBackPress = false,
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = false
        )
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

                // Warning Icon
                Image(
                    painter = painterResource(id = R.drawable.ic_attention_icon),
                    contentDescription = "Security Warning",
                    colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.primaryTextColor)
                )

                Spacer(modifier = Modifier.height(25.dp))

                // Title
                Text(
                    text = stringResource(id = BaseR.string.security_notice),
                    style = font16,
                    color = MaterialTheme.colorScheme.primaryTextColor,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Message
                Text(
                    text = stringResource(id = BaseR.string.encryption_unavailable_warning),
                    style = font16,
                    color = MaterialTheme.colorScheme.primaryTextColor.copy(alpha = 0.50f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )

                Spacer(modifier = Modifier.height(32.dp))

                // Acknowledge button
                PopupSecondaryActionButton(
                    text = stringResource(id = BaseR.string.i_understand),
                    onClick = onAcknowledge
                )

                Spacer(modifier = Modifier.weight(1f))
            }
        }
    }
}
