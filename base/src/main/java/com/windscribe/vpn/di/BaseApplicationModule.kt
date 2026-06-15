package com.windscribe.vpn.di

import android.app.AlarmManager
import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.windscribe.vpn.Windscribe
import com.windscribe.vpn.Windscribe.Companion.appContext
import com.windscribe.vpn.api.ApiCallManager
import com.windscribe.vpn.api.IApiCallManager
import com.windscribe.vpn.apppreference.PreferencesHelper
import com.windscribe.vpn.apppreference.SecurePreferences
import com.windscribe.vpn.apppreference.windscribeDataStore
import com.windscribe.vpn.autoconnection.AutoConnectionManager
import com.windscribe.vpn.backend.PlayIntegrityManager
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
import com.windscribe.vpn.billing.PurchaseManager
import com.windscribe.vpn.constants.NetworkKeyConstants
import com.windscribe.vpn.constants.NotificationConstants
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
import com.windscribe.vpn.repository.CheckUpdateRepository
import com.windscribe.vpn.repository.ConnectionDataRepository
import com.windscribe.vpn.repository.EmergencyConnectRepository
import com.windscribe.vpn.repository.EmergencyConnectRepositoryImpl
import com.windscribe.vpn.repository.FavouriteRepository
import com.windscribe.vpn.repository.IpRepository
import com.windscribe.vpn.repository.LatencyRepository
import com.windscribe.vpn.repository.LocationRepository
import com.windscribe.vpn.repository.LogRepository
import com.windscribe.vpn.repository.NotificationRepository
import com.windscribe.vpn.repository.ServerListRepository
import com.windscribe.vpn.repository.StaticIpRepository
import com.windscribe.vpn.repository.UnblockWgParamsRepository
import com.windscribe.vpn.repository.UserRepository
import com.windscribe.vpn.repository.WgConfigRepository
import com.windscribe.vpn.serverlist.dao.ConfigFileDao
import com.windscribe.vpn.serverlist.dao.DatacenterAndLocationDao
import com.windscribe.vpn.serverlist.dao.DatacenterDao
import com.windscribe.vpn.serverlist.dao.FavouriteDao
import com.windscribe.vpn.serverlist.dao.LocationAndDatacentersDao
import com.windscribe.vpn.serverlist.dao.LocationDao
import com.windscribe.vpn.serverlist.dao.PingTimeDao
import com.windscribe.vpn.serverlist.dao.ServerDao
import com.windscribe.vpn.serverlist.dao.StaticRegionDao
import com.windscribe.vpn.services.review.WindscribeReviewManagerImpl
import com.windscribe.vpn.services.sso.GoogleSignInManager
import com.windscribe.vpn.state.AppLifeCycleObserver
import com.windscribe.vpn.state.DeviceStateManager
import com.windscribe.vpn.state.DynamicShortcutManager
import com.windscribe.vpn.state.NetworkInfoManager
import com.windscribe.vpn.state.ShortcutStateManager
import com.windscribe.vpn.state.VPNConnectionStateManager
import com.windscribe.vpn.state.WindscribeReviewManager
import com.windscribe.vpn.workers.WindScribeWorkManager
import com.windscribe.vpn.wsnet.WSNetWrapper
import com.wireguard.android.backend.GoBackend
import dagger.Lazy
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import okhttp3.ConnectionPool
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import org.slf4j.LoggerFactory
import java.util.concurrent.TimeUnit
import javax.inject.Named
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
open class BaseApplicationModule {
    private val logger = LoggerFactory.getLogger("wsnet")

    // Migration note: `windscribeApp` field removed. Each @Provides that needs
    // the application instance now takes it as a parameter. Hilt auto-binds
    // `Application`; the legacy Dagger graph gets it via `provideApplication`
    // in the subclass `ApplicationModule` (google / fdroid flavors).

    @Provides
    @Singleton
    fun provideAlarmManager(app: Windscribe): AlarmManager = app.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    @Provides
    @Singleton
    fun provideDatacenterAndLocationDao(windscribeDatabase: WindscribeDatabase): DatacenterAndLocationDao =
        windscribeDatabase.datacenterAndLocationDao()

    @Provides
    @Singleton
    fun provideDatacenterDao(windscribeDatabase: WindscribeDatabase): DatacenterDao = windscribeDatabase.cityDao()

