package com.windscribe.mobile.upgradeactivity

import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.graphics.PixelFormat
import android.graphics.Rect
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.windscribe.mobile.R
import com.windscribe.mobile.di.ActivityComponent
import com.windscribe.mobile.di.ActivityModule
import com.windscribe.vpn.Windscribe.Companion.appContext
import com.windscribe.vpn.constants.PreferencesKeyConstants
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
        var boundingRect: List<Rect> = ArrayList()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val displayCutout = getWindow().decorView.rootWindowInsets.displayCutout
            if (displayCutout != null) {
                boundingRect = displayCutout.boundingRects
            }
        }
    }

    open fun setTheme(context: Context) {
        val savedThem = appContext.preference.selectedTheme
        if (savedThem == PreferencesKeyConstants.DARK_THEME) {
            context.setTheme(R.style.DarkTheme)
        } else {
            context.setTheme(R.style.LightTheme)
        }
    }

    open fun setWindow() {
        val statusBarColor = resources.getColor(android.R.color.transparent)
        window.decorView.systemUiVisibility = (
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                )
        window.statusBarColor = statusBarColor
    }

    fun openURLInBrowser(urlToOpen: String?) {
        val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(urlToOpen))
        if (browserIntent.resolveActivity(packageManager) != null) {
            startActivity(browserIntent)
        } else {
            Toast.makeText(
                this,
                "No available browser found to open the desired url!",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    protected fun setActivityModule(activityModule: ActivityModule?): ActivityComponent {
        return com.windscribe.mobile.di.DaggerActivityComponent.builder().activityModule(activityModule)
            .applicationComponent(
                appContext
                    .applicationComponent
            ).build()
    }

    protected fun setContentLayout(setTheme: Boolean = true) {
        if (setTheme) {
            setTheme(this)
        }
        setLanguage()
        coldLoad.set(true)
    }

    fun setLanguage() {
        val newLocale = appContext.getSavedLocale()
        Locale.setDefault(newLocale)
        val config = Configuration()
        config.locale = newLocale
        appContext.resources.updateConfiguration(config, baseContext.resources.displayMetrics)
        resources.updateConfiguration(config, baseContext.resources.displayMetrics)
    }
}