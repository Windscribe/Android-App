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
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.NavController
import com.windscribe.mobile.R
import com.windscribe.mobile.di.ComposeComponent
import com.windscribe.mobile.di.DaggerComposeComponent
import com.windscribe.mobile.ui.helper.PermissionHelper
import com.windscribe.mobile.ui.nav.NavigationStack
import com.windscribe.mobile.ui.nav.Screen
import com.windscribe.mobile.ui.theme.AndroidTheme
import com.windscribe.mobile.upgradeactivity.UpgradeActivity
import com.windscribe.vpn.Windscribe.Companion.appContext
import com.windscribe.vpn.api.response.PushNotificationAction
import com.windscribe.vpn.apppreference.PreferencesKeyConstants.DARK_THEME
import java.util.Locale
import androidx.core.graphics.toColorInt

class AppStartActivity : AppCompatActivity() {
    lateinit var di: ComposeComponent
    lateinit var viewmodel: AppStartActivityViewModel
    lateinit var navController: NavController
    lateinit var permissionHelper: PermissionHelper
    override fun onCreate(savedInstanceState: Bundle?) {
        val applicationComponent = appContext.applicationComponent
        di = DaggerComposeComponent.builder()
            .applicationComponent(applicationComponent)
            .build()
        viewmodel =
            ViewModelProvider(this, di.getViewModelFactory())[AppStartActivityViewModel::class.java]
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
                Box(modifier = Modifier.fillMaxSize()) {
                    if (appContext.preference.sessionHash != null) {
                        NavigationStack(Screen.Home)
                    } else {
                        NavigationStack(Screen.Start)
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