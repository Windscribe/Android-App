/*
 * Copyright (c) 2021 Windscribe Limited.
 */

package com.windscribe.vpn

import android.app.Activity
import android.app.LocaleManager
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.content.res.Resources
import android.os.Build.VERSION
import android.os.Build.VERSION_CODES
import android.os.LocaleList
import android.os.StrictMode
import android.os.StrictMode.ThreadPolicy.Builder
import android.os.StrictMode.VmPolicy
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.multidex.MultiDexApplication
import ch.qos.logback.classic.LoggerContext
import ch.qos.logback.classic.joran.JoranConfigurator
import ch.qos.logback.core.CoreConstants
import com.windscribe.vpn.apppreference.MigrationResult
import com.windscribe.vpn.apppreference.PreferencesHelper
import com.windscribe.vpn.apppreference.PreferencesKeyConstants
import com.windscribe.vpn.apppreference.TrayToDataStoreMigration
import com.windscribe.vpn.apppreference.windscribeDataStore
import com.windscribe.vpn.autoconnection.AutoConnectionManager
import com.windscribe.vpn.autoconnection.AutoConnectionModeCallback
import com.windscribe.vpn.autoconnection.FragmentType
import com.windscribe.vpn.autoconnection.ProtocolInformation
import com.windscribe.vpn.backend.tunnel.startVpnServiceWatching
import com.windscribe.vpn.backend.utils.ForegroundServiceHelper
import com.windscribe.vpn.backend.utils.WindVpnController
import com.windscribe.vpn.commonutils.WindUtilities
import com.windscribe.vpn.constants.ExtraConstants
import com.windscribe.vpn.debug.MainThreadWatchdog
import com.windscribe.vpn.localdatabase.LocalDbInterface
import com.windscribe.vpn.localdatabase.WindscribeDatabase
import com.windscribe.vpn.mocklocation.MockLocationManager
import com.windscribe.vpn.repository.LocationRepository
import com.windscribe.vpn.repository.UserRepository
import com.windscribe.vpn.services.FirebaseManager
import com.windscribe.vpn.state.AppLifeCycleObserver
import com.windscribe.vpn.state.ConnectionPolicy
import com.windscribe.vpn.state.ConnectionPolicyInputs
import com.windscribe.vpn.state.DeviceStateManager
import com.windscribe.vpn.state.DynamicShortcutManager
import com.windscribe.vpn.state.VPNConnectionStateManager
import com.windscribe.vpn.state.WindscribeReviewManager
import com.windscribe.vpn.workers.WindScribeWorkManager
import com.windscribe.vpn.wsnet.WSNetWrapper
import dagger.Lazy
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeoutOrNull
import org.slf4j.LoggerFactory
import java.util.Locale
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import javax.inject.Inject

open class Windscribe : MultiDexApplication() {
    /**
     * Base Module use this interface to access activity
     * from ui modules.
     */
    interface ApplicationInterface {
        val homeIntent: Intent
        val welcomeIntent: Intent
        val splashIntent: Intent
        val upgradeIntent: Intent
        val isTV: Boolean

        fun setTheme()

        fun launchFragment(
            protocolInformationList: List<ProtocolInformation>,
            fragmentType: FragmentType,
            autoConnectionModeCallback: AutoConnectionModeCallback,
            protocolInformation: ProtocolInformation? = null,
        ): Boolean

        fun cancelDialog() {}

        fun showSubscriptionGraceDialog(productId: String) {}

        fun showPinnedNodeErrorDialog(
            title: String,
            description: String,
        ) {}

        /** No-op unless the build activates with a partner licence code. */
        fun showLicenceSuspendedDialog() {}
    }

    private val logger = LoggerFactory.getLogger("windscribe-main")
    var activeActivity: AppCompatActivity? = null
    lateinit var applicationInterface: ApplicationInterface

    @Inject
    lateinit var preference: PreferencesHelper

    @Inject
    lateinit var dataStore: DataStore<Preferences>

    @Inject
    lateinit var appLifeCycleObserver: AppLifeCycleObserver

    @Inject
    lateinit var deviceStateManager: DeviceStateManager

    @Inject
    lateinit var workManager: WindScribeWorkManager

    @Inject
    lateinit var windscribeDatabase: WindscribeDatabase

    @Inject
    lateinit var firebaseManager: FirebaseManager

    @Inject
    lateinit var vpnConnectionStateManager: VPNConnectionStateManager

    @Inject
    lateinit var connectionPolicyInputs: ConnectionPolicyInputs

