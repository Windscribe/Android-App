package com.windscribe.mobile.ui.common

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import com.windscribe.mobile.ui.theme.AppColors
import com.windscribe.mobile.ui.theme.expandedServerItemTextColor
import com.windscribe.mobile.ui.theme.font16
import com.windscribe.mobile.ui.theme.serverItemTextColor

@Composable
fun ServerNodeName(name: String, modifier: Modifier) {
    Text(
        text = name,
        style = font16.copy(fontWeight = FontWeight.Medium),
        modifier = modifier,
        color = MaterialTheme.colorScheme.expandedServerItemTextColor,
        textAlign = TextAlign.Start
    )
}