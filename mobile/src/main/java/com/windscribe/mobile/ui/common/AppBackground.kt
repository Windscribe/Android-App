package com.windscribe.mobile.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.windscribe.mobile.ui.theme.AppColors
import com.windscribe.mobile.ui.theme.preferencesBackgroundColor

@Composable
fun AppBackground(content: @Composable BoxScope.() -> Unit) {
    Box(modifier = Modifier
        .fillMaxSize()
        .focusable()
        .clickable {}
        .background(AppColors.deepBlue)
        .focusable(true)) {
        content()
    }
}

@Composable
fun PreferenceBackground(content: @Composable BoxScope.() -> Unit) {
    Box(modifier = Modifier
        .fillMaxSize()
        .focusable()
        .clickable {}
        .background(MaterialTheme.colorScheme.preferencesBackgroundColor)
        .focusable(true)) {
        content()
    }
}