    @Inject
    lateinit var autoConnectionManager: AutoConnectionManager

    @Inject
    lateinit var mockLocationManager: MockLocationManager

    @Inject
    lateinit var shortcutManager: DynamicShortcutManager

    @Inject
    lateinit var reviewManager: WindscribeReviewManager

    @Inject
    lateinit var vpnController: WindVpnController

    @Inject
    lateinit var wsNetWrapper: WSNetWrapper

    @Inject
    lateinit var localDbInterface: LocalDbInterface

    // Lazy to avoid a dependency cycle, as elsewhere in the core.
    @Inject
    lateinit var userRepository: Lazy<UserRepository>

    @Inject
    lateinit var locationRepository: Lazy<LocationRepository>

    override fun attachBaseContext(base: Context) {
        super.attachBaseContext(base)
        bindLogDirectory(base)
    }

    /** logback's own `${DATA_DIR}` resolves empty this early, so the appender needs a real files dir. */
    private fun bindLogDirectory(base: Context) {
        val loggerContext = LoggerFactory.getILoggerFactory() as? LoggerContext ?: return
        try {
            // Opened before the reset, which would otherwise leave no appenders at all.
            base.assets.open(LOGBACK_CONFIG).use { config ->
                loggerContext.reset()
                loggerContext.putProperty(CoreConstants.DATA_DIR_KEY, base.filesDir.absolutePath)
                JoranConfigurator()
                    .apply { context = loggerContext }
                    .doConfigure(config)
            }
        } catch (e: Exception) {
            Log.e("windscribe-main", "Could not configure logging; the app log will be empty.", e)
        }
    }

    override fun onCreate() {
        if (BuildConfig.DEV) {
            setupStrictMode()
        }
        // DEBUG, not DEV: DEV means staging, and CI sets it on signed release builds for Internal
        // App Sharing, which would ship a main-thread sampler to real testers.
        if (BuildConfig.DEBUG) {
            MainThreadWatchdog().start()
        }
        // Must precede super.onCreate(): Hilt-constructed singletons read appContext from init blocks.
        appContext = this
        super.onCreate()
        // Ensure notification channel exists before any service can start.
        // This must happen before DI so foreground services can post immediately.
        ForegroundServiceHelper
            .ensureNotificationChannel(this)
        registerForegroundActivityObserver()
        AppCompatDelegate.setCompatVectorFromResourcesEnabled(true)
        runTrayMigrationEarly()
        ProcessLifecycleOwner.get().lifecycle.addObserver(appLifeCycleObserver)
        reloadAfterMigration()
        syncAppLocale()

        setUpNewInstallation()
        if (BuildConfig.APP_ID.isNotEmpty()) {
            firebaseManager.initialise()
        }
        deviceStateManager.init(this)

        // Bring the VPN service up in watch mode when the network changes, if needed
        applicationScope.launch {
            var previousNetwork: String? = null
            deviceStateManager.networkDetail.collect { detail ->
                val currentNetwork = detail?.name

                // The service connects itself once up; this only decides whether to start it.
                if (previousNetwork != null && previousNetwork != currentNetwork && currentNetwork != null) {
                    applicationScope.launch {
                        try {
                            val networkInfo = windscribeDatabase.networkInfoDao().getNetwork(currentNetwork)
                            if (ConnectionPolicy.shouldAutoConnect(connectionPolicyInputs.from(networkInfo))) {
                                logger.debug("Network changed to one we should secure - starting the watcher")
                                startVpnServiceWatching()
                            }
                        } catch (e: Exception) {
                            logger.debug("Error checking network for the watcher: ${e.message}")
                        }
                    }
                }

                previousNetwork = currentNetwork
            }
        }

        mockLocationManager.init()
        reviewManager.handleAppReview()

        // Initialize WSNet in background after DI is ready
        applicationScope.launch(Dispatchers.IO) {
            if (preference.deviceUuid == null) {
                preference.deviceUuid =
                    java.util.UUID
                        .randomUUID()
                        .toString()
            }
            val systemLanguageCode =
                resources.configuration.locales
                    .get(0)
                    .language
                    .substring(0..1)
            wsNetWrapper.initialize(
                if (applicationInterface.isTV) "android-tv" else "android",
                WindUtilities.getVersionName(),
                preference.deviceUuid ?: "",
                ExtraConstants.OPENVPN_VERSION,
                "4",
                BuildConfig.DEV,
                systemLanguageCode,
                preference.wsNetSettings,
                true,
                ExtraConstants.AMNEZIA_WG_VERSION,
                BuildConfig.INTEGRATION,
            )
        }
        applicationScope.launch {
            // Cold start: bring the service up if there is anything for it to watch for.
            if (preference.sessionHash != null &&
                ConnectionPolicy.shouldWatch(connectionPolicyInputs.current())
            ) {
                startVpnServiceWatching()
            }
        }
        logger.info("Application startup finished.")
    }

