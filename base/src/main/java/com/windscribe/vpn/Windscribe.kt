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
import android.util.Log
import androidx.appcompat.app.AppCompatDelegate
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.multidex.MultiDexApplication
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.windscribe.vpn.apppreference.PreferencesHelper
import com.windscribe.vpn.backend.ikev2.CharonVpnServiceWrapper
import com.windscribe.vpn.backend.ikev2.StrongswanCertificateManager.init
import com.windscribe.vpn.backend.utils.ProtocolManager
import com.windscribe.vpn.backend.utils.WindVpnController
import com.windscribe.vpn.billing.AmazonBillingManager
import com.windscribe.vpn.billing.GoogleBillingManager
import com.windscribe.vpn.constants.PreferencesKeyConstants
import com.windscribe.vpn.di.*
import com.windscribe.vpn.localdatabase.WindscribeDatabase
import com.windscribe.vpn.mocklocation.MockLocationManager
import com.windscribe.vpn.services.ping.PingTestService.Companion.startPingTestService
import com.windscribe.vpn.state.AppLifeCycleObserver
import com.windscribe.vpn.state.DeviceStateManager
import com.windscribe.vpn.state.VPNConnectionStateManager
import com.windscribe.vpn.workers.WindScribeWorkManager
import de.blinkt.openvpn.core.PRNGFixes
import io.reactivex.plugins.RxJavaPlugins
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import org.slf4j.LoggerFactory
import org.strongswan.android.logic.StrongSwanApplication
import java.util.*
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
    }

    private val logger = LoggerFactory.getLogger("Windscribe")
    var activeActivity: Activity? = null
    lateinit var applicationInterface: ApplicationInterface
    @Inject
    lateinit var preference: PreferencesHelper
    @Inject
    lateinit var appLifeCycleObserver: AppLifeCycleObserver
    @Inject
    lateinit var deviceStateManager: DeviceStateManager
    @Inject
    lateinit var workManager: WindScribeWorkManager
    @Inject
    lateinit var windscribeDatabase: WindscribeDatabase
    @Inject
    lateinit var vpnConnectionStateManager: VPNConnectionStateManager
    @Inject
    lateinit var mockLocationManager: MockLocationManager
    @Inject
    lateinit var vpnController: WindVpnController
    @Inject
    lateinit var protocolManager: ProtocolManager
    lateinit var applicationComponent: ApplicationComponent
    lateinit var activityComponent: ActivityComponent
    lateinit var serviceComponent: ServiceComponent

    override fun onCreate() {
        if (BuildConfig.DEV) {
            setupStrictMode()
        }
        super.onCreate()
        appContext = this
        registerForegroundActivityObserver()
        AppCompatDelegate.setCompatVectorFromResourcesEnabled(true)
        applicationComponent = getApplicationModuleComponent()
        applicationComponent.inject(this)
        activityComponent = DaggerActivityComponent.builder()
                .applicationComponent(applicationComponent)
                .build()
        serviceComponent = serviceComponent()
        ProcessLifecycleOwner.get().lifecycle.addObserver(appLifeCycleObserver)
        preference.isNewApplicationInstance = true
        WindContextWrapper.setAppLocale(this)
        try {
            PRNGFixes.apply()
        } catch (ignored: Exception) {
        }
        setUpNewInstallation()
        RxJavaPlugins.setErrorHandler { throwable: Throwable -> logger.info(throwable.toString()) }
        initStrongswan()
        if (BuildConfig.APP_ID.isNotEmpty()) {
            FirebaseApp.initializeApp(this, FirebaseOptions.Builder()
                .setGcmSenderId(BuildConfig.GCM_SENDER_ID)
                .setApplicationId(BuildConfig.APP_ID)
                .setProjectId(BuildConfig.PROJECT_ID)
                .setApiKey(BuildConfig.API_KEY)
                .build())
        }
        deviceStateManager.init(this)
        startPingTestService()
        mockLocationManager.init()
        logFileSizes()
    }

    val amazonBillingManager: AmazonBillingManager
        get() = AmazonBillingManager.getInstance(this)

    val billingManager: GoogleBillingManager
        get() = GoogleBillingManager.getInstance(this)

    private fun initStrongswan() {
        StrongSwanApplication.setContext(applicationContext)
        StrongSwanApplication.setService(CharonVpnServiceWrapper::class.java)
        init(this)
    }

    private fun languageToCode(language: String): String {
        val firstIndex =  language.indexOfLast { it == "(".single() }
        return language.substring(firstIndex + 1, language.length - 1)
    }

    private fun logFileSizes(){
        filesDir.listFiles().forEach {
            if(it.isFile){
                logger.debug("Data Directory: ${it.name} ${it.length()/1024}KB")
            }
        }
    }

    fun getAppSupportedSystemLanguage(): String{
        val languageCode = if (VERSION.SDK_INT >= VERSION_CODES.N) {
            resources.configuration.locales.get(0).language
        }else{
            resources.configuration.locale.language
        }
        return appContext.resources.getStringArray(R.array.language).firstOrNull{
            languageCode == languageToCode(it)
        }?: PreferencesKeyConstants.DEFAULT_LANGUAGE
    }

    fun getSavedLocale(): Locale{
        val selectedLanguage = appContext.preference.savedLanguage
        val firstIndex =  selectedLanguage.indexOfLast { it == "(".single() }
        val language = selectedLanguage.substring(firstIndex + 1, selectedLanguage.length - 1)
        return if(language.contains("-")){
            val splits = language.split("-")
            Locale(splits[0], splits[1])
        }else{
            Log.i("sfdsdf", language)
            Locale(language)
        }
    }

    private fun setUpNewInstallation() {
        if (preference.getResponseString(PreferencesKeyConstants.NEW_INSTALLATION) == null) {
            preference.saveResponseStringData(
                PreferencesKeyConstants.NEW_INSTALLATION,
                PreferencesKeyConstants.I_OLD
            )
            // This will be true for legacy app but not beta version users
            if (preference.getResponseString(PreferencesKeyConstants.CONNECTION_STATUS) == null) {
                // Only Recording for legacy to new version
                preference.saveResponseStringData(
                    PreferencesKeyConstants.NEW_INSTALLATION,
                    PreferencesKeyConstants.I_NEW
                )
                preference.removeResponseData(PreferencesKeyConstants.SESSION_HASH)
            }
        }
    }

    override fun onLowMemory() {
        super.onLowMemory()
        applicationScope.cancel("Releasing system resources.")
        applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
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
                    .build()
            )
            StrictMode.setVmPolicy(
                VmPolicy.Builder()
                    .detectLeakedSqlLiteObjects()
                    .detectLeakedClosableObjects()
                    .detectActivityLeaks()
                    .detectFileUriExposure()
                    .detectLeakedRegistrationObjects()
                    .detectContentUriWithoutPermission()
                    .penaltyLog()
                    .build()
            )
        }
    }


    companion object {
        @JvmStatic
        fun getExecutorService(): ExecutorService {
            return Executors.newSingleThreadExecutor()
        }

        /**
         * Provides access to global context.
         */
        @JvmStatic
        lateinit var appContext: Windscribe

        var applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    }

    open fun getApplicationModuleComponent(): ApplicationComponent {
        return DaggerApplicationComponent.builder()
            .applicationModule(ApplicationModule(this)).build()
    }

    private fun serviceComponent(): ServiceComponent {
        return DaggerServiceComponent.builder()
                .serviceModule(ServiceModule())
                .applicationComponent(applicationComponent)
                .build()
    }

    private fun registerForegroundActivityObserver() {
        registerActivityLifecycleCallbacks(object : ActivityLifecycleObserver {
            override fun onActivityResumed(activity: Activity) {
                activeActivity = activity
            }

            override fun onActivityPaused(activity: Activity) {
                activeActivity = null
            }
        })
    }
}
