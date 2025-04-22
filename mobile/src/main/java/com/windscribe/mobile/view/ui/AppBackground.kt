package com.windscribe.mobile.view.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.windscribe.mobile.view.theme.AppColors

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