    fun getLanguageCode(selectedLanguage: String): String {
        val appLanguageArray = appContext.resources.getStringArray(R.array.language)
        val appLanguageCodeArray = appContext.resources.getStringArray(R.array.language_codes)
        return if (appLanguageArray.contains(selectedLanguage)) {
            appLanguageCodeArray[appLanguageArray.indexOf(selectedLanguage)]
        } else {
            val firstIndex = selectedLanguage.indexOfLast { it == "(".single() }
            selectedLanguage.substring(firstIndex + 1, selectedLanguage.length - 1)
        }
    }

    /** The device language; from API 33 `resources.configuration` holds the per-app locale instead. */
    private fun systemLocale(): Locale? =
        if (VERSION.SDK_INT >= VERSION_CODES.TIRAMISU) {
            getSystemService(LocaleManager::class.java).systemLocales
        } else {
            Resources.getSystem().configuration.locales
        }.takeIf { !it.isEmpty }?.get(0)

    fun getAppSupportedSystemLanguage(): String = systemLocale()?.let { getLanguageEntry(it) } ?: PreferencesKeyConstants.DEFAULT_LANGUAGE

    fun getAppSupportedCurrentLanguage(): String =
        getLanguageEntry(resources.configuration.locales.get(0)) ?: PreferencesKeyConstants.DEFAULT_LANGUAGE

    val isRegionRestricted: Boolean
        get() {
            // Censored countries: Belarus, Iran, Russia, Turkey, China
            val censoredCountries = setOf("be", "fa", "ru", "tr", "zh")
            return censoredCountries.contains(systemLocale()?.language)
        }

    fun getSavedLocale(): Locale {
        val selectedLanguage = appContext.preference.savedLanguage
        val language = getLanguageCode(selectedLanguage)
        return if (language.contains("-")) {
            val splits = language.split("-")
            Locale
                .Builder()
                .setLanguage(splits[0])
                .setRegion(splits[1])
                .build()
        } else {
            Locale.Builder().setLanguage(language).build()
        }
    }

    /**
     * The `R.array.language` entry for [locale], or null when the app has no such language.
     *
     * `Locale` rewrites Indonesian to its withdrawn `in` code, so the language tag is compared
     * first and the bare language only as a fallback for region-qualified system locales. Separate
     * passes: one pass would match `zh-TW` against the earlier `zh` entry.
     */
    private fun getLanguageEntry(locale: Locale): String? {
        val entries = appContext.resources.getStringArray(R.array.language)
        val tag = locale.toLanguageTag()
        return entries.firstOrNull { getLanguageCode(it).equals(tag, ignoreCase = true) }
            ?: entries.firstOrNull { getLanguageCode(it).equals(locale.language, ignoreCase = true) }
    }

    @Volatile
    private var localizedContext: Context? = null

    /**
     * Resources in the language the user picked, rather than the device language.
     *
     * Below API 33 the platform has no per-app locale and AppCompat's backport reaches activities
     * only, so the Application context goes on resolving against the system language. Everything
     * that reads a string off [appContext] — notifications, dialogs, [ResourceHelper] — has to come
     * through here, or it renders in the device language while the screens render in the chosen one.
     */
    val localized: Context
        get() {
            localizedContext?.let { return it }
            // A service can post a notification before Hilt has supplied `preference`. Falling back
            // to the plain context costs one notification the right language, not a crash.
            val context = runCatching { createLocalizedContext() }.getOrNull() ?: return this
            localizedContext = context
            return context
        }

    private fun createLocalizedContext(): Context =
        createConfigurationContext(
            Configuration(resources.configuration).apply { setLocale(getSavedLocale()) },
        )

    /**
     * The per-app locale the platform holds, empty when it has none.
     *
     * Not [AppCompatDelegate]: from API 33 it reaches the `LocaleManager` through a live activity
     * delegate, and during `onCreate` there is none, so its getter and setter both do nothing at
     * all — silently, with no return value to check.
     */
    private fun platformAppLocales(): LocaleListCompat =
        if (VERSION.SDK_INT >= VERSION_CODES.TIRAMISU) {
            LocaleListCompat.wrap(getSystemService(LocaleManager::class.java).applicationLocales)
        } else {
            AppCompatDelegate.getApplicationLocales()
        }

