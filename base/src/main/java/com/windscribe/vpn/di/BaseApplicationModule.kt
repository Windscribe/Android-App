package com.windscribe.vpn.di

import android.app.AlarmManager
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.windscribe.vpn.BuildConfig.DEV
import com.windscribe.vpn.Windscribe
import com.windscribe.vpn.Windscribe.Companion.appContext
import com.windscribe.vpn.api.ApiCallManager
import com.windscribe.vpn.api.IApiCallManager
import com.windscribe.vpn.apppreference.PreferencesHelper
import com.windscribe.vpn.apppreference.SecurePreferences
import com.windscribe.vpn.apppreference.windscribeDataStore
import com.windscribe.vpn.autoconnection.AutoConnectionManager
import com.windscribe.vpn.backend.ProxyDNSManager
import com.windscribe.vpn.backend.TrafficCounter
import com.windscribe.vpn.backend.VpnBackendHolder
import com.windscribe.vpn.backend.ikev2.IKev2VpnBackend
import com.windscribe.vpn.backend.openvpn.OpenVPNBackend
import com.windscribe.vpn.backend.openvpn.ProxyTunnelManager
import com.windscribe.vpn.backend.utils.VPNProfileCreator
import com.windscribe.vpn.backend.utils.WindNotificationBuilder
import com.windscribe.vpn.backend.utils.WindVpnController
import com.windscribe.vpn.backend.wireguard.WgLogger
import com.windscribe.vpn.backend.wireguard.WireguardBackend
import com.windscribe.vpn.backend.wireguard.WireguardContextWrapper
import com.windscribe.vpn.commonutils.WindUtilities
import com.windscribe.vpn.constants.NetworkKeyConstants
import com.windscribe.vpn.constants.NotificationConstants
import com.windscribe.vpn.apppreference.PreferencesKeyConstants
import com.windscribe.vpn.constants.ExtraConstants
import com.windscribe.vpn.decoytraffic.DecoyTrafficController
import com.windscribe.vpn.localdatabase.LocalDatabaseImpl
import com.windscribe.vpn.localdatabase.LocalDbInterface
import com.windscribe.vpn.localdatabase.Migrations
import com.windscribe.vpn.localdatabase.NetworkInfoDao
import com.windscribe.vpn.localdatabase.PopupNotificationDao
import com.windscribe.vpn.localdatabase.ServerStatusDao
import com.windscribe.vpn.localdatabase.UnblockWgDao
import com.windscribe.vpn.localdatabase.UserStatusDao
import com.windscribe.vpn.localdatabase.WindNotificationDao
import com.windscribe.vpn.localdatabase.WindscribeDatabase
import com.windscribe.vpn.mocklocation.MockLocationManager
import com.windscribe.vpn.repository.AdvanceParameterRepository
import com.windscribe.vpn.repository.AdvanceParameterRepositoryImpl
import com.windscribe.vpn.repository.ConnectionDataRepository
import com.windscribe.vpn.repository.EmergencyConnectRepository
import com.windscribe.vpn.repository.EmergencyConnectRepositoryImpl
import com.windscribe.vpn.repository.FavouriteRepository
import com.windscribe.vpn.repository.IpRepository
import com.windscribe.vpn.repository.LatencyRepository
import com.windscribe.vpn.repository.LocationRepository
import com.windscribe.vpn.repository.NotificationRepository
import com.windscribe.vpn.repository.ServerListRepository
import com.windscribe.vpn.repository.StaticIpRepository
import com.windscribe.vpn.repository.UnblockWgParamsRepository
import com.windscribe.vpn.repository.UserRepository
import com.windscribe.vpn.repository.WgConfigRepository
import com.windscribe.vpn.serverlist.dao.CityAndRegionDao
import com.windscribe.vpn.serverlist.dao.CityDao
import com.windscribe.vpn.serverlist.dao.ConfigFileDao
import com.windscribe.vpn.serverlist.dao.FavouriteDao
import com.windscribe.vpn.serverlist.dao.PingTimeDao
import com.windscribe.vpn.serverlist.dao.RegionAndCitiesDao
import com.windscribe.vpn.serverlist.dao.RegionDao
import com.windscribe.vpn.serverlist.dao.StaticRegionDao
import com.windscribe.vpn.services.review.WindscribeReviewManagerImpl
import com.windscribe.vpn.services.sso.GoogleSignInManager
import com.windscribe.vpn.state.AppLifeCycleObserver
import com.windscribe.vpn.state.DeviceStateManager
import com.windscribe.vpn.state.DynamicShortcutManager
import com.windscribe.vpn.state.NetworkInfoManager
import com.windscribe.vpn.state.PreferenceChangeObserver
import com.windscribe.vpn.state.ShortcutStateManager
import com.windscribe.vpn.state.VPNConnectionStateManager
import com.windscribe.vpn.state.WindscribeReviewManager
import com.windscribe.vpn.workers.WindScribeWorkManager
import com.wireguard.android.backend.GoBackend
import com.wsnet.lib.WSNet
import com.wsnet.lib.WSNetServerAPI
import dagger.Lazy
import dagger.Module
import dagger.Provides
import kotlinx.coroutines.CoroutineScope
import okhttp3.ConnectionPool
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import org.slf4j.LoggerFactory
import java.util.UUID
import java.util.concurrent.TimeUnit
import javax.inject.Named
import javax.inject.Singleton
import com.wsnet.lib.WSNetBridgeAPI

