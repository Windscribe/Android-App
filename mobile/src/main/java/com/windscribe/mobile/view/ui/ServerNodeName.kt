package com.windscribe.mobile.view.ui

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import com.windscribe.mobile.view.theme.AppColors
import com.windscribe.mobile.view.theme.font16

@Composable
fun ServerNodeName(name: String, modifier: Modifier) {
    Text(
        text = name,
        style = font16.copy(fontWeight = FontWeight.Medium),
        modifier = modifier,
        color = AppColors.white,
        textAlign = TextAlign.Start
    )
}