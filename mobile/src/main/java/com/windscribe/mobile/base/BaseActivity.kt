/*
 * Copyright (c) 2021 Windscribe Limited.
 */
package com.windscribe.mobile.base

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
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import butterknife.ButterKnife
import com.windscribe.mobile.R
import com.windscribe.mobile.di.ActivityComponent
import com.windscribe.mobile.di.ActivityModule
import com.windscribe.mobile.windscribe.WindscribeActivity
import com.windscribe.vpn.Windscribe.Companion.appContext
import com.windscribe.vpn.commonutils.WindUtilities
import com.windscribe.vpn.constants.PreferencesKeyConstants
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.util.*
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
        if (boundingRect.isNotEmpty()) {
            val boundingRectHeight = boundingRect[0].height()
            if (this is WindscribeActivity) {
                this.adjustToolBarHeight(boundingRectHeight / 2)
            } else {
                val backButton = findViewById<ConstraintLayout>(R.id.nav_bar)
                backButton?.setPaddingRelative(
                        backButton.paddingStart,
                        backButton.paddingTop + boundingRectHeight / 2, backButton.paddingEnd,
                        backButton.paddingBottom
                )
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


    val isConnectedToNetwork: Boolean
        get() = WindUtilities.isOnline()

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

    protected fun setContentLayout(layoutID: Int, setTheme: Boolean = true) {
        if (setTheme) {
            setTheme(this)
        }
        setLanguage()
        coldLoad.set(true)
        setContentView(layoutID)
        ButterKnife.bind(this)
    }

    fun setLanguage() {
        val newLocale = appContext.getSavedLocale()
        Locale.setDefault(newLocale)
        val config = Configuration()
        config.locale = newLocale
        appContext.resources.updateConfiguration(config, baseContext.resources.displayMetrics)
        resources.updateConfiguration(config, baseContext.resources.displayMetrics)
    }

    fun activityScope(block: suspend CoroutineScope.() -> Unit) {
        lifecycleScope.launch {
            lifecycle.repeatOnLifecycle(Lifecycle.State.CREATED, block)
        }
    }

    companion object {
        const val FILE_PICK_REQUEST = 204
        const val CONNECTED_FLAG_PATH_PICK_REQUEST = 205
        const val DISCONNECTED_FLAG_PATH_PICK_REQUEST = 206
        const val REQUEST_BACKGROUND_PERMISSION = 207
    }
}