@Module
open class BaseApplicationModule {
    private val logger = LoggerFactory.getLogger("wsnet")

    open lateinit var windscribeApp: Windscribe

    @Provides
    @Singleton
    fun provideAlarmManager(): AlarmManager {
        return windscribeApp.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    }

    @Provides
    @Singleton
    fun provideApp(): Windscribe {
        return windscribeApp
    }

    @Provides
    @Singleton
    fun provideCityAndRegionDao(windscribeDatabase: WindscribeDatabase): CityAndRegionDao {
        return windscribeDatabase.cityAndRegionDao()
    }

    @Provides
    @Singleton
    fun provideCityDao(windscribeDatabase: WindscribeDatabase): CityDao {
        return windscribeDatabase.cityDao()
    }

    @Provides
    @Singleton
    fun provideConfigFileDao(windscribeDatabase: WindscribeDatabase): ConfigFileDao {
        return windscribeDatabase.configFileDao()
    }

    @Provides
    @Singleton
    fun provideConnectionDataUpdater(
        preferencesHelper: PreferencesHelper,
        apiCallManager: IApiCallManager,
        autoConnectionManager: Lazy<AutoConnectionManager>
    ): ConnectionDataRepository {
        return ConnectionDataRepository(preferencesHelper, apiCallManager, autoConnectionManager)
    }

    @Provides
    @Singleton
    fun provideLatencyRepository(
        preferencesHelper: PreferencesHelper,
        localDbInterface: LocalDbInterface,
        wsNet: WSNet,
        vpnConnectionStateManager: Lazy<VPNConnectionStateManager>,
        advanceParameterRepository: AdvanceParameterRepository
    ): LatencyRepository {
        return LatencyRepository(
            preferencesHelper,
            localDbInterface,
            wsNet.pingManager(),
            vpnConnectionStateManager,
            advanceParameterRepository
        )
    }

    @Provides
    @Singleton
    fun provideFavouriteRepository(
        scope: CoroutineScope, localDbInterface: LocalDbInterface
    ): FavouriteRepository {
        return FavouriteRepository(scope, localDbInterface)
    }

    @Provides
    @Singleton
    fun provideCoroutineScope(): CoroutineScope {
        return Windscribe.applicationScope
    }

    @Provides
    @Singleton
    fun provideDatabase(): WindscribeDatabase {
        return Room.databaseBuilder(windscribeApp, WindscribeDatabase::class.java, "wind_db")
            .fallbackToDestructiveMigration().addCallback(object : RoomDatabase.Callback() {
                override fun onDestructiveMigration(db: SupportSQLiteDatabase) {
                    logger.debug("No migration found for old database. Reconstructing from scratch.")
                    super.onDestructiveMigration(db)
                }
            }).addMigrations(Migrations.migration_26_27).addMigrations(Migrations.migration_27_28)
            .addMigrations(Migrations.migration_29_31)
            .addMigrations(Migrations.migration_33_34)
            .addMigrations(Migrations.migration_34_35)
            .addMigrations(Migrations.migration_35_36)
            .addMigrations(Migrations.migration_36_37)
            .addMigrations(Migrations.migration_37_38)
            .build()

    }

    @Provides
    @Singleton
    fun provideDeviceStateManager(scope: CoroutineScope): DeviceStateManager {
        return DeviceStateManager(scope)
    }

