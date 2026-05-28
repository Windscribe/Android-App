/*
 * Copyright (c) 2021 Windscribe Limited.
 */

package com.windscribe.vpn

import android.app.Activity
import android.content.Intent
import android.os.Build.VERSION
import android.os.Build.VERSION_CODES
import android.os.StrictMode
import android.os.StrictMode.ThreadPolicy.Builder
import android.os.StrictMode.VmPolicy
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.multidex.MultiDexApplication
import com.windscribe.vpn.apppreference.MigrationResult
import com.windscribe.vpn.apppreference.PreferencesHelper
import com.windscribe.vpn.apppreference.PreferencesKeyConstants
import com.windscribe.vpn.apppreference.TrayToDataStoreMigration
import com.windscribe.vpn.apppreference.windscribeDataStore
import com.windscribe.vpn.autoconnection.AutoConnectionModeCallback
import com.windscribe.vpn.autoconnection.FragmentType
import com.windscribe.vpn.autoconnection.ProtocolInformation
import com.windscribe.vpn.backend.utils.WindVpnController
import com.windscribe.vpn.cache.AppIconCache
import com.windscribe.vpn.commonutils.WindUtilities
import com.windscribe.vpn.constants.ExtraConstants
import com.windscribe.vpn.debug.MainThreadWatchdog
import com.windscribe.vpn.localdatabase.LocalDbInterface
import com.windscribe.vpn.localdatabase.WindscribeDatabase
import com.windscribe.vpn.mocklocation.MockLocationManager
import com.windscribe.vpn.services.FirebaseManager
import com.windscribe.vpn.services.canAccessNetworkName
import com.windscribe.vpn.services.startAutoConnectService
import com.windscribe.vpn.state.AppLifeCycleObserver
import com.windscribe.vpn.state.DeviceStateManager
import com.windscribe.vpn.state.DynamicShortcutManager
import com.windscribe.vpn.state.VPNConnectionStateManager
import com.windscribe.vpn.state.WindscribeReviewManager
import com.windscribe.vpn.workers.WindScribeWorkManager
import com.windscribe.vpn.wsnet.WSNetWrapper
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

        fun showPinnedNodeErrorDialog(
            title: String,
            description: String,
        ) {}
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
    lateinit var mockLocationManager: MockLocationManager

    @Inject
    lateinit var shortcutManager: DynamicShortcutManager

    @Inject
    lateinit var reviewManager: WindscribeReviewManager

    @Inject
    lateinit var vpnController: WindVpnController

    @Inject
    lateinit var appIconCache: AppIconCache

    @Inject
    lateinit var wsNetWrapper: WSNetWrapper

    @Inject
    lateinit var localDbInterface: LocalDbInterface

    override fun onCreate() {
        if (BuildConfig.DEV) {
            setupStrictMode()
            MainThreadWatchdog().start()
        }
        // Must precede super.onCreate(): Hilt-constructed singletons read appContext from init blocks.
        appContext = this
        super.onCreate()
        // Ensure notification channel exists before any service can start.
        // This must happen before DI so foreground services can post immediately.
        com.windscribe.vpn.backend.utils.ForegroundServiceHelper
            .ensureNotificationChannel(this)
        registerForegroundActivityObserver()
        AppCompatDelegate.setCompatVectorFromResourcesEnabled(true)
        runTrayMigrationEarly()
        ProcessLifecycleOwner.get().lifecycle.addObserver(appLifeCycleObserver)
        reloadAfterMigration()

        preference.isNewApplicationInstance = true
        setUpNewInstallation()
        if (BuildConfig.APP_ID.isNotEmpty()) {
            firebaseManager.initialise()
        }
        deviceStateManager.init(this)

        // Start AutoConnectService when network changes if needed
        applicationScope.launch {
            var previousNetwork: String? = null
            deviceStateManager.networkDetail.collect { detail ->
                val currentNetwork = detail?.name

                // Start AutoConnectService if needed when network changes
                if (previousNetwork != null && previousNetwork != currentNetwork && currentNetwork != null) {
                    applicationScope.launch {
                        try {
                            val networkInfo = windscribeDatabase.networkInfoDao().getNetwork(currentNetwork)
                            val isVpnActive = vpnConnectionStateManager.isVPNActive()

                            // Start service if: auto-secure ON, VPN not active, and autoConnect enabled
                            if (networkInfo?.isAutoSecureOn == true &&
                                !isVpnActive &&
                                preference.autoConnect &&
                                canAccessNetworkName()
                            ) {
                                logger.debug("Network changed to auto-secure ON network - starting AutoConnectService")
                                startAutoConnectService()
                            }
                        } catch (e: Exception) {
                            logger.debug("Error checking network for AutoConnectService: ${e.message}")
                        }
                    }
                }

                previousNetwork = currentNetwork
            }
        }

        mockLocationManager.init()
        reviewManager.handleAppReview()
        appIconCache.preloadIcons(applicationScope)

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
                "2.6.0",
                "4",
                BuildConfig.DEV,
                systemLanguageCode,
                preference.wsNetSettings,
                true,
                ExtraConstants.AMNEZIA_WG_VERSION,
            )
        }
        applicationScope.launch {
            if (preference.sessionHash != null && preference.autoConnect && canAccessNetworkName()) {
                startAutoConnectService()
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

    fun getAppSupportedSystemLanguage(): String {
        val systemLanguageCode =
            resources.configuration.locales
                .get(0)
                .language
        return appContext.resources.getStringArray(R.array.language).firstOrNull {
            systemLanguageCode == getLanguageCode(it)
        } ?: PreferencesKeyConstants.DEFAULT_LANGUAGE
    }

    val isRegionRestricted: Boolean
        get() {
            val systemLanguageCode =
                resources.configuration.locales
                    .get(0)
                    .language
            // Censored countries: Belarus, Iran, Russia, Turkey, China
            val censoredCountries = setOf("be", "fa", "ru", "tr", "zh")
            return censoredCountries.contains(systemLanguageCode)
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

                            is MigrationResult.NoTrayData -> {
                                logger.info("No Tray data to migrate (fresh install or already migrated)")
                            }

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