    @Provides
    @Singleton
    fun provideConfigFileDao(windscribeDatabase: WindscribeDatabase): ConfigFileDao = windscribeDatabase.configFileDao()

    @Provides
    @Singleton
    fun provideConnectionDataUpdater(
        preferencesHelper: PreferencesHelper,
        apiCallManager: IApiCallManager,
        autoConnectionManager: Lazy<AutoConnectionManager>,
    ): ConnectionDataRepository = ConnectionDataRepository(preferencesHelper, apiCallManager, autoConnectionManager)

    @Provides
    @Singleton
    fun provideLatencyRepository(
        preferencesHelper: PreferencesHelper,
        localDbInterface: LocalDbInterface,
        vpnConnectionStateManager: Lazy<VPNConnectionStateManager>,
        pinger: com.windscribe.vpn.services.ping.Pinger,
        deviceStateManager: DeviceStateManager,
    ): LatencyRepository =
        LatencyRepository(
            preferencesHelper,
            localDbInterface,
            vpnConnectionStateManager,
            pinger,
            deviceStateManager.isOnline,
        )

    @Provides
    @Singleton
    fun provideFavouriteRepository(
        scope: CoroutineScope,
        localDbInterface: LocalDbInterface,
    ): FavouriteRepository = FavouriteRepository(scope, localDbInterface)

    @Provides
    @Singleton
    fun provideCoroutineScope(): CoroutineScope = Windscribe.applicationScope

    @Provides
    @Singleton
    fun provideDatabase(app: Windscribe): WindscribeDatabase =
        Room
            .databaseBuilder(app, WindscribeDatabase::class.java, "wind_db")
            .fallbackToDestructiveMigration(dropAllTables = true)
            .addCallback(
                object : RoomDatabase.Callback() {
                    override fun onDestructiveMigration(db: SupportSQLiteDatabase) {
                        logger.debug("No migration found for old database. Reconstructing from scratch.")
                        super.onDestructiveMigration(db)
                    }
                },
            ).addMigrations(Migrations.migration_26_27)
            .addMigrations(Migrations.migration_27_28)
            .addMigrations(Migrations.migration_29_31)
            .addMigrations(Migrations.migration_33_34)
            .addMigrations(Migrations.migration_34_35)
            .addMigrations(Migrations.migration_35_36)
            .addMigrations(Migrations.migration_36_37)
            .addMigrations(Migrations.migration_37_38)
            .addMigrations(Migrations.migration_38_39)
            .addMigrations(Migrations.migration_39_40)
            .addMigrations(Migrations.migration_40_41)
            .build()

    @Provides
    @Singleton
    fun provideDeviceStateManager(
        scope: CoroutineScope,
        wsNetWrapper: WSNetWrapper,
    ): DeviceStateManager = DeviceStateManager(scope, wsNetWrapper)

    @Provides
    @Singleton
    fun provideFavouriteDao(windscribeDatabase: WindscribeDatabase): FavouriteDao = windscribeDatabase.favouriteDao()

    @Provides
    @Singleton
    fun provideGoBackend(app: Windscribe): GoBackend = GoBackend(WireguardContextWrapper(app.applicationContext))

    @Provides
    @Singleton
    fun provideCtrldManager(
        coroutineScope: CoroutineScope,
        preferencesHelper: PreferencesHelper,
    ): ProxyDNSManager = ProxyDNSManager(coroutineScope, preferencesHelper)

    @Provides
    @Singleton
    fun provideLocalDatabaseImpl(
        userStatusDao: UserStatusDao,
        popupNotificationDao: PopupNotificationDao,
        locationDao: LocationDao,
        serverDao: ServerDao,
        datacenterDao: DatacenterDao,
        datacenterAndLocationDao: DatacenterAndLocationDao,
        configFileDao: ConfigFileDao,
        staticRegionDao: StaticRegionDao,
        pingTimeDao: PingTimeDao,
        favouriteDao: FavouriteDao,
        locationAndDatacentersDao: LocationAndDatacentersDao,
        networkInfoDao: NetworkInfoDao,
        serverStatusDao: ServerStatusDao,
        windNotificationDao: WindNotificationDao,
        unblockWgDao: UnblockWgDao,
    ): LocalDbInterface =
        LocalDatabaseImpl(
            userStatusDao,
            popupNotificationDao,
            locationDao,
            serverDao,
            datacenterDao,
            datacenterAndLocationDao,
            configFileDao,
            staticRegionDao,
            pingTimeDao,
            favouriteDao,
            locationAndDatacentersDao,
            networkInfoDao,
            serverStatusDao,
            windNotificationDao,
            unblockWgDao,
        )

