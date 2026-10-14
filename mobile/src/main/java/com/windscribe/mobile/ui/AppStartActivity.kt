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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.core.graphics.toColorInt
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.navigation.NavController
import com.windscribe.mobile.R
import com.windscribe.mobile.ui.common.openUrl
import com.windscribe.mobile.ui.helper.PermissionHelper
import com.windscribe.mobile.ui.nav.NavigationStack
import com.windscribe.mobile.ui.nav.Screen
import com.windscribe.mobile.ui.popup.EncryptionWarningDialog
import com.windscribe.mobile.ui.popup.SubscriptionGraceDialog
import com.windscribe.mobile.ui.theme.AndroidTheme
import com.windscribe.vpn.Windscribe.Companion.appContext
import com.windscribe.vpn.api.response.PushNotificationAction
import com.windscribe.vpn.apppreference.PreferencesKeyConstants.DARK_THEME
import com.windscribe.vpn.billing.GooglePlaySubscriptionUrl
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class AppStartActivity : AppCompatActivity() {
    private val viewmodelImpl: AppStartActivityViewModelImpl by viewModels()
    val viewmodel: AppStartActivityViewModel get() = viewmodelImpl
    lateinit var navController: NavController
    lateinit var permissionHelper: PermissionHelper
    private var subscriptionGraceProductId by mutableStateOf<String?>(null)

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
                    subscriptionGraceProductId?.let { productId ->
                        SubscriptionGraceDialog(
                            onConfirm = {
                                subscriptionGraceProductId = null
                                GooglePlaySubscriptionUrl.build(packageName, productId)?.let { openUrl(it) }
                            },
                            onDismiss = {
                                subscriptionGraceProductId = null
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

    fun showSubscriptionGraceDialog(productId: String) {
        if (GooglePlaySubscriptionUrl.build(packageName, productId) == null) return
        runOnUiThread {
            if (!isFinishing && !isDestroyed) {
                subscriptionGraceProductId = productId
            }
        }
    }

    /**
     * Handles intent extras from FCM push notifications and external app launches.
     *
     * Security note: this activity is exported - as the launcher and via the icon activity-aliases
     * - so ANY installed app can invoke it with arbitrary extras. Everything read below is
     * untrusted input, not trusted FCM input. Treat it that way.
     *
     * The extras cannot be replaced by reading appLifeCycleObserver instead. When the app is not
     * running, Firebase renders the promo notification itself,
     * WindscribeCloudMessaging.onMessageReceived is never called, and the payload reaches us only
     * as Intent extras when the user taps the notification. That is the common case for a promo
     * push, so dropping the extras would break promo notifications for most users.
     *
     * What keeps forged extras harmless, per branch:
     * - "promo": pcpid/promo_code are only ever handed to the server as parameters; nothing on
     *   device decides anything based on them. getBillingPlans() returns the standard plans when
     *   the promo is not valid for this user, and postPromoPaymentConfirmation() is a best-effort
     *   call made only after verifyPurchaseReceipt() has already succeeded, so a forged pcpid
     *   cannot change entitlement, pricing or payment.
     *   getNotifications(pcpid) is server-authoritative in the same way: the newsfeed content it
     *   returns is decided server-side for this account, so a forged pcpid does not let a caller
     *   choose what the user is shown.
     * - GooglePlaySubscriptionUrl.NOTIFICATION_TYPE: builds a fixed Play Store URL from an encoded
     *   product ID, and build() returns null for anything it does not recognise.
     * - "user_expired"/"user_downgraded": only calls updateSession(). SessionWorker asks the server
     *   for the real account status and disconnects only if the server confirms the account is
     *   expired or banned, so a malicious app cannot force a VPN disconnect.
     *
     * Invariant for anything added here: promo data must stay a parameter the server validates.
     * Do not let it gate a client-side entitlement, trust or security decision - the server is the
     * only validation authority for it. This handler is safe because of that invariant, NOT because
     * the values are harmless.
     *
     * The promo deliberately persists for the lifetime of the process (AppLifeCycleObserver keeps
     * it in memory and nothing writes it to storage), so back-navigation can return the user to
     * the promo, and it is gone on a fresh launch. That is intended, not a leak.
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
            GooglePlaySubscriptionUrl.NOTIFICATION_TYPE -> {
                val productId = extras.getString(GooglePlaySubscriptionUrl.PRODUCT_ID_EXTRA).orEmpty()
                GooglePlaySubscriptionUrl.build(packageName, productId)?.let { openUrl(it) }
            }
            "user_expired", "user_downgraded" -> {
                appContext.workManager.updateSession()
            }
        }
    }

    /** Only the font scale is overridden here; AppCompat applies the app locale itself. */
    override fun attachBaseContext(newBase: Context) {
        val config = Configuration(newBase.resources.configuration)
        config.fontScale = 1.0f
        super.attachBaseContext(newBase.createConfigurationContext(config))
    }
}
