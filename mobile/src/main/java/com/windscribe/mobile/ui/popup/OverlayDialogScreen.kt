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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.windscribe.mobile.R
import com.windscribe.mobile.ui.theme.font16
import com.windscribe.mobile.ui.theme.font24
import com.windscribe.mobile.ui.common.NextButton
import com.windscribe.mobile.ui.common.theme
import com.windscribe.mobile.ui.AppStartActivityViewModel

@Composable
fun OverlayDialogScreen(appStartActivityViewModel: AppStartActivityViewModel? = null) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(theme(R.attr.wdPrimaryInvertedColor)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .padding(32.dp)
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
            Row(modifier = Modifier.fillMaxWidth()) {
                Spacer(modifier = Modifier.weight(1f))
                Image(
                    painter = painterResource(R.drawable.ic_close),
                    contentDescription = null,
                    colorFilter = ColorFilter.tint(theme(R.attr.wdPrimaryColor)),
                    modifier = Modifier
                        .size(24.dp)
                        .clickable { appStartActivityViewModel?.dialogCallback?.onDismiss() }
                )
            }
            Spacer(modifier = Modifier.weight(1f))
            Image(
                painter = painterResource(
                    appStartActivityViewModel?.dialogData?.icon ?: R.drawable.ic_warning_icon
                ),
                contentDescription = "Attention",
                colorFilter = ColorFilter.tint(theme(R.attr.wdPrimaryColor)),
                modifier = Modifier.padding(vertical = 16.dp)
            )

            Text(
                text = appStartActivityViewModel?.dialogData?.title ?: "",
                style = font24,
                color = theme(R.attr.wdPrimaryColor),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            Text(
                text = appStartActivityViewModel?.dialogData?.description ?: "",
                style = font16,
                color = theme(R.attr.wdPrimaryColor),
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .widthIn(max = 400.dp)
                    .padding(bottom = 16.dp)
            )
            Spacer(modifier = Modifier.weight(1f))
            NextButton(
                modifier = Modifier.width(400.dp),
                text = appStartActivityViewModel?.dialogData?.okLabel ?: "",
                enabled = true,
                onClick = { appStartActivityViewModel?.dialogCallback?.onConfirm() }
            )

            Spacer(modifier = Modifier.height(8.dp))

            TextButton(onClick = { appStartActivityViewModel?.dialogCallback?.onDismiss() }) {
                Text(
                    text = stringResource(com.windscribe.vpn.R.string.cancel),
                    style = font16,
                    color = theme(R.attr.wdPrimaryColor)
                )
            }
        }
    }
}