    private fun setPlatformAppLocale(locale: Locale) {
        if (VERSION.SDK_INT >= VERSION_CODES.TIRAMISU) {
            getSystemService(LocaleManager::class.java).applicationLocales =
                LocaleList.forLanguageTags(locale.toLanguageTag())
        } else {
            AppCompatDelegate.setApplicationLocales(LocaleListCompat.create(locale))
        }
    }

    /**
     * Brings the app language and the platform per-app locale into agreement at startup.
     *
     * On API 33+ the per-app locale outlives the process and the system language screen can change
     * it, so once set it is the source of truth and the in-app picker follows it. Below that it is
     * re-applied from preferences on every start, because nothing persists it.
     */
    fun syncAppLocale() {
        val platformLocale = platformAppLocales().takeIf { !it.isEmpty }?.get(0)
        when {
            // Set, so it wins: on API 33+ the system language screen can change it behind the app.
            platformLocale != null -> getLanguageEntry(platformLocale)?.let { preference.savedLanguage = it }

            // Below API 33 nothing persists it, so the stored choice is re-applied every start.
            VERSION.SDK_INT < VERSION_CODES.TIRAMISU -> setPlatformAppLocale(getSavedLocale())

            // First start on a build that has a per-app locale: hand the stored choice over once.
            // Only a real choice, or defaulting to the device language would pin the app to it.
            !preference.appLocaleMigrated -> {
                val chosen = preference.savedLanguage != getAppSupportedSystemLanguage()
                if (chosen) setPlatformAppLocale(getSavedLocale())
                // Consume the one-shot only once the platform took it, so a failed hand-off retries.
                preference.appLocaleMigrated = !chosen || !platformAppLocales().isEmpty
            }

            // Handed over already and now cleared: the user asked for the device language back.
            else -> preference.savedLanguage = getAppSupportedSystemLanguage()
        }
        lastKnownLocale = resources.configuration.locales.get(0)
        onAppLocaleChanged()
    }

    /** Applies [language], an entry of `R.array.language`, as the app language. */
    fun setAppLanguage(language: String) {
        preference.savedLanguage = language
        setPlatformAppLocale(getSavedLocale())
        onAppLocaleChanged()
    }

    /**
     * Drops the stale [localized] context and re-points JVM-wide formatting at the app language.
     *
     * `SimpleDateFormat` and `String.format` across the app pass `Locale.getDefault()`, which
     * otherwise stays on the device language.
     */
    private fun onAppLocaleChanged() {
        localizedContext = null
        runCatching { Locale.setDefault(getSavedLocale()) }
    }