    @Provides
    @Singleton
    fun provideNetworkInfoDao(windscribeDatabase: WindscribeDatabase): NetworkInfoDao = windscribeDatabase.networkInfoDao()

    @Provides
    @Singleton
    fun provideNotificationBuilder(
        @Named("ApplicationContext") appContext: Context,
    ): NotificationCompat.Builder =
        NotificationCompat.Builder(
            appContext,
            NotificationConstants.NOTIFICATION_CHANNEL_ID,
        )

    @Provides
    @Singleton
    fun provideNotificationManager(
        @Named("ApplicationContext") appContext: Context,
    ): NotificationManager = appContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    @Provides
    @Singleton
    fun provideNotificationUpdater(
        scope: CoroutineScope,
        preferencesHelper: PreferencesHelper,
        apiCallManager: IApiCallManager,
        localDbInterface: LocalDbInterface,
    ): NotificationRepository = NotificationRepository(scope, preferencesHelper, apiCallManager, localDbInterface)

    @Provides
    @Singleton
    fun providePingTimeDao(windscribeDatabase: WindscribeDatabase): PingTimeDao = windscribeDatabase.pingTimeDao()

    @Provides
    @Singleton
    fun providePopupNotificationDao(windscribeDatabase: WindscribeDatabase): PopupNotificationDao =
        windscribeDatabase.popupNotificationDao()

    @Provides
    @Singleton
    fun provideUnblockWgDao(windscribeDatabase: WindscribeDatabase): UnblockWgDao = windscribeDatabase.unblockWgDao()

    @Provides
    @Singleton
    fun provideDataStore(app: Windscribe): androidx.datastore.core.DataStore<androidx.datastore.preferences.core.Preferences> =
        app.windscribeDataStore

    @Provides
    @Singleton
    fun providePreferenceHelperInterface(
        dataStore: androidx.datastore.core.DataStore<androidx.datastore.preferences.core.Preferences>,
        securePreferences: SecurePreferences,
        scope: CoroutineScope,
    ): PreferencesHelper =
        com.windscribe.vpn.apppreference.DataStorePreferenceHelper(
            dataStore,
            securePreferences,
            scope,
        )

    @Provides
    @Singleton
    fun provideLocationAndDatacentersDao(windscribeDatabase: WindscribeDatabase): LocationAndDatacentersDao =
        windscribeDatabase.locationAndDatacentersDao()

    @Provides
    @Singleton
    fun provideLocationDao(windscribeDatabase: WindscribeDatabase): LocationDao = windscribeDatabase.locationDao()

    @Provides
    @Singleton
    fun provideServerDao(windscribeDatabase: WindscribeDatabase): ServerDao = windscribeDatabase.serverDao()

    @Provides
    @Singleton
    fun provideSelectedLocationUpdater(
        scope: CoroutineScope,
        preferencesHelper: PreferencesHelper,
        localDbInterface: LocalDbInterface,
        userRepository: Lazy<UserRepository>,
        pinger: com.windscribe.vpn.services.ping.Pinger,
    ): LocationRepository =
        LocationRepository(
            scope,
            preferencesHelper,
            localDbInterface,
            userRepository,
            pinger,
        )

    @Provides
    @Singleton
    fun providePinger(): com.windscribe.vpn.services.ping.Pinger =
        com.windscribe.vpn.services.ping
            .IcmpPinger()

    @Provides
    @Singleton
    fun provideServerListUpdater(
        scope: CoroutineScope,
        apiCallManager: IApiCallManager,
        localDbInterface: LocalDbInterface,
        userRepository: Lazy<UserRepository>,
        preferencesHelper: PreferencesHelper,
        favouriteRepository: FavouriteRepository,
    ): ServerListRepository =
        ServerListRepository(
            scope,
            apiCallManager,
            localDbInterface,
            userRepository,
            preferencesHelper,
            favouriteRepository,
        )