    @Provides
    @Singleton
    fun provideFavouriteDao(windscribeDatabase: WindscribeDatabase): FavouriteDao {
        return windscribeDatabase.favouriteDao()
    }

    @Provides
    @Singleton
    fun provideGoBackend(): GoBackend {
        return GoBackend(WireguardContextWrapper(windscribeApp.applicationContext))
    }

    @Provides
    @Singleton
    fun provideCtrldManager(
        coroutineScope: CoroutineScope,
        preferencesHelper: PreferencesHelper
    ): ProxyDNSManager {
        return ProxyDNSManager(coroutineScope, preferencesHelper)
    }

    @Provides
    @Singleton
    fun provideLocalDatabaseImpl(
        userStatusDao: UserStatusDao,
        popupNotificationDao: PopupNotificationDao,
        regionDao: RegionDao,
        cityDao: CityDao,
        cityAndRegionDao: CityAndRegionDao,
        configFileDao: ConfigFileDao,
        staticRegionDao: StaticRegionDao,
        pingTimeDao: PingTimeDao,
        favouriteDao: FavouriteDao,
        regionAndCitiesDao: RegionAndCitiesDao,
        networkInfoDao: NetworkInfoDao,
        serverStatusDao: ServerStatusDao,
        preferenceChangeObserver: PreferenceChangeObserver,
        windNotificationDao: WindNotificationDao,
        unblockWgDao: UnblockWgDao
    ): LocalDbInterface {
        return LocalDatabaseImpl(
            userStatusDao,
            popupNotificationDao,
            regionDao,
            cityDao,
            cityAndRegionDao,
            configFileDao,
            staticRegionDao,
            pingTimeDao,
            favouriteDao,
            regionAndCitiesDao,
            networkInfoDao,
            serverStatusDao,
            preferenceChangeObserver,
            windNotificationDao,
            unblockWgDao
        )
    }

    @Provides
    @Singleton
    fun provideNetworkInfoDao(windscribeDatabase: WindscribeDatabase): NetworkInfoDao {
        return windscribeDatabase.networkInfoDao()
    }

    @Provides
    @Singleton
    fun provideNotificationBuilder(@Named("ApplicationContext") appContext: Context): NotificationCompat.Builder {
        return NotificationCompat.Builder(
            appContext, NotificationConstants.NOTIFICATION_CHANNEL_ID
        )
    }

    @Provides
    @Singleton
    fun provideNotificationManager(@Named("ApplicationContext") appContext: Context): NotificationManager {
        return appContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    }

    @Provides
    @Singleton
    fun provideNotificationUpdater(
        scope: CoroutineScope,
        preferencesHelper: PreferencesHelper,
        apiCallManager: IApiCallManager,
        localDbInterface: LocalDbInterface
    ): NotificationRepository {
        return NotificationRepository(scope, preferencesHelper, apiCallManager, localDbInterface)
    }

    @Provides
    @Singleton
    fun providePingTimeDao(windscribeDatabase: WindscribeDatabase): PingTimeDao {
        return windscribeDatabase.pingTimeDao()
    }

    @Provides
    @Singleton
    fun providePopupNotificationDao(windscribeDatabase: WindscribeDatabase): PopupNotificationDao {
        return windscribeDatabase.popupNotificationDao()
    }

    @Provides
    @Singleton
    fun provideUnblockWgDao(windscribeDatabase: WindscribeDatabase): UnblockWgDao {
        return windscribeDatabase.unblockWgDao()
    }

    @Provides
    @Singleton
    fun provideDataStore(): androidx.datastore.core.DataStore<androidx.datastore.preferences.core.Preferences> {
        return windscribeApp.windscribeDataStore
    }

    @Provides
    @Singleton
    fun providePreferenceHelperInterface(
        dataStore: androidx.datastore.core.DataStore<androidx.datastore.preferences.core.Preferences>,
        securePreferences: SecurePreferences,
        scope: CoroutineScope
    ): PreferencesHelper {
        return com.windscribe.vpn.apppreference.DataStorePreferenceHelper(
            dataStore,
            securePreferences,
            scope
        )
    }

    @Provides
    @Singleton
    fun provideRegionAndCitiesDao(windscribeDatabase: WindscribeDatabase): RegionAndCitiesDao {
        return windscribeDatabase.regionAndCitiesDao()
    }

