package com.windscribe.mobile.ui.helper

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.windscribe.mobile.ui.theme.DarkColorScheme

@Composable
fun ForceStatusBarIcons(forceLight: Boolean = false) {
    val view = LocalView.current
    val activity = view.context as? Activity
    val isDark = MaterialTheme.colorScheme == DarkColorScheme
    if (activity == null) return
    WindowCompat.setDecorFitsSystemWindows(activity.window, false)
    SideEffect {
        WindowCompat.getInsetsController(activity.window, view)
            .isAppearanceLightStatusBars = if (forceLight) {
                false
        } else {
            !isDark
        }
    }
}