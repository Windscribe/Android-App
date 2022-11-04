/*
 * Copyright (c) 2021 Windscribe Limited.
 */
package com.windscribe.mobile.base

import android.Manifest
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.graphics.PixelFormat
import android.graphics.Rect
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.transition.Slide
import android.view.Gravity
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.GravityCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import butterknife.ButterKnife
import com.windscribe.mobile.R
import com.windscribe.mobile.alert.DisclaimerAlertListener
import com.windscribe.mobile.alert.LocationPermissionFragment
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

abstract class BaseActivity : AppCompatActivity(), DisclaimerAlertListener {
    val coldLoad = AtomicBoolean()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setWindow()
        window.setFormat(PixelFormat.RGBA_8888)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            permissionGranted(requestCode)
        } else {
            permissionDenied(requestCode)
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    fun checkLocationPermission(disclaimerContainer: Int, requestCode: Int) {
        if (isLocationPermissionAvailable) {
            permissionGranted(requestCode)
        } else {
            if (shouldShowLocationPermissionRationale()) {
                showLocationRational(requestCode)
            } else {
                showLocationDisclaimer(disclaimerContainer, requestCode)
            }
        }
    }

    private val isLocationPermissionAvailable: Boolean
        get() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            (
                    ContextCompat
                            .checkSelfPermission(
                                    appContext,
                                    Manifest.permission.ACCESS_FINE_LOCATION
                            )
                            == PackageManager.PERMISSION_GRANTED
                    )
        } else {
            true
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

    override fun onRequestCancel() {
        supportFragmentManager.popBackStack()
    }

    override fun onRequestPermission(requestCode: Int) {
        supportFragmentManager.popBackStack()
        ActivityCompat.requestPermissions(
                this,
                arrayOf(
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                ),
                requestCode
        )
    }

    open fun permissionDenied(requestCode: Int) {}
    open fun permissionGranted(requestCode: Int) {}

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

    private fun shouldShowLocationPermissionRationale(): Boolean {
        return ActivityCompat
                .shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_FINE_LOCATION)
    }

    private fun showLocationDisclaimer(disclaimerContainer: Int, requestCode: Int) {
        val fragment: Fragment = LocationPermissionFragment.newInstance(requestCode)
        val direction = GravityCompat
                .getAbsoluteGravity(Gravity.BOTTOM, resources.configuration.layoutDirection)
        fragment.enterTransition = Slide(direction).addTarget(disclaimerContainer)
        supportFragmentManager.beginTransaction()
                .replace(android.R.id.content, fragment)
                .addToBackStack(LocationPermissionFragment::class.java.name)
                .commit()
    }

    open fun showLocationRational(requestCode: Int) {
        val alertDialog = AlertDialog.Builder(this, R.style.alert_dialog_theme)
                .setCancelable(true)
                .setMessage(
                        "Location permission is required to get network SSID.Go to Settings > Apps > Windscribe > Location permission."
                )
                .setOnCancelListener { permissionDenied(requestCode) }
                .setOnDismissListener { permissionDenied(requestCode) }
                .setPositiveButton("Ok") { _: DialogInterface?, _: Int ->
                    permissionDenied(requestCode)
                }
                .create()
        alertDialog.show()
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

    protected fun setLanguage() {
        val newLocale = appContext.getSavedLocale()
        Locale.setDefault(newLocale)
        val config = Configuration()
        config.locale = newLocale
        appContext.resources
                .updateConfiguration(config, baseContext.resources.displayMetrics)
        resources.updateConfiguration(config, baseContext.resources.displayMetrics)
    }

    fun activityScope(block: suspend CoroutineScope.() -> Unit) {
        lifecycleScope.launch {
            lifecycle.repeatOnLifecycle(Lifecycle.State.CREATED, block)
        }
    }

    companion object {
        const val REQUEST_LOCATION_PERMISSION = 201
        const val REQUEST_LOCATION_PERMISSION_FOR_PREFERRED_NETWORK = 202
        const val NETWORK_NAME_PERMISSION = 203
        const val FILE_PICK_REQUEST = 204
        const val CONNECTED_FLAG_PATH_PICK_REQUEST = 205
        const val DISCONNECTED_FLAG_PATH_PICK_REQUEST = 206
        const val REQUEST_BACKGROUND_PERMISSION = 207
    }
}
