package com.windscribe.mobile.ui

import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.os.Bundle
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.core.graphics.toColorInt
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.navigation.NavController
import com.windscribe.mobile.R
import com.windscribe.mobile.ui.helper.PermissionHelper
import com.windscribe.mobile.ui.nav.NavigationStack
import com.windscribe.mobile.ui.nav.Screen
import com.windscribe.mobile.ui.popup.EncryptionWarningDialog
import com.windscribe.mobile.ui.theme.AndroidTheme
import com.windscribe.vpn.Windscribe.Companion.appContext
import com.windscribe.vpn.api.response.PushNotificationAction
import com.windscribe.vpn.apppreference.PreferencesKeyConstants.DARK_THEME
import dagger.hilt.android.AndroidEntryPoint
import java.util.Locale

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
        val splashScreen = installSplashScreen()
        // Keep splash screen visible until content is ready to prevent framework race condition
        var keepSplashScreen = true
        splashScreen.setKeepOnScreenCondition { keepSplashScreen }
        splashScreen.setOnExitAnimationListener { splashScreenView ->
            if (isFinishing || isDestroyed) {
                return@setOnExitAnimationListener
            }
            try {
                splashScreenView.remove()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        // Allow splash screen to be removed after a short delay
        window.decorView.post {
            keepSplashScreen = false
        }
        val navigationBarStyle =
            if (isDark) {
                SystemBarStyle.dark("#0B0F16".toColorInt())
            } else {
                SystemBarStyle.light("#FFFFFF".toColorInt(), "#0B0F16".toColorInt())
            }
        enableEdgeToEdge(navigationBarStyle = navigationBarStyle)

        super.onCreate(savedInstanceState)
        requestedOrientation =
            if (isTablet()) {
                ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
            } else {
                ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
            }
        setContent {
            AndroidTheme(isDark) {
                @OptIn(androidx.compose.ui.ExperimentalComposeUiApi::class)
                Box(
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .semantics { testTagsAsResourceId = true },
                ) {
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
                            },
                        )
                    }
                }
            }
        }
        handleIntent(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    fun Context.isTablet(): Boolean = resources.configuration.screenWidthDp >= 600

    /**
     * Handles intent extras from FCM push notifications and external app launches.
     *
     * Security Note: This activity is exported and can be launched by any app. We intentionally
     * keep this handler simple and permissive because:
     * - AppStartActivity is the launcher activity, so external apps can already launch it
     * - "promo" only deep-links to the upgrade screen (non-sensitive)
     * - "user_expired"/"user_downgraded" trigger server verification before any action
     *
     * SessionWorker validates account status with the server and only disconnects the VPN
     * if the server confirms the account is actually expired/banned. This prevents malicious
     * apps from forcing VPN disconnects.
     */
    private fun handleIntent(intent: Intent?) {
        val extras = intent?.extras ?: return
        val type = extras.getString("type") ?: return
        when (type) {
            "promo" -> {
                val pcpid = extras.getString("pcpid")
                val promoCode = extras.getString("promo_code")
                if (pcpid != null && promoCode != null) {
                    appContext.appLifeCycleObserver.pushNotificationAction =
                        PushNotificationAction(pcpid, promoCode, type)
                    viewmodel.requestDeepLink(Screen.Upgrade.route)
                }
            }
            "user_expired", "user_downgraded" -> {
                appContext.workManager.updateSession()
            }
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
