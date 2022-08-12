/*
 * Copyright (c) 2021 Windscribe Limited.
 */
package com.windscribe.vpn.di

import android.app.AlarmManager
import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.windscribe.vpn.BuildConfig
import com.windscribe.vpn.ServiceInteractor
import com.windscribe.vpn.ServiceInteractorImpl
import com.windscribe.vpn.Windscribe
import com.windscribe.vpn.Windscribe.Companion.applicationScope
import com.windscribe.vpn.api.*
import com.windscribe.vpn.api.HashDomainGenerator.create
import com.windscribe.vpn.apppreference.AppPreferenceHelper
import com.windscribe.vpn.apppreference.PreferencesHelper
import com.windscribe.vpn.apppreference.SecurePreferences
import com.windscribe.vpn.backend.TrafficCounter
import com.windscribe.vpn.backend.VpnBackendHolder
import com.windscribe.vpn.backend.ikev2.IKev2VpnBackend
import com.windscribe.vpn.backend.openvpn.OpenVPNBackend
import com.windscribe.vpn.backend.utils.ProtocolManager
import com.windscribe.vpn.backend.utils.VPNProfileCreator
import com.windscribe.vpn.backend.utils.WindNotificationBuilder
import com.windscribe.vpn.backend.utils.WindVpnController
import com.windscribe.vpn.backend.wireguard.WireguardBackend
import com.windscribe.vpn.backend.wireguard.WireguardContextWrapper
import com.windscribe.vpn.constants.NetworkKeyConstants
import com.windscribe.vpn.constants.NotificationConstants
import com.windscribe.vpn.constants.PreferencesKeyConstants
import com.windscribe.vpn.decoytraffic.DecoyTrafficController
import com.windscribe.vpn.localdatabase.*
import com.windscribe.vpn.mocklocation.MockLocationManager
import com.windscribe.vpn.repository.*
import com.windscribe.vpn.serverlist.dao.*
import com.windscribe.vpn.state.*
import com.windscribe.vpn.workers.WindScribeWorkManager
import com.wireguard.android.backend.GoBackend
import dagger.Lazy
import dagger.Module
import dagger.Provides
import kotlinx.coroutines.CoroutineScope
import net.grandcentrix.tray.AppPreferences
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import org.slf4j.LoggerFactory
import retrofit2.Retrofit
import javax.inject.Named
import javax.inject.Singleton

/**
 * Application module provides production dependencies
 * In future plan is break this module in to smaller modules
 * to ease swapping of modules for testing.
 * */
@Module
class ApplicationModule(private val windscribeApp: Windscribe) {
    private val logger = LoggerFactory.getLogger("di_")

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
    fun provideAuthGenerator(preferencesHelper: PreferencesHelper): AuthorizationGenerator {
        return AuthorizationGenerator(preferencesHelper)
    }

    @Provides
    @Named("backupEndPointList")
    fun provideBackupEndpoint(): List<String> {
        return create(NetworkKeyConstants.API_HOST_GENERIC)
    }

    @Provides
    @Named("backupEndPointListForIp")
    fun provideBackupEndpointForIp(): List<String> {
        return create(NetworkKeyConstants.API_HOST_CHECK_IP)
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
            preferencesHelper: PreferencesHelper,
            apiCallManager: IApiCallManager,
            protocolManager: ProtocolManager
    ): ConnectionDataRepository {
        return ConnectionDataRepository(preferencesHelper, apiCallManager, protocolManager)
    }

    @Provides
    @Singleton
    fun provideCoroutineScope(): CoroutineScope {
        return applicationScope
    }

