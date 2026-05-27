package com.windscribe.mobile.upgradeactivity

import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.graphics.PixelFormat
import android.os.Bundle
import android.widget.Toast
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.graphics.toColorInt
import androidx.core.net.toUri
import com.windscribe.vpn.Windscribe
import com.windscribe.vpn.Windscribe.Companion.appContext
import com.windscribe.vpn.apppreference.PreferencesKeyConstants
import java.util.Locale
import java.util.concurrent.atomic.AtomicBoolean

abstract class BaseActivity : AppCompatActivity() {
    val coldLoad = AtomicBoolean()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setWindow()
        window.setFormat(PixelFormat.RGBA_8888)
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        // Set a new pixel format for the window to use for rendering
        val window = window
        window.setFormat(PixelFormat.RGBA_8888)
    }

    open fun setWindow() {
        val isDark =
            Windscribe.appContext.preference.selectedTheme == PreferencesKeyConstants.DARK_THEME
        val navigationBarStyle =
            if (isDark) {
                SystemBarStyle.dark("#0B0F16".toColorInt())
            } else {
                SystemBarStyle.light("#FFFFFF".toColorInt(), "#0B0F16".toColorInt())
            }
        enableEdgeToEdge(navigationBarStyle = navigationBarStyle)
    }

    fun openURLInBrowser(urlToOpen: String) {
        val browserIntent = Intent(Intent.ACTION_VIEW, urlToOpen.toUri())
        if (browserIntent.resolveActivity(packageManager) != null) {
            startActivity(browserIntent)
        } else {
            Toast
                .makeText(
                    this,
                    "No available browser found to open the desired url!",
                    Toast.LENGTH_SHORT,
                ).show()
        }
    }

    override fun attachBaseContext(newBase: Context) {
        val newLocale = appContext.getSavedLocale()
        Locale.setDefault(newLocale)
        val config = Configuration(newBase.resources.configuration)
        config.setLocale(newLocale)
        config.fontScale = 1.0f
        super.attachBaseContext(newBase.createConfigurationContext(config))
    }
}
