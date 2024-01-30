package com.windscribe.vpn.di

import android.app.AlarmManager
import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.windscribe.vpn.ServiceInteractor
import com.windscribe.vpn.ServiceInteractorImpl
import com.windscribe.vpn.Windscribe
import com.windscribe.vpn.api.ApiCallManager
import com.windscribe.vpn.api.AuthorizationGenerator
import com.windscribe.vpn.api.DomainFailOverManager
import com.windscribe.vpn.api.EchApiFactory
import com.windscribe.vpn.api.HashDomainGenerator
import com.windscribe.vpn.api.HostType
import com.windscribe.vpn.api.IApiCallManager
import com.windscribe.vpn.api.ProtectedApiFactory
import com.windscribe.vpn.api.WindApiFactory
import com.windscribe.vpn.api.WindCustomApiFactory
import com.windscribe.vpn.api.WindscribeDnsResolver
import com.windscribe.vpn.apppreference.AppPreferenceHelper
import com.windscribe.vpn.apppreference.PreferencesHelper
import com.windscribe.vpn.apppreference.SecurePreferences
import com.windscribe.vpn.autoconnection.AutoConnectionManager
import com.windscribe.vpn.backend.TrafficCounter
import com.windscribe.vpn.backend.VpnBackendHolder
import com.windscribe.vpn.backend.ikev2.IKev2VpnBackend
import com.windscribe.vpn.backend.openvpn.OpenVPNBackend
import com.windscribe.vpn.backend.openvpn.ProxyTunnelManager
import com.windscribe.vpn.backend.utils.VPNProfileCreator
import com.windscribe.vpn.backend.utils.WindNotificationBuilder
import com.windscribe.vpn.backend.utils.WindVpnController
import com.windscribe.vpn.backend.wireguard.WireguardBackend
import com.windscribe.vpn.backend.wireguard.WireguardContextWrapper
import com.windscribe.vpn.constants.NetworkKeyConstants
import com.windscribe.vpn.constants.NotificationConstants
import com.windscribe.vpn.constants.PreferencesKeyConstants
import com.windscribe.vpn.decoytraffic.DecoyTrafficController
import com.windscribe.vpn.localdatabase.LocalDatabaseImpl
import com.windscribe.vpn.localdatabase.LocalDbInterface
import com.windscribe.vpn.localdatabase.Migrations
import com.windscribe.vpn.localdatabase.NetworkInfoDao
import com.windscribe.vpn.localdatabase.PingTestDao
import com.windscribe.vpn.localdatabase.PopupNotificationDao
import com.windscribe.vpn.localdatabase.ServerStatusDao
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
import com.windscribe.vpn.repository.UserRepository
import com.windscribe.vpn.repository.WgConfigRepository
import com.windscribe.vpn.serverlist.dao.CityAndRegionDao
import com.windscribe.vpn.serverlist.dao.CityDao
import com.windscribe.vpn.serverlist.dao.CityDetailDao
import com.windscribe.vpn.serverlist.dao.ConfigFileDao
import com.windscribe.vpn.serverlist.dao.FavouriteDao
import com.windscribe.vpn.serverlist.dao.PingTimeDao
import com.windscribe.vpn.serverlist.dao.RegionAndCitiesDao
import com.windscribe.vpn.serverlist.dao.RegionDao
import com.windscribe.vpn.serverlist.dao.StaticRegionDao
import com.windscribe.vpn.state.AppLifeCycleObserver
import com.windscribe.vpn.state.DeviceStateManager
import com.windscribe.vpn.state.NetworkInfoManager
import com.windscribe.vpn.state.PreferenceChangeObserver
import com.windscribe.vpn.state.ShortcutStateManager
import com.windscribe.vpn.state.VPNConnectionStateManager
import com.windscribe.vpn.workers.WindScribeWorkManager
import com.wireguard.android.backend.GoBackend
import dagger.Lazy
import dagger.Module
import dagger.Provides
import kotlinx.coroutines.CoroutineScope
import net.grandcentrix.tray.AppPreferences
import okhttp3.ConnectionPool
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import org.slf4j.LoggerFactory
import retrofit2.Retrofit
import java.util.concurrent.TimeUnit
import javax.inject.Named
import javax.inject.Singleton

@Module
open class BaseApplicationModule {
    private val logger = LoggerFactory.getLogger("di_")

    open lateinit var windscribeApp: Windscribe

