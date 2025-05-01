package com.windscribe.mobile.view

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.core.app.ActivityCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.windscribe.mobile.R
import com.windscribe.mobile.di.ComposeComponent
import com.windscribe.mobile.di.DaggerComposeComponent
import com.windscribe.mobile.networksecurity.networkdetails.NetworkDetailsActivity
import com.windscribe.mobile.upgradeactivity.UpgradeActivity
import com.windscribe.mobile.view.screen.Screen
import com.windscribe.mobile.view.theme.AndroidTheme
import com.windscribe.vpn.Windscribe.Companion.appContext
import com.windscribe.vpn.api.response.PushNotificationAction
import com.windscribe.vpn.constants.PreferencesKeyConstants.DARK_THEME

class AppStartActivity : AppCompatActivity() {
    var dialog: MutableState<(@Composable () -> Unit)?> = mutableStateOf(null)
    lateinit var di: ComposeComponent

    override fun onCreate(savedInstanceState: Bundle?) {
        val applicationComponent = appContext.applicationComponent
        di = DaggerComposeComponent.builder()
            .applicationComponent(applicationComponent)
            .build()
        val isDark = appContext.preference.selectedTheme == DARK_THEME
        if (isDark) {
            setTheme(R.style.DarkTheme)
        } else {
            setTheme(R.style.LightTheme)
        }

        val splashScreen = installSplashScreen()
        splashScreen.setOnExitAnimationListener { splashScreenView ->
            splashScreenView.remove()
        }

        enableEdgeToEdge()

        super.onCreate(savedInstanceState)
        setContent {
            AndroidTheme(isDark) {
                Box(modifier = Modifier.fillMaxSize()) {
                    if (appContext.preference.sessionHash != null) {
                        NavigationStack(Screen.Home)
                    } else {
                        NavigationStack(Screen.Start)
                    }
                    dialog.value?.let { content ->
                        // Dim background and show dialog content
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.Black.copy(alpha = 0.5f))
                        ) {
                            content()
                        }
                    }
                }
            }
        }
        handleIntent(intent)
    }

    fun isGranted(permission: String): Boolean {
        return ActivityCompat.checkSelfPermission(
            this,
            permission
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED
    }

    fun launchNetworkDetailsActivity(networkName: String?) {
        if (networkName != null) {
            startActivity(NetworkDetailsActivity.getStartIntent(this, networkName))
        } else {
            showToast("Network SSID is not available")
        }
    }

    fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    fun shouldShowRationale(permission: String): Boolean {
        return ActivityCompat.shouldShowRequestPermissionRationale(this, permission)
    }

    fun presentDialog(content: @Composable () -> Unit) {
        dialog.value = content
    }

    fun cancelDialog() {
        dialog.value = null
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

    private fun handleIntent(intent: Intent?) {
        val extras = intent?.extras ?: return
        val type = extras.getString("type") ?: return

        when (type) {
            "promo" -> {
                val pcpid = extras.getString("pcpid")
                val promoCode = extras.getString("promo_code")

                if (pcpid != null && promoCode != null) {
                    appContext.appLifeCycleObserver.pushNotificationAction = PushNotificationAction(
                        pcpid,
                        promoCode,
                        type
                    )
                    startActivity(UpgradeActivity.getStartIntent(this))
                }
            }

            "user_expired" -> {
                if (appContext.vpnConnectionStateManager.isVPNConnected()) {
                    appContext.vpnController.disconnectAsync()
                }
                appContext.workManager.updateSession()
            }

            "user_downgraded" -> {
                appContext.workManager.updateSession()
            }
        }
    }
}