    @Volatile
    private var lastKnownLocale: Locale? = null

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        // Built from the configuration that just went stale.
        localizedContext = null
        // Guarded on the locale: rotation and theme changes arrive here too, and below API 33
        // re-applying the locale recreates activities.
        val locale = newConfig.locales.get(0)
        if (locale != lastKnownLocale) {
            lastKnownLocale = locale
            syncAppLocale()
        }
    }

    private var notificationIdsToMigrate: List<Int> = emptyList()

    private fun runTrayMigrationEarly() {
        runBlocking(Dispatchers.IO) {
            val migrationCompleted =
                withTimeoutOrNull(3_000) {
                    try {
                        // Ensure DataStore directory exists before accessing
                        val dataStoreDir = java.io.File(appContext.filesDir, "datastore")
                        if (!dataStoreDir.exists()) {
                            logger.debug("Creating DataStore directory: ${dataStoreDir.absolutePath}")
                            val created = dataStoreDir.mkdirs()
                            if (!created && !dataStoreDir.exists()) {
                                logger.error("Failed to create DataStore directory - disk full or permissions issue")
                                return@withTimeoutOrNull false
                            }
                        }

                        val dataStore = appContext.windscribeDataStore
                        val migration = TrayToDataStoreMigration(appContext, dataStore)
                        when (val result = migration.migrate()) {
                            is MigrationResult.Success -> {
                                logger.info(
                                    "Tray migration completed: ${result.migratedCount} items migrated, ${result.errorCount} errors, ${result.unmigratedCount} not migrated",
                                )
                                notificationIdsToMigrate = result.notificationIdsToMarkRead
                                if (notificationIdsToMigrate.isNotEmpty()) {
                                    logger.info("Found ${notificationIdsToMigrate.size} notification read statuses to migrate later")
                                }
                            }

                            is MigrationResult.AlreadyCompleted -> { }

                            is MigrationResult.NoTrayData -> { }

                            is MigrationResult.Error -> {
                                logger.error("Tray migration failed: ${result.message}")
                            }
                        }
                        true
                    } catch (e: java.io.IOException) {
                        logger.error("IOException during DataStore initialization - disk full or permissions issue: ${e.message}", e)
                        false
                    } catch (e: Exception) {
                        logger.error("Unexpected error during Tray migration: ${e.message}", e)
                        false
                    }
                }

            if (migrationCompleted == null) {
                logger.error("Tray migration timed out after 2s - continuing with app initialization to avoid ANR")
            }
        }
    }

    private fun reloadAfterMigration() {
        // Both repositories read their state in constructors that Hilt runs during
        // super.onCreate(), before runTrayMigrationEarly() has committed: UserRepository caches a
        // null user so loggedIn() stays false, and LocationRepository caches selectedCity as -1 so
        // updateLocation() replaces the user's city with a fallback. Re-read both now.
        userRepository.get().reload()
        locationRepository.get().setSelectedCity()
        if (notificationIdsToMigrate.isNotEmpty()) {
            applicationScope.launch {
                try {
                    logger.info("Migrating ${notificationIdsToMigrate.size} notification read statuses to database")
                    var migratedCount = 0
                    for (notificationId in notificationIdsToMigrate) {
                        try {
                            localDbInterface.markNotificationAsRead(notificationId)
                            migratedCount++
                        } catch (e: Exception) {
                            logger.debug("Notification $notificationId not found in database, skipping")
                        }
                    }
                    logger.info("Migrated $migratedCount notification read statuses")
                } catch (e: Exception) {
                    logger.error("Error migrating notification read statuses: ${e.message}")
                }
            }
        }
    }

    private fun setUpNewInstallation() {
        if (preference.newInstallation == null) {
            preference.newInstallation = PreferencesKeyConstants.I_OLD
            // This will be true for legacy app but not beta version users
            if (preference.connectionStatus == null) {
                // Only Recording for legacy to new version
                preference.newInstallation = PreferencesKeyConstants.I_NEW
                preference.sessionHash = null
                // Set migration flag on fresh app launch to force full server list update
                preference.migrationRequired = true
                logger.debug("Fresh app launch - setting migration flag for full server list update")
            }
        }
    }

    /**
     * Strict mode log thread and vm violations in Dev environment.
     * */
    private fun setupStrictMode() {
        if (VERSION.SDK_INT >= VERSION_CODES.O) {
            StrictMode.setThreadPolicy(
                Builder()
                    .detectAll()
                    .permitDiskReads()
                    .permitDiskWrites()
                    .permitUnbufferedIo()
                    .penaltyLog()
                    .build(),
            )
            StrictMode.setVmPolicy(
                VmPolicy
                    .Builder()
                    .detectLeakedSqlLiteObjects()
                    .detectLeakedClosableObjects()
                    .detectActivityLeaks()
                    .detectFileUriExposure()
                    .detectLeakedRegistrationObjects()
                    .detectContentUriWithoutPermission()
                    .penaltyLog()
                    .build(),
            )
        }
    }

    companion object {
        @JvmStatic
        fun getExecutorService(): ExecutorService = Executors.newSingleThreadExecutor()

        /**
         * Provides access to global context.
         */
        @JvmStatic
        lateinit var appContext: Windscribe

        var applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

        /** In assets, and the only configuration logback is given. */
        private const val LOGBACK_CONFIG = "logback.xml"
    }

    override fun onLowMemory() {
        logger.debug("Device is running low on memory.")
        super.onLowMemory()
    }

    override fun onTerminate() {
        logger.debug("App is being terminated.")
        super.onTerminate()
    }

    override fun onTrimMemory(level: Int) {
        if (level > 60) {
            logger.debug("Device is asking for memory trim with level = $level.")
        }
        super.onTrimMemory(level)
    }

    private fun registerForegroundActivityObserver() {
        registerActivityLifecycleCallbacks(
            object : ActivityLifecycleObserver {
                override fun onActivityResumed(activity: Activity) {
                    activeActivity = activity as? AppCompatActivity
                }

                override fun onActivityPaused(activity: Activity) {
                    activeActivity = null
                }
            },
        )
    }
}