    @Provides
    @Singleton
    @Named("accessIpList")
    fun provideAccessIps(preferencesHelper: PreferencesHelper): List<String> {
        val accessIpList: MutableList<String> = ArrayList()
        val accessIp1 = preferencesHelper.getAccessIp(PreferencesKeyConstants.ACCESS_API_IP_1)
        val accessIp2 = preferencesHelper.getAccessIp(PreferencesKeyConstants.ACCESS_API_IP_2)
        if (accessIp1 != null && accessIp2 != null) {
            accessIpList.add(accessIp1)
            accessIpList.add(accessIp2)
        }
        return accessIpList
    }

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
    fun provideAuthGenerator(preferencesHelper: PreferencesHelper): AuthorizationGenerator {
        return AuthorizationGenerator(preferencesHelper)
    }

    @Provides
    @Named("backupEndPoint")
    fun provideBackupEndpoint(): String {
        return HashDomainGenerator.create(NetworkKeyConstants.API_HOST_GENERIC).random()
    }

    @Provides
    @Named("backupEndPointListForIp")
    fun provideBackupEndpointForIp(): List<String> {
        return HashDomainGenerator.create(NetworkKeyConstants.API_HOST_CHECK_IP)
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
    fun provideCityDetailDao(windscribeDatabase: WindscribeDatabase): CityDetailDao {
        return windscribeDatabase.cityDetailDao()
    }

    @Provides
    @Singleton
    fun provideConfigFileDao(windscribeDatabase: WindscribeDatabase): ConfigFileDao {
        return windscribeDatabase.configFileDao()
    }

    @Provides
    @Singleton
    fun provideConnectionDataUpdater(
            preferencesHelper: PreferencesHelper, apiCallManager: IApiCallManager
    ): ConnectionDataRepository {
        return ConnectionDataRepository(preferencesHelper, apiCallManager)
    }

    @Provides
    @Singleton
    fun provideLatencyRepository(
            preferencesHelper: PreferencesHelper,
            localDbInterface: LocalDbInterface,
            apiCallManager: IApiCallManager,
            vpnConnectionStateManager: Lazy<VPNConnectionStateManager>,
    ): LatencyRepository {
        return LatencyRepository(
                preferencesHelper,
                localDbInterface,
                apiCallManager,
                vpnConnectionStateManager
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
                .addMigrations(Migrations.migration_33_34).build()

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
    fun provideIkev2Backend(
            coroutineScope: CoroutineScope,
            networkInfoManager: NetworkInfoManager,
            vpnConnectionStateManager: VPNConnectionStateManager,
            serviceInteractor: ServiceInteractor,
            advanceParameterRepository: AdvanceParameterRepository
    ): IKev2VpnBackend {
        return IKev2VpnBackend(coroutineScope, networkInfoManager, vpnConnectionStateManager, serviceInteractor, advanceParameterRepository)
    }

    @Provides
    @Singleton
    fun provideLocalDatabaseImpl(
            pingTestDao: PingTestDao,
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
            windNotificationDao: WindNotificationDao
    ): LocalDbInterface {
        return LocalDatabaseImpl(
                pingTestDao,
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
                windNotificationDao
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
            preferencesHelper: PreferencesHelper,
            apiCallManager: IApiCallManager,
            localDbInterface: LocalDbInterface
    ): NotificationRepository {
        return NotificationRepository(preferencesHelper, apiCallManager, localDbInterface)
    }

    @Provides
    @Singleton
    fun provideOpenVPNBackend(
            goBackend: GoBackend,
            coroutineScope: CoroutineScope,
            networkInfoManager: NetworkInfoManager,
            vpnConnectionStateManager: VPNConnectionStateManager,
            serviceInteractor: ServiceInteractor,
            advanceParameterRepository: AdvanceParameterRepository
    ): OpenVPNBackend {
        return OpenVPNBackend(
                goBackend,
                coroutineScope,
                networkInfoManager,
                vpnConnectionStateManager,
                serviceInteractor,
                advanceParameterRepository)
    }

    @Provides
    @Singleton
    fun providePingTestDao(windscribeDatabase: WindscribeDatabase): PingTestDao {
        return windscribeDatabase.pingTestDao()
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
    fun providePreferenceHelperInterface(
            preferences: AppPreferences, securePreferences: SecurePreferences
    ): PreferencesHelper {
        return AppPreferenceHelper(preferences, securePreferences)
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
            userRepository: Lazy<UserRepository>
    ): LocationRepository {
        return LocationRepository(scope, preferencesHelper, localDbInterface, userRepository)
    }

    @Provides
    @Singleton
    fun provideServerListUpdater(
            scope: CoroutineScope,
            apiCallManager: IApiCallManager,
            localDbInterface: LocalDbInterface,
            preferenceChangeObserver: PreferenceChangeObserver,
            userRepository: UserRepository,
            appLifeCycleObserver: AppLifeCycleObserver,
            advanceParameterRepository: AdvanceParameterRepository
    ): ServerListRepository {
        return ServerListRepository(
                scope,
                apiCallManager,
                localDbInterface,
                preferenceChangeObserver,
                userRepository,
                appLifeCycleObserver,
                advanceParameterRepository
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
            serviceInteractor: ServiceInteractor,
            vpnController: WindVpnController
    ): UserRepository {
        return UserRepository(scope, serviceInteractor, vpnController, autoConnectionManager)
    }

    @Provides
    @Singleton
    fun provideWgConfigRepository(
            scope: CoroutineScope, serviceInteractor: ServiceInteractor
    ): WgConfigRepository {
        return WgConfigRepository(scope, serviceInteractor)
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
            proxyTunnelManager: ProxyTunnelManager
    ): VPNProfileCreator {
        return VPNProfileCreator(preferencesHelper, wgConfigRepository, proxyTunnelManager)
    }

    @Provides
    @Singleton
    fun provideVpnBackendHolder(
            coroutineScope: CoroutineScope,
            preferenceHelper: AppPreferenceHelper,
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
            interactor: ServiceInteractor
    ): WindNotificationBuilder {
        return WindNotificationBuilder(
                notificationManager,
                notificationBuilder,
                vpnConnectionStateManager,
                trafficCounter,
                scope,
                interactor
        )
    }

    @Provides
    @Singleton
    fun provideWindNotificationDao(windscribeDatabase: WindscribeDatabase): WindNotificationDao {
        return windscribeDatabase.windNotificationDao()
    }

    @Provides
    @Singleton
    fun provideWireguardBackend(
            goBackend: GoBackend,
            coroutineScope: CoroutineScope,
            networkInfoManager: NetworkInfoManager,
            vpnConnectionStateManager: VPNConnectionStateManager,
            serviceInteractor: ServiceInteractor,
            vpnProfileCreator: VPNProfileCreator,
            userRepository: Lazy<UserRepository>,
            deviceStateManager: DeviceStateManager,
            preferencesHelper: PreferencesHelper,
            advanceParameterRepository: AdvanceParameterRepository
    ): WireguardBackend {
        return WireguardBackend(
                goBackend,
                coroutineScope,
                networkInfoManager,
                vpnConnectionStateManager,
                serviceInteractor,
                vpnProfileCreator,
                userRepository,
                deviceStateManager,
                preferencesHelper,
                advanceParameterRepository)
    }

    @Provides
    @Singleton
    fun providesApiCallManagerInterface(
            windApiFactory: WindApiFactory,
            windCustomApiFactory: WindCustomApiFactory,
            @Named("backupEndPoint") backupEndpoint: String,
            authorizationGenerator: AuthorizationGenerator,
            @Named("accessIpList") accessIpList: List<String>,
            @Named("PrimaryApiEndpointMap") primaryApiEndpointMap: Map<HostType, String>,
            @Named("SecondaryApiEndpointMap") secondaryApiEndpointMap: Map<HostType, String>,
            domainFailOverManager: DomainFailOverManager
    ): IApiCallManager {
        return ApiCallManager(
                windApiFactory,
                windCustomApiFactory,
                backupEndpoint,
                authorizationGenerator,
                accessIpList,
                primaryApiEndpointMap,
                secondaryApiEndpointMap,
                domainFailOverManager
        )
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
            domainFailOverManager: DomainFailOverManager
    ): AppLifeCycleObserver {
        return AppLifeCycleObserver(workManager, networkInfoManager, domainFailOverManager)
    }

    @Provides
    @Singleton
    fun providesAppPreference(): AppPreferences {
        return AppPreferences(windscribeApp)
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
            interactor: ServiceInteractor,
            scope: CoroutineScope,
            connectionDataRepository: ConnectionDataRepository
    ): AutoConnectionManager {
        return AutoConnectionManager(
                scope,
                vpnConnectionStateManager,
                vpnController,
                networkInfoManager,
                interactor,
                connectionDataRepository
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
    @Singleton
    fun providesDomainFailOverManager(
            preferencesHelper: PreferencesHelper
    ): DomainFailOverManager {
        return DomainFailOverManager(preferencesHelper)
    }

    @Provides
    fun providesOkHttpBuilder(windscribeDnsResolver: WindscribeDnsResolver, advanceParameterRepository: AdvanceParameterRepository): OkHttpClient.Builder {
        val connectionPool = ConnectionPool(0, 5, TimeUnit.MINUTES)
        val httpLoggingInterceptor = getHttpLoggingInterceptor()
        httpLoggingInterceptor.level = HttpLoggingInterceptor.Level.BODY
        val builder = OkHttpClient.Builder()
        builder.connectTimeout(NetworkKeyConstants.NETWORK_REQUEST_CONNECTION_TIMEOUT, TimeUnit.SECONDS)
        builder.readTimeout(5, TimeUnit.SECONDS)
        builder.writeTimeout(5, TimeUnit.SECONDS)
        builder.callTimeout(15, TimeUnit.SECONDS)
        builder.retryOnConnectionFailure(false)
        builder.connectionPool(connectionPool)
        //.addInterceptor()
        builder.dns(windscribeDnsResolver)
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
    fun providesRetrofitBuilder(): Retrofit.Builder {
        return Retrofit.Builder()
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
            userRepository: Lazy<UserRepository>
    ): VPNConnectionStateManager {
        return VPNConnectionStateManager(
                scope, autoConnectionManager, preferencesHelper, userRepository
        )
    }

    @Provides
    @Singleton
    fun providesVPNServiceInteractor(
            mPrefHelper: PreferencesHelper,
            apiCallManager: IApiCallManager,
            localDbInterface: LocalDbInterface
    ): ServiceInteractor {
        return ServiceInteractorImpl(mPrefHelper, apiCallManager, localDbInterface)
    }

    @Provides
    @Singleton
    fun providesCustomDnsResolver(
            mainScope: CoroutineScope, preferencesHelper: PreferencesHelper
    ): WindscribeDnsResolver {
        return WindscribeDnsResolver(mainScope, preferencesHelper)
    }

    @Provides
    @Singleton
    fun providesWindApiFactory(
            retrofitBuilder: Retrofit.Builder,
            httpBuilder: OkHttpClient.Builder,
            protectedApiFactory: ProtectedApiFactory,
            dnsResolver: WindscribeDnsResolver
    ): WindApiFactory {
        return WindApiFactory(
                retrofitBuilder, httpBuilder, dnsResolver, protectedApiFactory,
        )
    }

    @Provides
    @Singleton
    fun providesEchFactory(
            retrofitBuilder: Retrofit.Builder,
            httpBuilder: OkHttpClient.Builder,
            protectedApiFactory: ProtectedApiFactory
    ): EchApiFactory {
        return EchApiFactory(
                retrofitBuilder, httpBuilder, protectedApiFactory
        )
    }

    @Provides
    @Singleton
    fun providesProtectedFactory(
            retrofitBuilder: Retrofit.Builder, okHttpClient: OkHttpClient.Builder
    ): ProtectedApiFactory {
        return ProtectedApiFactory(retrofitBuilder, okHttpClient)
    }

    @Provides
    @Singleton
    fun providesWindCustomApiFactory(
            retrofitBuilder: Retrofit.Builder, okHttpClient: OkHttpClient.Builder
    ): WindCustomApiFactory {
        return WindCustomApiFactory(retrofitBuilder, okHttpClient)
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
            interactor: ServiceInteractor,
            vpnController: WindVpnController
    ): ShortcutStateManager {
        return ShortcutStateManager(
                scope,
                userRepository,
                autoConnectionManager,
                networkInfoManager,
                interactor,
                vpnController
        )
    }

    @Provides
    @Singleton
    fun providesEmergencyConnectRepository(): EmergencyConnectRepository {
        return EmergencyConnectRepositoryImpl()
    }

    @Provides
    @Singleton
    fun providesAdvanceParameterRepository(scope: CoroutineScope, preferencesHelper: PreferencesHelper): AdvanceParameterRepository {
        return AdvanceParameterRepositoryImpl(scope, preferencesHelper)
    }
}