    @Provides
    @Singleton
    fun provideRegionDao(windscribeDatabase: WindscribeDatabase): RegionDao {
        return windscribeDatabase.regionDao()
    }

    @Provides
    @Singleton
    fun provideSelectedLocationUpdater(
        scope: CoroutineScope,
        preferencesHelper: PreferencesHelper,
        localDbInterface: LocalDbInterface,
        userRepository: Lazy<UserRepository>,
        wsNet: WSNet,
        advanceParameterRepository: AdvanceParameterRepository
    ): LocationRepository {
        return LocationRepository(
            scope,
            preferencesHelper,
            localDbInterface,
            userRepository,
            wsNet.pingManager(),
            advanceParameterRepository
        )
    }

    @Provides
    @Singleton
    fun provideServerListUpdater(
        scope: CoroutineScope,
        apiCallManager: IApiCallManager,
        localDbInterface: LocalDbInterface,
        preferenceChangeObserver: PreferenceChangeObserver,
        userRepository: Lazy<UserRepository>,
        appLifeCycleObserver: AppLifeCycleObserver,
        advanceParameterRepository: AdvanceParameterRepository,
        preferencesHelper: PreferencesHelper,
        favouriteRepository: FavouriteRepository
    ): ServerListRepository {
        return ServerListRepository(
            scope,
            apiCallManager,
            localDbInterface,
            preferenceChangeObserver,
            userRepository,
            appLifeCycleObserver,
            advanceParameterRepository,
            preferencesHelper,
            favouriteRepository
        )
    }

    @Provides
    @Singleton
    fun provideServerStatusDao(windscribeDatabase: WindscribeDatabase): ServerStatusDao {
        return windscribeDatabase.serverStatusDao()
    }

    @Provides
    @Singleton
    fun provideStaticListUpdater(
        scope: CoroutineScope,
        preferencesHelper: PreferencesHelper,
        apiCallManager: IApiCallManager,
        localDbInterface: LocalDbInterface
    ): StaticIpRepository {
        return StaticIpRepository(
            scope, preferencesHelper, apiCallManager, localDbInterface
        )
    }

    @Provides
    @Singleton
    fun provideStaticRegionDao(windscribeDatabase: WindscribeDatabase): StaticRegionDao {
        return windscribeDatabase.staticRegionDao()
    }

    @Provides
    @Singleton
    fun provideTrafficCounter(
        coroutineScope: CoroutineScope,
        vpnConnectionStateManager: VPNConnectionStateManager,
        preferencesHelper: PreferencesHelper,
        deviceStateManager: DeviceStateManager
    ): TrafficCounter {
        return TrafficCounter(
            coroutineScope, vpnConnectionStateManager, preferencesHelper, deviceStateManager
        )
    }

    @Provides
    @Singleton
    fun provideUserRepository(
        scope: CoroutineScope,
        autoConnectionManager: AutoConnectionManager,
        vpnController: WindVpnController,
        apiManager: IApiCallManager,
        preferenceHelper: PreferencesHelper,
        localDbInterface: LocalDbInterface,
        workManager: WindScribeWorkManager,
        connectionDataRepository: ConnectionDataRepository,
        serverListRepository: ServerListRepository,
        staticIpRepository: StaticIpRepository,
        googleSignInManager: GoogleSignInManager
    ): UserRepository {
        return UserRepository(
            scope,
            vpnController,
            autoConnectionManager,
            apiManager,
            preferenceHelper,
            localDbInterface,
            workManager,
            connectionDataRepository,
            serverListRepository,
            staticIpRepository,
            googleSignInManager
        )
    }

    @Provides
    @Singleton
    fun provideWgConfigRepository(apiManager: IApiCallManager, preferencesHelper: PreferencesHelper): WgConfigRepository {
        return WgConfigRepository(apiManager, preferencesHelper)
    }

    @Provides
    @Singleton
    fun provideUserStatusDao(windscribeDatabase: WindscribeDatabase): UserStatusDao {
        return windscribeDatabase.userStatusDao()
    }