    @Provides
    @Singleton
    fun provideDatabase(): WindscribeDatabase {
        return Room.databaseBuilder(windscribeApp, WindscribeDatabase::class.java, "wind_db")
                .fallbackToDestructiveMigration()
                .addCallback(object : RoomDatabase.Callback() {
                    override fun onDestructiveMigration(db: SupportSQLiteDatabase) {
                        logger.debug("No migration found for old database. Reconstructing from scratch.")
                        super.onDestructiveMigration(db)
                    }
                })
                .addMigrations(Migrations.migration_26_27)
                .addMigrations(Migrations.migration_27_28)
                .addMigrations(Migrations.migration_29_31)
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
    fun provideIkev2Backend(
            coroutineScope: CoroutineScope,
            networkInfoManager: NetworkInfoManager,
            vpnConnectionStateManager: VPNConnectionStateManager,
            serviceInteractor: ServiceInteractor,
            protocolManager: ProtocolManager,
    ): IKev2VpnBackend {
        return IKev2VpnBackend(
                coroutineScope,
                networkInfoManager,
                vpnConnectionStateManager,
                serviceInteractor,
                protocolManager
        )
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
                appContext,
                NotificationConstants.NOTIFICATION_CHANNEL_ID
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
            trafficCounter: TrafficCounter,
            networkInfoManager: NetworkInfoManager,
            vpnConnectionStateManager: VPNConnectionStateManager,
            serviceInteractor: ServiceInteractor,
            protocolManager: ProtocolManager,
    ): OpenVPNBackend {
        return OpenVPNBackend(
                goBackend, coroutineScope, trafficCounter, networkInfoManager,
                vpnConnectionStateManager,
                serviceInteractor, protocolManager
        )
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
            preferences: AppPreferences,
            securePreferences: SecurePreferences
    ): PreferencesHelper {
        return AppPreferenceHelper(preferences, securePreferences)
    }

    @Provides
    @Singleton
    fun provideProtocolManager(
            coroutineScope: CoroutineScope,
            networkInfoManager: NetworkInfoManager,
            serviceInteractor: ServiceInteractor
    ): ProtocolManager {
        return ProtocolManager(networkInfoManager, coroutineScope, serviceInteractor)
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
            localDbInterface: LocalDbInterface,userRepository: Lazy<UserRepository>
    ): LocationRepository {
        return LocationRepository(scope, preferencesHelper, localDbInterface,userRepository)
    }

    @Provides
    @Singleton
    fun provideServerListUpdater(
            scope: CoroutineScope,
            preferencesHelper: PreferencesHelper,
            apiCallManager: IApiCallManager,
            localDbInterface: LocalDbInterface,
            preferenceChangeObserver: PreferenceChangeObserver,
            userRepository: UserRepository,
            appLifeCycleObserver: AppLifeCycleObserver
    ): ServerListRepository {
        return ServerListRepository(
                scope,
                preferencesHelper,
                apiCallManager,
                localDbInterface,
                preferenceChangeObserver, userRepository,
                appLifeCycleObserver
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
        return StaticIpRepository(scope,
                preferencesHelper,
                apiCallManager,
                localDbInterface
        )
    }

    @Provides
    @Singleton
    fun provideStaticRegionDao(windscribeDatabase: WindscribeDatabase): StaticRegionDao {
        return windscribeDatabase.staticRegionDao()
    }

    @Provides
    @Singleton
    fun provideTrafficCounter(coroutineScope: CoroutineScope): TrafficCounter {
        return TrafficCounter(coroutineScope)
    }

    @Provides
    @Singleton
    fun provideUserRepository(
            scope: CoroutineScope,
            protocolManager: ProtocolManager,
            serviceInteractor: ServiceInteractor,
            vpnController: WindVpnController
    ): UserRepository {
        return UserRepository(scope, serviceInteractor, vpnController, protocolManager)
    }

    @Provides
    @Singleton
    fun provideWgConfigRepository(scope: CoroutineScope, serviceInteractor: ServiceInteractor): WgConfigRepository {
        return WgConfigRepository(scope, serviceInteractor)
    }

    @Provides
    @Singleton
    fun provideUserStatusDao(windscribeDatabase: WindscribeDatabase): UserStatusDao {
        return windscribeDatabase.userStatusDao()
    }

    @Provides
    @Singleton
    fun provideVPNProfileCreator(preferencesHelper: PreferencesHelper, wgConfigRepository: WgConfigRepository): VPNProfileCreator {
        return VPNProfileCreator(preferencesHelper, wgConfigRepository)
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
                coroutineScope, preferenceHelper, iKev2VpnBackend, wireguardBackend,
                openVPNBackend
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
                notificationManager, notificationBuilder, vpnConnectionStateManager,
                trafficCounter, scope, interactor
        )
    }

    @Provides
    @Singleton
    fun provideWindNotificationDao(windscribeDatabase: WindscribeDatabase): WindNotificationDao {
        return windscribeDatabase.windNotificationDao()
    }

    @Provides
    @Singleton
    fun provideWindVpnController(
            coroutineScope: CoroutineScope,
            serviceInteractor: ServiceInteractor,
            vpnProfileCreator: VPNProfileCreator,
            protocolManager: ProtocolManager,
            VPNConnectionStateManager: VPNConnectionStateManager,
            vpnBackendHolder: VpnBackendHolder,
            locationRepository: LocationRepository,
            wgConfigRepository: WgConfigRepository,
            userRepository: Lazy<UserRepository>
    ): WindVpnController {
        return WindVpnController(
                coroutineScope,
                serviceInteractor,
                vpnProfileCreator,
                VPNConnectionStateManager,
                vpnBackendHolder,
                locationRepository,
                protocolManager, wgConfigRepository,
                userRepository
        )
    }

    @Provides
    @Singleton
    fun provideWireguardBackend(
            trafficCounter: TrafficCounter,
            goBackend: GoBackend,
            coroutineScope: CoroutineScope,
            networkInfoManager: NetworkInfoManager,
            vpnConnectionStateManager: VPNConnectionStateManager,
            serviceInteractor: ServiceInteractor,
            protocolManager: ProtocolManager,
            vpnProfileCreator: VPNProfileCreator,
            userRepository: Lazy<UserRepository>,
            deviceStateManager: DeviceStateManager
    ): WireguardBackend {
        return WireguardBackend(
                trafficCounter, goBackend, coroutineScope, networkInfoManager,
                vpnConnectionStateManager,
                serviceInteractor, protocolManager,
                vpnProfileCreator,
                userRepository,
                deviceStateManager
        )
    }

    @Provides
    @Singleton
    fun providesApiCallManagerInterface(
        windApiFactory: WindApiFactory,
        windCustomApiFactory: WindCustomApiFactory,
        @Named("backupEndPointList") backupEndpoint: List<String>,
        authorizationGenerator: AuthorizationGenerator,
        @Named("accessIpList") accessIpList: List<String>,
        @Named("PrimaryApiEndpointMap") primaryApiEndpointMap : Map<HostType, String>,
        @Named("SecondaryApiEndpointMap") secondaryApiEndpointMap : Map<HostType, String>,
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
    @Named("SecondaryApiEndpointMap")
    fun providesSecondaryApiEndpointMap(): Map<HostType, String> {
        return mapOf(Pair(HostType.API, "https://api.totallyacdn.com"),
                Pair(HostType.ASSET, "https://assets.totallyacdn.com"),
                Pair(HostType.CHECK_IP, "https://checkip.totallyacdn.com"))
    }

    @Provides
    @Named("PrimaryApiEndpointMap")
    fun providesPrimaryApiEndpointMap(): Map<HostType, String> {
        return mapOf(Pair(HostType.API, NetworkKeyConstants.API_ENDPOINT),
                Pair(HostType.ASSET, NetworkKeyConstants.API_ENDPOINT_FOR_SERVER_LIST),
                Pair(HostType.CHECK_IP, BuildConfig.CHECK_IP_URL))
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
                windscribeApp,
                coroutineScope,
                vpnConnectionStateManager,
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
    @Singleton
    fun providesDomainFailOverManager(
        preferencesHelper: PreferencesHelper
    ): DomainFailOverManager {
        return DomainFailOverManager(preferencesHelper)
    }

    @Provides
    fun providesOkHttpBuilder(): OkHttpClient.Builder {
        return OkHttpClient.Builder()
            .addNetworkInterceptor {
                val url = it.request().url
                logger.debug("Request: $url")
                it.proceed(it.request())
            }
    }

    @Provides
    @Singleton
    fun providesPreferenceHelper(
            mPreference: AppPreferences,
            securePreferences: SecurePreferences
    ): AppPreferenceHelper {
        return AppPreferenceHelper(mPreference, securePreferences)
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
    fun providesVPNConnectionStateManager(scope: CoroutineScope, connectionDataRepository: ConnectionDataRepository, preferencesHelper: PreferencesHelper, userRepository: Lazy<UserRepository>): VPNConnectionStateManager {
        return VPNConnectionStateManager(scope, connectionDataRepository,preferencesHelper,userRepository)
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
            mainScope: CoroutineScope,
            preferencesHelper: PreferencesHelper
    ): WindscribeDnsResolver {
        return WindscribeDnsResolver(mainScope, preferencesHelper)
    }

    @Provides
    @Singleton
    fun providesWindApiFactory(
            retrofitBuilder: Retrofit.Builder,
            httpBuilder: OkHttpClient.Builder,
            protectedApiFactory: ProtectedApiFactory,
            windscribeDnsResolver: WindscribeDnsResolver
    ): WindApiFactory {
        return WindApiFactory(retrofitBuilder, httpBuilder, protectedApiFactory, windscribeDnsResolver)
    }

    @Provides
    @Singleton
    fun providesProtectedFactory(
            retrofitBuilder: Retrofit.Builder,
            windscribeDnsResolver: WindscribeDnsResolver
    ): ProtectedApiFactory {
        return ProtectedApiFactory(retrofitBuilder, windscribeDnsResolver)
    }

    @Provides
    @Singleton
    fun providesWindCustomApiFactory(
            retrofitBuilder: Retrofit.Builder,
            windscribeDnsResolver: WindscribeDnsResolver
    ): WindCustomApiFactory {
        return WindCustomApiFactory(retrofitBuilder, windscribeDnsResolver)
    }

    @Provides
    @Singleton
    fun providesWindScribeWorkManager(scope: CoroutineScope, vpnConnectionStateManager: VPNConnectionStateManager, preferencesHelper: PreferencesHelper): WindScribeWorkManager {
        return WindScribeWorkManager(windscribeApp, scope, vpnConnectionStateManager, preferencesHelper)
    }

    @Provides
    @Singleton
    fun providesDecoyTrafficController(scope: CoroutineScope, apiCallManager: IApiCallManager, preferencesHelper: PreferencesHelper, vpnConnectionStateManager: VPNConnectionStateManager): DecoyTrafficController {
        return DecoyTrafficController(scope, apiCallManager, preferencesHelper,vpnConnectionStateManager)
    }
}
