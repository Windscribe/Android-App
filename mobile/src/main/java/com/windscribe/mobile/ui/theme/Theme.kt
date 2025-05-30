package com.windscribe.mobile.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

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

val ColorScheme.preferencesBackgroundColor: Color
    @Composable
    get() = when {
        this == DarkColorScheme -> AppColors.charcoalBlue
        this == LightColorScheme -> AppColors.white
        else -> if (isSystemInDarkTheme()) AppColors.charcoalBlue else AppColors.white
    }

val ColorScheme.preferencesSubtitleColor: Color
    @Composable
    get() = when {
        this == DarkColorScheme -> AppColors.coolGray
        this == LightColorScheme -> AppColors.charcoalBlue
        else -> if (isSystemInDarkTheme()) AppColors.coolGray else AppColors.charcoalBlue
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
val ColorScheme.attention: Color
    @Composable
    get() = when {
        this == DarkColorScheme -> AppColors.neonGreen
        this == LightColorScheme -> AppColors.navyBlue
        else -> if (isSystemInDarkTheme()) AppColors.neonGreen else AppColors.navyBlue
    }