    @Provides
    @Singleton
    fun provideVPNProfileCreator(
        preferencesHelper: PreferencesHelper,
        wgConfigRepository: WgConfigRepository,
        proxyTunnelManager: ProxyTunnelManager,
        proxyDNSManager: ProxyDNSManager,
        unblockWgParamsRepository: UnblockWgParamsRepository
    ): VPNProfileCreator {
        return VPNProfileCreator(
            preferencesHelper,
            wgConfigRepository,
            proxyTunnelManager,
            proxyDNSManager,
            unblockWgParamsRepository
        )
    }

    @Provides
    @Singleton
    fun provideVpnBackendHolder(
        coroutineScope: CoroutineScope,
        preferenceHelper: PreferencesHelper,
        openVPNBackend: OpenVPNBackend,
        iKev2VpnBackend: IKev2VpnBackend,
        wireguardBackend: WireguardBackend
    ): VpnBackendHolder {
        return VpnBackendHolder(
            coroutineScope, preferenceHelper, iKev2VpnBackend, wireguardBackend, openVPNBackend
        )
    }

    @Provides
    @Singleton
    fun provideWindNotificationBuilder(
        notificationManager: NotificationManager,
        notificationBuilder: NotificationCompat.Builder,
        vpnConnectionStateManager: VPNConnectionStateManager,
        scope: CoroutineScope,
        trafficCounter: TrafficCounter,
        serverListRepository: ServerListRepository,
        preferencesHelper: PreferencesHelper
    ): WindNotificationBuilder {
        return WindNotificationBuilder(
            notificationManager,
            notificationBuilder,
            vpnConnectionStateManager,
            trafficCounter,
            scope,
            serverListRepository,
            preferencesHelper
        )
    }

    @Provides
    @Singleton
    fun provideWindNotificationDao(windscribeDatabase: WindscribeDatabase): WindNotificationDao {
        return windscribeDatabase.windNotificationDao()
    }

    @Provides
    @Singleton
    fun providesApiCallManagerInterface(
        wsNetServerAPI: WSNetServerAPI,
        preferencesHelper: PreferencesHelper,
        bridgeAPI: WSNetBridgeAPI
    ): IApiCallManager {
        return ApiCallManager(
            wsNetServerAPI,
            preferencesHelper,
            bridgeAPI
        )
    }

    @Provides
    @Singleton
    fun providesBridgeApi(wsNet: WSNet): WSNetBridgeAPI {
        return wsNet.bridgeAPI()
    }

    @Provides
    @Singleton
    fun providesIpRepository(
        scope: CoroutineScope,
        preferencesHelper: PreferencesHelper,
        apiCallManager: IApiCallManager,
        vpnConnectionStateManager: VPNConnectionStateManager
    ): IpRepository {
        return IpRepository(scope, preferencesHelper, apiCallManager, vpnConnectionStateManager)
    }

    @Provides
    @Singleton
    fun providesAppLifeCycleObserver(
        workManager: WindScribeWorkManager,
        networkInfoManager: NetworkInfoManager,
        vpnConnectionStateManager: VPNConnectionStateManager,
        proxyDNSManager: ProxyDNSManager,
        wsNet: WSNet,
        deviceStateManager: DeviceStateManager
    ): AppLifeCycleObserver {
        return AppLifeCycleObserver(
            workManager,
            networkInfoManager,
            vpnConnectionStateManager,
            proxyDNSManager,
            wsNet,
            deviceStateManager
        )
    }

    @Provides
    @Singleton
    @Named("ApplicationContext")
    fun providesApplicationContext(): Context {
        return windscribeApp
    }

    @Provides
    fun providesLoggingInterceptor(): HttpLoggingInterceptor {
        return HttpLoggingInterceptor()
    }

    @Provides
    @Singleton
    fun providesMockLocationController(
        coroutineScope: CoroutineScope,
        vpnConnectionStateManager: VPNConnectionStateManager,
        preferencesHelper: PreferencesHelper
    ): MockLocationManager {
        return MockLocationManager(
            windscribeApp, coroutineScope, vpnConnectionStateManager, preferencesHelper
        )
    }

    @Provides
    @Singleton
    fun providesAutoConnectionManager(
        vpnConnectionStateManager: Lazy<VPNConnectionStateManager>,
        vpnController: Lazy<WindVpnController>,
        networkInfoManager: NetworkInfoManager,
        scope: CoroutineScope,
        localDbInterface: LocalDbInterface,
        connectionDataRepository: ConnectionDataRepository,
        apiManager: IApiCallManager,
        preferencesHelper: PreferencesHelper
    ): AutoConnectionManager {
        return AutoConnectionManager(
            scope,
            vpnConnectionStateManager,
            vpnController,
            networkInfoManager,
            connectionDataRepository,
            localDbInterface,
            apiManager,
            preferencesHelper
        )
    }

