package com.windscribe.mobile.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import com.windscribe.mobile.ui.helper.MultiDevicePreview

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

// Main
val ColorScheme.backgroundColor: Color
    @Composable get() = colorForTheme(AppColors.deepBlue, AppColors.white)

val ColorScheme.primaryTextColor: Color
    @Composable get() = colorForTheme(AppColors.white, AppColors.deepBlue)

// Preferences
val ColorScheme.preferencesBackgroundColor: Color
    @Composable get() = colorForTheme(AppColors.charcoalBlue, AppColors.white)

val ColorScheme.preferencesSubtitleColor: Color
    @Composable get() = AppColors.slateGray

// Server list
val ColorScheme.serverListBackgroundColor: Color
    @Composable get() = colorForTheme(AppColors.midnightNavy, AppColors.white)

val ColorScheme.serverItemTextColor: Color
    @Composable get() = colorForTheme(AppColors.white.copy(alpha = 0.70f), AppColors.slateGray)

val ColorScheme.serverListNavigationGradientEnd: Color
    @Composable get() = colorForTheme(
        AppColors.midnightNavy.copy(alpha = 0.30f), AppColors.white.copy(alpha = 0.90f)
    )

val ColorScheme.serverListSecondaryColor: Color
    @Composable get() = colorForTheme(AppColors.white, AppColors.midnightNavy)

val ColorScheme.expandedServerItemTextColor: Color
    @Composable get() = colorForTheme(AppColors.white, AppColors.slateGray)

// Helper
@Composable
fun ColorScheme.colorForTheme(dark: Color, light: Color): Color = when {
    this == DarkColorScheme -> dark
    this == LightColorScheme -> light
    else -> if (isSystemInDarkTheme()) dark else light
}

@Composable
@MultiDevicePreview
fun ThemeColorPreview() {
    AndroidTheme {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.backgroundColor)
        ) {
            Text(
                text = "Preview Text",
                color = MaterialTheme.colorScheme.primaryTextColor, modifier = Modifier.align(
                    Alignment.Center
                )
            )
        }
    }
}