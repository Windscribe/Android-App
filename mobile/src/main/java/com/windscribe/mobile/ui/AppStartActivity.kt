package com.windscribe.mobile.ui

import android.app.TaskStackBuilder
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.graphics.Color
import android.os.Bundle
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.activity.viewModels
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.navigation.NavController
import com.windscribe.mobile.R
import com.windscribe.mobile.ui.helper.PermissionHelper
import com.windscribe.mobile.ui.nav.NavigationStack
import com.windscribe.mobile.ui.nav.Screen
import com.windscribe.mobile.ui.popup.EncryptionWarningDialog
import com.windscribe.mobile.ui.theme.AndroidTheme
import com.windscribe.mobile.upgradeactivity.UpgradeActivity
import com.windscribe.vpn.Windscribe.Companion.appContext
import com.windscribe.vpn.api.response.PushNotificationAction
import com.windscribe.vpn.apppreference.PreferencesKeyConstants.DARK_THEME
import dagger.hilt.android.AndroidEntryPoint
import java.util.Locale
import androidx.core.graphics.toColorInt

@AndroidEntryPoint
class AppStartActivity : AppCompatActivity() {
    private val viewmodelImpl: AppStartActivityViewModelImpl by viewModels()
    val viewmodel: AppStartActivityViewModel get() = viewmodelImpl
    lateinit var navController: NavController
    lateinit var permissionHelper: PermissionHelper
    override fun onCreate(savedInstanceState: Bundle?) {
        permissionHelper = PermissionHelper(this)
        val isDark = appContext.preference.selectedTheme == DARK_THEME
        if (isDark) {
            setTheme(R.style.DarkTheme)
        } else {
            setTheme(R.style.LightTheme)
        }
        setLanguage()
        val splashScreen = installSplashScreen()
        splashScreen.setOnExitAnimationListener { splashScreenView ->
            splashScreenView.remove()
        }
        val navigationBarStyle = if (isDark) {
            SystemBarStyle.dark("#0B0F16".toColorInt())
        } else {
            SystemBarStyle.light("#FFFFFF".toColorInt(), "#0B0F16".toColorInt())
        }
        enableEdgeToEdge(navigationBarStyle = navigationBarStyle)

        super.onCreate(savedInstanceState)
        requestedOrientation = if (isTablet()) {
            ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        } else {
            ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        }
        setContent {
            AndroidTheme(isDark) {
                @OptIn(androidx.compose.ui.ExperimentalComposeUiApi::class)
                Box(modifier = Modifier
                    .fillMaxSize()
                    .semantics { testTagsAsResourceId = true }) {
                    if (appContext.preference.sessionHash != null) {
                        NavigationStack(Screen.Home)
                    } else {
                        NavigationStack(Screen.Start)
                    }
                    val showWarning by viewmodel.showEncryptionWarning.collectAsState()
                    if (showWarning) {
                        EncryptionWarningDialog(
                            onAcknowledge = {
                                viewmodel.acknowledgeEncryptionWarning()
                            }
                        )
                    }
                }
            }
        }
        handleIntent(intent)
    }

    fun Context.isTablet(): Boolean {
        return resources.configuration.screenWidthDp >= 600
    }

    private fun handleIntent(intent: Intent?) {
        val extras = intent?.extras ?: return
        val type = extras.getString("type") ?: return

        // Security check: Verify the intent is from a trusted source
        if (!isIntentSecure(intent)) {
            // Log the security violation attempt (optional)
            android.util.Log.w("AppStartActivity", "Rejected untrusted intent with type: $type")
            return
        }

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

    private fun isIntentSecure(intent: Intent): Boolean {
        // Allow if the intent has our signature-protected permission
        val permissionName = "com.windscribe.mobile.permission.INTERNAL_INTENT"

        // Check if the calling package has the permission (only our app can have signature permission)
        if (callingActivity != null) {
            try {
                val callingPackage = callingActivity!!.packageName
                val pm = packageManager
                val result = pm.checkPermission(permissionName, callingPackage)
                if (result == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                    return true
                }
            } catch (e: Exception) {
                // Permission check failed
            }
        }

        // Additional check: Allow if intent came from a PendingIntent created by our app
        // PendingIntents from notifications will have the creator UID matching our app
        val creatorPackage = intent.getStringExtra("android.intent.extra.REFERRER_NAME")
        if (creatorPackage == packageName) {
            return true
        }

        // Reject all other intents (including external app intents)
        return false
    }

    private fun setLanguage() {
        val newLocale = appContext.getSavedLocale()
        Locale.setDefault(newLocale)
        val config = Configuration(baseContext.resources.configuration)
        config.locale = newLocale
        config.fontScale = 1.0f
        appContext.resources.updateConfiguration(config, baseContext.resources.displayMetrics)
        resources.updateConfiguration(config, baseContext.resources.displayMetrics)
    }
}