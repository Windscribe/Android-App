package com.windscribe.mobile.view.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import okhttp3.internal.threadName

private val DarkColorScheme = darkColorScheme(
    primary = AppColors.deepBlue,
    secondary = AppColors.deepBlue,
    background = AppColors.deepBlue,
)

private val LightColorScheme = lightColorScheme(
    primary = AppColors.deepBlue,
    secondary = AppColors.deepBlue,
    background = AppColors.deepBlue,
)

@Composable
fun AndroidTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    val colorScheme = when {
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }
    MaterialTheme(colorScheme = colorScheme, content = content)
}

val ColorScheme.backgroundColor: Color
    @Composable
    get() = when {
        this == DarkColorScheme -> AppColors.deepBlue
        this == LightColorScheme -> AppColors.white
        else -> if (isSystemInDarkTheme()) AppColors.deepBlue else AppColors.white
    }

val ColorScheme.backgroundColorInverted: Color
    @Composable
    get() = when {
        this == DarkColorScheme -> AppColors.white
        this == LightColorScheme -> AppColors.deepBlue
        else -> if (isSystemInDarkTheme()) AppColors.white else AppColors.deepBlue
    }

val ColorScheme.primaryTextColor: Color
    @Composable
    get() = when {
        this == DarkColorScheme -> AppColors.white
        this == LightColorScheme -> AppColors.deepBlue
        else -> if (isSystemInDarkTheme()) AppColors.white else AppColors.deepBlue
    }