    @Provides
    @Singleton
    fun providesNetworkInfoManager(
        preferencesHelper: PreferencesHelper,
        localDbInterface: LocalDbInterface,
        deviceStateManager: DeviceStateManager
    ): NetworkInfoManager {
        return NetworkInfoManager(preferencesHelper, localDbInterface, deviceStateManager)
    }

    @Provides
    fun providesOkHttpBuilder(): OkHttpClient.Builder {
        val connectionPool = ConnectionPool(0, 5, TimeUnit.MINUTES)
        val httpLoggingInterceptor = getHttpLoggingInterceptor()
        httpLoggingInterceptor.level = HttpLoggingInterceptor.Level.BODY
        val builder = OkHttpClient.Builder()
        builder.connectTimeout(
            NetworkKeyConstants.NETWORK_REQUEST_CONNECTION_TIMEOUT,
            TimeUnit.SECONDS
        )
        builder.readTimeout(5, TimeUnit.SECONDS)
        builder.writeTimeout(5, TimeUnit.SECONDS)
        builder.callTimeout(15, TimeUnit.SECONDS)
        builder.retryOnConnectionFailure(false)
        builder.connectionPool(connectionPool)
        return builder
    }

    private fun getHttpLoggingInterceptor(): HttpLoggingInterceptor {
        return HttpLoggingInterceptor(object : HttpLoggingInterceptor.Logger {
            override fun log(message: String) {
                logger.info(message)
            }
        })
    }

    @Provides
    @Singleton
    fun providesSecurePreference(): SecurePreferences {
        return SecurePreferences(windscribeApp)
    }

    @Provides
    @Singleton
    fun providesUserDataObserver(): PreferenceChangeObserver {
        return PreferenceChangeObserver()
    }

    @Provides
    @Singleton
    fun providesVPNConnectionStateManager(
        scope: CoroutineScope,
        autoConnectionManager: AutoConnectionManager,
        preferencesHelper: PreferencesHelper,
        userRepository: Lazy<UserRepository>,
        wsNet: Lazy<WSNet>,
        bridgeAPI: WSNetBridgeAPI
    ): VPNConnectionStateManager {
        return VPNConnectionStateManager(
            scope, autoConnectionManager, preferencesHelper, userRepository, wsNet, bridgeAPI
        )
    }

    @Provides
    @Singleton
    fun providesWindScribeWorkManager(
        scope: CoroutineScope,
        vpnConnectionStateManager: VPNConnectionStateManager,
        preferencesHelper: PreferencesHelper
    ): WindScribeWorkManager {
        return WindScribeWorkManager(
            windscribeApp, scope, vpnConnectionStateManager, preferencesHelper
        )
    }

    @Provides
    @Singleton
    fun providesDecoyTrafficController(
        scope: CoroutineScope,
        apiCallManager: IApiCallManager,
        preferencesHelper: PreferencesHelper,
        vpnConnectionStateManager: VPNConnectionStateManager
    ): DecoyTrafficController {
        return DecoyTrafficController(
            scope, apiCallManager, preferencesHelper, vpnConnectionStateManager
        )
    }

    @Provides
    @Singleton
    fun providesWsTunnelManager(
        scope: CoroutineScope, openVPNBackend: OpenVPNBackend
    ): ProxyTunnelManager {
        return ProxyTunnelManager(scope, openVPNBackend)
    }

    @Provides
    @Singleton
    fun providesShortcutStateManager(
        scope: CoroutineScope,
        userRepository: Lazy<UserRepository>,
        networkInfoManager: NetworkInfoManager,
        autoConnectionManager: AutoConnectionManager,
        preferencesHelper: PreferencesHelper,
        vpnController: WindVpnController
    ): ShortcutStateManager {
        return ShortcutStateManager(
            scope,
            userRepository,
            autoConnectionManager,
            networkInfoManager,
            preferencesHelper,
            vpnController
        )
    }

    @Provides
    @Singleton
    fun providesEmergencyConnectRepository(wsNet: WSNet): EmergencyConnectRepository {
        return EmergencyConnectRepositoryImpl(wsNet.emergencyConnect())
    }