    @Provides
    @Singleton
    fun provideServerStatusDao(windscribeDatabase: WindscribeDatabase): ServerStatusDao = windscribeDatabase.serverStatusDao()

    @Provides
    @Singleton
    fun provideStaticListUpdater(
        scope: CoroutineScope,
        preferencesHelper: PreferencesHelper,
        apiCallManager: IApiCallManager,
        localDbInterface: LocalDbInterface,
    ): StaticIpRepository =
        StaticIpRepository(
            scope,
            preferencesHelper,
            apiCallManager,
            localDbInterface,
        )

    @Provides
    @Singleton
    fun provideStaticRegionDao(windscribeDatabase: WindscribeDatabase): StaticRegionDao = windscribeDatabase.staticRegionDao()

    @Provides
    @Singleton
    fun provideTrafficCounter(
        coroutineScope: CoroutineScope,
        vpnConnectionStateManager: VPNConnectionStateManager,
        preferencesHelper: PreferencesHelper,
        deviceStateManager: DeviceStateManager,
    ): TrafficCounter =
        TrafficCounter(
            coroutineScope,
            vpnConnectionStateManager,
            preferencesHelper,
            deviceStateManager,
        )

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
        googleSignInManager: GoogleSignInManager,
        unblockWgParamsRepository: UnblockWgParamsRepository,
    ): UserRepository =
        UserRepository(
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
            googleSignInManager,
            unblockWgParamsRepository,
        )

    @Provides
    @Singleton
    fun provideWgConfigRepository(
        apiManager: IApiCallManager,
        preferencesHelper: PreferencesHelper,
    ): WgConfigRepository = WgConfigRepository(apiManager, preferencesHelper)

    @Provides
    @Singleton
    fun providePurchaseManager(
        scope: CoroutineScope,
        apiManager: IApiCallManager,
        userRepository: UserRepository,
    ): PurchaseManager = PurchaseManager(scope, apiManager, userRepository)

    @Provides
    @Singleton
    fun provideUserStatusDao(windscribeDatabase: WindscribeDatabase): UserStatusDao = windscribeDatabase.userStatusDao()

    @Provides
    @Singleton
    fun provideVPNProfileCreator(
        preferencesHelper: PreferencesHelper,
        wgConfigRepository: WgConfigRepository,
        proxyTunnelManager: ProxyTunnelManager,
        proxyDNSManager: ProxyDNSManager,
        unblockWgParamsRepository: UnblockWgParamsRepository,
    ): VPNProfileCreator =
        VPNProfileCreator(
            preferencesHelper,
            wgConfigRepository,
            proxyTunnelManager,
            proxyDNSManager,
            unblockWgParamsRepository,
        )

    @Provides
    @Singleton
    fun provideVpnBackendHolder(
        coroutineScope: CoroutineScope,
        preferenceHelper: PreferencesHelper,
        openVPNBackend: OpenVPNBackend,
        iKev2VpnBackend: IKev2VpnBackend,
        wireguardBackend: WireguardBackend,
    ): VpnBackendHolder =
        VpnBackendHolder(
            coroutineScope,
            preferenceHelper,
            iKev2VpnBackend,
            wireguardBackend,
            openVPNBackend,
        )

    @Provides
    @Singleton
    fun provideWindNotificationBuilder(
        notificationManager: NotificationManager,
        notificationBuilder: NotificationCompat.Builder,
        vpnConnectionStateManager: VPNConnectionStateManager,
        scope: CoroutineScope,
        trafficCounter: TrafficCounter,
        serverListRepository: ServerListRepository,
        preferencesHelper: PreferencesHelper,
    ): WindNotificationBuilder =
        WindNotificationBuilder(
            notificationManager,
            notificationBuilder,
            vpnConnectionStateManager,
            trafficCounter,
            scope,
            serverListRepository,
            preferencesHelper,
        )

    @Provides
    @Singleton
    fun provideWindNotificationDao(windscribeDatabase: WindscribeDatabase): WindNotificationDao = windscribeDatabase.windNotificationDao()

    @Provides
    @Singleton
    fun providesApiCallManagerInterface(
        wsNetWrapper: WSNetWrapper,
        preferencesHelper: PreferencesHelper,
    ): IApiCallManager = ApiCallManager(wsNetWrapper, preferencesHelper)

    @Provides
    @Singleton
    fun providesIpRepository(
        scope: CoroutineScope,
        preferencesHelper: PreferencesHelper,
        apiCallManager: IApiCallManager,
        vpnConnectionStateManager: VPNConnectionStateManager,
        deviceStateManager: DeviceStateManager,
    ): IpRepository =
        IpRepository(
            scope,
            preferencesHelper,
            apiCallManager,
            vpnConnectionStateManager,
            deviceStateManager.isOnline,
        )

    @Provides
    @Singleton
    fun providesAppLifeCycleObserver(
        workManager: WindScribeWorkManager,
        networkInfoManager: NetworkInfoManager,
        vpnConnectionStateManager: VPNConnectionStateManager,
        proxyDNSManager: ProxyDNSManager,
        wsNetWrapper: WSNetWrapper,
        deviceStateManager: DeviceStateManager,
    ): AppLifeCycleObserver =
        AppLifeCycleObserver(
            workManager,
            networkInfoManager,
            vpnConnectionStateManager,
            proxyDNSManager,
            wsNetWrapper,
            deviceStateManager,
        )

    @Provides
    @Singleton
    @Named("ApplicationContext")
    fun providesApplicationContext(app: Windscribe): Context = app

    @Provides
    fun providesLoggingInterceptor(): HttpLoggingInterceptor = HttpLoggingInterceptor()

    @Provides
    @Singleton
    fun providesMockLocationController(
        app: Windscribe,
        coroutineScope: CoroutineScope,
        vpnConnectionStateManager: VPNConnectionStateManager,
        preferencesHelper: PreferencesHelper,
    ): MockLocationManager =
        MockLocationManager(
            app,
            coroutineScope,
            vpnConnectionStateManager,
            preferencesHelper,
        )

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
        preferencesHelper: PreferencesHelper,
    ): AutoConnectionManager =
        AutoConnectionManager(
            scope,
            vpnConnectionStateManager,
            vpnController,
            networkInfoManager,
            connectionDataRepository,
            localDbInterface,
            apiManager,
            preferencesHelper,
        )

    @Provides
    @Singleton
    fun providesNetworkInfoManager(
        preferencesHelper: PreferencesHelper,
        localDbInterface: LocalDbInterface,
        deviceStateManager: DeviceStateManager,
    ): NetworkInfoManager = NetworkInfoManager(preferencesHelper, localDbInterface, deviceStateManager)

    @Provides
    fun providesOkHttpBuilder(): OkHttpClient.Builder {
        val connectionPool = ConnectionPool(0, 5, TimeUnit.MINUTES)
        val httpLoggingInterceptor = getHttpLoggingInterceptor()
        httpLoggingInterceptor.level = HttpLoggingInterceptor.Level.BODY
        val builder = OkHttpClient.Builder()
        builder.connectTimeout(
            NetworkKeyConstants.NETWORK_REQUEST_CONNECTION_TIMEOUT,
            TimeUnit.SECONDS,
        )
        builder.readTimeout(5, TimeUnit.SECONDS)
        builder.writeTimeout(5, TimeUnit.SECONDS)
        builder.callTimeout(15, TimeUnit.SECONDS)
        builder.retryOnConnectionFailure(false)
        builder.connectionPool(connectionPool)
        return builder
    }

    private fun getHttpLoggingInterceptor(): HttpLoggingInterceptor =
        HttpLoggingInterceptor(
            object : HttpLoggingInterceptor.Logger {
                override fun log(message: String) {
                    logger.info(message)
                }
            },
        )

    @Provides
    @Singleton
    fun providesSecurePreference(app: Windscribe): SecurePreferences = SecurePreferences(app)

    @Provides
    @Singleton
    fun providesVPNConnectionStateManager(
        scope: CoroutineScope,
        autoConnectionManager: AutoConnectionManager,
        preferencesHelper: PreferencesHelper,
        userRepository: Lazy<UserRepository>,
        wsNetWrapper: WSNetWrapper,
    ): VPNConnectionStateManager =
        VPNConnectionStateManager(
            scope,
            autoConnectionManager,
            preferencesHelper,
            userRepository,
            wsNetWrapper,
        )

    @Provides
    @Singleton
    fun providesWindScribeWorkManager(
        app: Windscribe,
        scope: CoroutineScope,
        vpnConnectionStateManager: VPNConnectionStateManager,
        preferencesHelper: PreferencesHelper,
        checkUpdateRepository: CheckUpdateRepository,
        wsNetWrapper: WSNetWrapper,
        deviceStateManager: DeviceStateManager,
    ): WindScribeWorkManager =
        WindScribeWorkManager(
            app,
            scope,
            vpnConnectionStateManager,
            preferencesHelper,
            checkUpdateRepository,
            wsNetWrapper,
            deviceStateManager,
        )

    @Provides
    @Singleton
    fun providesDecoyTrafficController(
        scope: CoroutineScope,
        apiCallManager: IApiCallManager,
        preferencesHelper: PreferencesHelper,
        vpnConnectionStateManager: VPNConnectionStateManager,
    ): DecoyTrafficController =
        DecoyTrafficController(
            scope,
            apiCallManager,
            preferencesHelper,
            vpnConnectionStateManager,
        )

    @Provides
    @Singleton
    fun providesWsTunnelManager(
        scope: CoroutineScope,
        openVPNBackend: OpenVPNBackend,
    ): ProxyTunnelManager = ProxyTunnelManager(scope, openVPNBackend)

    @Provides
    @Singleton
    fun providesShortcutStateManager(
        scope: CoroutineScope,
        userRepository: Lazy<UserRepository>,
        networkInfoManager: NetworkInfoManager,
        autoConnectionManager: AutoConnectionManager,
        preferencesHelper: PreferencesHelper,
        vpnController: WindVpnController,
    ): ShortcutStateManager =
        ShortcutStateManager(
            scope,
            userRepository,
            autoConnectionManager,
            networkInfoManager,
            preferencesHelper,
            vpnController,
        )

    @Provides
    @Singleton
    fun providesEmergencyConnectRepository(wsNetWrapper: WSNetWrapper): EmergencyConnectRepository =
        EmergencyConnectRepositoryImpl(wsNetWrapper)

    @Provides
    @Singleton
    fun providesAdvanceParameterRepository(
        scope: CoroutineScope,
        preferencesHelper: PreferencesHelper,
        wsNetWrapper: WSNetWrapper,
    ): AdvanceParameterRepository = AdvanceParameterRepositoryImpl(scope, preferencesHelper, wsNetWrapper)

    @Provides
    @Singleton
    fun providesWSNetWrapper(): WSNetWrapper = WSNetWrapper()

    @Provides
    @Singleton
    fun providesDynamicShortcutManager(
        app: Windscribe,
        scope: CoroutineScope,
        vpnConnectionStateManager: VPNConnectionStateManager,
        locationRepository: LocationRepository,
        localDbInterface: LocalDbInterface,
        serverListRepository: ServerListRepository,
    ): DynamicShortcutManager =
        DynamicShortcutManager(
            app,
            scope,
            vpnConnectionStateManager,
            locationRepository,
            localDbInterface,
            serverListRepository,
        )

    @Provides
    @Singleton
    fun providesReviewManager(
        app: Windscribe,
        scope: CoroutineScope,
        preferencesHelper: PreferencesHelper,
        userRepository: UserRepository,
    ): WindscribeReviewManager = WindscribeReviewManagerImpl(scope, app, preferencesHelper, userRepository)

    @Provides
    @Singleton
    fun provideWgLogger(): WgLogger = WgLogger()

    @Provides
    @Singleton
    fun providePortMapRepository(
        apiCallManager: IApiCallManager,
        preferencesHelper: PreferencesHelper,
    ): com.windscribe.vpn.repository.PortMapRepository =
        com.windscribe.vpn.repository
            .PortMapRepository(apiCallManager, preferencesHelper)

    @Provides
    @Singleton
    fun provideLogRepository(
        preferencesHelper: PreferencesHelper,
        apiCallManager: IApiCallManager,
        playIntegrityManager: PlayIntegrityManager,
    ): LogRepository = LogRepository(preferencesHelper, apiCallManager, playIntegrityManager)
}
