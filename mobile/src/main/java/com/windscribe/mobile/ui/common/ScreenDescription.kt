package com.windscribe.mobile.ui.common

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.windscribe.mobile.ui.helper.MultiDevicePreview
import com.windscribe.mobile.ui.helper.PreviewWithNav
import com.windscribe.mobile.ui.theme.font12
import com.windscribe.mobile.ui.theme.preferencesSubtitleColor
import com.windscribe.mobile.ui.theme.primaryTextColor

@Composable
fun ScreenDescription(text: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.primaryTextColor.copy(alpha = 0.10f),
                shape = RoundedCornerShape(12.dp)
            ).padding(top = 14.dp, bottom = 14.dp, start = 16.dp, end = 16.dp)
    ) {
        Text(text, style = font12, textAlign = TextAlign.Start, color = MaterialTheme.colorScheme.preferencesSubtitleColor)
    }
}

@MultiDevicePreview
@Composable
fun ScreenDescriptionPreview() {
    ScreenDescription(text = "R.O.B.E.R.T. is a customizable server-side domain and IP blocking tool. Select the block lists you wish to apply on all your devices by toggling the switch. Learn more")
}