    @Provides
    @Singleton
    fun providesAdvanceParameterRepository(
        scope: CoroutineScope,
        preferencesHelper: PreferencesHelper
    ): AdvanceParameterRepository {
        return AdvanceParameterRepositoryImpl(scope, preferencesHelper)
    }

    @Provides
    @Singleton
    fun providesWsNetServerApi(wsNet: WSNet): WSNetServerAPI {
        return wsNet.serverAPI()
    }

    @Provides
    @Singleton
    fun providesWsNet(
        preferencesHelper: PreferencesHelper,
        deviceStateManager: DeviceStateManager,
        advanceParameterRepository: AdvanceParameterRepository
    ): WSNet {
        if (preferencesHelper.deviceUuid == null) {
            preferencesHelper.deviceUuid = UUID.randomUUID().toString()
        }
        val systemLanguageCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            appContext.resources.configuration.locales.get(0).language.substring(0..1)
        } else {
            appContext.resources.configuration.locale.language.substring(0..1)
        }
        WSNet.initialize(
            getPlatformName(),
            getPlatformName(),
            WindUtilities.getVersionName(),
            preferencesHelper.deviceUuid ?: "",
            "2.6.0",
            "4",
            DEV,
            systemLanguageCode,
            preferencesHelper.wsNetSettings,
            { log -> logWsNetMessage(log) },
            true,
            ExtraConstants.AMNEZIA_WG_VERSION
        )
        advanceParameterRepository.getCountryOverride()?.let { override ->
            WSNet.instance().advancedParameters().setCountryOverrideValue(override)
        }
        deviceStateManager.updateNetworkStatus()
        WSNet.instance().advancedParameters().isAPIExtraTLSPadding =
            preferencesHelper.isAntiCensorshipOn
        return WSNet.instance()
    }

    private fun getPlatformName(): String {
        return if (appContext.applicationInterface.isTV) {
            "android-tv"
        } else {
            "android"
        }
    }
    @Provides
    @Singleton
    fun providesDynamicShortcutManager(
        app: Windscribe,
        scope: CoroutineScope,
        vpnConnectionStateManager: VPNConnectionStateManager,
        locationRepository: LocationRepository,
        localDbInterface: LocalDbInterface,
        serverListRepository: ServerListRepository
    ): DynamicShortcutManager {
        return DynamicShortcutManager(
            app,
            scope,
            vpnConnectionStateManager,
            locationRepository,
            localDbInterface,
            serverListRepository
        )
    }

    @Provides
    @Singleton
    fun providesReviewManager(
        app: Windscribe,
        scope: CoroutineScope,
        preferencesHelper: PreferencesHelper,
        userRepository: UserRepository
    ): WindscribeReviewManager {
        return WindscribeReviewManagerImpl(scope, app, preferencesHelper, userRepository)
    }

    @Provides
    @Singleton
    fun provideWgLogger(): WgLogger {
        return WgLogger()
    }

    @Provides
    @Singleton
    fun providePortMapRepository(
        apiCallManager: IApiCallManager,
        preferencesHelper: PreferencesHelper
    ): com.windscribe.vpn.repository.PortMapRepository {
        return com.windscribe.vpn.repository.PortMapRepository(apiCallManager, preferencesHelper)
    }

    @Provides
    @Singleton
    fun provideLogRepository(
        preferencesHelper: PreferencesHelper,
        apiCallManager: IApiCallManager
    ): com.windscribe.vpn.repository.LogRepository {
        return com.windscribe.vpn.repository.LogRepository(preferencesHelper, apiCallManager)
    }

    /**
     * Parses wsnet nested JSON and extracts the actual message.
     * Input: {"tm":"...","lvl":"debug","mod":"wsnet","msg":"{"tm": "...", "lvl": "info", "mod": "wsnet", "msg": "actual message"}"}
     * Output: actual message
     */
    private fun logWsNetMessage(log: String) {
        try {
            if (!log.contains("6464/latency")) {
                val outerMsg = log.substringAfter("\"msg\":\"").substringBeforeLast("\"}")
                val unescaped = outerMsg.replace("\\\"", "\"")
                val actualMsg = unescaped.substringAfter("\"msg\": \"").substringBeforeLast("\"")
                if (actualMsg.isNotEmpty()) {
                    logger.debug(actualMsg)
                }
            }
        } catch (_ : Exception) { }
    }
}